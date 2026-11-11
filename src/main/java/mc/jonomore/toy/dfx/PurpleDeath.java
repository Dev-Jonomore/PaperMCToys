package mc.jonomore.toy.dfx;

import com.destroystokyo.paper.ParticleBuilder;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import mc.jonomore.toy.PaperMCToy;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

public class PurpleDeath implements Listener {
  private final PaperMCToy plugin;
  static final List<BiConsumer<Location, PaperMCToy>> dfx = List.of(
    PurpleDeath::playHomingSiphonEffect,
    PurpleDeath::playAmethystGradientShatterEffect,
    PurpleDeath::playVoidPuddleEffect,
    PurpleDeath::playWitheredBlossomEffect
  );

  // Particle Builders
  private static final ParticleBuilder trailBuilder = Particle.TRAIL.builder();
  private static final ParticleBuilder flashBuilder = Particle.FLASH.builder();
  private static final ParticleBuilder dustTransitionBuilder = Particle.DUST_COLOR_TRANSITION.builder();
  private static final ParticleBuilder blockBuilder = Particle.BLOCK.builder();
  private static final ParticleBuilder dragonBreathBuilder = Particle.DRAGON_BREATH.builder();
  private static final ParticleBuilder witchBuilder = Particle.WITCH.builder();
  private static final ParticleBuilder tintedLeavesBuilder = Particle.TINTED_LEAVES.builder();
  private static final ParticleBuilder crimsonSporeBuilder = Particle.CRIMSON_SPORE.builder();
  private static final ParticleBuilder reversePortalBuilder = Particle.REVERSE_PORTAL.builder();

  // Pre-cached Particle Data payloads
  private static final BlockData amethystData = Bukkit.createBlockData(Material.AMETHYST_CLUSTER);
  private static final BlockData cryingObsidianData = Bukkit.createBlockData(Material.CRYING_OBSIDIAN);
  private static final Color royalPurple = Color.fromRGB(155, 48, 255);
  private static final Color flashPurple = Color.fromARGB(255, 185, 60, 255);
  private static final Color orchidColor = Color.fromRGB(147, 112, 219);
  private static final Color indigoColor = Color.fromRGB(75, 0, 130);
  private static final Particle.DustTransition neonToVoidTransition =
    new Particle.DustTransition(Color.fromRGB(255, 0, 255), Color.fromRGB(18, 0, 36), 2.4f);

