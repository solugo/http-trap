plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
    id("com.google.cloud.tools.jib") version "3.4.3"
    id("application")
}

group = "de.solugo"

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("io.ktor:ktor-bom:2.3.12"))
    implementation("io.ktor:ktor-server-cio")
    implementation("io.ktor:ktor-server-websockets")
    implementation("io.ktor:ktor-server-html-builder")
    implementation("ch.qos.logback:logback-classic:1.5.6")
}

application {
    mainClass.set("HttpTrap")
}

jib {
    to {
        image = "http-trap"
        tags = setOfNotNull(version.toString().takeUnless { it == "unspecified" })
    }
}

