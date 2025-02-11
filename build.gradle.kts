buildscript {

    val kotlinVersion = "2.0.20"

    dependencies {
        classpath(kotlin("gradle-plugin", version = kotlinVersion))
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath(kotlin("serialization", version = kotlinVersion))
    }

}

plugins {
    kotlin("multiplatform") version "2.0.20" apply false
    id("com.android.library") version "8.2.2" apply false
    id("org.jetbrains.kotlinx.atomicfu") version "0.25.0"
    //id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    //id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false

}


allprojects {
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "maven-publish")

    group = "com.github.KotlinGeekDev"
    version = "1.0-beta-06"

    val javadocJar = tasks.register<Jar>("javadocJar") {
        archiveClassifier.set("javadoc")
    }

    extensions.configure<PublishingExtension> {
        publications.withType<MavenPublication>().configureEach {
            version = project.version.toString()
            artifact(javadocJar)
        }
    }

}


