# Version Catalog Linter Gradle Plugin

[![build status](https://github.com/pemistahl/version-catalog-linter-gradle-plugin/actions/workflows/build.yml/badge.svg)](https://github.com/pemistahl/version-catalog-linter-gradle-plugin/actions/workflows/build.yml)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.pemistahl.version-catalog-linter)](https://plugins.gradle.org/plugin/io.github.pemistahl.version-catalog-linter)
[![license](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

## What does this plugin do?

This plugin helps to enforce a consistent formatting in
[Gradle version catalog TOML files](https://docs.gradle.org/current/userguide/version_catalogs.html).
It provides two Gradle tasks:
1. `checkVersionCatalog` looks for various formatting errors and reports all found errors sorted by line number.
The `check` task automatically depends on `checkVersionCatalog`, so it is easy to add this plugin to continuous integration pipelines.
2. `formatVersionCatalog` automatically formats the selected version catalog and fixes all errors found by `checkVersionCatalog`.

## How to apply this plugin?

Add the plugin to your Gradle build file.

```kotlin
plugins {
    id("io.github.pemistahl.version-catalog-linter") version "1.1.0"
}
```

### Locating version catalog TOML files

By default, the plugin looks for a file named `libs.versions.toml` within the directory `gradle` in the root build of your project.
If you have named your version catalog differently or put it in another location, specify it as follows:

```kotlin
versionCatalogLinter {
    versionCatalogFile = file("some/location/another-catalog.toml")
}
```

If you have multiple version catalog files for different purposes, you need to register additional Gradle tasks for each additional version catalog.
In order to create a custom check task, register a task with type
[`VersionCatalogChecker`](https://github.com/pemistahl/version-catalog-linter-gradle-plugin/blob/main/src/main/kotlin/io/github/pemistahl/versioncatalog/linter/plugin/VersionCatalogChecker.kt).
For a custom format task, register a task with type
[`VersionCatalogFormatter`](https://github.com/pemistahl/version-catalog-linter-gradle-plugin/blob/main/src/main/kotlin/io/github/pemistahl/versioncatalog/linter/plugin/VersionCatalogFormatter.kt).

```kotlin
import io.github.pemistahl.versioncatalog.linter.plugin.VersionCatalogChecker
import io.github.pemistahl.versioncatalog.linter.plugin.VersionCatalogFormatter

val customVersionCatalogLocation = "some/location/another-catalog.toml"

val checkCustomVersionCatalog by tasks.registering(VersionCatalogChecker::class) {
    versionCatalogFile = file(customVersionCatalogLocation)
}

val formatCustomVersionCatalog by tasks.registering(VersionCatalogFormatter::class) {
    versionCatalogFile = file(customVersionCatalogLocation)
}

tasks.check {
    dependsOn(checkCustomVersionCatalog)
}
```

### Handling of bills of materials (BOMs)

Consider the following version catalog:

```toml
[libraries]
quarkus = { module = "io.quarkus.platform:quarkus-bom", version = "3.21.2" }
quarkusArc = { module = "io.quarkus:quarkus-arc" }
```

If the task `checkVersionCatalog` encounters a library without a version,
it returns the following error by default:

```
Library with alias 'quarkusArc' has no version defined and no BOM declaration exists for it.
```

This plugin does not try to identify BOM declarations automatically as their naming scheme may vary.
Instead, you can specify associations of BOMs with dependencies they contain:

```kotlin
versionCatalogLinter {
    bomsAndDependencies.put("quarkus", listOf("quarkusArc"))
}
```

Afterwards, no error will be thrown anymore for the unversioned dependency.

## Example

Below, you find examples for a totally messed up version catalog and how the output
of the plugin's Gradle tasks looks like. Comments are currently filtered out in the output.
It is planned to preserve comments in a later release.

### Version catalog input

```toml


[libraries]
groovy = { group = "org.codehaus.groovy", name = "groovy", version.ref = "groovy" }
activation = { group = "com.sun.activation", name = "javax.activation", version = "1.2.0" }


jgoodiesDesktop = { group = "com.jgoodies", name = "jgoodies-desktop", version = "1.12.1" }
  jgoodiesFramework             = "com.jgoodies:jgoodies-framework:1.34.0"
jgoodiesDialogs = { group = "com.jgoodies", name = "jgoodies-dialogs", version = "1.20.0" }
antisamy = { group = "org.owasp.antisamy", name = "antisamy", version = "1.5.2" }
antlr = { module = "antlr:antlr",    version = "2.7.7" }
# This is a single-line comment.
apacheHttpClient = { group = "org.apache.httpcomponents", name = "httpclient", version = "4.5.14" }
apacheHttpCore = { group = "org.apache.httpcomponents", name = "httpcore", version = { prefer = "4.4.16", strictly = "[4.4, 4.5[" } }
apacheHttpMime = {name = "httpmime", version = "4.5.14", group = "org.apache.httpcomponents" } # This comment is for a key-value pair.

groovyTemplates = {name = "groovy-templates", group = "org.codehaus.groovy", version.ref = "groovy" }

[bundles]
groovy    = ["groovyTemplates", "groovy"]
 jgoodies = [
    "jgoodiesDesktop",
            "jgoodiesDialogs",
    "jgoodiesFramework",
]

[versions]


duns = "V0"
slf4j = { prefer = "1.7.25", strictly = "[1.7, 1.8[" }
   exact = "1.0"
groovy = "2.5.7"
axis =      "1.3"
ktlint = "12.0.2"
byteBuddy = "1.12.9"
springCore =   {require="4.2.9.RELEASE",reject=["4.3.18.RELEASE","4.3.16.RELEASE"]     }
cache2k = "2.0.0.Final"
dockerJava = "3.2.12"


[plugins]

   shadowJar =          { id = "com.github.johnrengelman.shadow", version = "8.1.1" }
ktlint = { version.ref = "ktlint", id = "org.jlleitschuh.gradle.ktlint" }


```

### Output of task `checkVersionCatalog`

```
> Task :checkVersionCatalog FAILED

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':checkVersionCatalog'.
> Line 4: Entries are not sorted alphabetically in section '[libraries]'. Found alias 'groovy' where 'activation' was expected.
  Line 5: Entries are not sorted alphabetically in section '[libraries]'. Found alias 'activation' where 'antisamy' was expected.
  Line 8: Entries are not sorted alphabetically in section '[libraries]'. Found alias 'jgoodiesDesktop' where 'antlr' was expected.
  Line 9: Entries are not sorted alphabetically in section '[libraries]'. Found alias 'jgoodiesFramework' where 'apacheHttpClient' was expected.
  Line 9: Entry with alias 'jgoodiesFramework' in section '[libraries]' must not have leading whitespace.
  Line 9: Entry with alias 'jgoodiesFramework' in section '[libraries]' must not have two or more adjacent whitespace characters.
  Line 10: Entries are not sorted alphabetically in section '[libraries]'. Found alias 'jgoodiesDialogs' where 'apacheHttpCore' was expected.
  Line 11: Entries are not sorted alphabetically in section '[libraries]'. Found alias 'antisamy' where 'apacheHttpMime' was expected.
  Line 12: Entries are not sorted alphabetically in section '[libraries]'. Found alias 'antlr' where 'groovy' was expected.
  Line 12: Entry with alias 'antlr' in section '[libraries]' must not have two or more adjacent whitespace characters.
  Line 12: Use table notation instead of string notation for library with alias 'antlr'. Required order: [module | group], name (, version(.ref))
  Line 14: Entries are not sorted alphabetically in section '[libraries]'. Found alias 'apacheHttpClient' where 'groovyTemplates' was expected.
  Line 15: Version attributes of entry with alias 'apacheHttpCore' are not sorted correctly. Required order: strictly, require, prefer, reject
  Line 15: Entries are not sorted alphabetically in section '[libraries]'. Found alias 'apacheHttpCore' where 'jgoodiesDesktop' was expected.
  Line 16: Attributes of library with alias 'apacheHttpMime' are not sorted correctly. Required order: [module | group], name (, version(.ref))
  Line 16: Entries are not sorted alphabetically in section '[libraries]'. Found alias 'apacheHttpMime' where 'jgoodiesDialogs' was expected.
  Line 18: Attributes of library with alias 'groovyTemplates' are not sorted correctly. Required order: [module | group], name (, version(.ref))
  Line 18: Entries are not sorted alphabetically in section '[libraries]'. Found alias 'groovyTemplates' where 'jgoodiesFramework' was expected.
  Line 21: Bundle with alias 'groovy' must be indented with each library on a separate line preceded by four whitespace characters.
  Line 21: Libraries of bundle with alias 'groovy' are not sorted alphabetically. Found library 'groovy' where 'groovyTemplates' was expected.
  Line 21: Libraries of bundle with alias 'groovy' are not sorted alphabetically. Found library 'groovyTemplates' where 'groovy' was expected.
  Lines 22-26: Bundle with alias 'jgoodies' must be indented with each library on a separate line preceded by four whitespace characters.
  Lines 22-26: Entry with alias 'jgoodies' in section '[bundles]' must not have leading whitespace.
  Line 31: Entries are not sorted alphabetically in section '[versions]'. Found alias 'duns' where 'axis' was expected.
  Line 32: Version attributes of entry with alias 'slf4j' are not sorted correctly. Required order: strictly, require, prefer, reject
  Line 32: Entries are not sorted alphabetically in section '[versions]'. Found alias 'slf4j' where 'byteBuddy' was expected.
  Line 33: Entries are not sorted alphabetically in section '[versions]'. Found alias 'exact' where 'cache2k' was expected.
  Line 33: Entry with alias 'exact' in section '[versions]' must not have leading whitespace.
  Line 33: Entry with alias 'exact' in section '[versions]' must not have two or more adjacent whitespace characters.
  Line 34: Entries are not sorted alphabetically in section '[versions]'. Found alias 'groovy' where 'dockerJava' was expected.
  Line 35: Entries are not sorted alphabetically in section '[versions]'. Found alias 'axis' where 'duns' was expected.
  Line 35: Entry with alias 'axis' in section '[versions]' must not have two or more adjacent whitespace characters.
  Line 36: Entries are not sorted alphabetically in section '[versions]'. Found alias 'ktlint' where 'exact' was expected.
  Line 37: Entries are not sorted alphabetically in section '[versions]'. Found alias 'byteBuddy' where 'groovy' was expected.
  Line 38: Entries are not sorted alphabetically in section '[versions]'. Found alias 'springCore' where 'ktlint' was expected.
  Line 38: Entry with alias 'springCore' in section '[versions]' must not have two or more adjacent whitespace characters.
  Line 39: Entries are not sorted alphabetically in section '[versions]'. Found alias 'cache2k' where 'slf4j' was expected.
  Line 40: Entries are not sorted alphabetically in section '[versions]'. Found alias 'dockerJava' where 'springCore' was expected.
  Line 45: Entries are not sorted alphabetically in section '[plugins]'. Found alias 'shadowJar' where 'ktlint' was expected.
  Line 45: Entry with alias 'shadowJar' in section '[plugins]' must not have leading whitespace.
  Line 45: Entry with alias 'shadowJar' in section '[plugins]' must not have two or more adjacent whitespace characters.
  Line 46: Attributes of plugin with alias 'ktlint' are not sorted correctly. Required order: id, version(.ref)
  Line 46: Entries are not sorted alphabetically in section '[plugins]'. Found alias 'ktlint' where 'shadowJar' was expected.
```

### Output of task `formatVersionCatalog`

```toml
[versions]
axis = "1.3"
byteBuddy = "1.12.9"
cache2k = "2.0.0.Final"
dockerJava = "3.2.12"
duns = "V0"
exact = "1.0"
groovy = "2.5.7"
ktlint = "12.0.2"
slf4j = { strictly = "[1.7, 1.8[", prefer = "1.7.25" }
springCore = { require = "4.2.9.RELEASE", reject = [ "4.3.16.RELEASE", "4.3.18.RELEASE" ] }

[libraries]
activation = { group = "com.sun.activation", name = "javax.activation", version = "1.2.0" }
antisamy = { group = "org.owasp.antisamy", name = "antisamy", version = "1.5.2" }
antlr = { group = "antlr", name = "antlr", version = "2.7.7" }
apacheHttpClient = { group = "org.apache.httpcomponents", name = "httpclient", version = "4.5.14" }
apacheHttpCore = { group = "org.apache.httpcomponents", name = "httpcore", version = { strictly = "[4.4, 4.5[", prefer = "4.4.16" } }
apacheHttpMime = { group = "org.apache.httpcomponents", name = "httpmime", version = "4.5.14" }
groovy = { group = "org.codehaus.groovy", name = "groovy", version.ref = "groovy" }
groovyTemplates = { group = "org.codehaus.groovy", name = "groovy-templates", version.ref = "groovy" }
jgoodiesDesktop = { group = "com.jgoodies", name = "jgoodies-desktop", version = "1.12.1" }
jgoodiesDialogs = { group = "com.jgoodies", name = "jgoodies-dialogs", version = "1.20.0" }
jgoodiesFramework = { group = "com.jgoodies", name = "jgoodies-framework", version = "1.34.0" }

[bundles]
groovy = [
    "groovy",
    "groovyTemplates"
]
jgoodies = [
    "jgoodiesDesktop",
    "jgoodiesDialogs",
    "jgoodiesFramework"
]

[plugins]
ktlint = { id = "org.jlleitschuh.gradle.ktlint", version.ref = "ktlint" }
shadowJar = { id = "com.github.johnrengelman.shadow", version = "8.1.1" }

```
