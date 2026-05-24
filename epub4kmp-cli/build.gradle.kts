plugins {
  alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
  applyDefaultHierarchyTemplate()

  val nativeTargets = listOf(
    mingwX64(),
    linuxX64(),
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
