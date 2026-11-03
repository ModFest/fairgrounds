plugins {
    id("maven-publish")
    alias(libs.plugins.loom)
    alias(libs.plugins.minotaur)
}

val slug = property("slug") as String
val compatibleVersions = property("compatibleVersions") as String

version = "${property("baseVersion")}+${property("branch")}"

repositories {
	maven("https://repo.sleeping.town/")
	maven("https://maven.theepicblock.nl/")
	maven("https://maven.nucleoid.xyz/")
	// maven("https://maven.isxander.dev/releases")
	mavenLocal()
}

dependencies {
    minecraft(libs.mc)
    implementation(libs.fl)
    implementation(libs.fapi)

    implementation(libs.placeholder)
    include(libs.placeholder)

    implementation(libs.commonsText)
    include(libs.commonsText)
}

loom {
	runs {
		configureEach {
			preferGradleTask = false
		}

		val clientMixinSwap = register("clientMixinSwap") {
			client()
			generateRunConfig = true
			displayName = "Minecraft Client - (Mixin Swap)"
			sourceSet = sourceSets.main.name

			jvmArguments.add("-Dmixin.debug.export=true")
		}

		afterEvaluate {
			try {
				val compileClasspath = project.configurations.getByName("compileClasspath")
				val resolved = compileClasspath.resolvedConfiguration.resolvedArtifacts
				val artifact = resolved.firstOrNull { it.name == "sponge-mixin" }

				if (artifact != null) {

					clientMixinSwap.get().jvmArguments.add("-javaagent:\"${artifact.file.absolutePath}\"")

					logger.lifecycle("Mixin Hotswap Run should be working")
				} else {
					logger.warn("Unable to locate file path for Mixin Jar, HotSwap Run will not work!")
				}
			} catch (e: Exception) {
				logger.warn("Failed to setup MixinHotswap! Enable logging to view why.")
				// logger.error("Failed to setup MixinHotswap!", e)
			}
		}
	}
}

tasks.processResources {
    val user = rootProject.property("user")
    val authors = rootProject.property("authors") as String
    val contributors = rootProject.property("contributors") as String
    val meta = mapOf(
        "version"			to version,
        "modId"				to rootProject.property("modId"),
        "modName"			to rootProject.property("modName"),
        "modDescription"	to rootProject.property("modDescription"),
        "homepage"			to "https://modrinth.com/mod/${slug}",
        "issues"			to "https://github.com/${user}/${slug}/issues",
        "sources"			to "https://github.com/${user}/${slug}",
        "license"			to rootProject.property("license"),
        "authors"			to authors.split(", ").joinToString("\",\n    \""),
        "contributors"		to contributors.split(", ").joinToString("\",\n    \""),
        "members"			to "$authors. Contributions by $contributors",
        "mc"				to compatibleVersions.split(", ")[0],
        "fl"				to libs.versions.fl.get(),
        "fapi"				to libs.versions.fapi.get(),
        "placeholder"		to libs.versions.placeholder.get(),
//		"polydex"			to libs.versions.polydex.get()
    )

    inputs.properties(meta)
    filesMatching("*.mod.json") { expand(meta) }
    filesMatching("META-INF/*mods.toml") { expand(meta) }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release = 25
}

java {
	withSourcesJar()
	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
}

tasks.jar {
	val projectName = project.name
	inputs.property("projectName", projectName)

	from("LICENSE") {
		rename { "${it}_$projectName" }
	}
}

// configure the maven publication
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

modrinth {
    val compatibleLoaders = property("compatibleLoaders") as String

    token = "${System.getenv("MODRINTH_TOKEN")}"
    projectId = slug
    versionNumber = project.version as String
    uploadFile.set(tasks.jar)
    gameVersions = compatibleVersions.split(", ").toList()
    loaders = compatibleLoaders.split(", ").toList()
    changelog = "${System.getenv("CHANGELOG")}"
    syncBodyFrom = "<!--DO NOT EDIT MANUALLY: synced from gh readme-->\n" + rootProject.file("README.md").readText()
    dependencies {
        required.version("fabric-api", libs.versions.fapi.get())
        embedded.version("placeholder-api", libs.versions.placeholder.get())
//		optional.version "polydex", libs.versions.polydex.get()
    }
}
