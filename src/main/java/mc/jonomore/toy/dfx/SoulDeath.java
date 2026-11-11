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
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

public class SoulDeath implements Listener {
  private final PaperMCToy plugin;
  private static Random r;
  private static final BlockData soulSandData = Bukkit.createBlockData(Material.SOUL_SAND);
  static final List<BiConsumer<Location, PaperMCToy>> dfx = List.of(
    SoulDeath::playSoulHarvestEffect,
    SoulDeath::playSoulQuicksandEffect,
    SoulDeath::playTetheredWispEffect,
    SoulDeath::playSoulSpeedShatterEffect
  );

  // Particle Builders
  private static final ParticleBuilder soulBuilder = Particle.SOUL.builder();
  private static final ParticleBuilder blockBuilder = Particle.BLOCK.builder();
  private static final ParticleBuilder skulkSoulBuilder = Particle.SCULK_SOUL.builder();
  private static final ParticleBuilder soulFireBuilder = Particle.SOUL_FIRE_FLAME.builder();
  private static final ParticleBuilder smokeBuilder = Particle.SMOKE.builder();
  private static final ParticleBuilder ominousBuilder = Particle.TRIAL_SPAWNER_DETECTION_OMINOUS.builder();

  public SoulDeath(PaperMCToy plugin) {
    this.plugin = plugin;
    r = new Random();
    r.setSeed(LocalDateTime.now().getDayOfMonth());
  }

  @EventHandler
  public void onDeath(EntityDeathEvent e) {
    dfx.get(r.nextInt(dfx.size())).accept(e.getEntity().getLocation(), plugin);
  }

  public static LiteralArgumentBuilder<CommandSourceStack> createCommand(PaperMCToy plugin) {
    return Commands.literal("souldeath")
      .requires(src -> src.getSender() instanceof Player)
      .then(Commands.literal("harvest")
        .executes(ctx -> dispatchCommand(ctx, SoulDeath::playSoulHarvestEffect, plugin))
      )
      .then(Commands.literal("quicksand")
        .executes(ctx -> dispatchCommand(ctx, SoulDeath::playSoulQuicksandEffect, plugin))
      )
      .then(Commands.literal("wisp")
        .executes(ctx -> dispatchCommand(ctx, SoulDeath::playTetheredWispEffect, plugin))
      )
      .then(Commands.literal("shatter")
        .executes(ctx -> dispatchCommand(ctx, SoulDeath::playSoulSpeedShatterEffect, plugin))
      );
  }

  // Precondition: ctx.getSource().getExecutor() instanceof Player
  private static int dispatchCommand(CommandContext<CommandSourceStack> ctx, BiConsumer<Location, PaperMCToy> deatheffect, PaperMCToy plugin) {
    Player p = (Player) ctx.getSource().getExecutor();
    Block b = p.getTargetBlockExact(128);
    Location l;
    if (b != null) l = b.getLocation();
    else l = p.getLocation().add(p.getLocation().getDirection().setY(0).normalize().multiply(3));
    deatheffect.accept(l, plugin);
    return Command.SINGLE_SUCCESS;
  }

