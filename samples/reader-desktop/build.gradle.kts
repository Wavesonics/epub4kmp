plugins {
	alias(libs.plugins.kotlin.multiplatform)
	alias(libs.plugins.compose)
	alias(libs.plugins.compose.compiler)
}

kotlin {
	jvm()

	sourceSets {
		jvmMain {
			dependencies {
				implementation(project(":epub4kmp-core"))
				implementation(project(":epub4kmp-compose-ui"))
				implementation(compose.desktop.currentOs)
				implementation(compose.material3)
				implementation(libs.okio)
				implementation(libs.filekit.dialogs.compose)
				implementation(libs.nucleus.darkmode.detector)
				implementation(libs.nucleus.decorated.window.material3)
				implementation(libs.nucleus.decorated.window.jni)
			}
		}
	}
}

compose.desktop {
	application {
		mainClass = "io.documentnode.epub4kmp.samples.reader.MainKt"
		// ComposeNativeWebview uses JNA to call into the Wry native lib.
		jvmArgs += "--enable-native-access=ALL-UNNAMED"
	}
}
