package mc.jonomore.toy;

import mc.jonomore.toy.fileIO.FileUtils;
import mc.jonomore.toy.fileIO.WorldIO;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Stream;

/**
 * Orchestrates world export/import for both solutions.
 * Owns the pipeline: save flush, unload, I/O, reload.
 */
public class BukkitWorldPipeline {

  private final PaperMCToy plugin;
  private final Path exportDir;     // wherever you want archives to land
  private final Path dimensionsDir; // destination dir for imports - i.e., Bukkit.getWorldContainer.toPath().resolve("world/dimensions/minecraft")
  private static final Set<String> DEFAULT_DIMENSIONS = Set.of("overworld", "the_nether", "the_end");
  private final List<String> MH_WORLD_NAMES = List.of("mh_overworld", "mh_the_nether", "mh_the_end");

  public BukkitWorldPipeline(PaperMCToy plugin, Path exportDir) {
    this.plugin = plugin;
    this.exportDir = exportDir;
    dimensionsDir = Bukkit.getWorldContainer().toPath().resolve("world/dimensions/minecraft");
  }

  public void exportSingle(World world, String zipName) {
    try {
      WorldIO.exportWorld(world, exportDir.resolve(zipName + ".zip"));
    } catch (IOException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to export world " + world.getName(), e);
    }
  }

  public void importSingle(String zipName) {
    try {
      Path zipPath = FileUtils.findAndVerifyZip(exportDir, zipName);
      WorldIO.importWorld(zipPath, dimensionsDir);
      try (Stream<Path> stream = Files.list(dimensionsDir)) {
        stream.filter(Files::isDirectory)
          .filter(this::isNotDefaultDimension)
          .filter(p -> !MH_WORLD_NAMES.contains(p.getFileName().toString()))
          .findFirst()
          .ifPresent(path -> {
            try {
              Files.move(path, dimensionsDir.resolve(MH_WORLD_NAMES.getFirst()));
              plugin.getLogger().info("Successfully renamed world to " + MH_WORLD_NAMES.getFirst());
              Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.setOverworld(new WorldCreator(MH_WORLD_NAMES.getFirst()).createWorld());
                long seed = plugin.getOverworld() != null ? plugin.getOverworld().getSeed() : 42;
                plugin.setNether(new WorldCreator(MH_WORLD_NAMES.get(1)).seed(seed).createWorld());
                plugin.setEnd(new WorldCreator(MH_WORLD_NAMES.getLast()).seed(seed).createWorld());
              });
            } catch (IOException e) {
              plugin.getLogger().log(Level.SEVERE, "Failed to rename world folder " + path, e);
            }
          });
      } catch (IOException e) {
        plugin.getLogger().log(Level.SEVERE, "Failed to list dimensions", e);
      }
    } catch (IOException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to import world " + zipName, e);
    }
  }

  public void exportSet(List<World> worlds, String zipName) {
    try {
      WorldIO.exportWorldSet(worlds, exportDir.resolve(zipName + ".zip"));
    } catch (IOException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to export world set " + zipName, e);
    }
  }

  public void importSet(String zipName) {
    try {
      Path zipPath = FileUtils.findAndVerifyZip(exportDir, zipName);
      WorldIO.importWorldSet(zipPath, dimensionsDir);
      try (Stream<Path> stream = Files.list(dimensionsDir)) {
        stream.filter(Files::isDirectory)
          .filter(this::isNotDefaultDimension)
          .forEach(p -> Bukkit.getScheduler().runTask(plugin,
            () -> {
              String name = p.getFileName().toString();
              World world = new WorldCreator(name).createWorld();
              if (world != null) {
                switch (world.getEnvironment()) {
                  case NORMAL -> plugin.setOverworld(world);
                  case NETHER -> plugin.setNether(world);
                  case THE_END -> plugin.setEnd(world);
                  default -> plugin.getLogger().warning("Erm...the world's environment is " + world.getEnvironment());
                }
              if (
                plugin.getOverworld() != null &&
                  plugin.getNether() != null &&
                  plugin.getEnd() != null
              ) plugin.getLogger().info(
                "Successfully imported and loaded worlds "
                  + plugin.getOverworld().getName() + ", "
                  + plugin.getNether().getName() + ", and "
                  + plugin.getEnd().getName() + '!'
              );
              } else {
                plugin.getLogger().warning("Failed to load world " + name);
              }
            }
          ));
      } catch (IOException e) {
        plugin.getLogger().log(Level.SEVERE, "Failed to list dimensions", e);
      }
    } catch (IOException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to import worlds " + zipName, e);
    }
  }

  // --- Internal ---

  private boolean isNotDefaultDimension(Path p) { return !DEFAULT_DIMENSIONS.contains(p.getFileName().toString()); }
}