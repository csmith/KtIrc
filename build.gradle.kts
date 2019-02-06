import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

version = "0.6.0"
group = "com.dmdirc.ktirc"

plugins {
    `maven-publish`
    jacoco
    kotlin("jvm") version "1.3.21"
    id("com.jfrog.bintray") version "1.8.4"
    id("org.jetbrains.dokka") version "0.9.17"
    id("name.remal.check-updates") version "1.0.112"
}

jacoco {
    toolVersion = "0.8.3"
}

configurations {
    create("itestImplementation") { extendsFrom(getByName("testImplementation")) }
    create("itestRuntime") { extendsFrom(getByName("testRuntime")) }

    all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion("1.3.21")
            }
        }
    }
}

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8", "1.3.21"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.1.1")
    implementation("io.ktor:ktor-network:1.1.2")
    implementation("io.ktor:ktor-network-tls:1.1.2")

    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.3.2")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:5.3.2")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8

    sourceSets {
        create("itest") {
            compileClasspath += getByName("main").output
            runtimeClasspath += getByName("main").output
            java.srcDirs("src/itest/kotlin")
        }
    }
}

tasks {
    create<Test>("itest") {
        group = "verification"
        testClassesDirs = sourceSets["itest"].output.classesDirs
        classpath = sourceSets["itest"].runtimeClasspath
    }

    create<Jar>("sourceJar") {
        description = "Creates a JAR that contains the source code."
        from(sourceSets["main"].allSource)
        archiveClassifier.set("sources")
    }

    create<JacocoReport>("codeCoverageReport") {
        executionData(fileTree(project.rootDir.absolutePath).include("**/build/jacoco/*.exec"))

        sourceSets(sourceSets["main"])

        reports {
            xml.isEnabled = true
            xml.destination = File("$buildDir/reports/jacoco/report.xml")
            html.isEnabled = true
            csv.isEnabled = false
        }

        dependsOn("test")
    }

    withType<Wrapper> {
        gradleVersion = "5.2"
    }

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("Publication") {
            groupId = "com.dmdirc"
            artifactId = "ktirc"
            version = project.version as String
            artifact(tasks["jar"])
            artifact(tasks["sourceJar"])
            pom.withXml {
                val root = asNode()
                root.appendNode("name", "KtIrc")
                root.appendNode("description", "Kotlin library for connecting to and interacting with IRC")

                val dependenciesNode = root.appendNode("dependencies")
                configurations.implementation.get().allDependencies.forEach {
                    val dependencyNode = dependenciesNode.appendNode("dependency")
                    dependencyNode.appendNode("groupId", it.group)
                    dependencyNode.appendNode("artifactId", it.name)
                    dependencyNode.appendNode("version", it.version)
                }
            }
        }
    }
}

bintray {
    user = System.getenv("BINTRAY_USER")
    key = System.getenv("BINTRAY_API_KEY")
    setPublications("Publication")
    with(pkg) {
        userOrg = "dmdirc"
        repo = "releases"
        name = "ktirc"
        publish = true
        desc = "A kotlin library for connecting to and interacting with IRC"
        setLicenses("MIT")
        vcsUrl = "https://g.c5h.io/public/ktirc"
    }
}
