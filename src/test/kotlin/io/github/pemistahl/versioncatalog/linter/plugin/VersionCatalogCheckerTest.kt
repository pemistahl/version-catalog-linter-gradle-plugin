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
import kotlin.test.Test
import kotlin.test.assertEquals

class VersionCatalogCheckerTest {
    private val task = createTask()

    @Test
    fun testCheckVersions() {
        assertEquals(
            emptyList(),
            task.checkVersions(
                listOf(
                    1..1 to "byteBuddy = \"1.12.9\"",
                    2..2 to "cache2k = \"2.0.0.Final\"",
                    3..3 to "slf4j = { strictly = \"[1.7, 1.8[\", prefer = \"1.7.25\" }",
                ),
            ),
        )

        assertEquals(
            emptyList(),
            task.checkVersions(
                listOf(
                    1..1 to "byteBuddy = \"1.12.9\"",
                    2..2 to "cache2k = \"2.0.0.Final\" # This is a comment.",
                    3..3 to "slf4j = { strictly = \"[1.7, 1.8[\", prefer = \"1.7.25\" }",
                ),
            ),
        )

        assertEquals(
            listOf("Line 1: Entry with key 'byteBuddy' in section '[versions]' must not have leading whitespace."),
            task.checkVersions(
                listOf(
                    1..1 to " byteBuddy = \"1.12.9\"",
                    2..2 to "cache2k = \"2.0.0.Final\"",
                    3..3 to "slf4j = { strictly = \"[1.7, 1.8[\", prefer = \"1.7.25\" }",
                ),
            ).map { it.toString() },
        )

        assertEquals(
            listOf(
                "Line 1: Entry with key 'byteBuddy' in section '[versions]' must not have leading whitespace.",
                "Line 1: Entry with key 'byteBuddy' in section '[versions]' must not have two or more adjacent whitespace characters.",
            ),
            task.checkVersions(
                listOf(
                    1..1 to " byteBuddy   = \"1.12.9\"",
                    2..2 to "cache2k = \"2.0.0.Final\"",
                    3..3 to "slf4j = { strictly = \"[1.7, 1.8[\", prefer = \"1.7.25\" }",
                ),
            ).map { it.toString() },
        )

        assertEquals(
            listOf(
                "Line 1: Entry with key 'byteBuddy' in section '[versions]' must not have leading whitespace.",
                "Line 1: Entry with key 'byteBuddy' in section '[versions]' must not have trailing whitespace.",
                "Line 1: Entry with key 'byteBuddy' in section '[versions]' must not have two or more adjacent whitespace characters.",
            ),
            task.checkVersions(
                listOf(
                    1..1 to " byteBuddy   = \"1.12.9\"  ",
                    2..2 to "cache2k = \"2.0.0.Final\"",
                    3..3 to "slf4j = { strictly = \"[1.7, 1.8[\", prefer = \"1.7.25\" }",
                ),
            ).map { it.toString() },
        )

        assertEquals(
            listOf(
                "Line 1: Entry with key 'byteBuddy' in section '[versions]' must not have leading whitespace.",
                "Line 1: Entry with key 'byteBuddy' in section '[versions]' must not have trailing whitespace.",
                "Line 1: Entry with key 'byteBuddy' in section '[versions]' must not have two or more adjacent whitespace characters.",
                "Line 2: Entry with key 'cache2k' in section '[versions]' must not have two or more adjacent whitespace characters.",
            ),
            task.checkVersions(
                listOf(
                    1..1 to " byteBuddy   = \"1.12.9\"  ",
                    2..2 to "cache2k =  \"2.0.0.Final\"",
                    3..3 to "slf4j = { strictly = \"[1.7, 1.8[\", prefer = \"1.7.25\" }",
                ),
            ).map { it.toString() },
        )

        assertEquals(
            listOf(
                "Line 1: Entries are not sorted alphabetically in section '[versions]'. " +
                    "Found key 'cache2k' where 'byteBuddy' was expected.",
                "Line 1: Entry with key 'cache2k' in section '[versions]' must not have two or more adjacent whitespace characters.",
                "Line 2: Entries are not sorted alphabetically in section '[versions]'. " +
                    "Found key 'byteBuddy' where 'cache2k' was expected.",
                "Line 2: Entry with key 'byteBuddy' in section '[versions]' must not have leading whitespace.",
                "Line 2: Entry with key 'byteBuddy' in section '[versions]' must not have trailing whitespace.",
                "Line 2: Entry with key 'byteBuddy' in section '[versions]' must not have two or more adjacent whitespace characters.",
            ),
            task.checkVersions(
                listOf(
                    1..1 to "cache2k =  \"2.0.0.Final\"",
                    2..2 to " byteBuddy   = \"1.12.9\"  ",
                    3..3 to "slf4j = { strictly = \"[1.7, 1.8[\", prefer = \"1.7.25\" }",
                ),
            ).map { it.toString() },
        )

        assertEquals(
            listOf(
                "Line 1: Entries are not sorted alphabetically in section '[versions]'. " +
                    "Found key 'cache2k' where 'byteBuddy' was expected.",
                "Line 2: Entries are not sorted alphabetically in section '[versions]'. " +
                    "Found key 'byteBuddy' where 'cache2k' was expected.",
                "Line 3: Attributes of version with key 'slf4j' are not sorted correctly. " +
                    "Required order: strictly, require, prefer, reject",
            ),
            task.checkVersions(
                listOf(
                    1..1 to "cache2k = \"2.0.0.Final\"",
                    2..2 to "byteBuddy = \"1.12.9\"",
                    3..3 to "slf4j = { prefer = \"1.7.25\", strictly = \"[1.7, 1.8[\" }",
                ),
            ).map { it.toString() },
        )

        assertEquals(
            listOf(
                "Line 1: Entries are not sorted alphabetically in section '[versions]'. " +
                    "Found key 'cache2k' where 'byteBuddy' was expected.",
                "Line 2: Entries are not sorted alphabetically in section '[versions]'. " +
                    "Found key 'byteBuddy' where 'cache2k' was expected.",
                "Line 3: Attributes of version with key 'slf4j' are not sorted correctly. " +
                    "Required order: strictly, require, prefer, reject",
                "Line 3: Entry with key 'slf4j' in section '[versions]' must not have two or more adjacent whitespace characters.",
            ),
            task.checkVersions(
                listOf(
                    1..1 to "cache2k = \"2.0.0.Final\"",
                    2..2 to "byteBuddy = \"1.12.9\"",
                    3..3 to "slf4j = { prefer = \"1.7.25\",  strictly = \"[1.7, 1.8[\" }",
                ),
            ).map { it.toString() },
        )
    }

