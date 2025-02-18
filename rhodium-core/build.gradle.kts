
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

val kotlinVersion = "2.0.20"
val ktorVersion = "3.0.1"
val kotlinCryptoVersion = "0.4.0"

val junitJupiterVersion = "5.10.1"

plugins {
    //`java-library`
    kotlin("multiplatform")
    id("com.android.library")

    kotlin("plugin.serialization")
//    `maven-publish`
}


kotlin {
    //explicitApi()
    jvmToolchain(17)

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_1_8)
        languageVersion.set(KotlinVersion.KOTLIN_1_8)
    }

    jvm("commonJvm") {

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)


        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
            testLogging {
                events("passed", "skipped", "failed")
            }
        }
    }


    androidTarget {

        publishAllLibraryVariants()
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }


    linuxX64("linux") {
//        compilations.all {
//            cinterops {
//                val libs by creating {
//                    defFile("src/linuxMain/cinterop/libs.def")
//                }
//            }
//        }
//
//        binaries {
//            sharedLib {
//
//            }
//        }
    }

    //Apple targets
    macosX64()
    macosArm64()
    iosX64()
    iosArm64()
    iosSimulatorArm64()


    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            //Ktor
            implementation("io.ktor:ktor-client-core:$ktorVersion")
            implementation("io.ktor:ktor-client-websockets:$ktorVersion")
            implementation("io.ktor:ktor-client-logging:$ktorVersion")

            //Kotlin base
            implementation("org.jetbrains.kotlin:kotlin-stdlib:${kotlinVersion}")
            implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")

            //Crypto(Secp256k1-utils, SecureRandom, Hashing, etc.)
            implementation("fr.acinq.secp256k1:secp256k1-kmp:0.15.0")
            implementation("dev.whyoleg.cryptography:cryptography-core:$kotlinCryptoVersion")
            implementation("dev.whyoleg.cryptography:cryptography-random:$kotlinCryptoVersion")

            //Serialization
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
            //Coroutines
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            //Atomics
            implementation("org.jetbrains.kotlinx:atomicfu:0.25.0")
            //Date-time
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
            //UUID
            implementation("com.benasher44:uuid:0.8.4")
            //ByteBuffer(until a kotlinx-io replacement appears)
            implementation("com.ditchoom:buffer:1.4.2")
            //Logging
            implementation("co.touchlab:kermit:2.0.5")
        }

        commonTest.dependencies {
            implementation(kotlin("test-common"))
            implementation(kotlin("test-annotations-common"))
        }

        val commonJvmMain by getting {
            dependsOn(commonMain.get())

            dependencies {
                implementation("dev.whyoleg.cryptography:cryptography-provider-jdk:$kotlinCryptoVersion")

                implementation("com.squareup.okhttp3:okhttp:4.12.0")
                implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
                //implementation("fr.acinq.secp256k1:secp256k1-kmp-jvm:0.6.4")
                implementation("fr.acinq.secp256k1:secp256k1-kmp-jni-jvm:0.15.0")
            }
        }

        val commonJvmTest by getting {
            dependsOn(commonTest.get())

            dependencies {
                implementation(kotlin("test-junit5"))

                implementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
                implementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
                implementation("org.assertj:assertj-core:3.23.1")
                runtimeOnly("fr.acinq.secp256k1:secp256k1-kmp-jni-jvm-linux:0.15.0")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
                runtimeOnly("org.junit.vintage:junit-vintage-engine:$junitJupiterVersion")
            }
        }



        val androidMain by getting {
            dependsOn(commonJvmMain)

            dependencies {
                implementation("androidx.appcompat:appcompat:1.7.0")
                implementation("fr.acinq.secp256k1:secp256k1-kmp-jni-android:0.15.0")
            }
        }

        androidInstrumentedTest.configure {
            dependsOn(commonJvmTest)
        }

        val androidUnitTest by getting {
            dependsOn(commonJvmTest)
        }

        linuxMain.configure {
            dependencies {
                implementation("io.ktor:ktor-client-cio:$ktorVersion")
                //implementation("io.ktor:ktor-client-curl:$ktorVersion")
                implementation("dev.whyoleg.cryptography:cryptography-provider-openssl3-prebuilt:$kotlinCryptoVersion")
            }
        }

        linuxTest.configure {
            dependencies {

            }

        }

        appleMain.configure {
            dependencies {
                implementation("io.ktor:ktor-client-darwin:$ktorVersion")
                implementation("dev.whyoleg.cryptography:cryptography-provider-apple:$kotlinCryptoVersion")
            }
        }
        macosMain.get().dependsOn(appleMain.get())
        iosMain.get().dependsOn(appleMain.get())

    }
}

android {
    namespace = "io.github.kotlingeekdev.rhodium.android"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
        targetSdk = 34
        compileSdk = 34

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            aarMetadata {

            }
            isMinifyEnabled = true
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = false
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    dependencies {
//        coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")
        testImplementation("junit:junit:4.13.2")
        androidTestImplementation("androidx.test.ext:junit:1.2.1")
        androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    }

}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>() {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

tasks.withType<KotlinNativeCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-opt-in=kotlinx.cinterop.ExperimentalForeignApi")
}
