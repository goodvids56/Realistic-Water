import groovy.json.JsonSlurper
import java.io.File
import java.util.Properties
import java.util.zip.ZipInputStream
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/// Shared build steps for compiling upstream's Fabric source on other loaders.
class UpstreamPortPlugin : Plugin<Project> {
	override fun apply(project: Project) {
		val upstream = Upstream(project.file(project.providers.gradleProperty("upstream_dir").get()))
		project.extensions.add("upstream", upstream)

		project.repositories.maven {
			url = project.uri("https://maven.fabricmc.net/")
			content { includeGroup("net.fabricmc.fabric-api") }
		}
		// Only read for its access wideners, never put on a classpath.
		val fabricApi = project.configurations.create("fabricApi") { isTransitive = false }
		project.dependencies.add(fabricApi.name, "net.fabricmc.fabric-api:fabric-api:${upstream.fabricApiVersion}")

		val patchUpstream = project.tasks.register("patchUpstream", PatchUpstream::class.java) {
			upstreamDir.set(upstream.dir)
			project.file("upstream-patches.json").takeIf { it.exists() }?.let { patches.set(it) }
			outputDir.set(upstream.patchedDir(project))
		}
		project.tasks.register("generateAccessTransformer", GenerateAccessTransformer::class.java) {
			wideners.from(fabricApi)
			// The patches only touch mixin targets, so upstream's own source has the same identifiers.
			sources.from(upstream.dir.resolve("src"))
			output.set(upstream.accessTransformer(project))
		}
	}
}

class Upstream(val dir: File) {
	val properties = Properties().apply { dir.resolve("gradle.properties").reader().use(::load) }
	val version: String = properties.getProperty("version")
	val group: String = properties.getProperty("group")
	val minecraftVersion: String = properties.getProperty("minecraft_version")
	val fabricApiVersion: String = properties.getProperty("fabric_api_version")
	val fabricModJson: File = dir.resolve("src/main/resources/fabric.mod.json")

	@Suppress("UNCHECKED_CAST")
	val metadata: Map<String, Any?> by lazy { JsonSlurper().parse(fabricModJson) as Map<String, Any?> }

	val mixinConfigs: List<String>
		get() = (metadata["mixins"] as? List<*>).orEmpty().map { (if (it is Map<*, *>) it["config"] else it).toString() }

	/// Where patchUpstream puts the patched copy; fixed so loader plugins can read it at configuration time.
	fun patchedDir(project: Project): File = project.layout.buildDirectory.dir("upstream").get().asFile

	fun accessTransformer(project: Project): File =
		project.layout.buildDirectory.file("generated/accessTransformer/accesstransformer.cfg").get().asFile

	/// For builds that need the access transformer while configuring (ForgeGradle reads it when
	/// resolving Minecraft): writes it right away instead of in a task.
	fun writeAccessTransformerNow(project: Project): File {
		val output = accessTransformer(project)
		val sources = project.fileTree(dir.resolve("src")) { include("**/*.java") }.files
		GenerateAccessTransformer.writeAccessTransformer(project.configurations.getByName("fabricApi").files, sources, output)
		return output
	}

	/// Patched upstream source directories, built by patchUpstream.
	fun sourceDirs(project: Project, kind: String): ConfigurableFileCollection {
		val patched = patchedDir(project)
		return project.files(patched.resolve("src/main/$kind"), patched.resolve("src/client/$kind")).builtBy("patchUpstream")
	}

	/// Versions this Minecraft release covers, the way Fabric's "~26.3" does: [26.3, 26.4).
	val minecraftRange: String
		get() {
			val (major, minor) = minecraftVersion.split(".").map(String::toInt)
			return "[$minecraftVersion,$major.${minor + 1})"
		}
}

/// Copies upstream's source and applies this loader's exact find/replace patches from
/// upstream-patches.json. A patch that no longer matches fails the build, so upstream changes
/// to patched code get looked at instead of silently breaking the port.
abstract class PatchUpstream : DefaultTask() {
	@get:InputDirectory
	@get:PathSensitive(PathSensitivity.RELATIVE)
	abstract val upstreamDir: DirectoryProperty

	@get:InputFile
	@get:Optional
	@get:PathSensitive(PathSensitivity.NONE)
	abstract val patches: RegularFileProperty

