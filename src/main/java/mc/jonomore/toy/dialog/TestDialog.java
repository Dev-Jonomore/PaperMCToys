package mc.jonomore.toy.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.keys.DialogKeys;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class TestDialog {

  public static TypedKey<Dialog> getDialogKey() {
    return DialogKeys.create(Key.key("toy:multi_action"));
  }

  public static DialogRegistryEntry.Builder multiAction(DialogRegistryEntry.Builder builder) {
    return builder
      .base(DialogBase.builder(mm("<b>Test Dialog</b>"))
        .inputs(List.of(
          DialogInput.bool("default", mm("Default settings")).initial(true).build()
        ))
        .canCloseWithEscape(true)
        .body(List.of(
          DialogBody.plainMessage(mm("<green>Pick an option, any option!"))
        ))
        .build()
      )
      .type(DialogType.multiAction(List.of(
            ActionButton.builder(mm("Open me!"))
              .tooltip(mm("A tooltip"))
              .action(DialogAction.customClick((view, audience) -> {
                  audience.showTitle(Title.title(mm("<green>Ploomsy is a Grape!"), Component.empty()));
                },
                getDefaultOptions()
              ))
              .build(),
            ActionButton.builder(mm("Don't open me!"))
              .tooltip(mm("Some other tooltip"))
              .action(DialogAction.customClick((view, audience) -> {
                  audience.sendActionBar(mm("<green>Pick an option, any option!"));
                },
                getDefaultOptions()
              )).build()
          ))
          .columns(2)
          .build()
      );
  }

  private static ClickCallback.Options getDefaultOptions() {
    return ClickCallback.Options.builder()
      .uses(1)
      .lifetime(ClickCallback.DEFAULT_LIFETIME)
      .build();
  }

  private static Component mm(String s) {
    return MiniMessage.miniMessage().deserialize(s);
  }
}
