pluginManagement {
	includeBuild("../build-logic")
	repositories {
		gradlePluginPortal()
		maven("https://maven.minecraftforge.net/")
	}
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "realistic-water-forge"
