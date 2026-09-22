plugins {
    java
}

group = "jp.feato"
version = providers.gradleProperty("releaseVersion").getOrElse("1.0.0")

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.126-stable")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }

    testImplementation("io.papermc.paper:paper-api:26.2.build.126-stable")
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

val pluginVersion = version.toString()
tasks.processResources {
    inputs.property("version", pluginVersion)
    filesMatching("plugin.yml") {
        expand("version" to pluginVersion)
    }
}

tasks.test {
    useJUnitPlatform()
}
