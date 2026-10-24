package me.skaffy.port.compat;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;

/// Forge and NeoForge need a FluidType for every fluid; upstream's fluids don't have one. Vanilla
/// decides how entities treat a fluid from the water/lava fluid tags, which those loaders replaced
/// with FluidTypes, so sort upstream's fluids by the tags it puts them in.
public final class UpstreamFluids {
	private UpstreamFluids() {
	}

	public enum Kind {
		WATER,
		LAVA,
		// Untagged: vanilla doesn't let entities swim in or burn from it.
		INERT
	}

	public static Map<Fluid, Kind> classify(String namespace) {
		Set<Identifier> water = tag("water");
		Set<Identifier> lava = tag("lava");
		Map<Fluid, Kind> kinds = new IdentityHashMap<>();
		for (Fluid fluid : BuiltInRegistries.FLUID) {
			Identifier id = BuiltInRegistries.FLUID.getKey(fluid);
			if (id.getNamespace().equals(namespace)) {
				kinds.put(fluid, water.contains(id) ? Kind.WATER : lava.contains(id) ? Kind.LAVA : Kind.INERT);
			}
		}
		return kinds;
	}

	private static Set<Identifier> tag(String name) {
		Set<Identifier> ids = new HashSet<>();
		try (InputStream in = UpstreamFluids.class.getResourceAsStream("/data/minecraft/tags/fluid/" + name + ".json")) {
			if (in == null) {
				return ids;
			}
			for (JsonElement value : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("values")) {
				String entry = value.isJsonObject() ? value.getAsJsonObject().get("id").getAsString() : value.getAsString();
				if (!entry.startsWith("#")) {
					ids.add(Identifier.parse(entry));
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return ids;
	}
}
