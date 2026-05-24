import org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi
import java.net.URI

plugins {
  alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
  applyDefaultHierarchyTemplate()

  val linuxX64Target = linuxX64()
  val nativeTargets = listOf(
    mingwX64(),
    linuxX64Target,
    linuxArm64(),
    macosArm64(),
  )

  nativeTargets.forEach { target ->
    target.binaries {
      executable {
        entryPoint = "com.darkrockstudios.epub4kmp.cli.main"
        baseName = "epub4kmp"
      }
    }
  }

  // Workaround for a clikt/clikt-mordant duplicate-symbol link error on
  // linuxX64 with Kotlin/Native static caches enabled. The two artifacts
  // expose the same `Context.selfAndAncestors` symbol when cached, and
  // ld.lld refuses to link. Disable the native cache for linuxX64 only.
  // See https://kotl.in/disable-native-cache
  @OptIn(KotlinNativeCacheApi::class)
  linuxX64Target.binaries.withType<Executable>().configureEach {
    disableNativeCache(
      version = DisableCacheInKotlinVersion.`2_3_20`,
      reason = "clikt + clikt-mordant duplicate symbol on linuxX64 link",
      issueUrl = URI("https://github.com/ajalt/clikt/issues"),
    )
  }

  sourceSets {
    nativeMain {
      dependencies {
        implementation(project(":epub4kmp-core"))
        implementation(libs.clikt)
        implementation(libs.jetbrains.markdown)
        implementation(libs.kotlinx.html)
        implementation(libs.okio)
        implementation(libs.xmlutil.core)
      }
    }
    nativeTest {
      dependencies {
        implementation(kotlin("test"))
      }
    }
  }
}
