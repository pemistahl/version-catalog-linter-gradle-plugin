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

import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class VersionCatalogFormatterTest {
    private val task = createTask()
    private val versions = createVersions()
    private val libraries = createLibraries()
    private val bundles = createBundles()
    private val plugins = createPlugins()

    @Test
    fun testFormatVersions() {
        val (inputVersions, expectedOutputVersions) = versions
        assertEquals(
            expectedOutputVersions,
            task.formatVersions(inputVersions),
        )
    }

    @Test
    fun testFormatLibraries() {
        val (inputLibraries, expectedOutputLibraries) = libraries
        assertEquals(
            expectedOutputLibraries,
            task.formatLibraries(inputLibraries),
        )
    }

    @Test
    fun testFormatBundles() {
        val (inputBundles, expectedOutputBundles) = bundles
        assertEquals(
            expectedOutputBundles,
            task.formatBundles(inputBundles),
        )
    }

    @Test
    fun testFormatPlugins() {
        val (inputPlugins, expectedOutputPlugins) = plugins
        assertEquals(
            expectedOutputPlugins,
            task.formatPlugins(inputPlugins),
        )
    }

    @Test
    fun testJoinCatalogSections() {
        val outputCatalogURL = javaClass.getResource("/outputVersionCatalog.toml")
        val expectedOutputCatalog = File(outputCatalogURL.toURI()).readText()
        val expectedOutputVersions = versions.second
        val expectedOutputLibraries = libraries.second
        val expectedOutputBundles = bundles.second
        val expectedOutputPlugins = plugins.second

        assertEquals(
            expectedOutputCatalog,
            task.joinCatalogSections(
                expectedOutputVersions,
                expectedOutputLibraries,
                expectedOutputBundles,
                expectedOutputPlugins,
            ),
        )
    }

    private fun createTask(): VersionCatalogFormatter {
        val project = ProjectBuilder.builder().build()
        return project.tasks.register("formatVersionCatalog", VersionCatalogFormatter::class.java).get()
    }

    private fun createVersions(): Pair<List<Pair<IntRange, String>>, List<String>> =
        Pair(
            listOf(
                2..2 to "duns = \"V0\"",
                3..3 to "slf4j = { prefer = \"1.7.25\", strictly = \"[1.7, 1.8[\" }",
                4..4 to "   exact = \"1.0\"",
                5..5 to "groovy = \"2.5.7\"   # This is a comment. ",
                6..6 to "axis =      \"1.3\"",
                7..7 to "ktlint = \"12.0.2\"",
                8..8 to "byteBuddy = \"1.12.9\"    ",
                9..9 to "springCore =   {require=\"4.2.9.RELEASE\",reject=[\"4.3.18.RELEASE\",\"4.3.16.RELEASE\"]     }",
                10..10 to "   cache2k = \"2.0.0.Final\"",
                11..11 to "dockerJava = \"3.2.12\"",
            ),
            listOf(
                "axis = \"1.3\"",
                "byteBuddy = \"1.12.9\"",
                "cache2k = \"2.0.0.Final\"",
                "dockerJava = \"3.2.12\"",
                "duns = \"V0\"",
                "exact = \"1.0\"",
                "groovy = \"2.5.7\"",
                "ktlint = \"12.0.2\"",
                "slf4j = { strictly = \"[1.7, 1.8[\", prefer = \"1.7.25\" }",
                "springCore = { require = \"4.2.9.RELEASE\", reject = [ \"4.3.16.RELEASE\", \"4.3.18.RELEASE\" ] }",
            ),
        )

    private fun createLibraries(): Pair<List<Pair<IntRange, String>>, List<String>> =
        Pair(
            listOf(
                14..14 to "groovy = { group = \"org.codehaus.groovy\", name = \"groovy\", version.ref = \"groovy\" }",
                15..15 to "activation = { group = \"com.sun.activation\", name = \"javax.activation\", version = \"1.2.0\" }",
                17..17 to "koin-bom = { module = \"io.insert-koin:koin-bom\", version = \"4.0.2\" }",
                18..18 to "koin-core = { module = \"io.insert-koin:koin-core\" }",
                19..19 to "koin-test = { module = \"io.insert-koin:koin-test\" }",
                21..21 to "jgoodiesDesktop = { group = \"com.jgoodies\", name = \"jgoodies-desktop\", version = \"1.12.1\" }",
                22..22 to "  jgoodiesFramework             = \"com.jgoodies:jgoodies-framework:1.34.0\"",
                23..23 to "jgoodiesDialogs = { group = \"com.jgoodies\", name = \"jgoodies-dialogs\", version = \"1.20.0\" }",
                24..24 to "antisamy = { group = \"org.owasp.antisamy\", name = \"antisamy\", version = \"1.5.2\" }",
                25..25 to "antlr = { module = \"antlr:antlr\",    version = \"2.7.7\" } #   This is a comment.",
                26..26 to "apacheHttpClient = { group = \"org.apache.httpcomponents\", name = \"httpclient\", version = \"4.5.14\" }",
                27..27 to
                    "apacheHttpCore = { group = \"org.apache.httpcomponents\", name = \"httpcore\", " +
                    "version = { prefer = \"4.4.16\", strictly = \"[4.4, 4.5[\" } }",
                28..28 to "apacheHttpMime = {name = \"httpmime\", version = \"4.5.14\", group = \"org.apache.httpcomponents\" }",
                30..30 to "groovyTemplates = {name = \"groovy-templates\", group = \"org.codehaus.groovy\", version.ref = \"groovy\" }",
            ),
            listOf(
                "activation = { group = \"com.sun.activation\", name = \"javax.activation\", version = \"1.2.0\" }",
                "antisamy = { group = \"org.owasp.antisamy\", name = \"antisamy\", version = \"1.5.2\" }",
                "antlr = { group = \"antlr\", name = \"antlr\", version = \"2.7.7\" }",
                "apacheHttpClient = { group = \"org.apache.httpcomponents\", name = \"httpclient\", version = \"4.5.14\" }",
                "apacheHttpCore = { group = \"org.apache.httpcomponents\", name = \"httpcore\", " +
                    "version = { strictly = \"[4.4, 4.5[\", prefer = \"4.4.16\" } }",
                "apacheHttpMime = { group = \"org.apache.httpcomponents\", name = \"httpmime\", version = \"4.5.14\" }",
                "groovy = { group = \"org.codehaus.groovy\", name = \"groovy\", version.ref = \"groovy\" }",
                "groovyTemplates = { group = \"org.codehaus.groovy\", name = \"groovy-templates\", version.ref = \"groovy\" }",
                "jgoodiesDesktop = { group = \"com.jgoodies\", name = \"jgoodies-desktop\", version = \"1.12.1\" }",
                "jgoodiesDialogs = { group = \"com.jgoodies\", name = \"jgoodies-dialogs\", version = \"1.20.0\" }",
                "jgoodiesFramework = { group = \"com.jgoodies\", name = \"jgoodies-framework\", version = \"1.34.0\" }",
                "koin-bom = { group = \"io.insert-koin\", name = \"koin-bom\", version = \"4.0.2\" }",
                "koin-core = { group = \"io.insert-koin\", name = \"koin-core\" }",
                "koin-test = { group = \"io.insert-koin\", name = \"koin-test\" }",
            ),
        )

    private fun createBundles(): Pair<List<Pair<IntRange, String>>, List<String>> =
        Pair(
            listOf(
                30..30 to "groovy    = [\"groovyTemplates\", \"groovy\"]  # This is a comment.",
                31..32 to " jgoodies = [    \"jgoodiesDesktop\",  \n            \"jgoodiesDialogs\",    \"jgoodiesFramework\"    ]",
            ),
            listOf(
                """
                groovy = [
                    "groovy",
                    "groovyTemplates"
                ]
                """.trimIndent(),
                """
                jgoodies = [
                    "jgoodiesDesktop",
                    "jgoodiesDialogs",
                    "jgoodiesFramework"
                ]
                """.trimIndent(),
            ),
        )

    private fun createPlugins(): Pair<List<Pair<IntRange, String>>, List<String>> =
        Pair(
            listOf(
                35..35 to "   shadowJar =          { id = \"com.github.johnrengelman.shadow\", version = \"8.1.1\" }  ",
                36..36 to "ktlint = { version.ref = \"ktlint\", id = \"org.jlleitschuh.gradle.ktlint\" }  #  This is a comment.",
                37..37 to "versionCatalogLinter = { id = \"io.github.pemistahl.version-catalog-linter\" }",
            ),
            listOf(
                "ktlint = { id = \"org.jlleitschuh.gradle.ktlint\", version.ref = \"ktlint\" }",
                "shadowJar = { id = \"com.github.johnrengelman.shadow\", version = \"8.1.1\" }",
                "versionCatalogLinter = { id = \"io.github.pemistahl.version-catalog-linter\" }",
            ),
        )
}
