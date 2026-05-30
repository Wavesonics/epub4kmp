plugins {
	alias(libs.plugins.kotlin.multiplatform)
	alias(libs.plugins.compose)
	alias(libs.plugins.compose.compiler)
}

kotlin {
	wasmJs {
		outputModuleName.set("reader-web")
		browser {
			commonWebpackConfig {
				outputFileName = "reader-web.js"
			}
		}
		binaries.executable()
	}

	sourceSets {
		val wasmJsMain by getting {
			dependencies {
				implementation(project(":epub4kmp-core"))
				implementation(project(":epub4kmp-compose-ui"))
				implementation(compose.runtime)
				implementation(compose.foundation)
				implementation(compose.material3)
				implementation(compose.ui)
				implementation(libs.okio)
				implementation(libs.filekit.dialogs.compose)
				implementation(libs.filekit.core)
				implementation(libs.kotlinx.coroutines.core)
				implementation(libs.kotlinx.browser)
			}
		}
	}
}

tasks.register<Sync>("updateDemo") {
	description = "Builds the WASM distribution and syncs it into the docs directory for GitHub Pages"
	group = "distribution"

	dependsOn("wasmJsBrowserDistribution")

	val docsDir = rootProject.layout.projectDirectory.dir("docs")

	from(layout.buildDirectory.dir("dist/wasmJs/productionExecutable"))
	into(docsDir)

	preserve {
		include("DEMO.md")
		include(".nojekyll")
	}

	doLast {
		// GitHub Pages runs Jekyll by default, which skips files webpack emits
		// (and treats `_`-prefixed paths specially). `.nojekyll` serves the dir as-is.
		docsDir.file(".nojekyll").asFile.writeText("")

		println("Demo updated in docs/ directory")
	}
}
