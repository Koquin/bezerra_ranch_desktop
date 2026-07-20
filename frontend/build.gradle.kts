plugins {
    kotlin("jvm") version "1.8.22"
    id("org.jetbrains.compose") version "1.5.0"
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(project(":backend"))
    implementation(compose.desktop.currentOs)
    implementation(compose.materialIconsExtended)
    implementation("org.apache.pdfbox:pdfbox:2.0.27")
}

configurations.all {
    resolutionStrategy.force("org.jetbrains.kotlin:kotlin-stdlib:1.8.22")
}

kotlin {
    jvmToolchain(17)
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb)
        }
    }
}
