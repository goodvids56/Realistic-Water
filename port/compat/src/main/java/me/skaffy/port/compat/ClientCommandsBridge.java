package me.skaffy.port.compat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.IdentityHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import org.jspecify.annotations.Nullable;

/// Runs ClientCommandRegistrationCallback and copies the resulting tree onto the loader's
/// client command dispatcher, which works with CommandSourceStack instead of FabricClientCommandSource.
public final class ClientCommandsBridge {
	private static volatile @Nullable CommandDispatcher<FabricClientCommandSource> active;

	private ClientCommandsBridge() {
	}

	public static @Nullable CommandDispatcher<FabricClientCommandSource> activeDispatcher() {
		return active;
	}

	public static void register(CommandDispatcher<CommandSourceStack> target, CommandBuildContext context) {
		CommandDispatcher<FabricClientCommandSource> dispatcher = new CommandDispatcher<>();
		ClientCommandRegistrationCallback.EVENT.invoker().register(dispatcher, context);
		Map<CommandNode<FabricClientCommandSource>, CommandNode<CommandSourceStack>> converted = new IdentityHashMap<>();
		converted.put(dispatcher.getRoot(), target.getRoot());
		for (CommandNode<FabricClientCommandSource> child : dispatcher.getRoot().getChildren()) {
			target.getRoot().addChild(convert(child, converted));
		}
		active = dispatcher;
	}

	private static CommandNode<CommandSourceStack> convert(
		CommandNode<FabricClientCommandSource> node, Map<CommandNode<FabricClientCommandSource>, CommandNode<CommandSourceStack>> converted
	) {
		CommandNode<CommandSourceStack> done = converted.get(node);
		if (done != null) {
			return done;
		}

		ArgumentBuilder<CommandSourceStack, ?> builder;
		if (node instanceof LiteralCommandNode<FabricClientCommandSource> literal) {
			builder = LiteralArgumentBuilder.literal(literal.getLiteral());
		} else if (node instanceof ArgumentCommandNode<FabricClientCommandSource, ?> argument) {
			builder = argument(argument);
		} else {
			throw new IllegalArgumentException("Unsupported command node " + node);
		}

		builder.requires(stack -> node.getRequirement().test(new ClientCommandSourceAdapter(stack)));
		if (node.getCommand() != null) {
			var command = node.getCommand();
			builder.executes(context -> command.run(retarget(context)));
		}
		if (node.getRedirect() != null) {
			var modifier = node.getRedirectModifier();
			builder.forward(convert(node.getRedirect(), converted), modifier == null ? null : context -> {
				var sources = modifier.apply(retarget(context));
				return sources.stream().map(source -> ((ClientCommandSourceAdapter) source).stack()).toList();
			}, node.isFork());
		}
		for (CommandNode<FabricClientCommandSource> child : node.getChildren()) {
			builder.then(convert(child, converted));
		}
		CommandNode<CommandSourceStack> built = builder.build();
		converted.put(node, built);
		return built;
	}

	private static <V> RequiredArgumentBuilder<CommandSourceStack, V> argument(ArgumentCommandNode<FabricClientCommandSource, V> node) {
		RequiredArgumentBuilder<CommandSourceStack, V> builder = RequiredArgumentBuilder.argument(node.getName(), node.getType());
		SuggestionProvider<FabricClientCommandSource> suggestions = node.getCustomSuggestions();
		if (suggestions != null) {
			builder.suggests((context, suggestionsBuilder) -> suggestions.getSuggestions(retarget(context), suggestionsBuilder));
		}
		return builder;
	}

	// Brigadier contexts only carry the source generically, so swapping it is safe after erasure.
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static CommandContext<FabricClientCommandSource> retarget(CommandContext<CommandSourceStack> context) {
		return (CommandContext<FabricClientCommandSource>) ((CommandContext) context).copyFor(new ClientCommandSourceAdapter(context.getSource()));
	}
}
