plugins {
	`kotlin-dsl`
}

repositories {
	gradlePluginPortal()
}

gradlePlugin {
	plugins {
		register("upstreamPort") {
			id = "realistic-water.upstream-port"
			implementationClass = "UpstreamPortPlugin"
		}
	}
}
