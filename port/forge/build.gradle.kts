import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.util.zip.ZipFile

plugins {
	id("java")
	id("net.minecraftforge.gradle") version "[7.0.17,8)"
	id("com.gradleup.shadow") version "9.6.1"
	id("realistic-water.upstream-port")
}

// Upstream's source is compiled with only the patches in upstream-patches.json;
// ../compat supplies the Fabric API classes it uses.
val upstream = extensions.getByType<Upstream>()
val forgeVersion: String = providers.gradleProperty("forge_version").get()
val forgeMixins = upstream.mixinConfigs + "realistic_water_forge.mixins.json"

version = upstream.version
group = upstream.group
base.archivesName = "realistic-water-forge"

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

repositories {
	minecraft.mavenizer(this)
	maven(fg.forgeMaven)
	maven(fg.minecraftLibsMaven)
	mavenCentral()
}

// ForgeGradle applies the access transformer while resolving Minecraft, before any task runs.
val accessTransformerFile = upstream.writeAccessTransformerNow(project)

minecraft.accessTransformer.from(accessTransformerFile)

dependencies {
	implementation(minecraft.dependency("net.minecraftforge:forge:$forgeVersion"))
}

tasks.named<PatchUpstream>("patchUpstream") {
	// Forge's Mixin only knows compatibility levels up to Java 21, and logs an error for configs without a minVersion.
	mixinCompatibilityCap = 21
	mixinMinVersion = "0.8"
}

val generateModMetadata = tasks.register<GenerateModsToml>("generateModMetadata") {
	fabricModJson = upstream.fabricModJson
	loader = "forge"
	modVersion = upstream.version
	loaderVersionRange = "[" + forgeVersion.substringAfter('-').substringBefore('.') + ",)"
	minecraftRange = upstream.minecraftRange
	// Forge reads mixin configs from the jar manifest.
	mixinConfigs = emptyList()
	outputDir = layout.buildDirectory.dir("generated/modMetadata")
}

// Forge only loads a mod's assets and data with a pack.mcmeta, which Fabric mods don't ship.
// Take the pack format from Forge's own jar so it always matches the Minecraft version.
val generatePackMetadata = tasks.register("generatePackMetadata") {
	val classpath = configurations.compileClasspath
	val output = layout.buildDirectory.dir("generated/packMetadata")
	val forgeJarName = "forge-$forgeVersion.jar"
	inputs.property("forge_version", forgeVersion)
	outputs.dir(output)
	doLast {
		val forgeJar = classpath.get().files.first { it.name == forgeJarName }
		val forgePack = ZipFile(forgeJar).use { zip ->
			zip.getInputStream(zip.getEntry("pack.mcmeta")).reader().readText()
		}
		@Suppress("UNCHECKED_CAST")
		val pack = (JsonSlurper().parseText(forgePack) as Map<String, Map<String, Any?>>).getValue("pack")
		val file = output.get().file("pack.mcmeta").asFile
		file.parentFile.mkdirs()
		file.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(mapOf("pack" to mapOf(
			"description" to "Realistic Water resources",
			"min_format" to pack["min_format"],
			"max_format" to pack["max_format"],
		)))))
	}
}

sourceSets.main {
	java.srcDirs(upstream.sourceDirs(project, "java"), "../compat/src/main/java")
	resources.srcDirs(upstream.sourceDirs(project, "resources"), generateModMetadata, generatePackMetadata)
}

minecraft {
	runs {
		configureEach {
			workingDir = layout.projectDirectory.dir("run")
			args(forgeMixins.flatMap { listOf("--mixin.config", it) })
		}
		register("client")
		register("server") {
			args("--nogui")
		}
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.encoding = "UTF-8"
	options.release = 25
}

tasks.processResources {
	from(accessTransformerFile) {
		into("META-INF")
	}
	// Keep Fabric (or Sinytra Connector) from loading this jar as a Fabric mod; the compat layer
	// still reads its entrypoints from the moved copy.
	filesMatching("fabric.mod.json") {
		path = "META-INF/realistic-water/fabric.mod.json"
		expand("version" to project.version)
	}
}

// The compat layer lives in net.fabricmc.* so upstream compiles unchanged. Relocate it in the
// shipped jar so it can't clash with a real Fabric API (e.g. Sinytra Connector + Forgified Fabric API).
tasks.shadowJar {
	archiveClassifier = ""
	configurations = emptyList()
	relocate("net.fabricmc", "me.skaffy.port.fabricmc")
	manifest.attributes("MixinConfigs" to forgeMixins.joinToString(","))
	from(upstream.dir.resolve("LICENSE")) {
		rename { "${it}_realistic-water" }
	}
}

tasks.jar {
	archiveClassifier = "slim"
	manifest.attributes("MixinConfigs" to forgeMixins.joinToString(","))
}

tasks.assemble {
	dependsOn(tasks.shadowJar)
}
