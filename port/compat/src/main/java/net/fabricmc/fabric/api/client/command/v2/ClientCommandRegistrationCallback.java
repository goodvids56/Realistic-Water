package net.fabricmc.fabric.api.client.command.v2;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.commands.CommandBuildContext;

@FunctionalInterface
public interface ClientCommandRegistrationCallback {
	Event<ClientCommandRegistrationCallback> EVENT = EventFactory.createArrayBacked(ClientCommandRegistrationCallback.class, listeners -> (dispatcher, context) -> {
		for (ClientCommandRegistrationCallback listener : listeners) {
			listener.register(dispatcher, context);
		}
	});

	void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext context);
}
