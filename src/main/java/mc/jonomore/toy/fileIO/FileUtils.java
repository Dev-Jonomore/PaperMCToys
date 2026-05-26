package mc.jonomore.toy.fileIO;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for file and archive operations in a Minecraft plugin context.
 * All methods are static; this class cannot be instantiated or subclassed.
 */
public final class FileUtils {

  private FileUtils() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * Locates a zip file by exact filename and verifies its integrity.
   *
   * @param directory the directory to search in
   * @param fileName  exact filename, with or without ".zip"
   * @return the verified {@link Path} to the zip file
   * @throws IOException if not found, unreadable, or corrupt
   */
  public static Path findAndVerifyZip(Path directory, String fileName) throws IOException {
    Objects.requireNonNull(directory, "Directory cannot be null");
    Objects.requireNonNull(fileName, "File name cannot be null");

    if (!Files.isDirectory(directory)) {
      throw new IOException("Not a valid directory: " + directory);
    }

    String name = fileName.endsWith(".zip") ? fileName : fileName + ".zip";
    return verifyZip(directory.resolve(name));
  }

  /**
   * Locates the first zip file in a directory whose filename starts with the given seed
   * and verifies its integrity. Intended for seed-based world pool lookups.
   *
   * @param directory the directory to search in
   * @param seed      the world seed used as a filename prefix
   * @return the verified {@link Path} to the matching zip file
   * @throws IOException if no match is found, the file is unreadable, or the archive is corrupt
   */
  public static Path findAndVerifyZip(Path directory, long seed) throws IOException {
    Objects.requireNonNull(directory, "Directory cannot be null");

    if (!Files.isDirectory(directory)) {
      throw new IOException("Not a valid directory: " + directory);
    }

    String prefix = String.valueOf(seed);

    try (Stream<Path> files = Files.list(directory)) {
      Path match = files
          .filter(Files::isRegularFile)
          .filter(p -> {
            String name = p.getFileName().toString();
            return name.startsWith(prefix) && name.endsWith(".zip");
          })
          .findFirst()
          .orElseThrow(() -> new IOException(
              "No zip found for seed %d in: %s".formatted(seed, directory)
          ));

      return verifyZip(match);
    }
  }

  /**
   * Shared verification logic — confirms the file is readable and a valid non-empty zip.
   */
  private static Path verifyZip(Path zipPath) throws IOException {
    if (!Files.isRegularFile(zipPath)) {
      throw new IOException("Zip file not found or is not a regular file: " + zipPath);
    }
    if (!Files.isReadable(zipPath)) {
      throw new IOException("Zip file is not readable: " + zipPath);
    }

    try (ZipFile zf = new ZipFile(zipPath.toFile())) {
      if (!zf.entries().hasMoreElements()) {
        throw new IOException("Zip file is empty: " + zipPath);
      }
    } catch (IOException e) {
      throw new IOException("Zip file is corrupt or invalid: " + zipPath, e);
    }

    return zipPath;
  }

