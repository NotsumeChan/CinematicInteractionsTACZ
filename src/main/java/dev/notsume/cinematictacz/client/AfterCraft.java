package dev.notsume.cinematictacz.client;

import dev.dyslexic.cinematic.client.Scene;
import dev.notsume.cinematictacz.tacz.TaczClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * After the forge: as soon as the new gun is in the inventory (while the part still rises and the camera comes back) it
 * is drawn into the main hand, so TACZ's draw plays during the scene's outro; once the scene is over it is inspected.
 * Gives up quietly when the gun never arrives (a full inventory leaves it on the ground).
 */
final class AfterCraft {
    private AfterCraft() {}

    /** Ticks from the craft to give up: the gun never arrived, or the scene never ended. */
    private static final int ARRIVAL = 100;

    private static ItemStack expected = ItemStack.EMPTY;
    /** Which inventory slots already held such a gun when it was crafted: the new one is in another. */
    private static boolean[] had = new boolean[0];
    private static boolean waiting, drawn;
    private static int ticks;

    /** At the craft: remembers what is coming and where such guns already are, and starts watching for it. */
    static void expect(ItemStack gun) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || gun.isEmpty()) return;
        expected = gun.copy();
        Inventory inventory = player.getInventory();
        had = new boolean[inventory.items.size()];
        for (int i = 0; i < had.length; i++) had[i] = ItemStack.isSameItemSameComponents(inventory.items.get(i), expected);
        waiting = true;
        drawn = false;
        ticks = 0;
    }

    static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if ((!waiting && !drawn) || player == null || mc.gameMode == null || mc.getConnection() == null) return;
        if (++ticks > ARRIVAL) {
            waiting = drawn = false;
            return;
        }
        if (drawn) {
            // a gun picked from outside the hotbar reaches the hand only when the server's swap comes back
            if (!Scene.active() && ItemStack.isSameItemSameComponents(player.getMainHandItem(), expected)) {
                drawn = false;
                TaczClient.inspect(player);
            }
            return;
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.items.size() && i < had.length; i++) {
            if (!had[i] && ItemStack.isSameItemSameComponents(inventory.items.get(i), expected)) {
                if (Inventory.isHotbarSlot(i)) {
                    //? if >=26.1 {
                    /*inventory.setSelectedSlot(i);
                    *///?} else {
                    inventory.selected = i;
                    //?}
                    mc.getConnection().send(new ServerboundSetCarriedItemPacket(i));
                } else {
                    //? if >=26.1 {
                    /*// 26.1 has no pick by slot: swapped into the hand in the inventory, once the table's menu is closed
                    if (player.containerMenu != player.inventoryMenu) return;
                    mc.gameMode.handleContainerInput(player.inventoryMenu.containerId, i, inventory.getSelectedSlot(),
                            net.minecraft.world.inventory.ContainerInput.SWAP, player);
                    *///?} else {
                    mc.gameMode.handlePickItem(i);
                    //?}
                }
                waiting = false;
                drawn = true;
                return;
            }
        }
    }
}
