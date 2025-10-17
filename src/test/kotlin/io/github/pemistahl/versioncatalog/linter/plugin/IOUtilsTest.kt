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

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class IOUtilsTest {
    @Test
    fun testReadIncorrectlyFormattedVersionCatalog() {
        val expectedVersionCatalog =
            VersionCatalog(
                versions =
                    listOf(
                        VersionCatalogEntry(34, "duns = \"V0\""),
                        VersionCatalogEntry(35, "slf4j = { prefer = \"1.7.25\", strictly = \"[1.7, 1.8[\" }"),
                        VersionCatalogEntry(36, "   exact = \"1.0\""),
                        VersionCatalogEntry(37, "groovy = \"2.5.7\""),
                        VersionCatalogEntry(38, "axis =      \"1.3\""),
                        VersionCatalogEntry(39, "ktlint = \"12.0.2\""),
                        VersionCatalogEntry(40, "byteBuddy = \"1.12.9\""),
                        VersionCatalogEntry(
                            41,
                            "springCore =   {require=\"4.2.9.RELEASE\",reject=[\"4.3.18.RELEASE\",\"4.3.16.RELEASE\"]     }",
                        ),
                        VersionCatalogEntry(42, "cache2k = \"2.0.0.Final\""),
                        VersionCatalogEntry(43, "dockerJava = \"3.2.12\""),
                    ),
                libraries =
                    listOf(
                        VersionCatalogEntry(4, "groovy = { group = \"org.codehaus.groovy\", name = \"groovy\", version.ref = \"groovy\" }"),
                        VersionCatalogEntry(
                            5,
                            "activation = { group = \"com.sun.activation\", name = \"javax.activation\", version = \"1.2.0\" }",
                        ),
                        VersionCatalogEntry(7, "koin-bom = { module = \"io.insert-koin:koin-bom\", version = \"4.0.2\" }"),
                        VersionCatalogEntry(8, "koin-core = { module = \"io.insert-koin:koin-core\" }"),
                        VersionCatalogEntry(9, "koin-test = { module = \"io.insert-koin:koin-test\" }"),
                        VersionCatalogEntry(
                            11,
                            "jgoodiesDesktop = { group = \"com.jgoodies\", name = \"jgoodies-desktop\", version = \"1.12.1\" }",
                        ),
                        VersionCatalogEntry(12, "  jgoodiesFramework             = \"com.jgoodies:jgoodies-framework:1.34.0\""),
                        VersionCatalogEntry(
                            13,
                            "jgoodiesDialogs = { group = \"com.jgoodies\", name = \"jgoodies-dialogs\", version = \"1.20.0\" }",
                        ),
                        VersionCatalogEntry(14, "antisamy = { group = \"org.owasp.antisamy\", name = \"antisamy\", version = \"1.5.2\" }"),
                        VersionCatalogEntry(15, "antlr = { module = \"antlr:antlr\",    version = \"2.7.7\" }"),
                        VersionCatalogEntry(
                            17,
                            "apacheHttpClient = { group = \"org.apache.httpcomponents\", name = \"httpclient\", version = \"4.5.14\" }",
                        ),
                        VersionCatalogEntry(
                            18,
                            "apacheHttpCore = { group = \"org.apache.httpcomponents\", name = \"httpcore\", " +
                                "version = { prefer = \"4.4.16\", strictly = \"[4.4, 4.5[\" } }",
                        ),
                        VersionCatalogEntry(
                            19,
                            "apacheHttpMime = {name = \"httpmime\", version = \"4.5.14\", group = \"org.apache.httpcomponents\" } " +
                                "# This comment is for a key-value pair.",
                        ),
                        VersionCatalogEntry(
                            21,
                            "groovyTemplates = {name = \"groovy-templates\", group = \"org.codehaus.groovy\", version.ref = \"groovy\" }",
                        ),
                    ),
                bundles =
                    listOf(
                        VersionCatalogEntry(24, "groovy    = [\"groovyTemplates\", \"groovy\"]"),
                        VersionCatalogEntry(
                            25..29,
                            """
                             jgoodies = [
                                "jgoodiesDesktop",
                                        "jgoodiesDialogs",
                                "jgoodiesFramework",
                            ]
                            """.trimIndent(),
                        ),
                    ),
                plugins =
                    listOf(
                        VersionCatalogEntry(
                            48,
                            "   shadowJar =          { id = \"com.github.johnrengelman.shadow\", version = \"8.1.1\" }",
                        ),
                        VersionCatalogEntry(49, "ktlint = { version.ref = \"ktlint\", id = \"org.jlleitschuh.gradle.ktlint\" }"),
                    ),
            )

        val actualVersionCatalog =
            readVersionCatalog(
                File(javaClass.getResource("/inputVersionCatalog.toml").toURI()),
            )

        assertEquals(expectedVersionCatalog, actualVersionCatalog)
    }

    @Test
    fun testReadCorrectlyFormattedVersionCatalog() {
        val expectedVersionCatalog =
            VersionCatalog(
                versions =
                    listOf(
                        VersionCatalogEntry(2, "axis = \"1.3\""),
                        VersionCatalogEntry(3, "byteBuddy = \"1.12.9\""),
                        VersionCatalogEntry(4, "cache2k = \"2.0.0.Final\""),
                        VersionCatalogEntry(5, "dockerJava = \"3.2.12\""),
                        VersionCatalogEntry(6, "duns = \"V0\""),
                        VersionCatalogEntry(7, "exact = \"1.0\""),
                        VersionCatalogEntry(8, "groovy = \"2.5.7\""),
                        VersionCatalogEntry(9, "ktlint = \"12.0.2\""),
                        VersionCatalogEntry(10, "slf4j = { strictly = \"[1.7, 1.8[\", prefer = \"1.7.25\" }"),
                        VersionCatalogEntry(
                            11,
                            "springCore = { require = \"4.2.9.RELEASE\", reject = [ \"4.3.16.RELEASE\", \"4.3.18.RELEASE\" ] }",
                        ),
                    ),
                libraries =
                    listOf(
                        VersionCatalogEntry(
                            14,
                            "activation = { group = \"com.sun.activation\", name = \"javax.activation\", version = \"1.2.0\" }",
                        ),
                        VersionCatalogEntry(15, "antisamy = { group = \"org.owasp.antisamy\", name = \"antisamy\", version = \"1.5.2\" }"),
                        VersionCatalogEntry(16, "antlr = { group = \"antlr\", name = \"antlr\", version = \"2.7.7\" }"),
                        VersionCatalogEntry(
                            17,
                            "apacheHttpClient = { group = \"org.apache.httpcomponents\", name = \"httpclient\", version = \"4.5.14\" }",
                        ),
                        VersionCatalogEntry(
                            18,
                            "apacheHttpCore = { group = \"org.apache.httpcomponents\", name = \"httpcore\", " +
                                "version = { strictly = \"[4.4, 4.5[\", prefer = \"4.4.16\" } }",
                        ),
                        VersionCatalogEntry(
                            19,
                            "apacheHttpMime = { group = \"org.apache.httpcomponents\", name = \"httpmime\", version = \"4.5.14\" }",
                        ),
                        VersionCatalogEntry(
                            20,
                            "groovy = { group = \"org.codehaus.groovy\", name = \"groovy\", version.ref = \"groovy\" }",
                        ),
                        VersionCatalogEntry(
                            21,
                            "groovyTemplates = { group = \"org.codehaus.groovy\", name = \"groovy-templates\", version.ref = \"groovy\" }",
                        ),
                        VersionCatalogEntry(
                            22,
                            "jgoodiesDesktop = { group = \"com.jgoodies\", name = \"jgoodies-desktop\", version = \"1.12.1\" }",
                        ),
                        VersionCatalogEntry(
                            23,
                            "jgoodiesDialogs = { group = \"com.jgoodies\", name = \"jgoodies-dialogs\", version = \"1.20.0\" }",
                        ),
                        VersionCatalogEntry(
                            24,
                            "jgoodiesFramework = { group = \"com.jgoodies\", name = \"jgoodies-framework\", version = \"1.34.0\" }",
                        ),
                        VersionCatalogEntry(25, "koin-bom = { group = \"io.insert-koin\", name = \"koin-bom\", version = \"4.0.2\" }"),
                        VersionCatalogEntry(26, "koin-core = { group = \"io.insert-koin\", name = \"koin-core\" }"),
                        VersionCatalogEntry(27, "koin-test = { group = \"io.insert-koin\", name = \"koin-test\" }"),
                    ),
                bundles =
                    listOf(
                        VersionCatalogEntry(
                            30..33,
                            """
                            groovy = [
                                "groovy",
                                "groovyTemplates"
                            ]
                            """.trimIndent(),
                        ),
                        VersionCatalogEntry(
                            34..38,
                            """
                            jgoodies = [
                                "jgoodiesDesktop",
                                "jgoodiesDialogs",
                                "jgoodiesFramework"
                            ]
                            """.trimIndent(),
                        ),
                    ),
                plugins =
                    listOf(
                        VersionCatalogEntry(41, "ktlint = { id = \"org.jlleitschuh.gradle.ktlint\", version.ref = \"ktlint\" }"),
                        VersionCatalogEntry(42, "shadowJar = { id = \"com.github.johnrengelman.shadow\", version = \"8.1.1\" }"),
                        VersionCatalogEntry(43, "versionCatalogLinter = { id = \"io.github.pemistahl.version-catalog-linter\" }"),
                    ),
            )

        val actualVersionCatalog =
            readVersionCatalog(
                File(javaClass.getResource("/outputVersionCatalog.toml").toURI()),
            )

        assertEquals(expectedVersionCatalog, actualVersionCatalog)
    }
}