	/// Highest Mixin compatibilityLevel the loader's Mixin understands, e.g. 21 for JAVA_21.
	@get:Input
	@get:Optional
	abstract val mixinCompatibilityCap: Property<Int>

	/// Mixin minVersion to add to upstream's configs that don't declare one (Forge's Mixin logs an error for those).
	@get:Input
	@get:Optional
	abstract val mixinMinVersion: Property<String>

	@get:OutputDirectory
	abstract val outputDir: DirectoryProperty

	@TaskAction
	fun run() {
		val from = upstreamDir.get().asFile
		val to = outputDir.get().asFile
		to.deleteRecursively()
		from.resolve("src").copyRecursively(to.resolve("src"))

		if (patches.isPresent) {
			@Suppress("UNCHECKED_CAST")
			val list = JsonSlurper().parse(patches.get().asFile) as List<Map<String, Any?>>
			for (patch in list) {
				val path = patch["file"].toString()
				val find = patch["find"].toString()
				val replace = patch["replace"].toString()
				val expected = (patch["count"] as? Number)?.toInt() ?: 1
				val file = to.resolve(path)
				if (!file.exists()) {
					throw GradleException("Upstream patch target $path no longer exists; update upstream-patches.json")
				}
				val text = file.readText()
				val found = generateSequence(text.indexOf(find).takeIf { it >= 0 }) { at ->
					text.indexOf(find, at + find.length).takeIf { it >= 0 }
				}.count()
				if (found != expected) {
					throw GradleException("Upstream patch for $path expected $expected match(es) of:\n$find\nbut found $found; upstream changed this code, update upstream-patches.json")
				}
				file.writeText(text.replace(find, replace))
			}
		}

		val mixinConfigs = to.walkTopDown().filter { it.isFile && it.name.endsWith(".mixins.json") }.toList()
		if (mixinCompatibilityCap.isPresent) {
			val cap = mixinCompatibilityCap.get()
			val level = Regex("\"compatibilityLevel\"\\s*:\\s*\"JAVA_(\\d+)\"")
			mixinConfigs.forEach { file ->
				file.writeText(level.replace(file.readText()) { match ->
					if (match.groupValues[1].toInt() > cap) "\"compatibilityLevel\": \"JAVA_$cap\"" else match.value
				})
			}
		}
		if (mixinMinVersion.isPresent) {
			mixinConfigs.forEach { file ->
				val text = file.readText()
				if (!text.contains("\"minVersion\"")) {
					file.writeText(text.replaceFirst("{", "{\n\t\"minVersion\": \"${mixinMinVersion.get()}\","))
				}
			}
		}
	}
}

/// On Fabric, upstream compiles and runs with Fabric API's transitive access wideners applied.
/// Turns the entries upstream can actually be using (member and owner names both appear in its
/// source) into an access transformer. Taking all of them would break the loaders' recompile of
/// Minecraft: widening a protected method clashes with subclasses that override it as protected,
/// which Fabric never has to recompile.
abstract class GenerateAccessTransformer : DefaultTask() {
	@get:InputFiles
	@get:PathSensitive(PathSensitivity.NONE)
	abstract val wideners: ConfigurableFileCollection

	@get:InputFiles
	@get:PathSensitive(PathSensitivity.RELATIVE)
	abstract val sources: ConfigurableFileCollection

	@get:OutputFile
	abstract val output: RegularFileProperty

	@TaskAction
	fun run() {
		writeAccessTransformer(wideners.files, sources.asFileTree.matching { include("**/*.java") }.files, output.get().asFile)
	}

