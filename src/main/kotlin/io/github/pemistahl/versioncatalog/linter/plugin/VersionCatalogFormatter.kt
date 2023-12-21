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

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.tomlj.Toml
import org.tomlj.TomlParseResult
import org.tomlj.TomlTable
import java.io.File

abstract class VersionCatalogFormatter : DefaultTask() {
    @get:InputFile
    abstract val versionCatalogFile: Property<File>

    @TaskAction
    fun formatVersionCatalog() {
        val catalog = readVersionCatalog(versionCatalogFile.get())
        val versions = formatVersions(catalog.versions)
        val libraries = formatLibraries(catalog.libraries)
        val bundles = formatBundles(catalog.bundles)
        val plugins = formatPlugins(catalog.plugins)
        val formattedCatalog = joinCatalogSections(versions, libraries, bundles, plugins)

        File(versionCatalogFile.get().toURI()).writeText(formattedCatalog)
    }

    internal fun formatVersions(versions: List<Pair<IntRange, String>>): List<String> {
        return versions.asSequence()
            .map { pair ->
                val version = pair.second.trim()
                val parseResult = Toml.parse(version)
                val key = parseResult.keySet().iterator().next()
                val formattedVersion =
                    if (parseResult.isString(key)) {
                        "$key = \"${parseResult.getString(key)}\""
                    } else if (parseResult.isTable(key)) {
                        parseVersionTable(parseResult, key)
                    } else {
                        version
                    }
                formattedVersion
            }
            .sorted()
            .toList()
    }

    internal fun formatLibraries(libraries: List<Pair<IntRange, String>>): List<String> {
        return libraries.asSequence()
            .map { pair ->
                val library = pair.second.trim()
                val parseResult = Toml.parse(library)
                val key = parseResult.keySet().iterator().next()
                val formattedLibrary =
                    if (parseResult.isString(key)) {
                        parseLibraryString(parseResult, key)
                    } else if (parseResult.isTable(key)) {
                        parseLibraryTable(parseResult, key)
                    } else {
                        library
                    }
                formattedLibrary
            }
            .sorted()
            .toList()
    }

    internal fun formatBundles(bundles: List<Pair<IntRange, String>>): List<String> {
        return bundles.asSequence()
            .map { pair ->
                val bundle = pair.second.trim()
                val parseResult = Toml.parse(bundle)
                val key = parseResult.keySet().iterator().next()
                val libraries =
                    parseResult.getArray(key)!!.toList()
                        .asSequence()
                        .map { library -> "\"$library\"" }
                        .sorted()
                        .toList()
                val separator = "\n    "
                "$key = [$separator${libraries.joinToString(",$separator")}\n]"
            }
            .sorted()
            .toList()
    }

    internal fun formatPlugins(plugins: List<Pair<IntRange, String>>): List<String> {
        return plugins.asSequence()
            .map { pair ->
                val plugin = pair.second.trim()
                val parseResult = Toml.parse(plugin)
                val key = parseResult.keySet().iterator().next()
                val values = parseResult.getTable(key)?.toMap()
                val id = values?.get("id")
                val version = values?.get("version")
                var formattedPlugin = "$key = { id = \"$id\""

                if (version is String) {
                    formattedPlugin = "$formattedPlugin, version = \"$version\" }"
                } else if (version is TomlTable) {
                    formattedPlugin = "$formattedPlugin, version.ref = \"${version.get("ref")}\" }"
                }

                formattedPlugin
            }
            .sorted()
            .toList()
    }

    internal fun joinCatalogSections(
        versions: List<String>,
        libraries: List<String>,
        bundles: List<String>,
        plugins: List<String>,
    ): String {
        val separator = "\n"
        return listOf(
            VersionCatalogSection.VERSIONS.label,
            versions.joinToString(separator),
            "",
            VersionCatalogSection.LIBRARIES.label,
            libraries.joinToString(separator),
            "",
            VersionCatalogSection.BUNDLES.label,
            bundles.joinToString(separator),
            "",
            VersionCatalogSection.PLUGINS.label,
            plugins.joinToString(separator) + separator,
        ).joinToString(separator)
    }

    private fun parseVersionTable(
        parseResult: TomlParseResult,
        key: String,
    ): String {
        val stringKeys = listOf("strictly", "require", "prefer")
        val arrayKey = "reject"

        return parseResult.getTable(key)!!.let { values ->
            var value = "$key = {"
            val tableValues = mutableListOf<String>()

            for (stringKey in stringKeys) {
                if (values.isString(stringKey)) {
                    values.getString(stringKey)!!.let { v ->
                        tableValues.add("$stringKey = \"$v\"")
                    }
                }
            }

            if (values.isArray(arrayKey)) {
                val versionsArray =
                    values.getArray(arrayKey)!!.toList()
                        .asSequence()
                        .map { v -> "\"$v\"" }
                        .sorted()
                        .toList()
                tableValues.add("$arrayKey = [ ${versionsArray.joinToString(", ")} ]")
            }

            if (tableValues.isNotEmpty()) {
                value = "$value ${tableValues.joinToString(", ")}"
            }

            value = "$value }"
            value
        }
    }

    private fun parseLibraryString(
        parseResult: TomlParseResult,
        key: String,
    ): String {
        return parseResult.getString(key)!!.let { value ->
            val valueParts = value.split(":")
            if (valueParts.size == 3) {
                val (group, name, version) = valueParts
                "$key = { group = \"$group\", name = \"$name\", version = \"$version\" }"
            } else {
                "$key = $value"
            }
        }
    }

    private fun parseLibraryTable(
        parseResult: TomlParseResult,
        key: String,
    ): String {
        return parseResult.getTable(key)!!.let { values ->
            var lib = "$key = {"
            if (values.isString("module")) {
                values.getString("module")!!.let { module ->
                    val moduleParts = module.split(":")
                    if (moduleParts.size == 2) {
                        val (group, name) = moduleParts
                        lib = "$lib group = \"$group\", name = \"$name\""
                    } else {
                        lib = "$lib module = \"$module\""
                    }
                }
            } else if (values.contains("group") && values.contains("name")) {
                val group = values["group"]
                val name = values["name"]
                lib = "$lib group = \"$group\", name = \"$name\""
            }
            if (values.contains("version")) {
                val version = values["version"]
                if (version is String) {
                    lib = "$lib, version = \"$version\""
                } else if (version is TomlTable) {
                    lib = "$lib, version.ref = \"${version.get("ref")}\""
                }
            }

            lib = "$lib }"
            lib
        }
    }
}