  private static void playSoulHarvestEffect(Location origin, PaperMCToy plugin) {
    World world = origin.getWorld();
    if (world == null) return;

    // Stage 0: Initial impact sound & Soul Sand ground shatter
    bcSound(Sound.sound(Key.key("particle.soul.escape"), Sound.Source.MASTER, 2f, .7f), origin);
    bcSound(Sound.sound(Key.key("block.soul_sand.break"), Sound.Source.MASTER, 1.5f, .8f), origin);
    blockBuilder.location(origin.add(0, .2, 0))
      .offset(.4, .1, .4)
      .count(40).speed(.1)
      .data(soulSandData)
      .receivers(32)
      .spawn();

    new BukkitRunnable() {
      int ticks = 0;
      final double radius = 0.9;

      @Override
      public void run() {
        // Phase 1 (Ticks 0 - 30): Double ascending soul helix
        if (ticks <= 30) {
          double y = ticks * 0.09;
          double angle = ticks * 0.45;
          double currentRadius = radius * (1.0 - (ticks / 35.0)); // Tapers inward as it rises

          for (int arm = 0; arm < 2; arm++) {
            double armAngle = angle + (arm * Math.PI);
            double x = Math.cos(armAngle) * currentRadius;
            double z = Math.sin(armAngle) * currentRadius;

            Location point = origin.clone().add(x, y, z);
            // Cyan flame trail defining the spiral
            soulFireBuilder.location(point)
              .offset(0, 0, 0)
              .count(1).speed(.01)
              .receivers(32)
              .spawn();

            // Spawn actual floating SOUL particles every 3 ticks with upward velocity (count = 0)
            if (ticks % 3 == 0) {
              soulBuilder.location(point)
                .offset(0, .15, 0)
                .count(0).speed(1.0)
                .receivers(32)
                .spawn();
            }
          }
        }

        // Phase 2 (Tick 31): Apex implosion & wail
        if (ticks == 31) {
          Location apex = origin.clone().add(0, 2.7, 0);
          bcSound(Sound.sound(Key.key("entity.warden.sonic_charge"), Sound.Source.MASTER, 1.2f, 1.8f), apex);
          bcSound(Sound.sound(Key.key("entity.vex.death"), Sound.Source.MASTER, 1.5f, .6f), apex);

          // Shoot souls outward in a ring using count = 0 directional vectors
          for (int i = 0; i < 16; i++) {
            double theta = (2 * Math.PI * i) / 16;
            double vx = Math.cos(theta);
            double vz = Math.sin(theta);
            soulBuilder.location(apex)
              .offset(vx, .05, vz)
              .count(0).speed(.12)
              .receivers(32)
              .spawn();
            skulkSoulBuilder.location(apex)
              .offset(vx * .5, .1, vz * .5)
              .count(0).speed(.08)
              .receivers(32)
              .spawn();
          }
          this.cancel();
        }
        ticks++;
      }
    }.runTaskTimer(plugin, 0L, 1L);
  }

  /**
   * Idea A: "Soul Quicksand" (Drag-Down Effect)
   * Spawns a boiling disc of Soul Sand dust on the floor while actively pulling
   * Soul particles downward into the ground.
   */
  private static void playSoulQuicksandEffect(Location origin, PaperMCToy plugin) {
    World world = origin.getWorld();
    if (world == null) return;

    new BukkitRunnable() {
      int ticks = 0;

      @Override
      public void run() {
        if (ticks > 40) {
          bcSound(Sound.sound(Key.key("entity.warden.dig"), Sound.Source.MASTER, 1.2f, .7f), origin);
          this.cancel();
          return;
        }

        // 1. Ground Layer: Boiling 1.2-block disc of Soul Sand dust & smoke
        for (int i = 0; i < 6; i++) {
          double angle = ThreadLocalRandom.current().nextDouble(0, 2 * Math.PI);
          double r = Math.sqrt(ThreadLocalRandom.current().nextDouble(0, 1.0)) * 1.2;
          Location floorPoint = origin.clone().add(Math.cos(angle) * r, 0.08, Math.sin(angle) * r);

          blockBuilder.location(floorPoint)
            .offset(.05, .02, .05)
            .count(2).speed(.02)
            .data(soulSandData)
            .receivers(32)
            .spawn();
          smokeBuilder.location(floorPoint)
            .offset(.02, .02, .02)
            .count(1).speed(.01)
            .receivers(32)
            .spawn();
        }

        // 2. The Pull: Spawn SOUL faces at chest/waist height with negative Y velocity (count = 0)
        if (ticks % 2 == 0) {
          for (int i = 0; i < 3; i++) {
            double angle = ThreadLocalRandom.current().nextDouble(0, 2 * Math.PI);
            double r = ThreadLocalRandom.current().nextDouble(0.1, 0.7);
            double startY = ThreadLocalRandom.current().nextDouble(0.6, 1.5);
            Location soulStart = origin.clone().add(Math.cos(angle) * r, startY, Math.sin(angle) * r);

            // count = 0 turns (0, -0.12, 0) into a downward velocity vector
            soulBuilder.location(soulStart)
              .offset(0, -.12, 0)
              .count(0).speed(1.0).receivers(32)
              .spawn();
            skulkSoulBuilder.location(soulStart)
              .offset(0, -.18, 0)
              .count(0).speed(1.0).receivers(32)
              .spawn();
          }
        }

        // 3. Audio: Pitch drops from 1.0f down to 0.5f as the player's soul sinks
        if (ticks % 4 == 0) {
          float pitch = 1.0f - (ticks / 80.0f);
          bcSound(Sound.sound(Key.key("block.soul_sand.step"), Sound.Source.MASTER, 1.5f, pitch), origin);
          bcSound(Sound.sound(Key.key("particle.soul.escape"), Sound.Source.MASTER, .8f, pitch), origin);
        }

        ticks++;
      }
    }.runTaskTimer(plugin, 0L, 1L);
  }

