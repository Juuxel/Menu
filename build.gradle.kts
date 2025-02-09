import net.fabricmc.filament.task.DownloadTask
import net.fabricmc.filament.task.MapJarTask

buildscript {
    repositories {
        gradlePluginPortal()
    }

    dependencies {
        classpath("org.ow2.asm:asm:9.7.1")
    }
}

plugins {
    id("base")
    id("net.fabricmc.filament")
}

configurations {
    minecraftLibraries {
        isTransitive = false

        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
            attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(TargetJvmEnvironment::class.java, TargetJvmEnvironment.STANDARD_JVM))
        }
    }

    register("enigmaRuntime") {
        isCanBeResolved = true
        isCanBeConsumed = false

        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
            attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(TargetJvmEnvironment::class.java, TargetJvmEnvironment.STANDARD_JVM))
        }
    }
}

repositories {
    mavenCentral {
		content {
			// Force LWJGL from Mojang's repo, as they have custom patched versions since 1.21-pre1
			excludeGroupByRegex("org.lwjgl")
		}

        metadataSources {
            mavenPom()
        }
    }
    maven("https://maven.fabricmc.net")
    maven("https://libraries.minecraft.net")
}

dependencies {
    "enigmaRuntime"("cuchaz:enigma-swing:2.5.2") {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling::class.java, Bundling.EXTERNAL))
        }

        exclude(group = "com.google.errorprone")
        exclude(group = "org.checkerframework")
        exclude(group = "com.google.code.findbugs")
    }
}

val gameVersion = "1.21.3"

filament {
    minecraftVersion = gameVersion
}

val downloadIntermediary = tasks.register<DownloadTask>("downloadIntermediary") {
	url = "https://github.com/FabricMC/intermediary/raw/master/mappings/$gameVersion.tiny"
	output = file("$gameVersion-intermediary.tiny")
}

val mapIntermediaryJar = tasks.register<MapJarTask>("mapIntermediaryJar") {
	dependsOn(downloadIntermediary, tasks.mergeMinecraftJars)
	output.set(layout.buildDirectory.file("$gameVersion-intermediary.jar"))
	input.set(tasks.mergeMinecraftJars.get().output)
	mappings.set(downloadIntermediary.get().output)
	classpath.from(configurations["minecraftLibraries"])
	from.set("official")
	to.set("intermediary")
}

tasks.register<JavaExec>("enigma") {
    dependsOn(mapIntermediaryJar)
    classpath = configurations["enigmaRuntime"]
    mainClass.set("cuchaz.enigma.gui.Main")
    jvmArgs("-Xmx2048m")
    args("-jar", mapIntermediaryJar.get().output.get().asFile.absolutePath)
    args("-mappings", file("mappings").absolutePath)
}

tasks.register<juuxel.menu.gradle.AddNestedClasses>("addNestedClasses") {
    minecraftJar.set(mapIntermediaryJar.flatMap { it.output })
    mappingDir.set(file("mappings"))
}
