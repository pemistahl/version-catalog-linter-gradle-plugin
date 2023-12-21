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

import org.gradle.api.tasks.TaskProvider
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VersionCatalogLinterPluginTest {
    @Test
    fun testPluginIsCorrectlyAppliedToProject() {
        val project = ProjectBuilder.builder().build()
        val checkTask = project.task("check")
        val defaultVersionCatalogFile = File(project.projectDir, "gradle/libs.versions.toml")
        val customVersionCatalogFile = File(project.projectDir, "libraries.toml")

        project.pluginManager.apply("io.github.pemistahl.version-catalog-linter")

        val checkVersionCatalogTask = project.tasks.findByName("checkVersionCatalog")
        assertTrue(checkVersionCatalogTask is VersionCatalogChecker)
        assertEquals(defaultVersionCatalogFile, checkVersionCatalogTask.versionCatalogFile.get())

        val checkTaskDependencies = checkTask.dependsOn.map { (it as TaskProvider<*>).get() }
        assertTrue(checkTaskDependencies.contains(checkVersionCatalogTask))

        val formatVersionCatalogTask = project.tasks.findByName("formatVersionCatalog")
        assertTrue(formatVersionCatalogTask is VersionCatalogFormatter)
        assertEquals(defaultVersionCatalogFile, formatVersionCatalogTask.versionCatalogFile.get())

        val versionCatalogLinterPluginExtension = project.extensions.findByName("versionCatalogLinter")
        assertTrue(versionCatalogLinterPluginExtension is VersionCatalogLinterPluginExtension)

        versionCatalogLinterPluginExtension.versionCatalogFile.set(customVersionCatalogFile)
        assertEquals(customVersionCatalogFile, checkVersionCatalogTask.versionCatalogFile.get())
        assertEquals(customVersionCatalogFile, formatVersionCatalogTask.versionCatalogFile.get())
    }
}
