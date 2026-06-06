plugins {
    `java-library`
}

dependencies {
    implementation(project(":modules:dreammaker"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.4")
    api(project(":modules:media"))
}
