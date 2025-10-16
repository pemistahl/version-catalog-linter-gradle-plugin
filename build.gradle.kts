/*
 * Copyright © 2024 Peter M. Stahl pemistahl@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    kotlin("jvm") version "2.2.20"
    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
    id("com.gradleup.shadow") version "9.2.2"
    id("com.gradle.plugin-publish") version "2.0.0"
}

group = "io.github.pemistahl"

kotlin {
    jvmToolchain(21)
}

testing {
    suites {
        configureEach {
            if (this is JvmTestSuite) {
                useJUnitJupiter()
                dependencies {
                    implementation(gradleApi())
                    implementation("org.jetbrains.kotlin:kotlin-test")
                    implementation("org.jetbrains.kotlin:kotlin-test-junit5")
                }
            }
        }

        val test by getting(JvmTestSuite::class)

        register<JvmTestSuite>("functionalTest") {
            dependencies {
                implementation(project())
            }
            targets {
                all {
                    testTask.configure {
                        mustRunAfter(test)
                    }
                }
            }
        }
    }
}

tasks.check {
    dependsOn(testing.suites.named("functionalTest"))
}

tasks.shadowJar {
    archiveClassifier = ""
    enableAutoRelocation = true
}

gradlePlugin {
    plugins {
        create("versionCatalogLinter") {
            id = "io.github.pemistahl.version-catalog-linter"
            displayName = "Plugin for linting Gradle version catalogs"
            description = "A plugin for checking and formatting version catalog TOML files"
            vcsUrl = "https://github.com/pemistahl/version-catalog-linter-gradle-plugin.git"
            website = "https://github.com/pemistahl/version-catalog-linter-gradle-plugin"
            tags = listOf("version-catalog", "dependency-management", "dependencies")
            implementationClass = "io.github.pemistahl.versioncatalog.linter.plugin.VersionCatalogLinterPlugin"
        }
    }
    testSourceSets(sourceSets.named("functionalTest").get())
}

dependencies {
    implementation("org.tomlj:tomlj:1.1.1")
}

repositories {
    mavenCentral()
}
