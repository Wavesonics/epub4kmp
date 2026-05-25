plugins {
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.maven.central.publish) apply false
  alias(libs.plugins.compose) apply false
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.android.library) apply false
}

allprojects {
  group = "com.darkrockstudios"
  version = providers.gradleProperty("library.version").get()
}
