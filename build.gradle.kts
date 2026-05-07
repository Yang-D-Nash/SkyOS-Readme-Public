plugins {
    id("com.android.application") version "9.1.1" apply false
    id("com.android.kotlin.multiplatform.library") version "9.1.1" apply false
    id("com.google.gms.google-services") version "4.4.3" apply false
    id("com.google.firebase.crashlytics") version "3.0.4" apply false
    kotlin("multiplatform") version "2.3.20" apply false
    kotlin("plugin.serialization") version "2.3.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false
    id("dev.detekt") version "2.0.0-alpha.2" apply false
}

// Aggregate: `./gradlew detektAll` (wired after each subproject applies the detekt plugin)
val detektAll =
    tasks.register("detektAll") {
        group = "verification"
        description = "Runs detekt on :androidApp and :shared (unused members, style, complexity)"
    }

subprojects {
    pluginManager.withPlugin("dev.detekt") {
        detektAll.configure {
            dependsOn(tasks.named("detekt"))
        }
    }
}
