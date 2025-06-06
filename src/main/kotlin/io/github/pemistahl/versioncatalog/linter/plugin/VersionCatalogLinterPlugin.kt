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

import org.gradle.api.Plugin
import org.gradle.api.Project

class VersionCatalogLinterPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions.create(
                "versionCatalogLinter",
                VersionCatalogLinterPluginExtension::class.java,
            )

        extension.versionCatalogFile.convention(project.file("gradle/libs.versions.toml"))

        val checkVersionCatalogTask =
            project.tasks.register("checkVersionCatalog", VersionCatalogChecker::class.java) {
                it.group = "verification"
                it.versionCatalogFile.set(extension.versionCatalogFile)
                it.bomsAndDependencies.set(extension.bomsAndDependencies)
            }

        project.tasks.findByName("check")?.dependsOn(checkVersionCatalogTask)

        project.tasks.register("formatVersionCatalog", VersionCatalogFormatter::class.java) {
            it.group = "formatting"
            it.versionCatalogFile.set(extension.versionCatalogFile)
        }
    }
}
