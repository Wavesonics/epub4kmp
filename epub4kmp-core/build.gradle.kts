plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.maven.central.publish)
}

kotlin {
  applyDefaultHierarchyTemplate()

  jvm()

  iosArm64()
  iosSimulatorArm64()

  macosArm64()

  linuxArm64()
  linuxX64()

  mingwX64()

  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.okio)
        implementation(libs.xmlutil.core)
        implementation(libs.kmp.zip)
        implementation(libs.kmp.zip.okio)
        implementation(libs.kotlinx.datetime)
      }
    }
    commonTest {
      dependencies {
        implementation(kotlin("test"))
      }
    }
  }
}

mavenPublishing {
  publishToMavenCentral(automaticRelease = false)
  // signAllPublications() requires a configured GPG signing key — see
  // https://vanniktech.github.io/gradle-maven-publish-plugin/central/#secrets
  signAllPublications()

  coordinates(
    groupId = project.group.toString(),
    artifactId = "epub4kmp-core",
    version = project.version.toString(),
  )

  pom {
    name.set("epub4kmp-core")
    description.set(
      "Kotlin Multiplatform library for reading/writing/manipulating EPUB files. " +
        "A KMP fork of epub4j (formerly epub4j-kotlin, which itself was a fork of epublib)."
    )
    url.set("https://github.com/Wavesonics/epub4kmp-kmp")
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
