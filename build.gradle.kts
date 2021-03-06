import com.jfrog.bintray.gradle.BintrayExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

val local = Properties()
val localProperties: java.io.File = rootProject.file("local.properties")
if (localProperties.exists()) {
    localProperties.inputStream().use { local.load(it) }
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.2.71"
    id("com.jfrog.bintray") version "1.8.1"
    `maven-publish`
}

defaultTasks("clean", "build")

group = "com.github.vokorm"
version = "0.11-SNAPSHOT"

repositories {
    jcenter()
    maven { setUrl("https://dl.bintray.com/mvysny/github") }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // logging
    compile("org.slf4j:slf4j-api:1.7.25")

    // db
    compile("org.sql2o:sql2o:1.5.4")
    compile("com.zaxxer:HikariCP:3.2.0")

    // validation support
    compile("javax.validation:validation-api:2.0.0.Final")  // to have JSR303 validations in the entities
    testCompile("org.hibernate.validator:hibernate-validator:6.0.11.Final")
    // EL is required: http://hibernate.org/validator/documentation/getting-started/
    testCompile("org.glassfish:javax.el:3.0.1-b08")

    // tests
    testCompile("com.github.mvysny.dynatest:dynatest-engine:0.11")
    testCompile("com.google.code.gson:gson:2.8.5")
    testCompile("ch.qos.logback:logback-classic:1.2.3")
    testCompile("com.h2database:h2:1.4.197")

    testCompile("org.postgresql:postgresql:42.2.1")
    testCompile("org.zeroturnaround:zt-exec:1.10")
    testCompile("mysql:mysql-connector-java:5.1.47")
    testCompile("org.mariadb.jdbc:mariadb-java-client:2.3.0")
}

val sourceJar = task("sourceJar", Jar::class) {
    dependsOn(tasks.findByName("classes"))
    classifier = "sources"
    from(java.sourceSets["main"].allSource)
}

publishing {
    publications {
        create("mavenJava", MavenPublication::class.java).apply {
            groupId = project.group.toString()
            this.artifactId = "vok-orm"
            version = project.version.toString()
            pom.withXml {
                val root = asNode()
                root.appendNode("description", "A very simple persistence framework, built on top of Sql2o")
                root.appendNode("name", "VoK-ORM")
                root.appendNode("url", "https://github.com/mvysny/vok-orm")
            }
            from(components.findByName("java")!!)
            artifact(sourceJar) {
                classifier = "sources"
            }
        }
    }
}

bintray {
    user = local.getProperty("bintray.user")
    key = local.getProperty("bintray.key")
    pkg(closureOf<BintrayExtension.PackageConfig> {
        repo = "github"
        name = "com.github.vokorm"
        setLicenses("MIT")
        vcsUrl = "https://github.com/mvysny/vok-orm"
        publish = true
        setPublications("mavenJava")
        version(closureOf<BintrayExtension.VersionConfig> {
            this.name = project.version.toString()
            released = Date().toString()
        })
    })
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        // to see the exceptions of failed tests in Travis-CI console.
        exceptionFormat = TestExceptionFormat.FULL
    }
}
