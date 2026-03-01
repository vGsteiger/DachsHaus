plugins {
    kotlin("jvm")
}

dependencies {
    implementation("io.jsonwebtoken:jjwt-api:0.12.0")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.0")
    implementation("io.jsonwebtoken:jjwt-jackson:0.12.0")
    implementation("org.springframework.kafka:spring-kafka:3.1.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.0")
}

tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}
