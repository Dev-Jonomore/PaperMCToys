package mc.jonomore.toy;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import mc.jonomore.toy.commands.DialogCommands;
import mc.jonomore.toy.commands.KeyCommands;
import mc.jonomore.toy.commands.PotionCommands;
import mc.jonomore.toy.commands.WorldCommands;
import mc.jonomore.toy.dfx.PurpleDeath;
import mc.jonomore.toy.dfx.RandomDeath;
import mc.jonomore.toy.dfx.SoulDeath;
import mc.jonomore.toy.dfx.ZParticleDeath;
import mc.jonomore.toy.listeners.PortalLinker;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PaperMCToy extends JavaPlugin {
  public static final String PREFIX = "<gray>[<gold>Toy<gray>] » <white>";

  private Path exportDir;
  private World overworld;
  private World nether;
  private World end;
  private BukkitWorldPipeline worldManager;

  @Override
  public void onEnable() {
    saveDefaultConfig();
    String exportPath = getConfig().getString("export-dir", "C:\\Users\\jaduv\\Documents\\TestServerFiles\\Worlds");
    exportDir = Paths.get(exportPath);
    worldManager = new BukkitWorldPipeline(this, exportDir);
    LiteralCommandNode<CommandSourceStack> toyCommands = Commands.literal("toy")
      .then(DialogCommands.createCommand())
      .then(WorldCommands.createCommand(this))
      .then(KeyCommands.createCommand(this))
      .then(PotionCommands.create())
      .then(SoulDeath.createCommand(this))
      .then(PurpleDeath.createCommand(this))
      .then(ZParticleDeath.createCommand(this))
      .build();
    getLifecycleManager().registerEventHandler(
        LifecycleEvents.COMMANDS,
        commands -> commands.registrar().register(
            toyCommands
        )
    );
    getServer().getPluginManager().registerEvents(new PortalLinker(this), this);
    getServer().getPluginManager().registerEvents(new RandomDeath(this), this);
  }

  public Path getExportDir() { return exportDir; }

  public World getOverworld() { return overworld; }
  public void  setOverworld(World overworld) { this.overworld = overworld; }

  public World getNether() { return nether; }
  public void setNether(World nether) { this.nether = nether; }

  public World getEnd() { return end; }
  public void setEnd(World end) { this.end = end; }

  public BukkitWorldPipeline getWorldManager() { return worldManager; }
}