    @Test
    fun testCheckLibraries() {
        assertEquals(
            emptyList(),
            task.checkLibraries(
                listOf(
                    1..1 to "activation = { group = \"com.sun.activation\", name = \"javax.activation\", version = \"1.2.0\" }",
                    2..2 to "antisamy = { group = \"org.owasp.antisamy\", name = \"antisamy\", version = \"1.5.2\" }",
                ),
            ),
        )

        assertEquals(
            emptyList(),
            task.checkLibraries(
                listOf(
                    1..1 to "activation = { group = \"com.sun.activation\", name = \"javax.activation\", version = \"1.2.0\" }",
                    2..2 to "antisamy = { group = \"org.owasp.antisamy\", name = \"antisamy\", version = \"1.5.2\" } # This is a comment.",
                ),
            ),
        )

        assertEquals(
            emptyList(),
            task.checkLibraries(
                listOf(
                    1..1 to "activation = { group = \"com.sun.activation\", name = \"javax.activation\", version = \"1.2.0\" }",
                    2..2 to "antisamy = { module = \"org.owasp.antisamy:antisamy\", version = \"1.5.2\" }",
                ),
            ),
        )

        assertEquals(
            emptyList(),
            task.checkLibraries(
                listOf(
                    1..1 to "activation = { group = \"com.sun.activation\", name = \"javax.activation\", version = \"1.2.0\" }",
                    2..2 to "antisamy = { module = \"org.owasp.antisamy:antisamy\", version = \"1.5.2\" }",
                ),
            ),
        )

        assertEquals(
            emptyList(),
            task.checkLibraries(
                listOf(
                    1..1 to "koin-bom = { module = \"io.insert-koin:koin-bom\", version.ref = \"koin-bom\" }",
                    2..2 to "koin-core = { module = \"io.insert-koin:koin-core\" }",
                ),
            ),
        )

        assertEquals(
            listOf(
                "Line 1: Attributes of library with key 'koin-core' " +
                    "has no version defined or no bom declaration exists for 'io.insert-koin'.",
                "Line 2: Attributes of library with key 'koin-test' " +
                    "has no version defined or no bom declaration exists for 'io.insert-koin'.",
            ),
            task.checkLibraries(
                listOf(
                    1..1 to "koin-core = { module = \"io.insert-koin:koin-core\" }",
                    2..2 to "koin-test = { module = \"io.insert-koin:koin-test\" }",
                ),
            ).map { it.toString() },
        )

        assertEquals(
            listOf(
                "Line 1: Attributes of library with key 'activation' are not sorted correctly. " +
                    "Required order: [module | group], name (, version(.ref))",
            ),
            task.checkLibraries(
                listOf(
                    1..1 to "activation = { name = \"javax.activation\", group = \"com.sun.activation\", version = \"1.2.0\" }",
                    2..2 to "antisamy = { module = \"org.owasp.antisamy:antisamy\", version.ref = \"antisamy\" }",
                ),
            ).map { it.toString() },
        )

        assertEquals(
            listOf(
                "Line 1: Use table notation instead of string notation for library with key 'activation'. " +
                    "Required order: [module | group], name (, version(.ref))",
            ),
            task.checkLibraries(
                listOf(
                    1..1 to "activation = \"javax.activation:com.sun.activation:1.2.0\"",
                    2..2 to "antisamy = { module = \"org.owasp.antisamy:antisamy\", version.ref = \"antisamy\" }",
                ),
            ).map { it.toString() },
        )

        assertEquals(
            listOf(
                "Line 1: Attributes of library with key 'activation' are not sorted correctly. " +
                    "Required order: [module | group], name (, version(.ref))",
                "Line 2: Attributes of library with key 'antisamy' are not sorted correctly. " +
                    "Required order: [module | group], name (, version(.ref))",
            ),
            task.checkLibraries(
                listOf(
                    1..1 to "activation = { name = \"javax.activation\", group = \"com.sun.activation\", version = \"1.2.0\" }",
                    2..2 to "antisamy = { version.ref = \"antisamy\", module = \"org.owasp.antisamy:antisamy\" }",
                ),
            ).map { it.toString() },
        )

        assertEquals(
            listOf(
                "Line 1: Attributes of library with key 'antisamy' are not sorted correctly. " +
                    "Required order: [module | group], name (, version(.ref))",
                "Line 1: Entries are not sorted alphabetically in section '[libraries]'. " +
                    "Found key 'antisamy' where 'activation' was expected.",
                "Line 2: Attributes of library with key 'activation' are not sorted correctly. " +
                    "Required order: [module | group], name (, version(.ref))",
                "Line 2: Entries are not sorted alphabetically in section '[libraries]'. " +
                    "Found key 'activation' where 'antisamy' was expected.",
            ),
            task.checkLibraries(
                listOf(
                    1..1 to "antisamy = { version.ref = \"antisamy\", module = \"org.owasp.antisamy:antisamy\" }",
                    2..2 to "activation = { name = \"javax.activation\", group = \"com.sun.activation\", version = \"1.2.0\" }",
                ),
            ).map { it.toString() },
        )

        assertEquals(
            listOf(
                "Line 1: Attributes of library with key 'antisamy' are not sorted correctly. " +
                    "Required order: [module | group], name (, version(.ref))",
                "Line 1: Entries are not sorted alphabetically in section '[libraries]'. " +
                    "Found key 'antisamy' where 'activation' was expected.",
                "Line 2: Attributes of library with key 'activation' are not sorted correctly. " +
                    "Required order: [module | group], name (, version(.ref))",
                "Line 2: Entries are not sorted alphabetically in section '[libraries]'. " +
                    "Found key 'activation' where 'antisamy' was expected.",
                "Line 2: Entry with key 'activation' in section '[libraries]' must not have two or more adjacent whitespace characters.",
            ),
            task.checkLibraries(
                listOf(
                    1..1 to "antisamy = { version.ref = \"antisamy\", module = \"org.owasp.antisamy:antisamy\" }",
                    2..2 to "activation =  { name = \"javax.activation\",   group = \"com.sun.activation\", version = \"1.2.0\" }",
                ),
            ).map { it.toString() },
        )

        assertEquals(
            listOf(
                "Line 1: Attributes of library with key 'antisamy' are not sorted correctly. " +
                    "Required order: [module | group], name (, version(.ref))",
                "Line 1: Entries are not sorted alphabetically in section '[libraries]'. " +
                    "Found key 'antisamy' where 'activation' was expected.",
                "Line 1: Entry with key 'antisamy' in section '[libraries]' must not have trailing whitespace.",
                "Line 2: Attributes of library with key 'activation' are not sorted correctly. " +
                    "Required order: [module | group], name (, version(.ref))",
                "Line 2: Entries are not sorted alphabetically in section '[libraries]'. " +
                    "Found key 'activation' where 'antisamy' was expected.",
                "Line 2: Entry with key 'activation' in section '[libraries]' must not have two or more adjacent whitespace characters.",
            ),
            task.checkLibraries(
                listOf(
                    1..1 to "antisamy = { version.ref = \"antisamy\", module = \"org.owasp.antisamy:antisamy\" } ",
                    2..2 to "activation =  { name = \"javax.activation\",   group = \"com.sun.activation\", version = \"1.2.0\" }",
                ),
            ).map { it.toString() },
        )

        assertEquals(
            listOf(
                "Line 1: Attributes of library with key 'antisamy' are not sorted correctly. " +
                    "Required order: [module | group], name (, version(.ref))",
                "Line 1: Entries are not sorted alphabetically in section '[libraries]'. " +
                    "Found key 'antisamy' where 'activation' was expected.",
                "Line 1: Entry with key 'antisamy' in section '[libraries]' must not have trailing whitespace.",
                "Line 2: Attributes of library with key 'activation' are not sorted correctly. " +
                    "Required order: [module | group], name (, version(.ref))",
                "Line 2: Entries are not sorted alphabetically in section '[libraries]'. " +
                    "Found key 'activation' where 'antisamy' was expected.",
                "Line 2: Entry with key 'activation' in section '[libraries]' must not have leading whitespace.",
                "Line 2: Entry with key 'activation' in section '[libraries]' must not have two or more adjacent whitespace characters.",
            ),
            task.checkLibraries(
                listOf(
                    1..1 to "antisamy = { version.ref = \"antisamy\", module = \"org.owasp.antisamy:antisamy\" } ",
                    2..2 to "  activation =  { name = \"javax.activation\",   group = \"com.sun.activation\", version = \"1.2.0\" }",
                ),
            ).map { it.toString() },
        )
    }

