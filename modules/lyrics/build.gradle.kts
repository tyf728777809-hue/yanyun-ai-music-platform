plugins {
    `java-library`
}

dependencies {
    api(project(":modules:agent-runtime"))
    api(project(":modules:creative-agent"))
    api(project(":modules:deepseek"))
    api(project(":modules:knowledge"))
    api(project(":modules:prompt"))
}
