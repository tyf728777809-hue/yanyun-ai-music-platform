plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":modules:common"))
    implementation(project(":modules:auth"))
    implementation(project(":modules:dreammaker"))
    implementation(project(":modules:minimax"))
    implementation(project(":modules:moderation"))
    implementation(project(":modules:music-provider"))
    implementation(project(":modules:production"))
    implementation(project(":modules:publish"))
    implementation(project(":modules:quota"))
    implementation(project(":modules:storage"))
    implementation(project(":modules:suno"))
    implementation(project(":modules:workflow"))
    implementation(project(":modules:observability"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("io.temporal:temporal-sdk:1.35.0")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.processResources {
    from(rootProject.file("database/migrations")) {
        include("V*.sql")
        into("db/migration")
    }
}

tasks.named<Jar>("jar") {
    enabled = false
}
