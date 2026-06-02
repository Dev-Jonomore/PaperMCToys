package mc.jonomore.toy.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import mc.jonomore.toy.PaperMCToy;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;

public class KeyCommands {
  public static LiteralArgumentBuilder<CommandSourceStack> createCommand(PaperMCToy plugin) {
    return Commands.literal("key")
      .then(Commands.literal("world-keys")
        .executes(ctx -> {
          ctx.getSource().getSender().sendMessage(MiniMessage.miniMessage().deserialize(
            PaperMCToy.PREFIX + "<green>--- World Names ---\n" +
              "<white>overworld: <yellow>" + plugin.getOverworld().getName() + '\n' +
              "<white>nether: <yellow>" + plugin.getNether().getName() + '\n' +
              "<white>end: <yellow>" + plugin.getEnd().getName() + "\n\n" +
              "<green>--- World Keys ---\n" +
              "<white>overworld: <yellow>" + plugin.getOverworld().key() + '\n' +
              "<white>nether: <yellow>" + plugin.getNether().key() + '\n' +
              "<white>end: <yellow>" + plugin.getEnd().key()
          ));
          return Command.SINGLE_SUCCESS;
        })
      )
      .then(Commands.literal("test")
        .executes(ctx -> {
          NamespacedKey key = new NamespacedKey("toy", "test");
          ctx.getSource().getSender().sendMessage("key is " + key.asString() +
            " or " + key.asMinimalString() +
            " or " + key.getKey());
          ctx.getSource().getSender().sendMessage("Therefore, a world name from a key is " + key.getNamespace() + "_" + key.getKey());
          return Command.SINGLE_SUCCESS;
        }));
  }
}
