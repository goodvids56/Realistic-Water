pluginManagement {
	includeBuild("../build-logic")
	repositories {
		gradlePluginPortal()
		maven("https://maven.neoforged.net/releases")
	}
}

rootProject.name = "realistic-water-neoforge"
