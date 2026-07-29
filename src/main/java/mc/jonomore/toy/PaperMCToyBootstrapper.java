package mc.jonomore.toy;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.handler.LifecycleEventHandler;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.event.RegistryComposeEvent;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.DialogKeys;
import mc.jonomore.toy.dialog.TestDialog;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;

@SuppressWarnings("UnstableApiUsage")
public class PaperMCToyBootstrapper implements PluginBootstrap {
  @Override
  public void bootstrap(@NonNull BootstrapContext context) {
    context.getLifecycleManager().registerEventHandler(RegistryEvents.DIALOG.compose()
      .newHandler(event -> event.registry().register(
        DialogKeys.create(Key.key("papermc:custom_dialog")),
        TestDialog::multiAction))
    );
  }

  private static @NonNull LifecycleEventHandler<RegistryComposeEvent<Dialog, DialogRegistryEntry.Builder>> registerDialog() {
    return event -> event.registry().register(
      DialogKeys.create(Key.key("papermc:custom_dialog")),
      buildDialog()
    );
  }

  private static @NonNull Consumer<DialogRegistryEntry.Builder> buildDialog() {
    return builder -> getBuilder(builder);
  }

  private static DialogRegistryEntry.Builder getBuilder(DialogRegistryEntry.Builder builder) {
    return builder
      .base(DialogBase.builder(Component.text("Title")).build())
      .type(DialogType.notice());
  }

}
