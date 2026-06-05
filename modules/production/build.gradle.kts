plugins {
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.9")
    }
}

dependencies {
    implementation(project(":modules:moderation"))
    implementation(project(":modules:music-provider"))
    implementation(project(":modules:publish"))
    implementation(project(":modules:quota"))
    implementation(project(":modules:storage"))
    implementation(project(":modules:work-domain"))
    implementation(project(":modules:workflow"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-jdbc")
}
