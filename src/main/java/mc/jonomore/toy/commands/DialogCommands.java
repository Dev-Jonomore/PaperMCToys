package mc.jonomore.toy.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class DialogCommands {
  public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
    return Commands.literal("shop")
      .executes(DialogCommands::handleShop)
      .then(Commands.literal("head")
        .executes(ctx -> {
          if (ctx.getSource().getSender() instanceof Player player) {
            ItemStack h = ItemStack.of(Material.PLAYER_HEAD);
            SkullMeta sm = (SkullMeta) h.getItemMeta();
            sm.setEnchantmentGlintOverride(true);
            h.setItemMeta(sm);
            player.getInventory().setItemInMainHand(h);
          }
          return Command.SINGLE_SUCCESS;
        }));
  }

  private static int handleShop(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    MiniMessage mm = MiniMessage.miniMessage();
    if (!(sender instanceof Player)) {
      sender.sendMessage(mm.deserialize("""
                  <click:open_url:'https://www.ploomsy.net'><green>\
                  Visit our store to purchase the <c:#97DF01><b>DREAM</b></c> \
                  <head:Dream> rank!</green></click>"""
      ));
      return Command.SINGLE_SUCCESS;
    }

    sender.showDialog(Dialog.create(builder -> builder.empty()
      .base(DialogBase.builder(mm.deserialize("Buy the <#97DF01>DREAM<reset> rank!"))
        .body(List.of(
          DialogBody.item(
            getDreamHead(),
            null,
            true,
            false,
            25,
            25
          ),
          DialogBody.plainMessage(
            mm.deserialize("""
                <gray><b>Have bigger <light_purple>PARTIES</light_purple>,
                custom armor <light_purple>TRIMS</light_purple> in games,
                <light_purple>SAVE</light_purple> manhunts for later,
                and much more!</b>                                                                                           </gray>\
                """)
          )
        ))
        .build())
      .type(DialogType.confirmation(
        ActionButton.builder(mm.deserialize("<head:Dream> <green>Buy the <b>Dream</b> rank!</green>"))
          .tooltip(mm.deserialize("Click to get the link to our shop"))
          .action(DialogAction.customClick(
            (_, audience) -> {
              if (audience instanceof Player player) {
                player.closeDialog();
                player.sendMessage(mm.deserialize("""
                  <click:open_url:'https://www.ploomsy.net'><green>\
                  Visit our store to purchase the <c:#97DF01><b>DREAM</b></c> \
                  <head:Dream> rank!</green></click>"""
                ));
                player.playSound(Sound.sound(
                  Key.key("block.note_block.bell"),
                  Sound.Source.MASTER,
                  2.0f,
                  1.0f
                ));
              }
            },
            getClickCallbackOptions()
          ))
          .build(),
        ActionButton.builder(mm.deserialize("<sprite:'minecraft:items':item/barrier> <red>No thanks."))
          .tooltip(mm.deserialize("I'm not interested in this skibidi DREAM rank"))
          .action(DialogAction.customClick(
            (_, audience) -> audience.closeDialog(),
            getClickCallbackOptions()
          ))
          .build()
      ))
    ));

    return Command.SINGLE_SUCCESS;
  }

  private static ItemStack getDreamHead() {
    ItemStack i = ItemStack.of(Material.PLAYER_HEAD);
    ItemMeta m = i.getItemMeta();
    if (m instanceof SkullMeta sm) {
      MiniMessage mm = MiniMessage.miniMessage();
      sm.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString("ec70bcaf-702f-4bb8-b48d-276fa52a780c")));
      sm.displayName(mm.deserialize("<gold><bold>DREAM RANK</bold></gold>"));
      sm.lore(List.of(
        mm.deserialize("<green>CLICK TO BUY!</green>"),
        Component.empty(),
        mm.deserialize("<gray>Have bigger <light_purple>PARTIES</light_purple>, custom armor</gray>"),
        mm.deserialize("<gray><light_purple>TRIMS</light_purple> in games, <light_purple>SAVE</light_purple> manhunts for </gray>"),
        mm.deserialize("<gray>later and much more!</gray>")
      ));
      sm.setEnchantable(100);
      sm.setEnchantmentGlintOverride(true);
      i.setItemMeta(sm);
    }
    i.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
    return i;
  }

  private static ClickCallback.Options getClickCallbackOptions() {
    return ClickCallback.Options.builder()
      .uses(1)
      .lifetime(ClickCallback.DEFAULT_LIFETIME)
      .build();
  }
}
