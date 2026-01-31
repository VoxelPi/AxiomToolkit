import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

plugins {
    id("axiom.build")
    alias(libs.plugins.shadow)
    application
}

dependencies {
    // Kotlin
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    implementation(libs.bundles.kotlinx.coroutines)

    // Project
    api(projects.axiomCore)
    api(projects.axiomAsm)
    api(projects.axiomBridge)
    api(projects.axiomArch.axiomArchAx08)
    api(projects.axiomArch.axiomArchDev08)
    api(projects.axiomArch.axiomArchDev16)
    api(projects.axiomArch.axiomArchDev32)
    api(projects.axiomArch.axiomArchDev64)
    api(projects.axiomArch.axiomArchMcpc08)
    api(projects.axiomArch.axiomArchMcpc16)

    // Logging
    implementation(libs.kotlin.logging.jvm)
    runtimeOnly(libs.slf4j.api)
    runtimeOnly(libs.log4j.slf4j.impl)

    // Other
    implementation(libs.clikt)
    implementation(libs.cloud.core)
    implementation(libs.cloud.kotlin)
    implementation(libs.jline)
    implementation(libs.mordant)
}

application {
    mainClass.set("net.voxelpi.axiom.cli.MainKt")
}

tasks {
    shadowJar {
        archiveClassifier = ""
        transform(Log4j2PluginsCacheFileTransformer::class.java)

        manifest {
            attributes(
                "Main-Class" to "net.voxelpi.axiom.cli.MainKt",
                "Multi-Release" to true
            )
        }
    }

    named<JavaExec>("run") {
        standardInput = System.`in`
    }

    jar {
        archiveClassifier = "thin"
        enabled = false
    }
}
