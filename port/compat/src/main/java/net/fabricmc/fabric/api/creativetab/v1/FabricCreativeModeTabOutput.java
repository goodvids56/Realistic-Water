package net.fabricmc.fabric.api.creativetab.v1;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

/// Same API as Fabric's class, applied to the loader's tab contents through a Backend.
/// Like Fabric, inserting relative to an entry that isn't in the tab appends instead.
public class FabricCreativeModeTabOutput implements CreativeModeTab.Output {
	private final Backend backend;

	public FabricCreativeModeTabOutput(Backend backend) {
		this.backend = backend;
	}

	public CreativeModeTab.ItemDisplayParameters getContext() {
		return this.backend.parameters();
	}

	public FeatureFlagSet getEnabledFeatures() {
		return this.backend.parameters().enabledFeatures();
	}

	public boolean shouldShowOpRestrictedItems() {
		return this.backend.parameters().hasPermissions();
	}

	public List<ItemStack> getDisplayStacks() {
		return this.backend.entries(false);
	}

	public List<ItemStack> getSearchTabStacks() {
		return this.backend.entries(true);
	}

	@Override
	public void accept(ItemStack stack, CreativeModeTab.TabVisibility visibility) {
		this.insert(Position.APPEND, null, List.of(stack), visibility);
	}

	public void prepend(ItemStack stack) {
		this.prepend(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
	}

	public void prepend(ItemStack stack, CreativeModeTab.TabVisibility visibility) {
		this.insert(Position.PREPEND, null, List.of(stack), visibility);
	}

	public void prepend(ItemLike item) {
		this.prepend(new ItemStack(item));
	}

	public void prepend(ItemLike item, CreativeModeTab.TabVisibility visibility) {
		this.prepend(new ItemStack(item), visibility);
	}

	public void insertAfter(ItemLike after, ItemStack... stacks) {
		this.insertAfter(after, Arrays.asList(stacks));
	}

	public void insertAfter(ItemStack after, ItemStack... stacks) {
		this.insertAfter(after, Arrays.asList(stacks));
	}

	public void insertAfter(ItemLike after, ItemLike... items) {
		this.insertAfter(after, stacks(items));
	}

	public void insertAfter(ItemStack after, ItemLike... items) {
		this.insertAfter(after, stacks(items));
	}

	public void insertAfter(ItemLike after, Collection<ItemStack> stacks) {
		this.insertAfter(after, stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
	}

	public void insertAfter(ItemStack after, Collection<ItemStack> stacks) {
		this.insertAfter(after, stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
	}

	public void insertAfter(ItemLike after, Collection<ItemStack> stacks, CreativeModeTab.TabVisibility visibility) {
		this.insertAfter(isItem(after), stacks, visibility);
	}

	public void insertAfter(ItemStack after, Collection<ItemStack> stacks, CreativeModeTab.TabVisibility visibility) {
		this.insertAfter(isStack(after), stacks, visibility);
	}

	public void insertAfter(Predicate<ItemStack> after, Collection<ItemStack> stacks, CreativeModeTab.TabVisibility visibility) {
		this.insert(Position.AFTER, after, stacks, visibility);
	}

	public void insertBefore(ItemLike before, ItemStack... stacks) {
		this.insertBefore(before, Arrays.asList(stacks));
	}

	public void insertBefore(ItemStack before, ItemStack... stacks) {
		this.insertBefore(before, Arrays.asList(stacks));
	}

	public void insertBefore(ItemLike before, ItemLike... items) {
		this.insertBefore(before, stacks(items));
	}

	public void insertBefore(ItemStack before, ItemLike... items) {
		this.insertBefore(before, stacks(items));
	}

	public void insertBefore(ItemLike before, Collection<ItemStack> stacks) {
		this.insertBefore(before, stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
	}

	public void insertBefore(ItemStack before, Collection<ItemStack> stacks) {
		this.insertBefore(before, stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
	}

	public void insertBefore(ItemLike before, Collection<ItemStack> stacks, CreativeModeTab.TabVisibility visibility) {
		this.insertBefore(isItem(before), stacks, visibility);
	}

	public void insertBefore(ItemStack before, Collection<ItemStack> stacks, CreativeModeTab.TabVisibility visibility) {
		this.insertBefore(isStack(before), stacks, visibility);
	}

	public void insertBefore(Predicate<ItemStack> before, Collection<ItemStack> stacks, CreativeModeTab.TabVisibility visibility) {
		this.insert(Position.BEFORE, before, stacks, visibility);
	}

	private void insert(Position position, @Nullable Predicate<ItemStack> anchor, Collection<ItemStack> stacks, CreativeModeTab.TabVisibility visibility) {
		if (visibility != CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY) {
			this.insertInto(false, position, anchor, stacks);
		}
		if (visibility != CreativeModeTab.TabVisibility.PARENT_TAB_ONLY) {
			this.insertInto(true, position, anchor, stacks);
		}
	}

	private void insertInto(boolean search, Position position, @Nullable Predicate<ItemStack> anchor, Collection<ItemStack> stacks) {
		ItemStack target = null;
		if (anchor != null) {
			for (ItemStack entry : this.backend.entries(search)) {
				if (anchor.test(entry)) {
					target = entry;
					break;
				}
			}
		}
		for (ItemStack stack : stacks) {
			switch (position) {
				case PREPEND -> {
					if (target == null) {
						this.backend.prepend(search, stack);
					} else {
						this.backend.insertAfter(search, target, stack);
					}
					target = stack;
				}
				case AFTER -> {
					if (target == null) {
						this.backend.append(search, stack);
					} else {
						this.backend.insertAfter(search, target, stack);
						// Keep the inserted stacks in the order they were given.
						target = stack;
					}
				}
				case BEFORE -> {
					if (target == null) {
						this.backend.append(search, stack);
					} else {
						this.backend.insertBefore(search, target, stack);
					}
				}
				case APPEND -> this.backend.append(search, stack);
			}
		}
	}

	private enum Position {
		APPEND,
		PREPEND,
		AFTER,
		BEFORE
	}

	private static Predicate<ItemStack> isItem(ItemLike item) {
		return entry -> entry.is(item.asItem());
	}

	private static Predicate<ItemStack> isStack(ItemStack stack) {
		return entry -> ItemStack.isSameItemSameComponents(entry, stack);
	}

	private static List<ItemStack> stacks(ItemLike... items) {
		return Arrays.stream(items).map(ItemStack::new).toList();
	}

	/// The loader's view of one tab's contents. `search` picks the search tab list over the tab's own list.
	public interface Backend {
		CreativeModeTab.ItemDisplayParameters parameters();

		List<ItemStack> entries(boolean search);

		void append(boolean search, ItemStack stack);

		void prepend(boolean search, ItemStack stack);

		void insertAfter(boolean search, ItemStack existing, ItemStack stack);

		void insertBefore(boolean search, ItemStack existing, ItemStack stack);
	}
}
