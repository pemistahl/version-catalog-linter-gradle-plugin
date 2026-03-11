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

internal data class VersionCatalog(
    val versions: List<VersionCatalogEntry>,
    val libraries: List<VersionCatalogEntry>,
    val bundles: List<VersionCatalogEntry>,
    val plugins: List<VersionCatalogEntry>,
    val versionsPrecedingComments: List<String> = emptyList(),
    val librariesPrecedingComments: List<String> = emptyList(),
    val bundlesPrecedingComments: List<String> = emptyList(),
    val pluginsPrecedingComments: List<String> = emptyList(),
    val trailingComments: List<String> = emptyList(),
)
