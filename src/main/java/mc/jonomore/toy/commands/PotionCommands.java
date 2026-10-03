package mc.jonomore.toy.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PotionCommands {
  public static LiteralArgumentBuilder<CommandSourceStack> create() {
    return Commands.literal("potion")
      .then(Commands.literal("weakness")
        .executes(ctx -> givePotion(ctx, PotionEffectType.WEAKNESS))
      ).then(Commands.literal("strength")
        .executes(ctx -> givePotion(ctx, PotionEffectType.STRENGTH))
      ).then(Commands.literal("resistance")
        .executes(ctx -> givePotion(ctx, PotionEffectType.RESISTANCE))
      ).then(Commands.literal("speed")
        .executes(ctx -> givePotion(ctx, PotionEffectType.SPEED))
      ).then(Commands.literal("blindness")
        .executes(ctx -> givePotion(ctx, PotionEffectType.BLINDNESS))
      );
  }

  private static int givePotion(CommandContext<CommandSourceStack> ctx, PotionEffectType type) {
    Entity executor = ctx.getSource().getExecutor();
    if (executor instanceof Player player) {
      if (player.hasPotionEffect(type)) player.removePotionEffect(type);
      else player.addPotionEffect(new PotionEffect(
        type,
        PotionEffect.INFINITE_DURATION,
        255,
        false,
        false,
        false
      ));
    }
    return Command.SINGLE_SUCCESS;
  }
}
