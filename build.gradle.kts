import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.20"
    application
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
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
//    implementation("edu.stanford.nlp:stanford-corenlp:4.4.0")
    implementation("edu.stanford.nlp:stanford-corenlp:4.4.0:models")
    implementation("org.threeten:threeten-extra:1.7.0")

    implementation("javax.json:javax.json-api:1.1.4")

    implementation("org.ejml:ejml-simple:0.39")
    implementation("org.ejml:ejml-ddense:0.39")
    implementation("org.ejml:ejml-core:0.39")
    compileOnly("javax.servlet:javax.servlet-api:4.0.1")
    implementation("org.apache.lucene:lucene-core:9.1.0")
    implementation("joda-time:joda-time:2.10.14")
    implementation("org.apache.lucene:lucene-queryparser:9.1.0")
    implementation("org.apache.lucene:lucene-analyzers:3.6.2")
    implementation("org.json:json:20220320")
    implementation("xom:xom:1.3.7")
    implementation("org.apache.lucene:lucene-analyzers-common:8.11.1")
    implementation("de.jollyday:jollyday:0.4.9")
    implementation("com.googlecode.json-simple:json-simple:1.1.1")
    implementation("com.apple:AppleJavaExtensions:1.4")
    implementation("org.slf4j:slf4j-api:2.0.0-alpha7")
    testImplementation("org.slf4j:slf4j-simple:2.0.0-alpha7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
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