  /**
   * Extracts a zip archive into a target directory.
   * Creates the target if it doesn't exist. Overwrites existing files.
   * Prevents zip-slip attacks by validating each entry's resolved path.
   *
   * @param zipPath   path to the zip file
   * @param targetDir directory to extract into
   * @throws IllegalArgumentException if any input is null
   * @throws IOException if extraction fails, zip-slip is detected, or any file extracts with a size mismatch
   */
  public static void extractZip(Path zipPath, Path targetDir) throws IOException {
    Objects.requireNonNull(zipPath, "Zip path cannot be null");
    Objects.requireNonNull(targetDir, "Target directory cannot be null");

    if (!Files.isRegularFile(zipPath)) {
      throw new IOException("Zip archive does not exist or is not a file: " + zipPath);
    }

    Files.createDirectories(targetDir);
    Path normalizedTarget = targetDir.toAbsolutePath().normalize();

    try (ZipFile zf = new ZipFile(zipPath.toFile())) {
      Enumeration<? extends ZipEntry> entries = zf.entries();

      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        Path resolved = normalizedTarget.resolve(entry.getName()).normalize();

        // Zip-slip guard: resolved path must remain inside target
        if (!resolved.startsWith(normalizedTarget)) {
          throw new IOException("Zip-slip detected on entry: " + entry.getName());
        }

        if (entry.isDirectory()) {
          Files.createDirectories(resolved);
        } else {
          Files.createDirectories(resolved.getParent());

          try (InputStream in = new BufferedInputStream(zf.getInputStream(entry))) {
            Files.copy(in, resolved, StandardCopyOption.REPLACE_EXISTING);
          }

          // Verify exact byte count if the entry declares a size
          if (entry.getSize() >= 0 && Files.size(resolved) != entry.getSize()) {
            throw new IOException(
                "Extraction size mismatch for entry '%s': expected %d bytes, got %d"
                    .formatted(entry.getName(), entry.getSize(), Files.size(resolved))
            );
          }
        }
      }
    }
  }

  /**
   * Recursively deletes a directory and all its contents.
   * No-op if the directory does not exist.
   *
   * @param directory the directory to delete
   * @throws IllegalArgumentException if input is null
   * @throws IOException if any file or directory cannot be deleted, or the path still exists after deletion
   */
  public static void deleteDirectory(Path directory) throws IOException {
    Objects.requireNonNull(directory, "Directory cannot be null");

    if (!Files.exists(directory)) return;

    if (!Files.isDirectory(directory)) {
      throw new IOException("Path is not a directory: " + directory);
    }

    Files.walkFileTree(directory, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        throw exc;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (exc != null) throw exc;
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });

    if (Files.exists(directory)) {
      throw new IOException("Directory still exists after deletion attempt: " + directory);
    }
  }

  /**
   * Compresses a directory into a zip archive.
   *
   * <p>Exclusions use a hybrid approach:
   * <ul>
   *   <li>{@code dirExclusions} — absolute {@link Path} objects matched against directories;
   *       matching directories are pruned entirely (no traversal of children).</li>
   *   <li>{@code extensionExclusions} — strings like {@code ".json"} or {@code ".log"} matched
   *       against filenames; safe for extension filtering without substring collision risk.</li>
   * </ul>
   *
   * @param sourceDir           the directory to compress
   * @param outputZip           the output zip path (created or overwritten)
   * @param dirExclusions       absolute paths of directories to exclude entirely; may be null or empty
   * @param extensionExclusions file extensions or exact filenames to exclude (e.g. {@code ".log"},
   *                            {@code "session.lock"}); may be null or empty
   * @return the path of the created zip file
   * @throws IllegalArgumentException if required inputs are null
   * @throws IOException if the source is invalid, writing fails, or the archive is empty/corrupt
   */
  public static Path zipDirectory(
      Path sourceDir,
      Path baseDir,
      Path outputZip,
      Set<Path> dirExclusions,
      Set<String> extensionExclusions
  ) throws IOException {
    Objects.requireNonNull(sourceDir, "Source directory cannot be null");
    Objects.requireNonNull(outputZip, "Output zip path cannot be null");

    if (!Files.isDirectory(sourceDir)) {
      throw new IOException("Source is not a directory or does not exist: " + sourceDir);
    }

    Path sourceDirNormalized = sourceDir.toAbsolutePath().normalize();

    // Normalize dir exclusions once up front
    Set<Path> normalizedDirExclusions = dirExclusions == null ? Set.of() : dirExclusions.stream()
        .map(p -> p.toAbsolutePath().normalize())
        .collect(Collectors.toUnmodifiableSet());

    Set<String> normalizedExtExclusions = extensionExclusions == null ? Set.of() : extensionExclusions;

    Files.createDirectories(outputZip.getParent());

    // int[] to allow mutation inside SimpleFileVisitor
    int[] filesAdded = {0};

    try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(outputZip)))) {
      Files.walkFileTree(sourceDirNormalized, new SimpleFileVisitor<>() {

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
          Path normalized = dir.toAbsolutePath().normalize();

          // SKIP_SUBTREE actually stops traversal — unlike `continue` with Files.walk
          if (normalizedDirExclusions.contains(normalized)) {
            return FileVisitResult.SKIP_SUBTREE;
          }

          // Write explicit directory entry (except the root itself)
          if (!normalized.equals(sourceDirNormalized)) {
            String entryName = sourceDirNormalized.relativize(normalized).toString().replace('\\', '/') + "/";
            zos.putNextEntry(new ZipEntry(entryName));
            zos.closeEntry();
          }

          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          String fileName = file.getFileName().toString();

          // Skip files matching exact name or extension (e.g. "session.lock", ".log")
          if (normalizedExtExclusions.stream().anyMatch(fileName::endsWith)) {
            return FileVisitResult.CONTINUE;
          }

          String entryName = sourceDirNormalized.relativize(file.toAbsolutePath().normalize())
              .toString().replace('\\', '/');

          zos.putNextEntry(new ZipEntry(entryName));
          try (BufferedInputStream in = new BufferedInputStream(Files.newInputStream(file))) {
            in.transferTo(zos);
          }
          zos.closeEntry();
          filesAdded[0]++;

          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
          throw exc; // don't silently skip unreadable files
        }
      });
    }

    if (filesAdded[0] == 0) {
      Files.deleteIfExists(outputZip);
      throw new IOException("Zip archive is empty (directory was empty or all files excluded): " + outputZip);
    }

    // Structural verify — reopen and confirm entry count is non-zero
    try (ZipFile verify = new ZipFile(outputZip.toFile())) {
      if (verify.size() == 0) {
        throw new IOException("Zip verification failed — archive contains no entries: " + outputZip);
      }
    }

    return outputZip;
  }
}