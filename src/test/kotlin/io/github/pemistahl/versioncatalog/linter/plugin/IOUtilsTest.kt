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
                        34..34 to "duns = \"V0\"",
                        35..35 to "slf4j = { prefer = \"1.7.25\", strictly = \"[1.7, 1.8[\" }",
                        36..36 to "   exact = \"1.0\"",
                        37..37 to "groovy = \"2.5.7\"",
                        38..38 to "axis =      \"1.3\"",
                        39..39 to "ktlint = \"12.0.2\"",
                        40..40 to "byteBuddy = \"1.12.9\"",
                        41..41 to "springCore =   {require=\"4.2.9.RELEASE\",reject=[\"4.3.18.RELEASE\",\"4.3.16.RELEASE\"]     }",
                        42..42 to "cache2k = \"2.0.0.Final\"",
                        43..43 to "dockerJava = \"3.2.12\"",
                    ),
                libraries =
                    listOf(
                        4..4 to "groovy = { group = \"org.codehaus.groovy\", name = \"groovy\", version.ref = \"groovy\" }",
                        5..5 to "activation = { group = \"com.sun.activation\", name = \"javax.activation\", version = \"1.2.0\" }",
                        7..7 to "koin-bom = { module = \"io.insert-koin:koin-bom\", version = \"4.0.2\" }",
                        8..8 to "koin-core = { module = \"io.insert-koin:koin-core\" }",
                        9..9 to "koin-test = { module = \"io.insert-koin:koin-test\" }",
                        11..11 to "jgoodiesDesktop = { group = \"com.jgoodies\", name = \"jgoodies-desktop\", version = \"1.12.1\" }",
                        12..12 to "  jgoodiesFramework             = \"com.jgoodies:jgoodies-framework:1.34.0\"",
                        13..13 to "jgoodiesDialogs = { group = \"com.jgoodies\", name = \"jgoodies-dialogs\", version = \"1.20.0\" }",
                        14..14 to "antisamy = { group = \"org.owasp.antisamy\", name = \"antisamy\", version = \"1.5.2\" }",
                        15..15 to "antlr = { module = \"antlr:antlr\",    version = \"2.7.7\" }",
                        17..17 to
                            "apacheHttpClient = { group = \"org.apache.httpcomponents\", name = \"httpclient\", version = \"4.5.14\" }",
                        18..18 to "apacheHttpCore = { group = \"org.apache.httpcomponents\", name = \"httpcore\", version = \"4.4.16\" }",
                        19..19 to "apacheHttpMime = {name = \"httpmime\", version = \"4.5.14\", group = \"org.apache.httpcomponents\" } " +
                            "# This comment is for a key-value pair.",
                        21..21 to
                            "groovyTemplates = {name = \"groovy-templates\", group = \"org.codehaus.groovy\", version.ref = \"groovy\" }",
                    ),
                bundles =
                    listOf(
                        24..24 to "groovy    = [\"groovyTemplates\", \"groovy\"]",
                        25..29 to
                            """
                             jgoodies = [
                                "jgoodiesDesktop",
                                        "jgoodiesDialogs",
                                "jgoodiesFramework",
                            ]
                            """.trimIndent(),
                    ),
                plugins =
                    listOf(
                        48..48 to "   shadowJar =          { id = \"com.github.johnrengelman.shadow\", version = \"8.1.1\" }",
                        49..49 to "ktlint = { version.ref = \"ktlint\", id = \"org.jlleitschuh.gradle.ktlint\" }",
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
                        2..2 to "axis = \"1.3\"",
                        3..3 to "byteBuddy = \"1.12.9\"",
                        4..4 to "cache2k = \"2.0.0.Final\"",
                        5..5 to "dockerJava = \"3.2.12\"",
                        6..6 to "duns = \"V0\"",
                        7..7 to "exact = \"1.0\"",
                        8..8 to "groovy = \"2.5.7\"",
                        9..9 to "ktlint = \"12.0.2\"",
                        10..10 to "slf4j = { strictly = \"[1.7, 1.8[\", prefer = \"1.7.25\" }",
                        11..11 to "springCore = { require = \"4.2.9.RELEASE\", reject = [ \"4.3.16.RELEASE\", \"4.3.18.RELEASE\" ] }",
                    ),
                libraries =
                    listOf(
                        14..14 to "activation = { group = \"com.sun.activation\", name = \"javax.activation\", version = \"1.2.0\" }",
                        15..15 to "antisamy = { group = \"org.owasp.antisamy\", name = \"antisamy\", version = \"1.5.2\" }",
                        16..16 to "antlr = { group = \"antlr\", name = \"antlr\", version = \"2.7.7\" }",
                        17..17 to
                            "apacheHttpClient = { group = \"org.apache.httpcomponents\", name = \"httpclient\", version = \"4.5.14\" }",
                        18..18 to "apacheHttpCore = { group = \"org.apache.httpcomponents\", name = \"httpcore\", version = \"4.4.16\" }",
                        19..19 to "apacheHttpMime = { group = \"org.apache.httpcomponents\", name = \"httpmime\", version = \"4.5.14\" }",
                        20..20 to "groovy = { group = \"org.codehaus.groovy\", name = \"groovy\", version.ref = \"groovy\" }",
                        21..21 to
                            "groovyTemplates = { group = \"org.codehaus.groovy\", name = \"groovy-templates\", version.ref = \"groovy\" }",
                        22..22 to "jgoodiesDesktop = { group = \"com.jgoodies\", name = \"jgoodies-desktop\", version = \"1.12.1\" }",
                        23..23 to "jgoodiesDialogs = { group = \"com.jgoodies\", name = \"jgoodies-dialogs\", version = \"1.20.0\" }",
                        24..24 to "jgoodiesFramework = { group = \"com.jgoodies\", name = \"jgoodies-framework\", version = \"1.34.0\" }",
                        25..25 to "koin-bom = { group = \"io.insert-koin\", name = \"koin-bom\", version = \"4.0.2\" }",
                        26..26 to "koin-core = { group = \"io.insert-koin\", name = \"koin-core\" }",
                        27..27 to "koin-test = { group = \"io.insert-koin\", name = \"koin-test\" }",
                    ),
                bundles =
                    listOf(
                        30..33 to
                            """
                            groovy = [
                                "groovy",
                                "groovyTemplates"
                            ]
                            """.trimIndent(),
                        34..38 to
                            """
                            jgoodies = [
                                "jgoodiesDesktop",
                                "jgoodiesDialogs",
                                "jgoodiesFramework"
                            ]
                            """.trimIndent(),
                    ),
                plugins =
                    listOf(
                        41..41 to "ktlint = { id = \"org.jlleitschuh.gradle.ktlint\", version.ref = \"ktlint\" }",
                        42..42 to "shadowJar = { id = \"com.github.johnrengelman.shadow\", version = \"8.1.1\" }",
                    ),
            )

        val actualVersionCatalog =
            readVersionCatalog(
                File(javaClass.getResource("/outputVersionCatalog.toml").toURI()),
            )

        assertEquals(expectedVersionCatalog, actualVersionCatalog)
    }
}
