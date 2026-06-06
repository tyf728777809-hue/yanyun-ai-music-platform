pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "yanyun-ai-music-platform"

include(
    "apps:music-api",
    "apps:music-worker",
    "modules:common",
    "modules:auth",
    "modules:quota",
    "modules:moderation",
    "modules:publish",
    "modules:work-domain",
    "modules:agent-runtime",
    "modules:lyrics",
    "modules:knowledge",
    "modules:prompt",
    "modules:music-provider",
    "modules:production",
    "modules:dreammaker",
    "modules:minimax",
    "modules:suno",
    "modules:deepseek",
    "modules:image2",
    "modules:media",
    "modules:storage",
    "modules:workflow",
    "modules:config-center",
    "modules:observability",
)
