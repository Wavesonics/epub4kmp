@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
	alias(libs.plugins.kotlin.multiplatform)
	alias(libs.plugins.android.library)
	alias(libs.plugins.compose)
	alias(libs.plugins.compose.compiler)
	alias(libs.plugins.maven.central.publish)
}

kotlin {
	applyDefaultHierarchyTemplate()

	jvm()
	androidTarget {
		publishLibraryVariants("release")
	}
	iosArm64()
	iosSimulatorArm64()

	wasmJs {
		browser()
	}

	sourceSets {
		commonMain {
			dependencies {
				api(project(":epub4kmp-core"))

				implementation(compose.runtime)
				implementation(compose.foundation)
				implementation(compose.material3)
				implementation(compose.ui)
				implementation(compose.components.resources)

				implementation(libs.compose.native.webview)
				implementation(libs.kotlinx.coroutines.core)
			}
		}
		commonTest {
			dependencies {
				implementation(kotlin("test"))
			}
		}
	}
}

android {
	namespace = "io.documentnode.epub4kmp.compose"
	compileSdk = libs.versions.androidCompileSdk.get().toInt()
	defaultConfig {
		minSdk = libs.versions.androidMinSdk.get().toInt()
	}
}

mavenPublishing {
	publishToMavenCentral(automaticRelease = true)
	signAllPublications()

	coordinates(
		groupId = project.group.toString(),
		artifactId = "epub4kmp-compose-ui",
		version = project.version.toString(),
	)

	pom {
		name.set("epub4kmp-compose-ui")
		description.set(
			"Compose Multiplatform UI helpers for rendering EPUB files loaded with " +
					"epub4kmp-core. Ships building-block composables and a batteries-included " +
					"EpubReader screen."
		)
		url.set("https://github.com/Wavesonics/epub4kmp")
		licenses {
			license {
				name.set("Apache License, Version 2.0")
				url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
				distribution.set("repo")
			}
		}
		developers {
			developer {
				id.set("darkrockstudios")
				name.set("Adam Brown")
				email.set("adamwbrown@gmail.com")
			}
		}
		scm {
			connection.set("scm:git:git://github.com/Wavesonics/epub4kmp.git")
			developerConnection.set("scm:git:ssh://github.com/Wavesonics/epub4kmp.git")
			url.set("https://github.com/Wavesonics/epub4kmp")
		}
	}
}
