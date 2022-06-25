val scarletVersion = "0.1.12"
val kotlinVersion = "1.5.31"
val jacksonVersion = "2.13.3"

plugins {
    `java-library`
    kotlin("jvm") version "1.5.31"
    `maven-publish`
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.31")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.31")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.31")
    implementation("com.tinder.scarlet:scarlet:${scarletVersion}")
    implementation("com.tinder.scarlet:websocket-okhttp:${scarletVersion}")
    implementation("com.tinder.scarlet:stream-adapter-built-in:${scarletVersion}")
    implementation("com.tinder.scarlet:message-adapter-jackson:${scarletVersion}")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:${jacksonVersion}")
    implementation("fr.acinq.secp256k1:secp256k1-kmp:0.6.4")
    //implementation("fr.acinq.secp256k1:secp256k1-kmp-jvm:0.6.4")
    implementation("fr.acinq.secp256k1:secp256k1-kmp-jni-jvm:0.6.4")
    //  implementation("fr.acinq.secp256k1:secp256k1-kmp-jni-common:0.6.1")
    //implementation("fr.acinq.secp256k1:secp256k1-kmp-jni-jvm-extract:0.6.2")
    //implementation("org.junit.jupiter:junit-jupiter:5.8.2")
    // implementation("fr.acinq.secp256k1:secp256k1-kmp-jni-jvm:0.6.1")


    // implementation("com.squareup.moshi:moshi:1.13.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${jacksonVersion}")
    implementation("org.jetbrains.kotlin:kotlin-test-junit:1.5.31")


    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")
    testImplementation("org.assertj:assertj-core:3.23.1")
    runtimeOnly("fr.acinq.secp256k1:secp256k1-kmp-jni-jvm-linux:0.6.4")
    testRuntimeOnly("fr.acinq.secp256k1:secp256k1-kmp-jni-jvm-linux:0.6.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.8.2")
}



tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}


publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.parent?.group.toString()
            artifactId = project.name
            version = project.parent?.version.toString()
            //artifact(tasks.kotlinSourcesJar)
            from(components["kotlin"])
        }
    }
}