  public PurpleDeath(PaperMCToy plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onDeath(EntityDeathEvent e) {
    dfx.get(ThreadLocalRandom.current().nextInt(dfx.size())).accept(e.getEntity().getLocation(), plugin);
  }

  public static LiteralArgumentBuilder<CommandSourceStack> createCommand(PaperMCToy plugin) {
    return Commands.literal("purpledeath")
      .requires(src -> src.getSender() instanceof Player)
      .then(Commands.literal("siphon")
        .executes(ctx -> dispatchCommand(ctx, PurpleDeath::playHomingSiphonEffect, plugin))
      )
      .then(Commands.literal("shatter")
        .executes(ctx -> dispatchCommand(ctx, PurpleDeath::playAmethystGradientShatterEffect, plugin))
      )
      .then(Commands.literal("puddle")
        .executes(ctx -> dispatchCommand(ctx, PurpleDeath::playVoidPuddleEffect, plugin))
      )
      .then(Commands.literal("blossom")
        .executes(ctx -> dispatchCommand(ctx, PurpleDeath::playWitheredBlossomEffect, plugin))
      );
  }

  // Precondition: ctx.getSource().getExecutor() instanceof Player
  private static int dispatchCommand(CommandContext<CommandSourceStack> ctx, BiConsumer<Location, PaperMCToy> deatheffect, PaperMCToy plugin) {
    Player p = (Player) ctx.getSource().getExecutor();
    Block b = p.getTargetBlockExact(128);
    Location l;
    // Effects expect a feet position, so stand on top of the targeted block
    if (b != null) l = b.getLocation().add(.5, 1, .5);
    else l = p.getLocation().add(p.getLocation().getDirection().setY(0).normalize().multiply(3));
    deatheffect.accept(l, plugin);
    return Command.SINGLE_SUCCESS;
  }

  /**
   * 1. "Homing Soul Siphon" (Particle.TRAIL + Tinted Particle.FLASH)
   * Spawns 3 staggered waves of homing purple ribbons in a sphere that curve
   * and snap directly into the victim's chest, culminating in a purple flash.
   */
  private static void playHomingSiphonEffect(Location origin, PaperMCToy plugin) {
    World world = origin.getWorld();
    if (world == null) return;

    Location core = origin.clone().add(0, 1.1, 0);
    bcSound(Sound.sound(Key.key("block.beacon.power_select"), Sound.Source.MASTER, 1.8f, 1.5f), core);

    new BukkitRunnable() {
      int ticks = 0;

      @Override
      public void run() {
        // Waves at tick 0, 4, and 8: Spawn 10 homing TRAIL ribbons snapping inward
        if (ticks == 0 || ticks == 4 || ticks == 8) {
          ThreadLocalRandom rng = ThreadLocalRandom.current();
          for (int i = 0; i < 10; i++) {
            double theta = rng.nextDouble(0, 2 * Math.PI);
            double phi = rng.nextDouble(0.2, Math.PI - 0.2);
            double r = 2.4 - (ticks * 0.12);

            Location start = core.clone().add(
              r * Math.sin(phi) * Math.cos(theta),
              r * Math.cos(phi),
              r * Math.sin(phi) * Math.sin(theta)
            );

            Particle.Trail trailData = new Particle.Trail(core, royalPurple, 14);
            trailBuilder.location(start)
              .offset(0, 0, 0)
              .count(1).speed(0.0)
              .data(trailData)
              .receivers(32)
              .spawn();
          }
          bcSound(Sound.sound(Key.key("entity.enderman.teleport"), Sound.Source.MASTER, .7f, 1.4f + (ticks * .08f)), core);
        }

        // Tick 20: Convergence point implodes with a purple flash burst
        if (ticks == 20) {
          flashBuilder.location(core)
            .offset(0, 0, 0)
            .count(1).speed(0.0)
            .data(flashPurple)
            .receivers(32)
            .spawn();
          reversePortalBuilder.location(core)
            .offset(.15, .15, .15)
            .count(35).speed(.25)
            .receivers(32)
            .spawn();

          bcSound(Sound.sound(Key.key("block.respawn_anchor.deplete"), Sound.Source.MASTER, 2.0f, .8f), core);
          this.cancel();
          return;
        }

        ticks++;
      }
    }.runTaskTimer(plugin, 0L, 1L);
  }

  /**
   * 2. "Amethyst & Obsidian Gradient Shatter" (DUST_COLOR_TRANSITION + BLOCK)
   * Bursts physical Amethyst and Crying Obsidian shards while expanding a ring
   * of neon-magenta dust that cools into void-black smoke.
   */
  private static void playAmethystGradientShatterEffect(Location origin, PaperMCToy plugin) {
    World world = origin.getWorld();
    if (world == null) return;

    Location chest = origin.clone().add(0, 1.0, 0);

    // Initial crystalline shatter
    bcSound(Sound.sound(Key.key("block.amethyst_cluster.break"), Sound.Source.MASTER, 2.5f, .6f), chest);
    bcSound(Sound.sound(Key.key("block.respawn_anchor.set_spawn"), Sound.Source.MASTER, 1.5f, .5f), chest);

    blockBuilder.location(chest)
      .offset(.35, .45, .35)
      .count(35).speed(.15)
      .data(amethystData)
      .receivers(32)
      .spawn();
    blockBuilder.location(chest)
      .offset(.3, .4, .3)
      .count(25).speed(.12)
      .data(cryingObsidianData)
      .receivers(32)
      .spawn();

    new BukkitRunnable() {
      int ticks = 0;

      @Override
      public void run() {
        if (ticks > 16) {
          this.cancel();
          return;
        }

        // Expanding shockwave ring of Neon Magenta -> Void Black gradient dust
        double radius = 0.25 + (ticks * 0.14);
        double y = 1.0 - (ticks * 0.04);

        for (int i = 0; i < 12; i++) {
          double angle = (2 * Math.PI * i / 12) + (ticks * 0.15);
          Location ringPoint = origin.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);

          dustTransitionBuilder.location(ringPoint)
            .offset(.06, .06, .06)
            .count(1).speed(0.0)
            .data(neonToVoidTransition)
            .receivers(32)
            .spawn();
        }

        if (ticks % 4 == 0) {
          bcSound(Sound.sound(Key.key("block.amethyst_block.chime"), Sound.Source.MASTER, 1.5f, .6f + (ticks * .05f)), chest);
        }

        ticks++;
      }
    }.runTaskTimer(plugin, 0L, 1L);
  }

