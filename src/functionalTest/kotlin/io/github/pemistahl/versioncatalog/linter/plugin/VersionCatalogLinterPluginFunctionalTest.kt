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

package io.github.pemistahl.versioncatalog.linter.plugin

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VersionCatalogLinterPluginFunctionalTest {
    private val projectDir = File("build/functionalTest")
    private val versionCatalogDir = File(projectDir, "gradle")
    private val buildFilePath = File(projectDir, "build.gradle.kts").toPath()
    private val settingsFilePath = File(projectDir, "settings.gradle.kts").toPath()
    private val versionCatalogFilePath = File(versionCatalogDir, "libs.versions.toml").toPath()
    private val customVersionCatalogFileName = "libraries.toml"
    private val customVersionCatalogFilePath = File(projectDir, customVersionCatalogFileName).toPath()

    private val unformattedVersionCatalogContent =
        """
        [libraries]
        activation =   { name = "javax.activation", group = "com.sun.activation", version = "1.2.0" }
        antisamy = { group = "org.owasp.antisamy", name = "antisamy", version = "1.5.2" }
        antlr = "antlr:antlr:2.7.7"

        [plugins]
        shadowJar = { id = "com.github.johnrengelman.shadow", version = "8.1.1" }

        ktlint = { version = "12.0.2", id = "org.jlleitschuh.gradle.ktlint" }

        [versions]
        axis = "1.3"
         byteBuddy = "1.12.9"
        cache2k =   "2.0.0.Final"
        slf4j = { prefer = "1.7.25", strictly = "[1.7, 1.8[" }

        [bundles]
        foo = [ "antlr", "activation", "antisamy" ]
        bar = [
            "antisamy",
            "activation"
        ]
        """.trimIndent()

    private val formattedVersionCatalogContent =
        """
        [versions]
        axis = "1.3"
        byteBuddy = "1.12.9"
        cache2k = "2.0.0.Final"
        slf4j = { strictly = "[1.7, 1.8[", prefer = "1.7.25" }

        [libraries]
        activation = { group = "com.sun.activation", name = "javax.activation", version = "1.2.0" }
        antisamy = { group = "org.owasp.antisamy", name = "antisamy", version = "1.5.2" }
        antlr = { group = "antlr", name = "antlr", version = "2.7.7" }

        [bundles]
        bar = [
            "activation",
            "antisamy"
        ]
        foo = [
            "activation",
            "antisamy",
            "antlr"
        ]

        [plugins]
        ktlint = { id = "org.jlleitschuh.gradle.ktlint", version = "12.0.2" }
        shadowJar = { id = "com.github.johnrengelman.shadow", version = "8.1.1" }

        """.trimIndent()

    private val formattedVersionCatalogContentWithBom =
        """
        [libraries]
        activation = { group = "com.sun.activation", name = "javax.activation", version = "1.2.0" }
        azure = { group = "com.azure", name = "azure-sdk", version = "1.2.35" }
        quarkus = { module = "io.quarkus.platform:quarkus-bom", version = "3.21.2" }
        quarkusArc = { module = "io.quarkus:quarkus-arc" }

        """.trimIndent()

    private val errorMessages =
        listOf(
            "Line 2: Attributes of library with alias 'activation' are not sorted correctly. " +
                "Required order: [module | group], name (, version(.ref))",
            "Line 2: Entry with alias 'activation' in section '[libraries]' must not have " +
                "two or more adjacent whitespace characters.",
            "Line 4: Use table notation instead of string notation for library with alias 'antlr'. " +
                "Required order: [module | group], name (, version(.ref))",
            "Line 7: Entries are not sorted alphabetically in section '[plugins]'. " +
                "Found alias 'shadowJar' where 'ktlint' was expected.",
            "Line 9: Attributes of plugin with alias 'ktlint' are not sorted correctly. " +
                "Required order: id, version(.ref)",
            "Line 9: Entries are not sorted alphabetically in section '[plugins]'. " +
                "Found alias 'ktlint' where 'shadowJar' was expected.",
            "Line 13: Entry with alias 'byteBuddy' in section '[versions]' must not have leading whitespace.",
            "Line 14: Entry with alias 'cache2k' in section '[versions]' must not have " +
                "two or more adjacent whitespace characters.",
            "Line 15: Version attributes of entry with alias 'slf4j' are not sorted correctly. " +
                "Required order: strictly, require, prefer, reject",
            "Line 18: Bundle with alias 'foo' must be indented with each library on a " +
                "separate line preceded by four whitespace characters.",
            "Line 18: Entries are not sorted alphabetically in section '[bundles]'. " +
                "Found alias 'foo' where 'bar' was expected.",
            "Line 18: Libraries of bundle with alias 'foo' are not sorted alphabetically. " +
                "Found library 'activation' where 'antisamy' was expected.",
            "Line 18: Libraries of bundle with alias 'foo' are not sorted alphabetically. " +
                "Found library 'antisamy' where 'antlr' was expected.",
            "Line 18: Libraries of bundle with alias 'foo' are not sorted alphabetically. " +
                "Found library 'antlr' where 'activation' was expected.",
            "Lines 19-22: Entries are not sorted alphabetically in section '[bundles]'. " +
                "Found alias 'bar' where 'foo' was expected.",
            "Line 20: Libraries of bundle with alias 'bar' are not sorted alphabetically. " +
                "Found library 'antisamy' where 'activation' was expected.",
            "Line 21: Libraries of bundle with alias 'bar' are not sorted alphabetically. " +
                "Found library 'activation' where 'antisamy' was expected.",
        )

    init {
        versionCatalogDir.toPath().createDirectories()
    }

    @BeforeTest
    fun beforeTest() {
        buildFilePath.createFile()
        settingsFilePath.createFile()
        versionCatalogFilePath.createFile()
        customVersionCatalogFilePath.createFile()
    }

    @AfterTest
    fun afterTest() {
        buildFilePath.deleteIfExists()
        settingsFilePath.deleteIfExists()
        versionCatalogFilePath.deleteIfExists()
        customVersionCatalogFilePath.deleteIfExists()
    }

    @Test
    fun testPluginWithDefaultSettingsAndWellFormattedVersionCatalog() {
        buildFilePath.writeText(
            """
            plugins {
                id("io.github.pemistahl.version-catalog-linter")
            }
            """.trimIndent(),
        )

        versionCatalogFilePath.writeText(formattedVersionCatalogContent)

        val checkTaskResult =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("checkVersionCatalog")
                .build()

        assertEquals(
            TaskOutcome.SUCCESS,
            checkTaskResult.task(":checkVersionCatalog")?.outcome,
        )

        val formatTaskResult =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("formatVersionCatalog")
                .build()

        assertEquals(
            TaskOutcome.SUCCESS,
            formatTaskResult.task(":formatVersionCatalog")?.outcome,
        )

        val actualVersionCatalogContent = versionCatalogFilePath.readText()
        assertEquals(formattedVersionCatalogContent, actualVersionCatalogContent)
    }

    @Test
    fun testPluginWithDefaultSettingsAndUnformattedVersionCatalog() {
        buildFilePath.writeText(
            """
            plugins {
                id("io.github.pemistahl.version-catalog-linter")
            }
            """.trimIndent(),
        )

        versionCatalogFilePath.writeText(unformattedVersionCatalogContent)
        customVersionCatalogFilePath.writeText(unformattedVersionCatalogContent)

        val checkTaskResult =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("checkVersionCatalog")
                .buildAndFail()

        assertEquals(
            TaskOutcome.FAILED,
            checkTaskResult.task(":checkVersionCatalog")?.outcome,
        )

        assertTrue(checkTaskResult.output.containsAll(errorMessages))

        val formatTaskResult =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("formatVersionCatalog")
                .build()

        assertEquals(
            TaskOutcome.SUCCESS,
            formatTaskResult.task(":formatVersionCatalog")?.outcome,
        )

        val defaultVersionCatalogContent = versionCatalogFilePath.readText()
        assertEquals(formattedVersionCatalogContent, defaultVersionCatalogContent)

        val customVersionCatalogContent = customVersionCatalogFilePath.readText()
        assertEquals(unformattedVersionCatalogContent, customVersionCatalogContent)
    }

    @Test
    fun testPluginWithCustomSettingsAndUnformattedVersionCatalog() {
        buildFilePath.writeText(
            """
            plugins {
                id("io.github.pemistahl.version-catalog-linter")
            }

            versionCatalogLinter {
                versionCatalogFile = file("$customVersionCatalogFileName")
            }
            """.trimIndent(),
        )

        customVersionCatalogFilePath.writeText(unformattedVersionCatalogContent)
        versionCatalogFilePath.writeText(unformattedVersionCatalogContent)

        val checkTaskResult =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("checkVersionCatalog")
                .buildAndFail()

        assertEquals(
            TaskOutcome.FAILED,
            checkTaskResult.task(":checkVersionCatalog")?.outcome,
        )

        assertTrue(checkTaskResult.output.containsAll(errorMessages))

        val formatTaskResult =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("formatVersionCatalog")
                .build()

        assertEquals(
            TaskOutcome.SUCCESS,
            formatTaskResult.task(":formatVersionCatalog")?.outcome,
        )

        val customVersionCatalogContent = customVersionCatalogFilePath.readText()
        assertEquals(formattedVersionCatalogContent, customVersionCatalogContent)

        val defaultVersionCatalogContent = versionCatalogFilePath.readText()
        assertEquals(unformattedVersionCatalogContent, defaultVersionCatalogContent)
    }

    @Test
    fun testPluginWithUnavailableVersionCatalog() {
        versionCatalogFilePath.deleteIfExists()

        buildFilePath.writeText(
            """
            plugins {
                id("io.github.pemistahl.version-catalog-linter")
            }
            """.trimIndent(),
        )

        val checkTaskResult =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("checkVersionCatalog")
                .buildAndFail()

        assertEquals(
            TaskOutcome.FAILED,
            checkTaskResult.task(":checkVersionCatalog")?.outcome,
        )

        assertTrue(
            checkTaskResult.output.contains(
                "An input file was expected to be present but it doesn't exist.",
            ),
        )
    }

    @Test
    fun testPluginWithDefaultSettingsAndFormattedVersionCatalogWithBom() {
        buildFilePath.writeText(
            """
            plugins {
                id("io.github.pemistahl.version-catalog-linter")
            }
            """.trimIndent(),
        )

        versionCatalogFilePath.writeText(formattedVersionCatalogContentWithBom)

        val checkTaskResult =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("checkVersionCatalog")
                .buildAndFail()

        assertEquals(
            TaskOutcome.FAILED,
            checkTaskResult.task(":checkVersionCatalog")?.outcome,
        )

        assertTrue(
            checkTaskResult.output.contains(
                "Line 5: Library with alias 'quarkusArc' has no version defined " +
                    "and no BOM declaration exists for it.",
            ),
        )
    }

    @Test
    fun testPluginWithValidCustomSettingsAndFormattedVersionCatalogWithBom() {
        buildFilePath.writeText(
            """
            plugins {
                id("io.github.pemistahl.version-catalog-linter")
            }

            versionCatalogLinter {
                bomsAndDependencies.put("quarkus", listOf("quarkusArc"))
            }
            """.trimIndent(),
        )

        versionCatalogFilePath.writeText(formattedVersionCatalogContentWithBom)

        val checkTaskResult =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("checkVersionCatalog")
                .build()

        assertEquals(
            TaskOutcome.SUCCESS,
            checkTaskResult.task(":checkVersionCatalog")?.outcome,
        )
    }

    @Test
    fun testPluginWithInvalidCustomSettingsAndFormattedVersionCatalogWithBom() {
        buildFilePath.writeText(
            """
            plugins {
                id("io.github.pemistahl.version-catalog-linter")
            }

            versionCatalogLinter {
                bomsAndDependencies.put("azure", listOf())
                bomsAndDependencies.put("spring", listOf("spring-boot"))
                bomsAndDependencies.put("quarkus", listOf("quarkusArc"))
            }
            """.trimIndent(),
        )

        versionCatalogFilePath.writeText(formattedVersionCatalogContentWithBom)

        val checkTaskResult =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("checkVersionCatalog")
                .buildAndFail()

        assertEquals(
            TaskOutcome.FAILED,
            checkTaskResult.task(":checkVersionCatalog")?.outcome,
        )

        assertTrue(
            checkTaskResult.output.containsAll(
                listOf(
                    "The following aliases in the version catalog " +
                        "linter settings cannot be matched with " +
                        "a library in the version catalog: 'spring', 'spring-boot'",
                    "The libraries identified by the following aliases " +
                        "do not seem to be proper BOMs as their names " +
                        "do not end with the suffix '-bom' or '-dependencies': 'azure'",
                ),
            ),
        )
    }

    private fun String.containsAll(elements: List<CharSequence>): Boolean = elements.all { this.contains(it) }
}
