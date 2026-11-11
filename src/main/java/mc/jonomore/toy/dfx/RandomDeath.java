package mc.jonomore.toy.dfx;

import mc.jonomore.toy.PaperMCToy;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Plays one random effect from every dfx class's pool per death. Register this
 * instead of the individual SoulDeath / PurpleDeath / ZParticleDeath listeners, otherwise
 * each death plays one effect per registered listener.
 */
public class RandomDeath implements Listener {
  private static final List<BiConsumer<Location, PaperMCToy>> dfx =
    Stream.of(SoulDeath.dfx, PurpleDeath.dfx, ZParticleDeath.dfx).flatMap(List::stream).toList();

  private final PaperMCToy plugin;

  public RandomDeath(PaperMCToy plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onDeath(EntityDeathEvent e) {
    dfx.get(ThreadLocalRandom.current().nextInt(dfx.size())).accept(e.getEntity().getLocation(), plugin);
  }
}