  /**
   * 3. "Void Puddle" (Directional DRAGON_BREATH + Perimeter WITCH Tendrils)
   * Shoots 24 Dragon Breath clouds horizontally along the floor to form a boiling
   * violet pool while Witch magic curls up from the outer rim.
   */
  private static void playVoidPuddleEffect(Location origin, PaperMCToy plugin) {
    World world = origin.getWorld();
    if (world == null) return;

    Location floor = origin.clone().add(0, 0.1, 0);
    bcSound(Sound.sound(Key.key("entity.ender_dragon.growl"), Sound.Source.MASTER, 1.2f, .55f), floor);
    bcSound(Sound.sound(Key.key("block.end_portal_frame.fill"), Sound.Source.MASTER, 2.0f, .5f), floor);

    // Shoot 24 Dragon Breath particles outward horizontally along the ground (count = 0)
    for (int i = 0; i < 24; i++) {
      double theta = (2 * Math.PI * i) / 24;
      dragonBreathBuilder.location(floor)
        .offset(Math.cos(theta), 0.01, Math.sin(theta))
        .count(0).speed(.11)
        .data(1.2f)
        .receivers(32)
        .spawn();
    }

    new BukkitRunnable() {
      int ticks = 0;

      @Override
      public void run() {
        if (ticks > 30) {
          this.cancel();
          return;
        }

        // Spawn rising Witch swirls around the 1.4-block outer perimeter of the puddle
        for (int i = 0; i < 4; i++) {
          double angle = (ticks * 0.3) + (i * (Math.PI / 2));
          Location rim = origin.clone().add(Math.cos(angle) * 1.4, 0.15, Math.sin(angle) * 1.4);

          witchBuilder.location(rim)
            .offset(.05, .15, .05)
            .count(2).speed(.04)
            .receivers(32)
            .spawn();
        }

        if (ticks == 15) {
          bcSound(Sound.sound(Key.key("entity.illusioner.cast_spell"), Sound.Source.MASTER, 1.2f, .6f), floor);
        }

        ticks++;
      }
    }.runTaskTimer(plugin, 0L, 1L);
  }

  /**
   * 4. "Withered Blossom Fall" (Violet & Indigo TINTED_LEAVES + CRIMSON_SPORE)
   * Quiet, melancholic effect where dyed purple leaves and spores drift lazily
   * to the ground around where the player stood.
   */
  private static void playWitheredBlossomEffect(Location origin, PaperMCToy plugin) {
    World world = origin.getWorld();
    if (world == null) return;

    Location canopy = origin.clone().add(0, 1.7, 0);
    bcSound(Sound.sound(Key.key("block.chorus_flower.death"), Sound.Source.MASTER, 2.0f, .7f), canopy);
    bcSound(Sound.sound(Key.key("block.spore_blossom.fall"), Sound.Source.MASTER, 2.0f, .6f), canopy);

    new BukkitRunnable() {
      int ticks = 0;

      @Override
      public void run() {
        if (ticks > 35) {
          this.cancel();
          return;
        }

        // NOTE: everything from here to the end of this method was reconstructed from the
        // javadoc above; the source this class was ported from was cut off at this point.
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < 3; i++) {
          double angle = rng.nextDouble(0, 2 * Math.PI);
          double r = Math.sqrt(rng.nextDouble(0, 1.0)) * 1.1;
          Location leafPoint = canopy.clone().add(Math.cos(angle) * r, rng.nextDouble(-.3, .3), Math.sin(angle) * r);

          // Dyed leaves fall on their own; alternate violet and indigo
          tintedLeavesBuilder.location(leafPoint)
            .offset(0, 0, 0)
            .count(1).speed(0.0)
            .data(rng.nextBoolean() ? orchidColor : indigoColor)
            .receivers(32)
            .spawn();
        }

        // Sparse spores hanging in the air around the body
        if (ticks % 2 == 0) {
          crimsonSporeBuilder.location(origin.clone().add(0, 1.0, 0))
            .offset(.6, .5, .6)
            .count(2).speed(0.0)
            .receivers(32)
            .spawn();
        }

        if (ticks % 12 == 0) {
          bcSound(Sound.sound(Key.key("block.azalea_leaves.fall"), Sound.Source.MASTER, 1.0f, .6f), canopy);
        }

        ticks++;
      }
    }.runTaskTimer(plugin, 0L, 1L);
  }

  private static void bcSound(Sound sound, Location origin) {
    World world = origin.getWorld();
    if (world == null) return;
    for (Player player : world.getPlayers()) {
      if (player.getLocation().distanceSquared(origin) <= 32 * 32) {
        player.playSound(sound, origin.getX(), origin.getY(), origin.getZ());
      }
    }
  }
}
