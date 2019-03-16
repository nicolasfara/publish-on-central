import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.palantir.gradle.gitversion.*
import groovy.lang.Closure
import org.gradle.jvm.tasks.Jar
import java.net.URI

plugins {
    val gitVersionVersion = "0.12.0-rc2"
    val gradlePluginPublisVersion = "0.10.1"
    val kotlinVersion = "1.3.21"

    `java-gradle-plugin`
    `java`
    `maven-publish`
    `signing`
    id("com.palantir.git-version") version gitVersionVersion
    kotlin("jvm") version kotlinVersion
    id("com.gradle.plugin-publish") version gradlePluginPublisVersion
    id ("org.danilopianini.publish-on-central") version "0.1.0-archeo+14dc9b3"
}

group = "org.danilopianini"
val projectId = "$group.$name"
val fullName = "Gradle Publish On Maven Central Plugin"
val websiteUrl = "https://github.com/DanySK/maven-central-gradle-plugin"
val projectDetails = "A Plugin for easily publishing artifacts on Maven Central"
val pluginImplementationClass = "org.danilopianini.gradle.mavencentral.PublishOnCentral"

val versionDetails: VersionDetails = (property("versionDetails") as? Closure<VersionDetails>)?.call()
    ?: throw IllegalStateException("Unable to fetch the git version for this repository")
fun Int.asBase(base: Int = 36, digits: Int = 2) = toString(base).let {
    if (it.length >= digits) it
    else generateSequence {"0"}.take(digits - it.length).joinToString("") + it
}
val minVer = "0.1.0"
val semVer = """^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(-(0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(\.(0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*)?(\+[0-9a-zA-Z-]+(\.[0-9a-zA-Z-]+)*)?${'$'}""".toRegex()
version = with(versionDetails) {
    val tag = lastTag ?.takeIf { it.matches(semVer) }
    val baseVersion = tag ?: minVer
    val appendix = tag?.let {
        "".takeIf { commitDistance == 0 } ?: "-dev${commitDistance.asBase()}+${gitHash}"
    } ?: "-archeo+${gitHash}"
    baseVersion + appendix
}.take(20)
if (!version.toString().matches(semVer)) {
    throw IllegalStateException("Version ${version} does not match Semantic Versioning requirements")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(gradleApi())
    testImplementation(gradleTestKit())
    testImplementation("io.kotlintest:kotlintest-runner-junit5:+")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_6
}

publishOnCentral {
    //    projectnaname.set(fullName)
    projectDescription.set(projectDetails)
    scmRepoName.set("maven-central-gradle-plugin")
    repositoryURL.set(URI("sadaswdasdsa"))
    println(scmRepoName)
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
//    register<Jar>("sourcesJar") {
//        archiveClassifier.set("sources")
//        val sourceSets = project.properties["sourceSets"] as? SourceSetContainer
//            ?: throw IllegalStateException("Unable to get sourceSets for project $project. Got ${project.properties["sourceSets"]}")
//        val main = sourceSets.getByName("main").allSource
//        from(main)
//    }
//    register<Jar>("javadocJar") {
//        archiveClassifier.set("javadoc")
//        val javadoc = project.tasks.findByName("javadoc") as? Javadoc
//            ?: throw IllegalStateException("Unable to get javadoc task for project $project. Got ${project.task("javadoc")}")
//        from(javadoc.destinationDir)
//    }
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.6"
    }
//    withType<Sign> {
//        onlyIf { project.property("signArchivesIsEnabled")?.toString()?.toBoolean() ?: false }
//    }
}

// Add the classpath file to the test runtime classpath
dependencies {
    testRuntimeOnly(files(tasks["createClasspathManifest"]))
}


publishing {
//    publications {
//        create<MavenPublication>("mavenCentral") {
//            groupId = project.group.toString()
//            artifactId = project.name
//            version = project.version.toString()
//            from(components["java"])
//            artifact(project.property("sourcesJar"))
//            artifact(project.property("javadocJar"))
//            pom {
//                name.set(fullName)
//                description.set(projectDetails)
//                url.set(websiteUrl)
//                licenses {
//                    license {
//                        name.set("")
//                        url.set("")
//                    }
//                }
//                scm {
//                    url.set(this@pom.url.get())
//                    connection.set("git@github.com:DanySK/maven-central-gradle-plugin.git")
//                    developerConnection.set(connection.get())
//                }
//            }
//        }
//    }
    repositories {
        maven {
            url = URI("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = "danysk"
                password = project.property("ossrhPassword").toString()
            }
        }
    }
}

//configure<SigningExtension> {
//    sign(publishing.publications.getByName("mavenCentral"))
//}

pluginBundle {
    website = websiteUrl
    vcsUrl = websiteUrl
    tags = listOf("maven", "maven central", "ossrh", "central", "publish")
}

gradlePlugin {
    plugins {
        create(projectId) {
            id = projectId
            displayName = fullName
            description = projectDetails
            implementationClass = pluginImplementationClass
        }
    }
}