    @Test
    fun testCheckBundles() {
        assertEquals(
            emptyList(),
            task.checkBundles(
                listOf(
                    1..5 to
                        """
                        groovy = [
                            "groovy",
                            "groovyTemplates",
                            "spock"
                        ]
                        """.trimIndent(),
                    6..10 to
                        """
                        jgoodies = [
                            "jgoodiesDesktop",
                            "jgoodiesDialogs",
                            "jgoodiesFramework"
                        ]
                        """.trimIndent(),
                ),
            ),
        )

        assertEquals(
            emptyList(),
            task.checkBundles(
                listOf(
                    1..5 to
                        """
                        groovy = [
                            "groovy",
                            "groovyTemplates", # This is a comment.
                            "spock"
                        ]
                        """.trimIndent(),
                    6..10 to
                        """
                        jgoodies = [ # This a another comment.
                            "jgoodiesDesktop",
                            "jgoodiesDialogs",
                            "jgoodiesFramework"
                        ]
                        """.trimIndent(),
                ),
            ),
        )

        assertEquals(
            listOf(
                "Lines 1-5: Bundle with key 'groovy' must be indented with " +
                    "each library on a separate line preceded by four whitespace characters.",
            ),
            task.checkBundles(
                listOf(
                    1..5 to
                        """
                        groovy = [
                            "groovy",
                              "groovyTemplates",
                            "spock"
                        ]
                        """.trimIndent(),
                    6..10 to
                        """
                        jgoodies = [
                            "jgoodiesDesktop",
                            "jgoodiesDialogs",
                            "jgoodiesFramework"
                        ]
                        """.trimIndent(),
                ),
            ).map { it.toString() },
        )

        assertEquals(
            listOf(
                "Lines 1-5: Bundle with key 'groovy' must be indented with " +
                    "each library on a separate line preceded by four whitespace characters.",
            ),
            task.checkBundles(
                listOf(
                    1..5 to "groovy = [ \"groovy\", \"groovyTemplates\", \"spock\" ]",
                    6..10 to
                        """
                        jgoodies = [
                            "jgoodiesDesktop",
                            "jgoodiesDialogs",
                            "jgoodiesFramework"
                        ]
                        """.trimIndent(),
                ),
            ).map { it.toString() },
        )

        assertEquals(
            listOf(
                "Lines 1-5: Bundle with key 'groovy' must be indented with " +
                    "each library on a separate line preceded by four whitespace characters.",
                "Lines 6-10: Bundle with key 'jgoodies' must be indented with " +
                    "each library on a separate line preceded by four whitespace characters.",
            ),
            task.checkBundles(
                listOf(
                    1..5 to
                        """
                        groovy = [
                            "groovy",
                              "groovyTemplates",
                            "spock"
                        ]
                        """.trimIndent(),
                    6..10 to
                        """
                        jgoodies = [
                            "jgoodiesDesktop",
                            "jgoodiesDialogs",
                           "jgoodiesFramework"
                        ]
                        """.trimIndent(),
                ),
            ).map { it.toString() },
        )

        assertEquals(
            listOf(
                "Lines 1-5: Entries are not sorted alphabetically in section '[bundles]'. " +
                    "Found key 'jgoodies' where 'groovy' was expected.",
                "Lines 6-10: Entries are not sorted alphabetically in section '[bundles]'. " +
                    "Found key 'groovy' where 'jgoodies' was expected.",
            ),
            task.checkBundles(
                listOf(
                    1..5 to
                        """
                        jgoodies = [
                            "jgoodiesDesktop",
                            "jgoodiesDialogs",
                            "jgoodiesFramework"
                        ]
                        """.trimIndent(),
                    6..10 to
                        """
                        groovy = [
                            "groovy",
                            "groovyTemplates",
                            "spock"
                        ]
                        """.trimIndent(),
                ),
            ).map { it.toString() },
        )

        assertEquals(
            listOf(
                "Line 8: Libraries of bundle with key 'jgoodies' are not sorted alphabetically. " +
                    "Found library 'jgoodiesFramework' where 'jgoodiesDialogs' was expected.",
                "Line 9: Libraries of bundle with key 'jgoodies' are not sorted alphabetically. " +
                    "Found library 'jgoodiesDialogs' where 'jgoodiesFramework' was expected.",
            ),
            task.checkBundles(
                listOf(
                    1..5 to
                        """
                        groovy = [
                            "groovy",
                            "groovyTemplates",
                            "spock"
                        ]
                        """.trimIndent(),
                    6..10 to
                        """
                        jgoodies = [
                            "jgoodiesDesktop",
                            "jgoodiesFramework",
                            "jgoodiesDialogs"
                        ]
                        """.trimIndent(),
                ),
            ).map { it.toString() },
        )

        assertEquals(
            listOf(
                "Lines 1-5: Bundle with key 'groovy' must be indented with " +
                    "each library on a separate line preceded by four whitespace characters.",
                "Line 8: Libraries of bundle with key 'jgoodies' are not sorted alphabetically. " +
                    "Found library 'jgoodiesFramework' where 'jgoodiesDialogs' was expected.",
                "Line 9: Libraries of bundle with key 'jgoodies' are not sorted alphabetically. " +
                    "Found library 'jgoodiesDialogs' where 'jgoodiesFramework' was expected.",
            ),
            task.checkBundles(
                listOf(
                    1..5 to
                        """
                        groovy = [
                          "groovy",
                            "groovyTemplates",
                            "spock"
                        ]
                        """.trimIndent(),
                    6..10 to
                        """
                        jgoodies = [
                            "jgoodiesDesktop",
                            "jgoodiesFramework",
                            "jgoodiesDialogs"
                        ]
                        """.trimIndent(),
                ),
            ).map { it.toString() },
        )

        assertEquals(
            listOf(
                "Lines 1-5: Bundle with key 'groovy' must be indented with " +
                    "each library on a separate line preceded by four whitespace characters.",
                "Lines 6-9: Bundle with key 'jgoodies' must be indented with " +
                    "each library on a separate line preceded by four whitespace characters.",
                "Line 8: Libraries of bundle with key 'jgoodies' are not sorted alphabetically. " +
                    "Found library 'jgoodiesFramework' where 'jgoodiesDialogs' was expected.",
                "Line 9: Libraries of bundle with key 'jgoodies' are not sorted alphabetically. " +
                    "Found library 'jgoodiesDialogs' where 'jgoodiesFramework' was expected.",
            ),
            task.checkBundles(
                listOf(
                    1..5 to
                        """
                        groovy = [
                          "groovy",
                            "groovyTemplates",
                            "spock"
                        ]
                        """.trimIndent(),
                    6..9 to
                        """
                        jgoodies = [
                            "jgoodiesDesktop",
                            "jgoodiesFramework",
                            "jgoodiesDialogs"]
                        """.trimIndent(),
                ),
            ).map { it.toString() },
        )

        assertEquals(
            listOf(
                "Lines 1-4: Bundle with key 'jgoodies' must be indented with " +
                    "each library on a separate line preceded by four whitespace characters.",
                "Lines 1-4: Entries are not sorted alphabetically in section '[bundles]'. " +
                    "Found key 'jgoodies' where 'groovy' was expected.",
                "Line 3: Libraries of bundle with key 'jgoodies' are not sorted alphabetically. " +
                    "Found library 'jgoodiesFramework' where 'jgoodiesDialogs' was expected.",
                "Line 4: Libraries of bundle with key 'jgoodies' are not sorted alphabetically. " +
                    "Found library 'jgoodiesDialogs' where 'jgoodiesFramework' was expected.",
                "Lines 5-9: Bundle with key 'groovy' must be indented with " +
                    "each library on a separate line preceded by four whitespace characters.",
                "Lines 5-9: Entries are not sorted alphabetically in section '[bundles]'. " +
                    "Found key 'groovy' where 'jgoodies' was expected.",
            ),
            task.checkBundles(
                listOf(
                    1..4 to
                        """
                        jgoodies = [
                            "jgoodiesDesktop",
                            "jgoodiesFramework",
                            "jgoodiesDialogs"]
                        """.trimIndent(),
                    5..9 to
                        """
                        groovy = [
                          "groovy",
                            "groovyTemplates",
                            "spock"
                        ]
                        """.trimIndent(),
                ),
            ).map { it.toString() },
        )
    }

