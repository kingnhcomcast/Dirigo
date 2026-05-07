package io.drahlek.dirigo.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public abstract class DirigoContainerMenu extends AbstractContainerMenu {
    protected final Container container;

    protected DirigoContainerMenu(MenuType<?> menuType, int containerId, Container container, int expectedContainerSize) {
        super(menuType, containerId);
        checkContainerSize(container, expectedContainerSize);
        this.container = container;
    }

    protected void addContainerRowSlots(int slotCount, int x, int y) {
        for (int slot = 0; slot < slotCount; slot++) {
            this.addSlot(new Slot(this.container, slot, x + slot * 18, y));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();

        if (slotIndex < this.container.getContainerSize()) {
            if (!this.moveItemStackTo(stack, this.container.getContainerSize(), this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, 0, this.container.getContainerSize(), false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return clicked;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}
