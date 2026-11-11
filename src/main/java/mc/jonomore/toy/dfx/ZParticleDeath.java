package mc.jonomore.toy.dfx;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Quaternion4f;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import mc.jonomore.toy.PaperMCToy;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public final class ZParticleDeath implements Listener {
  private final PaperMCToy plugin;
  static final List<BiConsumer<Location, PaperMCToy>> dfx = List.of(
    ZParticleDeath::playCrystalEncasementEffect,
    ZParticleDeath::playOrbitalLanternsEffect,
    ZParticleDeath::playEnderSignalShatterEffect,
    ZParticleDeath::playGroundCorruptionEffect,
    ZParticleDeath::playLightningSilhouetteEffect
  );

  public ZParticleDeath(PaperMCToy plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onDeath(EntityDeathEvent e) {
    dfx.get(ThreadLocalRandom.current().nextInt(dfx.size())).accept(e.getEntity().getLocation(), plugin);
  }

  public static LiteralArgumentBuilder<CommandSourceStack> createCommand(PaperMCToy plugin) {
    return Commands.literal("zpdeath")
      .requires(src -> src.getSender() instanceof Player)
      .then(Commands.literal("crystal")
        .executes(ctx -> dispatchCommand(ctx, ZParticleDeath::playCrystalEncasementEffect, plugin))
      )
      .then(Commands.literal("lanterns")
        .executes(ctx -> dispatchCommand(ctx, ZParticleDeath::playOrbitalLanternsEffect, plugin))
      )
      .then(Commands.literal("endereye")
        .executes(ctx -> dispatchCommand(ctx, ZParticleDeath::playEnderSignalShatterEffect, plugin))
      )
      .then(Commands.literal("corruption")
        .executes(ctx -> dispatchCommand(ctx, ZParticleDeath::playGroundCorruptionEffect, plugin))
      )
      .then(Commands.literal("silhouette")
        .executes(ctx -> dispatchCommand(ctx, ZParticleDeath::playLightningSilhouetteEffect, plugin))
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

  // High virtual entity ID generator so fake entities never collide with real Bukkit entities
  private static final AtomicInteger VIRTUAL_EID = new AtomicInteger(1_500_000_000);

  // 1.20.2 - 1.21+ Display Entity Metadata Indices
  private static final int IDX_ENTITY_FLAGS = 0;
  private static final int IDX_INTERP_DELAY = 8;
  private static final int IDX_TRANSFORM_DURATION = 9;
  private static final int IDX_TELEPORT_DURATION = 10;
  private static final int IDX_TRANSLATION = 11;
  private static final int IDX_SCALE = 12;
  private static final int IDX_LEFT_ROTATION = 13;
  private static final int IDX_BRIGHTNESS = 16;
  private static final int IDX_BLOCK_STATE = 23; // BlockDisplay block state index (1.20.2+)
  private static final int IDX_EYE_ITEM = 8;     // EyeOfEnder item stack index

  // Pre-cached PacketEvents block state IDs
  private static final int AMETHYST_STATE = WrappedBlockState.getDefaultState(StateTypes.AMETHYST_CLUSTER).getGlobalId();
  private static final int SOUL_LANTERN_STATE = WrappedBlockState.getDefaultState(StateTypes.SOUL_LANTERN).getGlobalId();
  private static final int CRYING_OBSIDIAN_STATE = WrappedBlockState.getDefaultState(StateTypes.CRYING_OBSIDIAN).getGlobalId();
  private static final int SCULK_STATE = WrappedBlockState.getDefaultState(StateTypes.SCULK).getGlobalId();
  private static final int FULL_BRIGHT = (15 << 4) | (15 << 20);

  /**
   * 1. "3D Crystal Encasement" (PacketEvents Virtual BlockDisplay Interpolation)
   * Spawns 3 virtual Amethyst Cluster BlockDisplays rotated at slight angles,
   * smoothly scales them up out of the floor, then shrinks and destroys them.
   */
  public static void playCrystalEncasementEffect(Location origin, PaperMCToy plugin) {
    World world = origin.getWorld();
    if (world == null) return;

    int[] eids = {
      VIRTUAL_EID.getAndIncrement(),
      VIRTUAL_EID.getAndIncrement(),
      VIRTUAL_EID.getAndIncrement()
    };

    // Slight tilted quaternions so the 3 crystal clusters jut out at intersecting angles
    Quaternion4f[] rotations = {
      new Quaternion4f(0.15f, 0.0f, 0.12f, 0.98f),
      new Quaternion4f(-0.14f, 0.70f, -0.10f, 0.69f),
      new Quaternion4f(0.10f, -0.65f, 0.16f, 0.73f)
    };

    Vector3d spawnPos = new Vector3d(origin.getX(), origin.getY(), origin.getZ());

    // Tick 0: Spawn 3 virtual BlockDisplays at scale (0, 0, 0)
    for (int i = 0; i < 3; i++) {
      WrapperPlayServerSpawnEntity spawnPkt = new WrapperPlayServerSpawnEntity(
        eids[i], Optional.of(UUID.randomUUID()), EntityTypes.BLOCK_DISPLAY,
        spawnPos, 0f, 0f, 0f, 0, Optional.empty()
      );
      List<EntityData<?>> initMeta = List.of(
        new EntityData<>(IDX_BLOCK_STATE, EntityDataTypes.BLOCK_STATE, AMETHYST_STATE),
        new EntityData<>(IDX_BRIGHTNESS, EntityDataTypes.INT, FULL_BRIGHT),
        new EntityData<>(IDX_TRANSLATION, EntityDataTypes.VECTOR3F, new Vector3f(-0.5f, 0f, -0.5f)),
        new EntityData<>(IDX_SCALE, EntityDataTypes.VECTOR3F, new Vector3f(0f, 0f, 0f)),
        new EntityData<>(IDX_LEFT_ROTATION, EntityDataTypes.QUATERNION, rotations[i])
      );
      bcPacket(spawnPkt, origin);
      bcPacket(new WrapperPlayServerEntityMetadata(eids[i], initMeta), origin);
    }

    bcSound(Sound.sound(Key.key("block.amethyst_cluster.place"), Sound.Source.MASTER, 1.8f, .6f), origin);

    new BukkitRunnable() {
      int ticks = 0;

      @Override
      public void run() {
        // Tick 1: Trigger 8-tick client-side scale interpolation upward
        if (ticks == 1) {
          for (int eid : eids) {
            List<EntityData<?>> growMeta = List.of(
              new EntityData<>(IDX_INTERP_DELAY, EntityDataTypes.INT, 0),
              new EntityData<>(IDX_TRANSFORM_DURATION, EntityDataTypes.INT, 8),
              new EntityData<>(IDX_SCALE, EntityDataTypes.VECTOR3F, new Vector3f(1.3f, 1.9f, 1.3f))
            );
            bcPacket(new WrapperPlayServerEntityMetadata(eid, growMeta), origin);
          }
        }

        // Tick 26: Collapse crystals back into the floor over 6 ticks
        if (ticks == 26) {
          bcSound(Sound.sound(Key.key("block.amethyst_cluster.break"), Sound.Source.MASTER, 2.0f, .7f), origin);
          for (int eid : eids) {
            List<EntityData<?>> shrinkMeta = List.of(
              new EntityData<>(IDX_INTERP_DELAY, EntityDataTypes.INT, 0),
              new EntityData<>(IDX_TRANSFORM_DURATION, EntityDataTypes.INT, 6),
              new EntityData<>(IDX_SCALE, EntityDataTypes.VECTOR3F, new Vector3f(0f, 0f, 0f))
            );
            bcPacket(new WrapperPlayServerEntityMetadata(eid, shrinkMeta), origin);
          }
        }

        // Tick 33: Destroy virtual entity IDs on client
        if (ticks >= 33) {
          bcPacket(new WrapperPlayServerDestroyEntities(eids), origin);
          this.cancel();
          return;
        }
        ticks++;
      }
    }.runTaskTimer(plugin, 0L, 1L);
  }

  /**
   * 2. "Orbital Soul Lanterns" (PacketEvents Smooth Teleport Duration)
   * Spawns 3 fullbright virtual Soul Lantern BlockDisplays that orbit inward
   * and sink into the ground using 2-tick client teleport interpolation.
   */
  public static void playOrbitalLanternsEffect(Location origin, PaperMCToy plugin) {
    World world = origin.getWorld();
    if (world == null) return;

    int[] eids = {
      VIRTUAL_EID.getAndIncrement(),
      VIRTUAL_EID.getAndIncrement(),
      VIRTUAL_EID.getAndIncrement()
    };

    for (int i = 0; i < 3; i++) {
      double angle = i * (2 * Math.PI / 3);
      Vector3d startPos = new Vector3d(
        origin.getX() + Math.cos(angle) * 1.2,
        origin.getY() + 1.4,
        origin.getZ() + Math.sin(angle) * 1.2
      );

      bcPacket(new WrapperPlayServerSpawnEntity(
        eids[i], Optional.of(UUID.randomUUID()), EntityTypes.BLOCK_DISPLAY,
        startPos, 0f, 0f, 0f, 0, Optional.empty()
      ), origin);

      List<EntityData<?>> meta = List.of(
        new EntityData<>(IDX_BLOCK_STATE, EntityDataTypes.BLOCK_STATE, SOUL_LANTERN_STATE),
        new EntityData<>(IDX_BRIGHTNESS, EntityDataTypes.INT, FULL_BRIGHT),
        new EntityData<>(IDX_TELEPORT_DURATION, EntityDataTypes.INT, 2),
        new EntityData<>(IDX_TRANSLATION, EntityDataTypes.VECTOR3F, new Vector3f(-0.3f, 0f, -0.3f)),
        new EntityData<>(IDX_SCALE, EntityDataTypes.VECTOR3F, new Vector3f(0.65f, 0.65f, 0.65f))
      );
      bcPacket(new WrapperPlayServerEntityMetadata(eids[i], meta), origin);
    }

    bcSound(Sound.sound(Key.key("block.soul_lantern.place"), Sound.Source.MASTER, 1.6f, .8f), origin);

    new BukkitRunnable() {
      int ticks = 0;

      @Override
      public void run() {
        if (ticks > 28) {
          bcSound(Sound.sound(Key.key("block.beacon.deactivate"), Sound.Source.MASTER, 1.4f, 1.6f), origin);
          bcPacket(new WrapperPlayServerDestroyEntities(eids), origin);
          this.cancel();
          return;
        }

        double progress = ticks / 28.0;
        double radius = 1.2 * (1.0 - progress);
        double yOffset = 1.4 * (1.0 - progress);

        for (int i = 0; i < 3; i++) {
          double angle = (ticks * 0.38) + (i * (2 * Math.PI / 3));
          Vector3d nextPos = new Vector3d(
            origin.getX() + Math.cos(angle) * radius,
            origin.getY() + yOffset,
            origin.getZ() + Math.sin(angle) * radius
          );

          bcPacket(new WrapperPlayServerEntityTeleport(eids[i], nextPos, (float) (ticks * 18), 0f, false), origin);
        }

        ticks++;
      }
    }.runTaskTimer(plugin, 0L, 1L);
  }

  /**
   * 3. "Ender Eye Shatter" (Virtual EYE_OF_ENDER + Status Byte 3)
   * Spawns a client-side Eye of Ender disguised as a Soul Lantern, floats it
   * upward, and triggers Mojang's native EntityStatus 3 shatter animation.
   */
  public static void playEnderSignalShatterEffect(Location origin, PaperMCToy plugin) {
    World world = origin.getWorld();
    if (world == null) return;

    int eid = VIRTUAL_EID.getAndIncrement();
    Vector3d pos = new Vector3d(origin.getX(), origin.getY() + 0.8, origin.getZ());

    bcPacket(new WrapperPlayServerSpawnEntity(
      eid, Optional.of(UUID.randomUUID()), EntityTypes.EYE_OF_ENDER,
      pos, 0f, 0f, 0f, 0, Optional.empty()
    ), origin);

    // Disguise the Eye of Ender sprite as a Soul Lantern (or swap to ItemTypes.ECHO_SHARD / PLAYER_HEAD)
    ItemStack customSprite = ItemStack.builder().type(ItemTypes.SOUL_LANTERN).amount(1).build();
    List<EntityData<?>> meta = List.of(
      new EntityData<>(IDX_EYE_ITEM, EntityDataTypes.ITEMSTACK, customSprite)
    );
    bcPacket(new WrapperPlayServerEntityMetadata(eid, meta), origin);
    bcSound(Sound.sound(Key.key("entity.ender_eye.launch"), Sound.Source.MASTER, 1.5f, .7f), origin);

    new BukkitRunnable() {
      int ticks = 0;

      @Override
      public void run() {
        if (ticks < 22) {
          // Smooth easing upward curve
          double y = 0.8 + (2.2 * (1.0 - Math.pow(1.0 - (ticks / 22.0), 2)));
          Vector3d newPos = new Vector3d(origin.getX(), origin.getY() + y, origin.getZ());
          bcPacket(new WrapperPlayServerEntityTeleport(eid, newPos, 0f, 0f, false), origin);
        }

        if (ticks == 22) {
          Location apex = origin.clone().add(0, 3.0, 0);
          // Status 3 triggers the native Eye of Ender shatter burst on the client!
          bcPacket(new WrapperPlayServerEntityStatus(eid, 3), apex);
          bcSound(Sound.sound(Key.key("entity.ender_eye.death"), Sound.Source.MASTER, 2.0f, .6f), apex);
        }

        if (ticks >= 25) {
          bcPacket(new WrapperPlayServerDestroyEntities(eid), origin);
          this.cancel();
          return;
        }
        ticks++;
      }
    }.runTaskTimer(plugin, 0L, 1L);
  }

  /**
   * 4. "Localized Ground Corruption" (WrapperPlayServerBlockChange)
   * Corrupts a 3x3 patch of solid floor under the player into Crying Obsidian & Sculk
   * via raw block change packets, then heals them from the outside in.
   */
  public static void playGroundCorruptionEffect(Location origin, PaperMCToy plugin) {
    World world = origin.getWorld();
    if (world == null) return;

    List<Block> affectedBlocks = new ArrayList<>();
    ThreadLocalRandom rng = ThreadLocalRandom.current();

    for (int dx = -1; dx <= 1; dx++) {
      for (int dz = -1; dz <= 1; dz++) {
        Block floor = origin.clone().add(dx, -1, dz).getBlock();
        if (floor.getType().isSolid()) {
          affectedBlocks.add(floor);
          int fakeState = (dx == 0 && dz == 0)
            ? CRYING_OBSIDIAN_STATE
            : (rng.nextBoolean() ? SCULK_STATE : CRYING_OBSIDIAN_STATE);

          Vector3i blockPos = new Vector3i(floor.getX(), floor.getY(), floor.getZ());
          bcPacket(new WrapperPlayServerBlockChange(blockPos, fakeState), origin);
        }
      }
    }

    bcSound(Sound.sound(Key.key("block.sculk_catalyst.bloom"), Sound.Source.MASTER, 2.0f, .7f), origin);

    // Revert blocks one by one starting after 25 ticks
    new BukkitRunnable() {
      int index = 0;

      @Override
      public void run() {
        if (index >= affectedBlocks.size()) {
          this.cancel();
          return;
        }

        Block b = affectedBlocks.get(index++);
        int realStateId = SpigotConversionUtil.fromBukkitBlockData(b.getBlockData()).getGlobalId();
        Vector3i blockPos = new Vector3i(b.getX(), b.getY(), b.getZ());

        bcPacket(new WrapperPlayServerBlockChange(blockPos, realStateId), origin);
        bcSound(Sound.sound(Key.key("block.sculk.place"), Sound.Source.MASTER, .8f, 1.2f), b.getLocation());
      }
    }.runTaskTimer(plugin, 25L, 3L);
  }

  /**
   * 5. "Silent Lightning Silhouette" (Packet Lightning + Glowing Equipment Stand)
   * Flashes a silent packet lightning bolt while sinking an invisible, glowing
   * virtual ArmorStand wearing a Wither Skeleton Skull into the floor.
   */
  public static void playLightningSilhouetteEffect(Location origin, PaperMCToy plugin) {
    World world = origin.getWorld();
    if (world == null) return;

    int boltEid = VIRTUAL_EID.getAndIncrement();
    int standEid = VIRTUAL_EID.getAndIncrement();
    Vector3d pos = new Vector3d(origin.getX(), origin.getY(), origin.getZ());

    // 1. Spawn silent client-side Lightning Bolt
    bcPacket(new WrapperPlayServerSpawnEntity(
      boltEid, Optional.of(UUID.randomUUID()), EntityTypes.LIGHTNING_BOLT,
      pos, 0f, 0f, 0f, 0, Optional.empty()
    ), origin);
    bcSound(Sound.sound(Key.key("entity.illusioner.mirror_move"), Sound.Source.MASTER, 2.0f, .6f), origin);

    // 2. Spawn Invisible + Glowing (0x20 | 0x40 = 0x60) virtual ArmorStand
    bcPacket(new WrapperPlayServerSpawnEntity(
      standEid, Optional.of(UUID.randomUUID()), EntityTypes.ARMOR_STAND,
      pos, origin.getYaw(), 0f, origin.getYaw(), 0, Optional.empty()
    ), origin);

    List<EntityData<?>> standMeta = List.of(
      new EntityData<>(IDX_ENTITY_FLAGS, EntityDataTypes.BYTE, (byte) (0x20 | 0x40)) // Invisible + Glowing outline
    );
    bcPacket(new WrapperPlayServerEntityMetadata(standEid, standMeta), origin);

    // Equip a Wither Skull + Netherite Chestplate so the glowing outline forms a stark silhouette
    List<Equipment> gear = List.of(
      new Equipment(EquipmentSlot.HELMET, ItemStack.builder().type(ItemTypes.WITHER_SKELETON_SKULL).amount(1).build()),
      new Equipment(EquipmentSlot.CHEST_PLATE, ItemStack.builder().type(ItemTypes.NETHERITE_CHESTPLATE).amount(1).build())
    );
    bcPacket(new WrapperPlayServerEntityEquipment(standEid, gear), origin);

    new BukkitRunnable() {
      int ticks = 0;

      @Override
      public void run() {
        if (ticks == 12) {
          bcPacket(new WrapperPlayServerDestroyEntities(boltEid), origin);
        }

        if (ticks > 20) {
          bcPacket(new WrapperPlayServerDestroyEntities(standEid), origin);
          this.cancel();
          return;
        }

        // Sink the glowing silhouette smoothly into the floor
        Vector3d sinkPos = new Vector3d(origin.getX(), origin.getY() - (ticks * 0.09), origin.getZ());
        bcPacket(new WrapperPlayServerEntityTeleport(standEid, sinkPos, origin.getYaw(), 0f, false), origin);
        ticks++;
      }
    }.runTaskTimer(plugin, 0L, 1L);
  }

  // --- Packet & Sound Broadcast Helpers ---

  private static void bcPacket(PacketWrapper<?> packet, Location origin) {
    World world = origin.getWorld();
    if (world == null) return;
    for (Player player : world.getPlayers()) {
      if (player.getLocation().distanceSquared(origin) <= 32 * 32) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
      }
    }
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
