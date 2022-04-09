import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    application
}

group = "me.richa"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.apache.opennlp:opennlp-tools:1.9.4")
    implementation("us.codecraft:xsoup:0.3.2")
    implementation("org.apache.commons:commons-lang3:3.12.0")

    implementation("com.fasterxml.jackson.core:jackson-core:2.13.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.2.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.2")
    implementation("org.seleniumhq.selenium:selenium-java:4.1.3")

    testImplementation("org.jetbrains.kotlin:kotlin-test:1.6.10")
    implementation("mysql:mysql-connector-java:8.0.28")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("com.konghq:unirest-java:4.0.0-RC2")
    implementation("org.apache.httpcomponents:httpclient:4.5.13")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.2.2")
    implementation("com.fasterxml.jackson.core:jackson-core:2.13.2")
    implementation("com.github.javafaker:javafaker:1.0.2")
    implementation(kotlin("reflect"))
    implementation("io.github.bonigarcia:webdrivermanager:5.1.0")


}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "16"
}

application {
    mainClass.set("MainKt")
}