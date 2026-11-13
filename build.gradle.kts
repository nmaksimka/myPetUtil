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

// gradlew run -Dmypetutil.home=папка — запустить с отдельными настройками (удобно для проверки)
tasks.named<JavaExec>("run") {
    System.getProperty("mypetutil.home")?.let { systemProperty("mypetutil.home", it) }
    System.getProperty("mypetutil.debug")?.let { systemProperty("mypetutil.debug", it) }
}

// Показать окна, которые видит сканер (для отладки)
tasks.register<JavaExec>("dumpWindows") {
    group = "tools"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass = "io.github.nmaksimka.mypetutil.platform.WindowDump"
}

// Скриншоты окна настроек для README: docs/screenshots
tasks.register<JavaExec>("uiScreenshots") {
    group = "tools"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass = "io.github.nmaksimka.mypetutil.tools.UiScreenshots"
    jvmArgs("--module-path", classpath.filter { it.name.startsWith("javafx") }.asPath,
            "--add-modules", "javafx.controls")
    args(layout.projectDirectory.dir("docs/screenshots").asFile.absolutePath)
}

// Иконка программы из спрайта пупса: src/main/packaging/myPetUtil.ico
tasks.register<JavaExec>("exportIcon") {
    group = "tools"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass = "io.github.nmaksimka.mypetutil.tools.IconExporter"
    args(layout.projectDirectory.file("src/main/packaging/myPetUtil.ico").asFile.absolutePath)
}

// ---------- сборка exe ----------
// gradlew packageExe  -> build/dist/myPetUtil/myPetUtil.exe (папка с exe и встроенной Java)
// gradlew zipExe      -> build/dist/myPetUtil-1.0.0-windows.zip (её можно скинуть друзьям)

val jpackageInput = layout.buildDirectory.dir("jpackage/input")
val distDir = layout.buildDirectory.dir("dist")

tasks.register<Sync>("prepareJpackage") {
    from(tasks.jar)
    from(configurations.runtimeClasspath)
    into(jpackageInput)
}

tasks.register<Exec>("packageExe") {
    group = "distribution"
    description = "Собирает myPetUtil.exe со встроенной Java через jpackage"
    dependsOn("prepareJpackage")
    val jdk = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(21) }
        .get().metadata.installationPath.asFile
    val dest = distDir.get().asFile
    doFirst { delete(File(dest, "myPetUtil")) }
    commandLine(
        File(jdk, "bin/jpackage.exe").absolutePath,
        "--type", "app-image",
        "--name", "myPetUtil",
        "--app-version", project.version.toString(),
        "--vendor", "nmaksimka",
        "--description", "Pixel pets for your desktop",
        "--input", jpackageInput.get().asFile.absolutePath,
        "--main-jar", tasks.jar.get().archiveFileName.get(),
        "--main-class", "io.github.nmaksimka.mypetutil.Launcher",
        "--icon", layout.projectDirectory.file("src/main/packaging/myPetUtil.ico").asFile.absolutePath,
        "--java-options", "-Dfile.encoding=UTF-8",
        "--jlink-options", "--strip-debug --no-header-files --no-man-pages --strip-native-commands",
        "--dest", dest.absolutePath
    )
}

tasks.register<Zip>("zipExe") {
    group = "distribution"
    dependsOn("packageExe")
    from(distDir.map { it.dir("myPetUtil") })
    into("myPetUtil")
    archiveFileName = "myPetUtil-${project.version}-windows.zip"
    destinationDirectory = distDir
}

// Картинка-витрина для README: docs/showcase.png
tasks.register<JavaExec>("exportShowcase") {
    group = "tools"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass = "io.github.nmaksimka.mypetutil.tools.ShowcaseExporter"
    args(layout.projectDirectory.file("docs/showcase.png").asFile.absolutePath)
}
