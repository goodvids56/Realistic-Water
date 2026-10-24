package me.skaffy.port.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;

/// Runs the "main" and "client" entrypoints listed in upstream's fabric.mod.json,
/// which the build packs as META-INF/realistic-water/fabric.mod.json.
public final class Entrypoints {
	public static final String METADATA = "META-INF/realistic-water/fabric.mod.json";

	private Entrypoints() {
	}

	public static void runMain() {
		for (String name : names("main")) {
			create(name, ModInitializer.class).onInitialize();
		}
	}

	public static void runClient() {
		for (String name : names("client")) {
			create(name, ClientModInitializer.class).onInitializeClient();
		}
	}

	/// The mod id upstream declares, which is also the namespace of everything it registers.
	public static String modId() {
		return metadata().get("id").getAsString();
	}

	private static List<String> names(String key) {
		JsonObject entrypoints = metadata().getAsJsonObject("entrypoints");
		List<String> names = new ArrayList<>();
		if (entrypoints == null || !entrypoints.has(key)) {
			return names;
		}
		JsonArray array = entrypoints.getAsJsonArray(key);
		for (JsonElement element : array) {
			names.add(element.isJsonObject() ? element.getAsJsonObject().get("value").getAsString() : element.getAsString());
		}
		return names;
	}

	private static JsonObject metadata() {
		try (InputStream in = Entrypoints.class.getClassLoader().getResourceAsStream(METADATA)) {
			if (in == null) {
				throw new IllegalStateException("Missing " + METADATA);
			}
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static <T> T create(String name, Class<T> type) {
		if (name.contains("::")) {
			throw new IllegalArgumentException("Method/field entrypoints aren't supported: " + name);
		}
		try {
			return type.cast(Class.forName(name, true, Entrypoints.class.getClassLoader()).getDeclaredConstructor().newInstance());
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Could not create entrypoint " + name, e);
		}
	}
}
