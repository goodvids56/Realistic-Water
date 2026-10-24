plugins {
	id("net.neoforged.moddev") version "2.0.148"
	id("com.gradleup.shadow") version "9.6.1"
	id("realistic-water.upstream-port")
}

// Upstream's source is compiled with only the patches in upstream-patches.json;
// ../compat supplies the Fabric API classes it uses.
val upstream = extensions.getByType<Upstream>()
val neoforgeVersion: String = providers.gradleProperty("neoforge_version").get()

version = upstream.version
group = upstream.group
base.archivesName = "realistic-water-neoforge"

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

val generateAccessTransformer = tasks.named<GenerateAccessTransformer>("generateAccessTransformer")

val generateModMetadata = tasks.register<GenerateModsToml>("generateModMetadata") {
	fabricModJson = upstream.fabricModJson
	loader = "neoforge"
	modVersion = upstream.version
	loaderVersionRange = "[" + neoforgeVersion.split(".").take(2).joinToString(".") + ",)"
	minecraftRange = upstream.minecraftRange
	mixinConfigs = upstream.mixinConfigs + "realistic_water_neoforge.mixins.json"
	outputDir = layout.buildDirectory.dir("generated/modMetadata")
}

sourceSets.main {
	java.srcDirs(upstream.sourceDirs(project, "java"), "../compat/src/main/java")
	resources.srcDirs(upstream.sourceDirs(project, "resources"), generateModMetadata)
}

neoForge {
	version = neoforgeVersion
	accessTransformers.from(files(upstream.accessTransformer(project)).builtBy(generateAccessTransformer))

	runs {
		register("client") {
			client()
		}
		register("server") {
			server()
			programArgument("--nogui")
		}
	}

	mods {
		register("realistic_water") {
			sourceSet(sourceSets.main.get())
		}
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.encoding = "UTF-8"
	options.release = 25
}

tasks.processResources {
	from(generateAccessTransformer) {
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
	from(upstream.dir.resolve("LICENSE")) {
		rename { "${it}_realistic-water" }
	}
}

tasks.jar {
	archiveClassifier = "slim"
}

tasks.assemble {
	dependsOn(tasks.shadowJar)
}