  /**
   * Idea B: "The Tethered Wisp" (Heartburst)
   * Charges a tight cyan flame core at chest height for 15 ticks, then bursts outward
   * into 8 curving ordinal soul trails that arc toward the sky.
   */
  private static void playTetheredWispEffect(Location origin, PaperMCToy plugin) {
    World world = origin.getWorld();
    if (world == null) return;

    Location core = origin.clone().add(0, 1.1, 0);
    bcSound(Sound.sound(Key.key("block.respawn_anchor.charge"), Sound.Source.MASTER, 1.5f, 1.6f), core);

    new BukkitRunnable() {
      int ticks = 0;

      @Override
      public void run() {
        // Phase 1 (Ticks 0 - 14): Tight pulsing sphere hovering at chest height
        if (ticks < 15) {
          double pulseRadius = 0.25 + Math.sin(ticks * 0.6) * 0.08;
          for (int i = 0; i < 8; i++) {
            double theta = ThreadLocalRandom.current().nextDouble(0, 2 * Math.PI);
            double phi = ThreadLocalRandom.current().nextDouble(0, Math.PI);
            double x = pulseRadius * Math.sin(phi) * Math.cos(theta);
            double y = pulseRadius * Math.cos(phi);
            double z = pulseRadius * Math.sin(phi) * Math.sin(theta);

            soulFireBuilder.location(core.clone().add(x, y, z))
              .offset(0, 0, 0)
              .count(1).speed(0.0)
              .receivers(32)
              .spawn();
          }
        }

        // Phase 2 (Tick 15): Core rupture & spectral glow flash
        if (ticks == 15) {
          bcSound(Sound.sound(Key.key("particle.soul.escape"), Sound.Source.MASTER, 2.5f, 1.3f), core);
          bcSound(Sound.sound(Key.key("entity.allay.hurt"), Sound.Source.MASTER, 1.2f, .5f), core);

          ominousBuilder.location(core)
            .offset(.2, .2, .2)
            .count(25).speed(.05)
            .receivers(32)
            .spawn();
        }

        // Phase 3 (Ticks 15 - 35): 8 ordinal soul wisps curving sharply upward
        if (ticks >= 15 && ticks <= 35) {
          int burstTick = ticks - 15;
          double horizontalDist = burstTick * 0.11;
          // Parabolic Y curve: shoots out flat, then accelerates sharply upward
          double yOffset = Math.pow(burstTick * 0.14, 2.2);

          for (int i = 0; i < 8; i++) {
            double angle = i * (Math.PI / 4.0); // 8 ordinal directions (45 deg apart)
            double x = Math.cos(angle) * horizontalDist;
            double z = Math.sin(angle) * horizontalDist;
            Location wispLoc = core.clone().add(x, yOffset, z);

            skulkSoulBuilder.location(wispLoc)
              .offset(.02, .02, .02)
              .count(1).speed(0.0)
              .receivers(32)
              .spawn();
            soulFireBuilder.location(wispLoc)
              .offset(0, 0, 0)
              .count(1).speed(0.0)
              .receivers(32)
              .spawn();

            // Spawn actual upward-drifting SOUL faces along the curve tips
            if (burstTick % 4 == 0) {
              soulBuilder.location(wispLoc)
                .offset(0, .15, 0)
                .count(0).speed(1.0)
                .receivers(32)
                .spawn();
            }
          }
        }

        if (ticks > 35) {
          this.cancel();
          return;
        }
        ticks++;
      }
    }.runTaskTimer(plugin, 0L, 1L);
  }

