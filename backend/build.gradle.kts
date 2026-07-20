plugins {
    kotlin("jvm") version "1.8.22"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.apache.pdfbox:pdfbox:2.0.27")
    implementation("org.xerial:sqlite-jdbc:3.46.1.3")
    implementation("com.google.code.gson:gson:2.11.0")

    testImplementation(kotlin("test-junit5"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
