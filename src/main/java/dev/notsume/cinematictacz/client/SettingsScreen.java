package dev.notsume.cinematictacz.client;

import dev.notsume.cinematictacz.CinematicTacz;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The settings, opened from Mod Menu: a heading per section and a switch per option, its tooltip saying what it does.
 * Changes are kept here until Done; Cancel (or Esc) forgets them.
 */
// ponytail: one plain column; Cinematic Interactions' tabbed, searchable list is worth copying once there are many more options
public final class SettingsScreen extends Screen {
    private static final int WIDTH = 240, ROW = 22, HEADING = 16;

    @Nullable
    private final Screen parent;
    private final Map<Settings.Option, Boolean> edits = new HashMap<>();
    private record Heading(Component text, int y) {}
    private final List<Heading> headings = new ArrayList<>();

    public SettingsScreen(@Nullable Screen parent) {
        super(Component.translatable(CinematicTacz.ID + ".configuration.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        headings.clear();
        int x = (width - WIDTH) / 2, y = 32;
        String section = null;
        for (Settings.Option option : Settings.ALL) {
            if (!option.section().equals(section)) {
                section = option.section();
                headings.add(new Heading(Component.translatable(CinematicTacz.ID + ".configuration." + section), y));
                y += HEADING;
            }
            Tooltip tooltip = Tooltip.create(Component.translatable(option.translation() + ".tooltip"));
            addRenderableWidget(CycleButton.onOffBuilder(edits.getOrDefault(option, Settings.get(option))).withTooltip(value -> tooltip)
                    .create(x, y, WIDTH, 20, Component.translatable(option.translation()), (button, value) -> edits.put(option, value)));
            y += ROW;
        }
        y += 8;
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            edits.forEach(Settings::set);
            Settings.save();
            onClose();
        }).bounds(x, y, WIDTH / 2 - 2, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds(x + WIDTH / 2 + 2, y, WIDTH / 2 - 2, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        for (Heading heading : headings) graphics.drawCenteredString(font, heading.text(), width / 2, heading.y() + 4, 0xE0C080);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
