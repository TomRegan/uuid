import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val junitVersion = "5.2.0"

plugins {
    kotlin("jvm") version "1.3.11"
}

group = "uu.id"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
}
dependencies {
    compile(kotlin("stdlib-jdk8"))
    // tests
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.hamcrest:hamcrest-all:1.3")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}