    @Test
    fun testCheckPlugins() {
        assertEquals(
            emptyList(),
            task.checkPlugins(
                listOf(
                    1..1 to "ktlint = { id = \"org.jlleitschuh.gradle.ktlint\", version.ref = \"ktlint\" }",
                    2..2 to "shadowJar = { id = \"com.github.johnrengelman.shadow\", version = \"8.1.1\" }",
                ),
            ),
        )

        assertEquals(
            emptyList(),
            task.checkPlugins(
                listOf(
                    1..1 to "ktlint = { id = \"org.jlleitschuh.gradle.ktlint\", version.ref = \"ktlint\" } # This is a comment.",
                    2..2 to "shadowJar = { id = \"com.github.johnrengelman.shadow\", version = \"8.1.1\" }",
                ),
            ),
        )

        assertEquals(
            listOf(
                "Line 1: Attributes of plugin with key 'ktlint' are not sorted correctly. Required order: id, version(.ref)",
            ),
            task.checkPlugins(
                listOf(
                    1..1 to "ktlint = { version.ref = \"ktlint\", id = \"org.jlleitschuh.gradle.ktlint\" }",
                    2..2 to "shadowJar = { id = \"com.github.johnrengelman.shadow\", version = \"8.1.1\" }",
                ),
            ).map { it.toString() },
        )

        assertEquals(
            listOf(
                "Line 1: Attributes of plugin with key 'ktlint' are not sorted correctly. Required order: id, version(.ref)",
                "Line 2: Attributes of plugin with key 'shadowJar' are not sorted correctly. Required order: id, version(.ref)",
            ),
            task.checkPlugins(
                listOf(
                    1..1 to "ktlint = { version.ref = \"ktlint\", id = \"org.jlleitschuh.gradle.ktlint\" }",
                    2..2 to "shadowJar = { version = \"8.1.1\", id = \"com.github.johnrengelman.shadow\" }",
                ),
            ).map { it.toString() },
        )

        assertEquals(
            listOf(
                "Line 1: Attributes of plugin with key 'shadowJar' are not sorted correctly. Required order: id, version(.ref)",
                "Line 1: Entries are not sorted alphabetically in section '[plugins]'. " +
                    "Found key 'shadowJar' where 'ktlint' was expected.",
                "Line 2: Attributes of plugin with key 'ktlint' are not sorted correctly. Required order: id, version(.ref)",
                "Line 2: Entries are not sorted alphabetically in section '[plugins]'. " +
                    "Found key 'ktlint' where 'shadowJar' was expected.",
            ),
            task.checkPlugins(
                listOf(
                    1..1 to "shadowJar = { version = \"8.1.1\", id = \"com.github.johnrengelman.shadow\" }",
                    2..2 to "ktlint = { version.ref = \"ktlint\", id = \"org.jlleitschuh.gradle.ktlint\" }",
                ),
            ).map { it.toString() },
        )

        assertEquals(
            listOf(
                "Line 1: Attributes of plugin with key 'shadowJar' are not sorted correctly. Required order: id, version(.ref)",
                "Line 1: Entries are not sorted alphabetically in section '[plugins]'. " +
                    "Found key 'shadowJar' where 'ktlint' was expected.",
                "Line 2: Attributes of plugin with key 'ktlint' are not sorted correctly. Required order: id, version(.ref)",
                "Line 2: Entries are not sorted alphabetically in section '[plugins]'. " +
                    "Found key 'ktlint' where 'shadowJar' was expected.",
                "Line 2: Entry with key 'ktlint' in section '[plugins]' must not have leading whitespace.",
            ),
            task.checkPlugins(
                listOf(
                    1..1 to "shadowJar = { version = \"8.1.1\", id = \"com.github.johnrengelman.shadow\" }",
                    2..2 to " ktlint = { version.ref = \"ktlint\", id = \"org.jlleitschuh.gradle.ktlint\" }",
                ),
            ).map { it.toString() },
        )

        assertEquals(
            listOf(
                "Line 1: Attributes of plugin with key 'shadowJar' are not sorted correctly. Required order: id, version(.ref)",
                "Line 1: Entries are not sorted alphabetically in section '[plugins]'. " +
                    "Found key 'shadowJar' where 'ktlint' was expected.",
                "Line 1: Entry with key 'shadowJar' in section '[plugins]' must not have trailing whitespace.",
                "Line 1: Entry with key 'shadowJar' in section '[plugins]' must not have two or more adjacent whitespace characters.",
                "Line 2: Attributes of plugin with key 'ktlint' are not sorted correctly. Required order: id, version(.ref)",
                "Line 2: Entries are not sorted alphabetically in section '[plugins]'. " +
                    "Found key 'ktlint' where 'shadowJar' was expected.",
                "Line 2: Entry with key 'ktlint' in section '[plugins]' must not have leading whitespace.",
            ),
            task.checkPlugins(
                listOf(
                    1..1 to "shadowJar = { version = \"8.1.1\", id =  \"com.github.johnrengelman.shadow\" }   ",
                    2..2 to " ktlint = { version.ref = \"ktlint\", id = \"org.jlleitschuh.gradle.ktlint\" }",
                ),
            ).map { it.toString() },
        )
    }

    private fun createTask(): VersionCatalogChecker {
        val project = ProjectBuilder.builder().build()
        val task =
            project.task(
                mapOf("type" to VersionCatalogChecker::class.java),
                "checkVersionCatalog",
            ) as VersionCatalogChecker
        return task
    }
}
