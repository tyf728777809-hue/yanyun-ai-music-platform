plugins {
    `java-library`
}

dependencies {
    api(project(":modules:agent-runtime"))
    api(project(":modules:creative-agent"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.4")
}
