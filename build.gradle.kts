import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import groovy.lang.Closure
import org.danilopianini.gradle.mavencentral.PublishOnCentralExtension
import org.gradle.jvm.tasks.Jar
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URI

plugins {
    `java-gradle-plugin`
    `java`
    `maven-publish`
    `signing`
    id("org.danilopianini.git-sensitive-semantic-versioning")
    kotlin("jvm")
    id("com.gradle.plugin-publish")
    id ("org.danilopianini.publish-on-central")
    id("org.jetbrains.dokka")
}

gitSemVer {
    version = computeGitSemVer()
}

group = "org.danilopianini"
val projectId = "$group.$name"
val fullName = "Gradle Publish On Maven Central Plugin"
val websiteUrl = "https://github.com/DanySK/maven-central-gradle-plugin"
val projectDetails = "A Plugin for easily publishing artifacts on Maven Central"
val pluginImplementationClass = "org.danilopianini.gradle.mavencentral.PublishOnCentral"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(gradleApi())
    testImplementation(gradleTestKit())
    testImplementation("io.kotest:kotest-runner-junit5:_")
    testImplementation("io.kotest:kotest-assertions-core-jvm:_")
}

tasks.withType<DokkaTask> {
    outputDirectory = "$buildDir/javadoc"
    jdkVersion = 8
    reportUndocumented = false
    outputFormat = "javadoc"
}

publishOnCentral {
    projectDescription.set(projectDetails)
    projectLongName.set(fullName)
    projectUrl.set(websiteUrl)
    scmConnection.set("git:git@github.com:DanySK/maven-central-gradle-plugin")
}

tasks {
    "test"(Test::class) {
        useJUnitPlatform()
        testLogging.showStandardStreams = true
        testLogging {
            showCauses = true
            showStackTraces = true
            showStandardStreams = true
            events(*TestLogEvent.values())
        }
    }
    register("createClasspathManifest") {
        val outputDir = file("$buildDir/$name")
        inputs.files(sourceSets.main.get().runtimeClasspath)
        outputs.dir(outputDir)
        doLast {
            outputDir.mkdirs()
            file("$outputDir/plugin-classpath.txt").writeText(sourceSets.main.get().runtimeClasspath.joinToString("\n"))
        }
    }
}

// Add the classpath file to the test runtime classpath
dependencies {
    testRuntimeOnly(files(tasks["createClasspathManifest"]))
}

publishing {
    publications {
        withType<MavenPublication>() {
            pom {
                developers {
                    developer {
                        name.set("Danilo Pianini")
                        email.set("danilo.pianini@gmail.com")
                        url.set("http://www.danilopianini.org/")
                    }
                }
            }
        }
    }
}

pluginBundle {
    website = websiteUrl
    vcsUrl = websiteUrl
    tags = listOf("maven", "maven central", "ossrh", "central", "publish")
}

gradlePlugin {
    plugins {
        create("PublishOnCentralPlugin") {
            id = projectId
            displayName = fullName
            description = projectDetails
            implementationClass = pluginImplementationClass
        }
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
}

val registerCredentials = tasks.register("registerGradlePluginPortalCredentials") {
    doLast {
        listOf("gradle.publish.key", "gradle.publish.secret").forEach {
            if (!(project.hasProperty(it) or System.getenv().containsKey(it))) {
                val bashName = it.toUpperCase().replace(".", "_")
                System.getProperties().setProperty(it, System.getenv(bashName)
                    ?: throw IllegalStateException("Property $it is unset and environment variable $bashName unavailable")
                )
            }
        }
    }
}

tasks.publishPlugins {
    dependsOn(registerCredentials)
}
