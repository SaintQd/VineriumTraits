plugins {
    java
    kotlin("jvm") version "2.3.0"
}

group = "org.saintqd"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven(url = "https://jitpack.io")
    maven(url = "https://mvn.lumine.io/repository/maven-public/")
    maven(url = "https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.jsinco.dev/releases")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly(files("../VineriumLib/build/libs/VineriumLib-1.0-SNAPSHOT.jar"))

    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("com.github.Zrips:CMI-API:9.8.6.4")
    compileOnly("io.lumine:Mythic-Dist:5.13.0-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6") // repo.extendedclip.com
    compileOnly("com.dre.brewery:BreweryX:3.7.0")

    compileOnly("com.zaxxer:HikariCP:7.0.2")
    compileOnly("com.mysql:mysql-connector-j:9.5.0")
    compileOnly("org.jdbi:jdbi3-core:3.53.0")

    compileOnly("io.github.classgraph:classgraph:4.8.184")
    implementation(kotlin("reflect"))
}

tasks.withType<Jar> {

    // To avoid the duplicate handling strategy error
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // To add all the dependencies otherwise a "NoClassDefFoundError" error
    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })

}
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}