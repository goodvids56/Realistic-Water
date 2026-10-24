package me.skaffy.port.forge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.MutableHashedLinkedMap;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

/// Forge keeps one ordered map of stack -> visibility instead of separate tab and search lists.
final class ForgeTabOutput {
	private ForgeTabOutput() {
	}

	static FabricCreativeModeTabOutput create(BuildCreativeModeTabContentsEvent event) {
		MutableHashedLinkedMap<ItemStack, CreativeModeTab.TabVisibility> entries = event.getEntries();
		return new FabricCreativeModeTabOutput(new FabricCreativeModeTabOutput.Backend() {
			@Override
			public CreativeModeTab.ItemDisplayParameters parameters() {
				return event.getParameters();
			}

			@Override
			public List<ItemStack> entries(boolean search) {
				List<ItemStack> stacks = new ArrayList<>();
				for (Map.Entry<ItemStack, CreativeModeTab.TabVisibility> entry : entries) {
					if (shows(entry.getValue(), search)) {
						stacks.add(entry.getKey());
					}
				}
				return stacks;
			}

			@Override
			public void append(boolean search, ItemStack stack) {
				if (!this.widen(search, stack)) {
					entries.put(stack, visibility(search));
				}
			}

			@Override
			public void prepend(boolean search, ItemStack stack) {
				if (!this.widen(search, stack)) {
					entries.putFirst(stack, visibility(search));
				}
			}

			@Override
			public void insertAfter(boolean search, ItemStack existing, ItemStack stack) {
				if (!this.widen(search, stack)) {
					entries.putAfter(existing, stack, visibility(search));
				}
			}

			@Override
			public void insertBefore(boolean search, ItemStack existing, ItemStack stack) {
				if (!this.widen(search, stack)) {
					entries.putBefore(existing, stack, visibility(search));
				}
			}

			// The same stack added to the other list just becomes visible in both.
			private boolean widen(boolean search, ItemStack stack) {
				CreativeModeTab.TabVisibility current = entries.get(stack);
				if (current == null) {
					return false;
				}
				if (!shows(current, search)) {
					entries.put(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
				}
				return true;
			}
		});
	}

	private static boolean shows(CreativeModeTab.TabVisibility visibility, boolean search) {
		return visibility == CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
			|| visibility == (search ? CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY : CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
	}

	private static CreativeModeTab.TabVisibility visibility(boolean search) {
		return search ? CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY : CreativeModeTab.TabVisibility.PARENT_TAB_ONLY;
	}
}
