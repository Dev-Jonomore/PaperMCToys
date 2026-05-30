package mc.jonomore.toy.listeners;

import mc.jonomore.toy.PaperMCToy;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.Material;

public class PortalLinker implements Listener {
  private final PaperMCToy plugin;

  public PortalLinker(PaperMCToy plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onEntityPortal(org.bukkit.event.entity.EntityPortalEvent event) {
    World fromWorld = event.getFrom().getWorld();
    World.Environment fromEnv = fromWorld.getEnvironment();
    World toWorld = event.getTo().getWorld();
    World.Environment toEnv = toWorld.getEnvironment();

    if (plugin.getOverworld() == null) return;

    Location toLoc = null;

    if (
      fromEnv == World.Environment.NORMAL &&
        toEnv == World.Environment.NETHER
    ) {
      // Overworld -> Nether
      toLoc = event.getFrom().clone();
      toLoc.setWorld(plugin.getNether());
      toLoc.setX(toLoc.getX() / 8.0);
      toLoc.setZ(toLoc.getZ() / 8.0);
    } else if (
      fromEnv == World.Environment.NETHER &&
        toEnv == World.Environment.NORMAL
    ) {
      // Nether -> Overworld
      toLoc = event.getFrom().clone();
      toLoc.setWorld(plugin.getOverworld());
      toLoc.setX(toLoc.getX() * 8.0);
      toLoc.setZ(toLoc.getZ() * 8.0);
    } else if (
      fromEnv == World.Environment.NORMAL &&
        toEnv == World.Environment.THE_END
    ) {
      // Overworld -> End
      World endWorld = plugin.getEnd();
      toLoc = new Location(endWorld, 100.5, 49, 0.5, 90, 0);
      toLoc.setWorld(endWorld);
      generateEndPlatform(endWorld);
    } else if (
      fromEnv == World.Environment.THE_END &&
        toEnv == World.Environment.NORMAL
    ) {
      // End -> Overworld
      toLoc = plugin.getOverworld().getSpawnLocation();
      toLoc.setWorld(plugin.getOverworld());
    }

    if (toLoc != null) {
      event.setTo(toLoc);
    }
  }

  @EventHandler
  public void onPlayerPortal(org.bukkit.event.player.PlayerPortalEvent event) {
    World fromWorld = event.getFrom().getWorld();

    if (plugin.getOverworld() == null) return;

    Location toLoc = null;

    if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
      if (fromWorld.equals(plugin.getOverworld())) {
        // Overworld -> Nether
        toLoc = event.getFrom().clone();
        toLoc.setWorld(plugin.getNether());
        toLoc.setX(toLoc.getX() / 8.0);
        toLoc.setZ(toLoc.getZ() / 8.0);
      } else if (fromWorld.equals(plugin.getNether())) {
        // Nether -> Overworld
        toLoc = event.getFrom().clone();
        toLoc.setWorld(plugin.getOverworld());
        toLoc.setX(toLoc.getX() * 8.0);
        toLoc.setZ(toLoc.getZ() * 8.0);
      }
    } else if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
      if (fromWorld.equals(plugin.getOverworld())) {
        // Overworld -> End
        World endWorld = plugin.getEnd();
        toLoc = new Location(endWorld, 100.5, 49, 0.5, 90, 0);
        generateEndPlatform(endWorld);
      } else if (fromWorld.equals(plugin.getEnd())) {
        // End -> Overworld
        toLoc = plugin.getOverworld().getSpawnLocation();
      }
    }

    if (toLoc != null) {
      event.setTo(toLoc);
    }
  }

  private void generateEndPlatform(World endWorld) {
    // 5x5 Obsidian platform at 100, 48, 0 (y=48 is the floor, player at y=49)
    for (int x = 98; x <= 102; x++) {
      for (int z = -2; z <= 2; z++) {
        endWorld.getBlockAt(x, 48, z).setType(Material.OBSIDIAN);
        // Clear 3 blocks of air above
        for (int y = 49; y <= 51; y++) {
          org.bukkit.block.Block block = endWorld.getBlockAt(x, y, z);
          if (block.getType() != Material.AIR) {
            endWorld.dropItem(block.getLocation(), new org.bukkit.inventory.ItemStack(block.getType()));
            endWorld.getBlockAt(x, y, z).setType(Material.AIR);
          }
        }
      }
    }
  }
}
