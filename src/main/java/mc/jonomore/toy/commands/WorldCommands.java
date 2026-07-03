package mc.jonomore.toy.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import mc.jonomore.toy.PaperMCToy;
import mc.jonomore.toy.fileIO.FileUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;


import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.nio.file.Files;
import java.util.Set;
import java.util.logging.Level;

public class WorldCommands {
  public static LiteralArgumentBuilder<CommandSourceStack> createCommand(PaperMCToy plugin) {
    return Commands.literal("world")
      .then(Commands.literal("create")
        .then(Commands.argument("name", StringArgumentType.word())
          .executes(ctx -> {
            plugin.setOverworld(new WorldCreator(StringArgumentType.getString(ctx, "name")).createWorld());
            if (plugin.getOverworld() != null)
              ctx.getSource().getSender().sendMessage(MiniMessage.miniMessage().deserialize(
                PaperMCToy.PREFIX + "Successfully created world" + plugin.getOverworld().getName() + '!'
              ));

            return Command.SINGLE_SUCCESS;
          })
        )
        .executes(ctx -> {
          plugin.setOverworld(new WorldCreator("test_world").createWorld());
          if (plugin.getOverworld() != null)
            ctx.getSource().getSender().sendMessage(MiniMessage.miniMessage().deserialize(
              PaperMCToy.PREFIX + "Successfully created world " + plugin.getOverworld().getName() + '!'
            ));
          return Command.SINGLE_SUCCESS;
        })
      )
      .then(Commands.literal("create-all")
        .executes(ctx -> {
          plugin.setOverworld(new WorldCreator(new NamespacedKey("toy", "overworld")).seed(67).createWorld());
          plugin.setNether(new WorldCreator(new NamespacedKey("toy", "nether")).environment(World.Environment.NETHER).seed(67).createWorld());
          plugin.setEnd(new WorldCreator(new NamespacedKey("toy", "end")).environment(World.Environment.THE_END).seed(67).createWorld());
          if (
            plugin.getOverworld() != null &&
            plugin.getNether() != null &&
            plugin.getEnd() != null
          ) {
            ctx.getSource().getSender().sendMessage(MiniMessage.miniMessage().deserialize(
              PaperMCToy.PREFIX + "Successfully created toy worlds!"
            ));
          }
          return Command.SINGLE_SUCCESS;
        })
      )
      .then(Commands.literal("export")
        .then(Commands.argument("zip-name", StringArgumentType.word())
          .executes(ctx -> {
            Path worldPath = plugin.getOverworld().getWorldPath();
            Bukkit.unloadWorld(plugin.getOverworld(), true);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
              plugin.getWorldManager().exportSingle(plugin.getOverworld(), ctx.getArgument("zip-name", String.class));
              Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                try {
                  FileUtils.deleteDirectory(worldPath);
                } catch (IOException e) {
                  ctx.getSource().getSender().sendMessage(MiniMessage.miniMessage().deserialize(
                    PaperMCToy.PREFIX + "<red>Failed to remove world directory " + worldPath + e.getMessage()
                  ));
                }
              }, 20 * 10L);
            });
            return Command.SINGLE_SUCCESS;
          })
        )
      )
      .then(Commands.literal("import")
        .then(Commands.argument("zip-name", StringArgumentType.word())
          .executes(ctx -> {
            String zipName = StringArgumentType.getString(ctx, "zip-name");
            ctx.getSource().getSender().sendMessage(MiniMessage.miniMessage().deserialize(
              PaperMCToy.PREFIX + "Starting import of " + zipName + "..."
            ));
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
              plugin.getWorldManager().importSingle(zipName)
            );
            return Command.SINGLE_SUCCESS;
          })
        )
      )
      .then(Commands.literal("exportAll")
        .then(Commands.argument("zip-name", StringArgumentType.word())
          .executes(ctx -> {
            List<World> mhWorlds = List.of(
              plugin.getOverworld(),
              plugin.getNether(),
              plugin.getEnd()
            );
            List<Path> mhPaths = mhWorlds.stream().map(World::getWorldPath).toList();
            for (World world : mhWorlds) {
              Bukkit.unloadWorld(world, true);
            }
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
              plugin.getWorldManager().exportSet(mhWorlds, ctx.getArgument("zip-name", String.class));
              Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> mhPaths.forEach(p -> {
                try {
                  FileUtils.deleteDirectory(p);
                } catch (IOException e) {
                  ctx.getSource().getSender().sendMessage(MiniMessage.miniMessage().deserialize(
                    PaperMCToy.PREFIX + "Failed to remove world directory " + p + e.getLocalizedMessage()
                  ));
                }
              }), 20 * 30L);
            });
            return Command.SINGLE_SUCCESS;
          })
        )
      )
      .then(Commands.literal("importAll")
        .then(Commands.argument("zip-name", StringArgumentType.word())
          .executes(ctx -> {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
              plugin.getWorldManager().importSet(ctx.getArgument("zip-name", String.class))
            );
            return Command.SINGLE_SUCCESS;
          })
        )
      )
      .then(Commands.literal("test-save-zip")
        .executes(ctx -> {
          Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
              Path worldDir = Bukkit.getWorldContainer().toPath().resolve("world");
              Path dimensionsDir = worldDir.resolve("dimensions/minecraft");

              // --- 1. Test: single world zip with explicit baseDir (5-arg) ---
              // entries should be: toy_overworld/region/..., toy_overworld/level.dat, etc.
              Path singleZip = plugin.getExportDir().resolve("test_single.zip");
              FileUtils.zipDirectory(
                plugin.getOverworld().getWorldPath(),  // sourceDir
                dimensionsDir,                          // baseDir → entries include folder name
                singleZip,
                null,
                Set.of("session.lock", "uid.dat")
              );
              plugin.getLogger().info("[test] single zip OK: " + singleZip);
              inspectZip(plugin, singleZip);

              // --- 2. Test: multi-world zip (zipDirectories) ---
              // entries should be: toy_overworld/..., toy_nether/..., toy_end/...
              Path multiZip = plugin.getExportDir().resolve("test_multi.zip");
              FileUtils.zipDirectories(
                List.of(
                  plugin.getOverworld().getWorldPath(),
                  plugin.getNether().getWorldPath(),
                  plugin.getEnd().getWorldPath()
                ),
                dimensionsDir,
                multiZip,
                null,
                Set.of("session.lock", "uid.dat")
              );
              plugin.getLogger().info("[test] multi zip OK: " + multiZip);
              inspectZip(plugin, multiZip);

              // --- 3. Test: what SHOULD work but DOESN'T yet ---
              // Player .dat files live at world/players/data/<uuid>.dat
              // They are NOT under dimensionsDir, so zipDirectories can't include them.
              // This test demonstrates the limitation:
              Path playerDataDir = worldDir.resolve("players/data");
              if (Files.isDirectory(playerDataDir)) {
                try (var stream = Files.list(playerDataDir)) {
                  List<Path> datFiles = stream
                    .filter(p -> p.toString().endsWith(".dat"))
                    .toList();
                  plugin.getLogger().info("[test] Found " + datFiles.size() + " .dat files in players/data/");
                  plugin.getLogger().info("[test] These CANNOT be added to the world zip via zipDirectories — needs ZipSource.");
                  // Attempting zipDirectories with a file path would throw:
                  //   IOException: Source is not a directory or does not exist: .../abc123.dat
                  // Attempting zipDirectories with playerDataDir + dimensionsDir as baseDir would throw:
                  //   IllegalArgumentException from relativize() — playerDataDir is not under dimensionsDir
                }
              } else {
                plugin.getLogger().warning("[test] players/data/ doesn't exist — are any players online?");
              }

            } catch (IOException e) {
              plugin.getLogger().log(Level.SEVERE, "[test] test-save-zip failed", e);
            }
          });
          ctx.getSource().getSender().sendMessage(
            MiniMessage.miniMessage().deserialize(PaperMCToy.PREFIX + "Running zip tests async, check console...")
          );
          return Command.SINGLE_SUCCESS;
        })
      );
  }

  private static void inspectZip(PaperMCToy plugin, Path zipPath) throws IOException {
    try (var zf = new java.util.zip.ZipFile(zipPath.toFile())) {
      plugin.getLogger().info("[test]   entries in " + zipPath.getFileName() + ":");
      zf.entries().asIterator().forEachRemaining(e ->
        plugin.getLogger().info("[test]     " + e.getName())
      );
    }
  }
}
