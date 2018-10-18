import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.0-rc-80"
}

repositories {
    jcenter()
    mavenCentral()
    maven("http://dl.bintray.com/kotlin/kotlin-eap")
    maven("https://dl.bintray.com/kotlin/ktor")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.30.0-eap13")
    implementation("io.ktor:ktor-network:0.9.6-alpha-1-rc13")

    testCompile("org.junit.jupiter:junit-jupiter-api:5.3.1")
    testCompile("org.junit.jupiter:junit-jupiter-params:5.3.1")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:5.3.1")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.0.0-RC3")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion("1.3.0-rc-80")
        }
    }
}