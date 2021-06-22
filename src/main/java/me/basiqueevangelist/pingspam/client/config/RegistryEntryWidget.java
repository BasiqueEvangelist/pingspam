package me.basiqueevangelist.pingspam.client.config;

import dev.inkwell.conrad.api.gui.ValueWidgetFactory;
import dev.inkwell.vivian.api.screen.ConfigScreen;
import dev.inkwell.vivian.api.widgets.value.DropdownWidgetComponent;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class RegistryEntryWidget extends DropdownWidgetComponent<Identifier> {
    public static final ValueWidgetFactory<Identifier> SOUND_EVENT =
        (screen, x, y, width, name, config, constraints, data, defaultValueSupplier, changedListener, saveConsumer, value)
            -> new RegistryEntryWidget(screen, x, y, width, 20, defaultValueSupplier, changedListener, saveConsumer, value, Registry.SOUND_EVENT);

    public RegistryEntryWidget(ConfigScreen parent, int x, int y, int width, int height, Supplier<@NotNull Identifier> defaultValueSupplier, Consumer<Identifier> changedListener, Consumer<Identifier> saveConsumer, @NotNull Identifier value, Registry<?> registry) {
        super(parent, x, y, width, height, defaultValueSupplier, changedListener, saveConsumer, value, registry.getIds().toArray(new Identifier[0]));
    }

    @Override
    protected MutableText fromValue(Identifier value) {
        return new LiteralText(value.toString());
    }

    @Override
    protected @Nullable Text getDefaultValueAsText() {
        return fromValue(getDefaultValue());
    }
}
