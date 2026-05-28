package mc.jonomore.toy.fileIO;

import org.bukkit.World;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Solution 2 — Flat zip archive.
 *
 * <p>Zip entries start at the world folder name, not at {@code dimensions/minecraft/...}.
 * Opening the archive shows only:
 * <pre>
 *   mh_world/
 *   mh_world_nether/
 *   mh_world_the_end/
 * </pre>
 *
 * <p>This is achieved by relativizing file paths against {@code getWorldFolder().getParent()}
 * (i.e. {@code dimensions/minecraft/}) rather than {@code getWorldFolder()} itself.
 *
 * <p>Import is just {@link FileUtils#extractZip} pointed at the same parent — the entry
 * paths self-describe exactly where each file belongs, so no path manipulation is needed.
 *
 * <p>World folder structure (26.1+):
 * {@code ./world/dimensions/minecraft/{world_name}}
 * {@code getWorldFolder()} returns the dimension folder directly.
 */
public final class WorldIO {

  /** Files that should never be included in a world archive. */
  private static final Set<String> DEFAULT_EXCLUSIONS = Set.of("session.lock");

  private WorldIO() {
    throw new UnsupportedOperationException("Utility class");
  }

  // ---------------------------------------------------------------------------
  // Single world
  // ---------------------------------------------------------------------------

  /**
   * Exports a single world as a flat zip archive.
   *
   * <p>Zip entries: {@code mh_world/region/r.0.0.mca}, etc.
   *
   * @param world     the loaded world to export
   * @param outputZip destination zip path (created or overwritten)
   * @return the path of the created zip
   * @throws IOException if zipping fails or the archive is empty
   */
  public static Path exportWorld(World world, Path outputZip) throws IOException {
    Objects.requireNonNull(world, "World cannot be null");
    Objects.requireNonNull(outputZip, "Output zip path cannot be null");

    Path sourceDir = world.getWorldPath(); // .../dimensions/minecraft/mh_world
    Path baseDir = sourceDir.getParent();  // .../dimensions/minecraft/

    // Walk sourceDir, relativize from baseDir → entries start with mh_world/
    return FileUtils.zipDirectory(sourceDir, baseDir, outputZip, null, DEFAULT_EXCLUSIONS);
  }

  /**
   * Imports a single world from a flat zip archive.
   *
   * <p>Unzips directly into {@code dimensionsDir}; entry paths reconstruct the
   * world folder correctly with no path manipulation required.
   *
   * @param zipPath       path to the zip produced by {@link #exportWorld}
   * @param dimensionsDir {@code ./world/dimensions/minecraft/}
   *                      i.e. {@code getWorldFolder().getParent()}
   * @throws IOException if extraction fails
   */
  public static void importWorld(Path zipPath, Path dimensionsDir) throws IOException {
    Objects.requireNonNull(zipPath, "Zip path cannot be null");
    Objects.requireNonNull(dimensionsDir, "Dimensions directory cannot be null");

    // Entry paths already start with the world folder name — just unzip here
    FileUtils.extractZip(zipPath, dimensionsDir);
  }

  // ---------------------------------------------------------------------------
  // World set (overworld + nether + end)
  // ---------------------------------------------------------------------------

  /**
   * Exports a set of worlds into a single flat zip archive.
   *
   * <p>All world folders share the same {@code baseDir} ({@code dimensions/minecraft/}),
   * so entries from each world are correctly prefixed:
   * <pre>
   *   mh_world/region/...
   *   mh_world_nether/region/...
   *   mh_world_the_end/region/...
   * </pre>
   *
   * @param worlds    the worlds to include; must all share the same parent dimensions dir
   * @param outputZip destination zip path (created or overwritten)
   * @return the path of the created zip
   * @throws IOException if zipping fails or worlds have mismatched parents
   */
  public static Path exportWorldSet(List<World> worlds, Path outputZip) throws IOException {
    Objects.requireNonNull(worlds, "World list cannot be null");
    Objects.requireNonNull(outputZip, "Output zip path cannot be null");

    if (worlds.isEmpty()) {
      throw new IllegalArgumentException("World list cannot be empty");
    }

    List<Path> sourceDirs = worlds.stream()
        .map(w -> w.getWorldFolder().toPath())
        .toList();

    // All worlds share the same parent — validate that assumption
    Path baseDir = sourceDirs.getFirst().getParent();
    for (Path sourceDir : sourceDirs) {
      if (!sourceDir.getParent().toAbsolutePath().normalize()
          .equals(baseDir.toAbsolutePath().normalize())) {
        throw new IOException(
            "All worlds must share the same dimensions directory. Mismatched: " + sourceDir
        );
      }
    }

    return FileUtils.zipDirectories(sourceDirs, baseDir, outputZip, null, DEFAULT_EXCLUSIONS);
  }

  /**
   * Imports a world set from a flat zip archive.
   *
   * <p>Identical to {@link #importWorld} — the zip is self-describing regardless
   * of how many world folders it contains.
   *
   * @param zipPath       path to the zip produced by {@link #exportWorldSet}
   * @param dimensionsDir {@code ./world/dimensions/minecraft/}
   * @throws IOException if extraction fails
   */
  public static void importWorldSet(Path zipPath, Path dimensionsDir) throws IOException {
    // Import is the same operation whether the zip has one world or three —
    // the flat entry structure handles it automatically
    importWorld(zipPath, dimensionsDir);
  }
}