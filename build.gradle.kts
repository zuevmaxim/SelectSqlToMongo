import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    kotlin("jvm") version "1.3.61"
    antlr
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    // parsing
    implementation("org.antlr:antlr4-runtime:4.7")
    antlr("org.antlr:antlr4:4.7")

    // tests
    val junitVersion = "5.6.0"
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testCompile("org.junit.jupiter:junit-jupiter-params:$junitVersion")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
        dependsOn(generateGrammarSource)
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    generateGrammarSource {
        arguments.addAll(listOf("-package", "ru.sqltomongo.select.parser", "-visitor"))
        outputDirectory = File("$buildDir/generated-src/antlr/main/ru/sqltomongo/select/parser")
    }
    "test"(Test::class) {
        dependsOn("cleanTest")
        useJUnitPlatform()
        testLogging {
            events = setOf(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED)

        }
    }
}
