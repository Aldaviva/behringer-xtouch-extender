group = "com.aldaviva.midi"
version = "0.0.1-SNAPSHOT"

plugins {
    `maven-publish`
    kotlin("jvm") version "1.3.40"
}

repositories {
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
}

publishing {
    publications {
        create<MavenPublication>("default") {
            from(components["java"])
//            artifact(dokkaJar)
        }
    }
    repositories {
        maven {
            url = uri("$buildDir/repository")
        }
    }
}