  /**
   * Idea C: "Soul Speed Shatter" (Directional Momentum)
   * Uses the Location's facing direction (yaw) to blast a directional cone of souls
   * out of the player's hitbox while tearing up a skid-mark trail of Soul Sand along the floor.
   */
  private static void playSoulSpeedShatterEffect(Location origin, PaperMCToy plugin) {
    World world = origin.getWorld();
    if (world == null) return;

    // Extract horizontal knockback/facing direction from the Location's yaw
    Vector dir = origin.getDirection().setY(0);
    if (dir.lengthSquared() < 1e-4) {
      dir = new Vector(1, 0, 0); // Fallback if pitch was straight up/down
    }
    Vector forward = dir.normalize();

    // Initial impact burst
    bcSound(Sound.sound(Key.key("block.soul_sand.break"), Sound.Source.MASTER, 2.0f, .6f), origin);
    bcSound(Sound.sound(Key.key("particle.soul.escape"), Sound.Source.MASTER, 2.5f, .9f), origin);
    bcSound(Sound.sound(Key.key("entity.breeze.land"), Sound.Source.MASTER, 1.5f, .7f), origin);

    Location chest = origin.clone().add(0, 1.0, 0);
    ThreadLocalRandom rng = ThreadLocalRandom.current();

    // 1. Instant Directional Soul Cone: Blast 22 souls & flames in a 40-degree cone
    for (int i = 0; i < 22; i++) {
      Vector coneVec = forward.clone()
        .rotateAroundY(rng.nextDouble(-0.4, 0.4))
        .add(new Vector(0, rng.nextDouble(0.05, 0.35), 0))
        .normalize();

      double speed = rng.nextDouble(0.15, 0.42);
      soulBuilder.location(chest)
        .offset(coneVec.getX(), coneVec.getY(), coneVec.getZ())
        .count(0).speed(speed)
        .receivers(32)
        .spawn();
      soulFireBuilder.location(chest)
        .offset(coneVec.getX(), coneVec.getY(), coneVec.getZ())
        .count(0).speed(speed * 1.15)
        .receivers(32)
        .spawn();
    }

    // 2. Ground Skid Trail: Rapid 10-tick ripple of Soul Sand tearing forward along the floor
    new BukkitRunnable() {
      int ticks = 0;

      @Override
      public void run() {
        if (ticks >= 10) {
          this.cancel();
          return;
        }

        double skidDist = ticks * 0.35;
        Location skidPoint = origin.clone().add(forward.clone().multiply(skidDist)).add(0, 0.1, 0);

        // Kick up Soul Sand clods & lingering cyan flames along the skid path
        blockBuilder.location(skidPoint)
          .offset(.25, .05, .25)
          .count(12).speed(.08)
          .data(soulSandData)
          .receivers(32)
          .spawn();
        soulFireBuilder.location(skidPoint)
          .offset(.2, .05, .2)
          .count(2).speed(.02)
          .receivers(32)
          .spawn();

        bcSound(Sound.sound(Key.key("block.soul_sand.step"), Sound.Source.MASTER, 1.0f, .8f + (ticks * .04f)), skidPoint);

        ticks++;
      }
    }.runTaskTimer(plugin, 0L, 1L);
  }

  private static void bcSound(Sound s, Location l) {
    Bukkit.getOnlinePlayers().forEach(p -> p.playSound(s, l.x(), l.y(), l.z()));
  }
}
