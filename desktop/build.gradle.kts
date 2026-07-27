plugins {
    application
}

val gdxVersion = "1.12.1"

application {
    mainClass.set("com.autobattler.desktop.DesktopLauncherKt")
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    implementation("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-desktop")
}

kotlin {
    jvmToolchain(17)
}

/**
 * Run the World 1 campaign balance analysis.
 *
 *   ./gradlew :desktop:runCampaignBalance
 */
tasks.register<JavaExec>("runCampaignBalance") {
    group = "autobattler"
    description = "Simulate World 1 (1-1 to 1-5) campaign balance with gold/XP progression"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.autobattler.desktop.CampaignBalanceToolKt")
    workingDir = rootDir
}

/**
 * Run the headless balance simulation tool.
tasks.register<JavaExec>("runBalanceTool") {
    group = "autobattler"
    description = "Run the headless balance simulation tool"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.autobattler.desktop.BalanceToolKt")
    workingDir = rootDir
    // Forward --args if provided
    if (project.hasProperty("args")) {
        args = (project.property("args") as String).split(" ")
    }
}

