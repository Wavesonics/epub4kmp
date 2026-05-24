plugins {
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.maven.central.publish) apply false
}

allprojects {
  group = "com.darkrockstudios"
  version = providers.gradleProperty("library.version").get()
}
