package me.skaffy.port.neoforge;

import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

final class NeoForgeTabOutput {
	private NeoForgeTabOutput() {
	}

	static FabricCreativeModeTabOutput create(BuildCreativeModeTabContentsEvent event) {
		return new FabricCreativeModeTabOutput(new FabricCreativeModeTabOutput.Backend() {
			@Override
			public CreativeModeTab.ItemDisplayParameters parameters() {
				return event.getParameters();
			}

			@Override
			public List<ItemStack> entries(boolean search) {
				return List.copyOf(search ? event.getSearchEntries() : event.getParentEntries());
			}

			@Override
			public void append(boolean search, ItemStack stack) {
				if (!this.contains(search, stack)) {
					event.accept(stack, visibility(search));
				}
			}

			@Override
			public void prepend(boolean search, ItemStack stack) {
				if (!this.contains(search, stack)) {
					event.insertFirst(stack, visibility(search));
				}
			}

			@Override
			public void insertAfter(boolean search, ItemStack existing, ItemStack stack) {
				if (!this.contains(search, stack)) {
					event.insertAfter(existing, stack, visibility(search));
				}
			}

			@Override
			public void insertBefore(boolean search, ItemStack existing, ItemStack stack) {
				if (!this.contains(search, stack)) {
					event.insertBefore(existing, stack, visibility(search));
				}
			}

			// NeoForge throws on duplicates; Fabric doesn't, so skip them instead.
			private boolean contains(boolean search, ItemStack stack) {
				return (search ? event.getSearchEntries() : event.getParentEntries()).contains(stack);
			}
		});
	}

	private static CreativeModeTab.TabVisibility visibility(boolean search) {
		return search ? CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY : CreativeModeTab.TabVisibility.PARENT_TAB_ONLY;
	}
}
