plugins {
    kotlin("jvm") version "1.8.22" apply false
    id("org.jetbrains.compose") version "1.5.0" apply false
}

allprojects {
    repositories {
        mavenCentral()
        google()
    }

    // Ensure consistent Kotlin stdlib when resolving transitive deps
    configurations.all {
        resolutionStrategy.force("org.jetbrains.kotlin:kotlin-stdlib:1.8.22")
    }
}

// Optionally configure common kotlin toolchain here if desired
//subprojects {
//    plugins.withId("org.jetbrains.kotlin.jvm") {
//        kotlin {
//            jvmToolchain(17)
//        }
//    }
//}
