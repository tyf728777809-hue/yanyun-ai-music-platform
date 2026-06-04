plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":modules:common"))
    implementation(project(":modules:observability"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.14")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.named<Jar>("jar") {
    enabled = false
}
