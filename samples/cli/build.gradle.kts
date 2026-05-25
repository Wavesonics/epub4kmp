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
  // linuxX64 with Kotlin/Native static caches enabled. Both archives export
  // `Context.selfAndAncestors` and ld.lld refuses to link them together.
  // Tracked upstream:
  //   - https://github.com/ajalt/clikt/issues/598
  //   - https://youtrack.jetbrains.com/issue/KMT-1222
  //   - https://youtrack.jetbrains.com/issue/KT-75928
  // The legacy `kotlin.native.cacheKind.linuxX64=none` gradle property is
  // silently ignored on Kotlin 2.3.20+ — the new DSL below is required.
  @OptIn(KotlinNativeCacheApi::class)
  linuxX64Target.binaries.withType<Executable>().configureEach {
    disableNativeCache(
      version = DisableCacheInKotlinVersion.`2_3_21`,
      reason = "clikt + clikt-mordant duplicate symbol on linuxX64 link",
      issueUrl = URI("https://github.com/ajalt/clikt/issues/598"),
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
