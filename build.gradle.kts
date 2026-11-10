plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "io.github.nmaksimka"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

javafx {
    version = "21.0.5"
    modules = listOf("javafx.controls")
}

application {
    mainClass = "io.github.nmaksimka.mypetutil.Launcher"
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}

// Выгрузить все спрайты в build/sprites/sheet.png, чтобы посмотреть глазами
tasks.register<JavaExec>("exportSprites") {
    group = "tools"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass = "io.github.nmaksimka.mypetutil.tools.SpriteSheetExporter"
    args(layout.buildDirectory.dir("sprites").get().asFile.absolutePath)
}
