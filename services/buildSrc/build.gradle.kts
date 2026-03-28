plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.20")
    implementation("org.jetbrains.kotlin:kotlin-allopen:2.3.20")
    implementation("org.springframework.boot:spring-boot-gradle-plugin:4.0.3")
    implementation("io.spring.gradle:dependency-management-plugin:1.1.7")
}
