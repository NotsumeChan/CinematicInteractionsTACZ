package dev.notsume.cinematictacz.client;

import dev.dyslexic.cinematic.client.Scene;
import dev.notsume.cinematictacz.tacz.TaczClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * After the forge: once the scene has closed and the new gun is in the inventory, it is drawn into the main hand (TACZ
 * plays its draw) and then inspected. Gives up quietly when the gun never arrives (a full inventory leaves it on the
 * ground).
 */
final class AfterCraft {
    private AfterCraft() {}

    /** Ticks to wait for the scene to close and the gun to be picked up; for TACZ's draw animation before inspecting. */
    private static final int ARRIVAL = 100, DRAW = 25;

    private static ItemStack expected = ItemStack.EMPTY;
    /** Which inventory slots already held such a gun when it was crafted: the new one is in another. */
    private static boolean[] had = new boolean[0];
    private static boolean waiting, drawing;
    private static int ticks;

    /** At the craft: remembers what is coming and where such guns already are. */
    static void expect(ItemStack gun) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        expected = gun.copy();
        Inventory inventory = player.getInventory();
        had = new boolean[inventory.items.size()];
        for (int i = 0; i < had.length; i++) had[i] = ItemStack.isSameItemSameComponents(inventory.items.get(i), expected);
    }

    /** The scene is closing: draw the gun once it is in. */
    static void go() {
        waiting = !expected.isEmpty();
        drawing = false;
        ticks = 0;
    }

    static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if ((!waiting && !drawing) || player == null || mc.gameMode == null || mc.getConnection() == null) return;
        ticks++;
        if (drawing) {
            if (ticks >= DRAW) {
                drawing = false;
                TaczClient.inspect(player);
            }
            return;
        }
        if (ticks > ARRIVAL) {
            waiting = false;
            return;
        }
        if (Scene.active()) return;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.items.size() && i < had.length; i++) {
            if (!had[i] && ItemStack.isSameItemSameComponents(inventory.items.get(i), expected)) {
                if (Inventory.isHotbarSlot(i)) {
                    inventory.selected = i;
                    mc.getConnection().send(new ServerboundSetCarriedItemPacket(i));
                } else {
                    mc.gameMode.handlePickItem(i);
                }
                waiting = false;
                drawing = true;
                ticks = 0;
                return;
            }
        }
    }
}
