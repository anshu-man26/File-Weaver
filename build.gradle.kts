plugins {
    java
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.fileweaver"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

extra["springCloudAwsVersion"] = "3.2.1"
extra["awsSdkVersion"] = "2.28.16"

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Caffeine cache (used for API key validation)
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // AWS SDK v2
    implementation(platform("software.amazon.awssdk:bom:${property("awsSdkVersion")}"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:sqs")
    implementation("software.amazon.awssdk:url-connection-client")

    // Spring Cloud AWS — @SqsListener
    implementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies:${property("springCloudAwsVersion")}"))
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")

    // Lambda / API Gateway
    implementation("com.amazonaws.serverless:aws-serverless-java-container-springboot3:2.1.2")
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("com.amazonaws:aws-lambda-java-events:3.13.0")

    // File-format writers
    implementation("com.github.librepdf:openpdf:1.3.43")
    implementation("org.apache.poi:poi-ooxml:5.3.0")
    implementation("com.opencsv:opencsv:5.9")

    // JSON
    implementation("com.fasterxml.jackson.core:jackson-databind")

    // JSON-format logs for CloudWatch
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")

    // Tests
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter:1.20.2")
    testImplementation("org.testcontainers:mongodb:1.20.2")
    testImplementation("org.testcontainers:localstack:1.20.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveClassifier.set("aws")
    archiveBaseName.set("fileweaver")
    archiveVersion.set("")
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
