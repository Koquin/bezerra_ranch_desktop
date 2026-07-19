plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.apache.pdfbox:pdfbox:2.0.27")
}

kotlin {
    jvmToolchain(17)
}
