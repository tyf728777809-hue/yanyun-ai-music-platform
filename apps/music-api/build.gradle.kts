plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":modules:common"))
    implementation(project(":modules:auth"))
    implementation(project(":modules:quota"))
    implementation(project(":modules:moderation"))
    implementation(project(":modules:publish"))
    implementation(project(":modules:work-domain"))
    implementation(project(":modules:music-provider"))
    implementation(project(":modules:workflow"))
    implementation(project(":modules:storage"))
    implementation(project(":modules:observability"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.14")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.processResources {
    from(rootProject.file("database/migrations")) {
        include("V*.sql")
        into("db/migration")
    }
}
