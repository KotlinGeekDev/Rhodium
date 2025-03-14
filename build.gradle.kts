import com.vanniktech.maven.publish.SonatypeHost

buildscript {

    val kotlinVersion = "2.1.10"

    dependencies {
        classpath(kotlin("gradle-plugin", version = kotlinVersion))
        classpath("com.android.tools.build:gradle:8.7.3")
        classpath(kotlin("serialization", version = kotlinVersion))
    }

}

plugins {
    kotlin("multiplatform") version "2.1.10" apply false
    id("com.android.library") version "8.7.3" apply false
    id("org.jetbrains.kotlinx.atomicfu") version "0.27.0"
    id("com.vanniktech.maven.publish") version "0.30.0"
    //id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    //id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false

}


allprojects {
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "com.vanniktech.maven.publish")
//    apply(plugin = "maven-publish")

    val isJitpack = System.getenv("JITPACK") == "true"

    group = "io.github.kotlingeekdev"
    version = "1.0-beta-18"


//    val javadocJar = tasks.register<Jar>("javadocJar") {
//        archiveClassifier.set("javadoc")
//    }

    mavenPublishing {
        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
        if (!isJitpack){
            signAllPublications()
        }


        coordinates(group.toString(), "rhodium", version.toString())

//        configure(KotlinMultiplatform(
//            javadocJar = JavadocJar.Javadoc(),
//            sourcesJar = true
//        ))

        pom {
            name = "Rhodium"
            description = " A Kotlin Multiplatform library for Nostr"
            url = "https://github.com/KotlinGeekDev/Rhodium"

            licenses {
                license {
                    name = "The MIT License"
                    url = "https://opensource.org/license/MIT"
                    distribution = "https://opensource.org/license/MIT"
                }
            }

            developers {
                developer {
                    name = "KotlinGeekDev"
                    email = "kotlingeek@protonmail.com"
                    url = "https://github.com/KotlinGeekDev"
                }
            }

            scm {
                connection = "scm:git:git://github.com/KotlinGeekDev/Rhodium.git"
                url = "https://github.com/KotlinGeekDev/Rhodium"

            }
        }
    }

//    extensions.configure<PublishingExtension> {
//        publications.withType<MavenPublication>().configureEach {
//            version = project.version.toString()
//            artifact(javadocJar)
//        }
//    }

}


