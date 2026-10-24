package me.skaffy.port.compat;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/// FabricClientCommandSource backed by the loader's client-side CommandSourceStack.
public record ClientCommandSourceAdapter(CommandSourceStack stack) implements FabricClientCommandSource {
	@Override
	public void sendFeedback(Component message) {
		Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(message);
	}

	@Override
	public void sendError(Component message) {
		Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(Component.empty().append(message).withStyle(ChatFormatting.RED));
	}

	@Override
	public Minecraft getClient() {
		return Minecraft.getInstance();
	}

	@Override
	public LocalPlayer getPlayer() {
		return Minecraft.getInstance().player;
	}

	@Override
	public Entity getEntity() {
		Entity entity = this.stack.getEntity();
		return entity != null ? entity : this.getPlayer();
	}

	@Override
	public Vec3 getPosition() {
		return this.stack.getPosition();
	}

	@Override
	public Vec2 getRotation() {
		return this.stack.getRotation();
	}

	@Override
	public ClientLevel getLevel() {
		return Minecraft.getInstance().level;
	}

	@Override
	public boolean attended() {
		return true;
	}

	@Override
	public Collection<String> getOnlinePlayerNames() {
		return this.stack.getOnlinePlayerNames();
	}

	@Override
	public Collection<String> getCustomTabSuggestions() {
		return this.stack.getCustomTabSuggestions();
	}

	@Override
	public Collection<String> getSelectedEntities() {
		return this.stack.getSelectedEntities();
	}

	@Override
	public Collection<String> getAllTeams() {
		return this.stack.getAllTeams();
	}

	@Override
	public Stream<Identifier> getAvailableSounds() {
		return this.stack.getAvailableSounds();
	}

	@Override
	public Stream<Identifier> getAvailablePostEffects() {
		return this.stack.getAvailablePostEffects();
	}

	@Override
	public CompletableFuture<Suggestions> customSuggestion(CommandContext<?> context) {
		return this.stack.customSuggestion(context);
	}

	@Override
	public Collection<TextCoordinates> getRelevantCoordinates() {
		return this.stack.getRelevantCoordinates();
	}

	@Override
	public Collection<TextCoordinates> getAbsoluteCoordinates() {
		return this.stack.getAbsoluteCoordinates();
	}

	@Override
	public <E> CompletableFuture<Suggestions> suggestRegistryElements(
		ResourceKey<? extends Registry<E>> key, ElementSuggestionType elements, SuggestionsBuilder builder, CommandContext<?> context, Predicate<E> filter
	) {
		return this.stack.suggestRegistryElements(key, elements, builder, context, filter);
	}

	@Override
	public Set<ResourceKey<Level>> levels() {
		return this.stack.levels();
	}

	@Override
	public RegistryAccess registryAccess() {
		return this.stack.registryAccess();
	}

	@Override
	public FeatureFlagSet enabledFeatures() {
		return this.stack.enabledFeatures();
	}

	@Override
	public PermissionSet permissions() {
		return this.stack.permissions();
	}
}