	companion object {
		fun writeAccessTransformer(wideners: Iterable<File>, sources: Iterable<File>, output: File) {
			val identifier = Regex("[A-Za-z_$][A-Za-z0-9_$]*")
			val used = HashSet<String>()
			sources.forEach { file -> identifier.findAll(file.readText()).forEach { used += it.value } }

			val lines = sortedSetOf<String>()
			fun convert(text: String) {
				for (raw in text.lineSequence()) {
					val parts = raw.substringBefore('#').trim().split(Regex("\\s+"))
					if (parts.size < 3 || !parts[0].startsWith("transitive-")) continue
					val access = when (parts[0].removePrefix("transitive-")) {
						"accessible" -> "public"
						"extendable", "mutable" -> "public-f"
						else -> continue
					}
					val owner = parts[2].replace('/', '.')
					val simpleName = owner.substringAfterLast('.').substringAfterLast('$')
					val name = if (parts[1] == "class" || parts.getOrNull(3) == "<init>") simpleName else parts[3]
					if (name !in used || simpleName !in used) continue
					lines += when (parts[1]) {
						"class" -> "$access $owner"
						"method" -> "$access $owner ${parts[3]}${parts[4]}"
						"field" -> "$access $owner ${parts[3]}"
						else -> continue
					}
				}
			}
			fun scan(zip: ZipInputStream) {
				while (true) {
					val entry = zip.nextEntry ?: break
					when {
						entry.name.endsWith(".jar") -> scan(ZipInputStream(zip))
						entry.name.endsWith(".classtweaker") || entry.name.endsWith(".accesswidener") -> convert(zip.readBytes().decodeToString())
					}
				}
			}
			wideners.forEach { jar -> ZipInputStream(jar.inputStream()).use(::scan) }
			output.parentFile.mkdirs()
			output.writeText(lines.joinToString("\n", postfix = "\n"))
		}
	}
}

/// Writes neoforge.mods.toml or Forge's mods.toml from upstream's fabric.mod.json.
abstract class GenerateModsToml : DefaultTask() {
	@get:InputFile
	@get:PathSensitive(PathSensitivity.NONE)
	abstract val fabricModJson: RegularFileProperty

	/// "neoforge" or "forge".
	@get:Input
	abstract val loader: Property<String>

	@get:Input
	abstract val modVersion: Property<String>

	@get:Input
	abstract val loaderVersionRange: Property<String>

	@get:Input
	abstract val minecraftRange: Property<String>

	/// Extra mixin configs to declare in the toml (Forge declares mixins in the jar manifest instead).
	@get:Input
	abstract val mixinConfigs: ListProperty<String>

	@get:OutputDirectory
	abstract val outputDir: DirectoryProperty

	@TaskAction
	fun run() {
		@Suppress("UNCHECKED_CAST")
		val meta = JsonSlurper().parse(fabricModJson.get().asFile) as Map<String, Any?>
		fun quote(value: Any?) = "\"" + value.toString().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
		val authors = (meta["authors"] as? List<*>).orEmpty().map { if (it is Map<*, *>) it["name"] else it }
		val neoforge = loader.get() == "neoforge"

		val text = buildString {
			appendLine("modLoader=\"javafml\"")
			appendLine("loaderVersion=\"[1,)\"")
			appendLine("license=${quote(meta["license"] ?: "All Rights Reserved")}")
			appendLine()
			appendLine("[[mods]]")
			appendLine("modId=\"realistic_water\"")
			appendLine("version=${quote(modVersion.get())}")
			appendLine("displayName=${quote(meta["name"])}")
			appendLine("authors=${quote(authors.joinToString(", "))}")
			appendLine("description=${quote(meta["description"] ?: "")}")
			meta["icon"]?.let { appendLine((if (neoforge) "iconFile=" else "logoFile=") + quote(it)) }
			for (config in mixinConfigs.get()) {
				appendLine()
				appendLine("[[mixins]]")
				appendLine("config=${quote(config)}")
			}
			if (neoforge) {
				appendLine()
				appendLine("[[accessTransformers]]")
				appendLine("file=\"META-INF/accesstransformer.cfg\"")
			}
			for ((modId, range) in listOf(loader.get() to loaderVersionRange.get(), "minecraft" to minecraftRange.get())) {
				appendLine()
				appendLine("[[dependencies.realistic_water]]")
				appendLine("modId=\"$modId\"")
				appendLine(if (neoforge) "type=\"required\"" else "mandatory=true")
				appendLine("versionRange=\"$range\"")
				appendLine("ordering=\"NONE\"")
				appendLine("side=\"BOTH\"")
			}
		}
		val dir = outputDir.get().asFile.resolve("META-INF")
		dir.mkdirs()
		dir.resolve(if (neoforge) "neoforge.mods.toml" else "mods.toml").writeText(text)
	}
}
