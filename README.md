# Version Catalog Linter Gradle Plugin

[![build status](https://github.com/pemistahl/version-catalog-linter-gradle-plugin/actions/workflows/build.yml/badge.svg)](https://github.com/pemistahl/version-catalog-linter-gradle-plugin/actions/workflows/build.yml)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.pemistahl.version-catalog-linter)](https://plugins.gradle.org/plugin/io.github.pemistahl.version-catalog-linter)
[![license](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

## What does this plugin do?

This plugin helps to enforce a consistent formatting in
[Gradle version catalog TOML files](https://docs.gradle.org/current/userguide/platforms.html#sub:conventional-dependencies-toml).
It provides two Gradle tasks:
1. `checkVersionCatalog` looks for various formatting errors and reports all found errors sorted by line number.
The `check` task automatically depends on `checkVersionCatalog`, so it is easy to add this plugin to continuous integration pipelines.
2. `formatVersionCatalog` automatically formats the selected version catalog and fixes all errors found by `checkVersionCatalog`.

## How to apply this plugin?

Add the plugin to your Gradle build file.

```kotlin
plugins {
    id("io.github.pemistahl.version-catalog-linter") version "1.0.2"
}
```

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

## Example

Below, you find examples for a totally messed up version catalog and how the output
of the plugin's Gradle tasks looks like.

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
apacheHttpClient = { group = "org.apache.httpcomponents", name = "httpclient", version = "4.5.14" }
apacheHttpCore = { group = "org.apache.httpcomponents", name = "httpcore", version = "4.4.16" }
apacheHttpMime = {name = "httpmime", version = "4.5.14", group = "org.apache.httpcomponents" }

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
> Line 4: Entries are not sorted alphabetically in section '[libraries]'. Found key 'groovy' where 'activation' was expected.
  Line 5: Entries are not sorted alphabetically in section '[libraries]'. Found key 'activation' where 'antisamy' was expected.
  Line 8: Entries are not sorted alphabetically in section '[libraries]'. Found key 'jgoodiesDesktop' where 'antlr' was expected.
  Line 9: Entries are not sorted alphabetically in section '[libraries]'. Found key 'jgoodiesFramework' where 'apacheHttpClient' was expected.
  Line 9: Entry with key 'jgoodiesFramework' in section '[libraries]' must not have leading whitespace.
  Line 9: Entry with key 'jgoodiesFramework' in section '[libraries]' must not have two or more adjacent whitespace characters.
  Line 10: Entries are not sorted alphabetically in section '[libraries]'. Found key 'jgoodiesDialogs' where 'apacheHttpCore' was expected.
  Line 11: Entries are not sorted alphabetically in section '[libraries]'. Found key 'antisamy' where 'apacheHttpMime' was expected.
  Line 12: Entries are not sorted alphabetically in section '[libraries]'. Found key 'antlr' where 'groovy' was expected.
  Line 12: Entry with key 'antlr' in section '[libraries]' must not have two or more adjacent whitespace characters.
  Line 12: Use table notation instead of string notation for library with key 'antlr'. Required order: [module | group], name (, version(.ref))
  Line 13: Entries are not sorted alphabetically in section '[libraries]'. Found key 'apacheHttpClient' where 'groovyTemplates' was expected.
  Line 14: Entries are not sorted alphabetically in section '[libraries]'. Found key 'apacheHttpCore' where 'jgoodiesDesktop' was expected.
  Line 15: Attributes of library with key 'apacheHttpMime' are not sorted correctly. Required order: [module | group], name (, version(.ref))
  Line 15: Entries are not sorted alphabetically in section '[libraries]'. Found key 'apacheHttpMime' where 'jgoodiesDialogs' was expected.
  Line 17: Attributes of library with key 'groovyTemplates' are not sorted correctly. Required order: [module | group], name (, version(.ref))
  Line 17: Entries are not sorted alphabetically in section '[libraries]'. Found key 'groovyTemplates' where 'jgoodiesFramework' was expected.
  Line 20: Bundle with key 'groovy' must be indented with each library on a separate line preceded by four whitespace characters.
  Line 20: Libraries of bundle with key 'groovy' are not sorted alphabetically. Found library 'groovy' where 'groovyTemplates' was expected.
  Line 20: Libraries of bundle with key 'groovy' are not sorted alphabetically. Found library 'groovyTemplates' where 'groovy' was expected.
  Lines 21-25: Bundle with key 'jgoodies' must be indented with each library on a separate line preceded by four whitespace characters.
  Lines 21-25: Entry with key 'jgoodies' in section '[bundles]' must not have leading whitespace.
  Line 30: Entries are not sorted alphabetically in section '[versions]'. Found key 'duns' where 'axis' was expected.
  Line 31: Attributes of version with key 'slf4j' are not sorted correctly. Required order: strictly, require, prefer, reject
  Line 31: Entries are not sorted alphabetically in section '[versions]'. Found key 'slf4j' where 'byteBuddy' was expected.
  Line 32: Entries are not sorted alphabetically in section '[versions]'. Found key 'exact' where 'cache2k' was expected.
  Line 32: Entry with key 'exact' in section '[versions]' must not have leading whitespace.
  Line 32: Entry with key 'exact' in section '[versions]' must not have two or more adjacent whitespace characters.
  Line 33: Entries are not sorted alphabetically in section '[versions]'. Found key 'groovy' where 'dockerJava' was expected.
  Line 34: Entries are not sorted alphabetically in section '[versions]'. Found key 'axis' where 'duns' was expected.
  Line 34: Entry with key 'axis' in section '[versions]' must not have two or more adjacent whitespace characters.
  Line 35: Entries are not sorted alphabetically in section '[versions]'. Found key 'ktlint' where 'exact' was expected.
  Line 36: Entries are not sorted alphabetically in section '[versions]'. Found key 'byteBuddy' where 'groovy' was expected.
  Line 37: Entries are not sorted alphabetically in section '[versions]'. Found key 'springCore' where 'ktlint' was expected.
  Line 37: Entry with key 'springCore' in section '[versions]' must not have two or more adjacent whitespace characters.
  Line 38: Entries are not sorted alphabetically in section '[versions]'. Found key 'cache2k' where 'slf4j' was expected.
  Line 39: Entries are not sorted alphabetically in section '[versions]'. Found key 'dockerJava' where 'springCore' was expected.
  Line 44: Entries are not sorted alphabetically in section '[plugins]'. Found key 'shadowJar' where 'ktlint' was expected.
  Line 44: Entry with key 'shadowJar' in section '[plugins]' must not have leading whitespace.
  Line 44: Entry with key 'shadowJar' in section '[plugins]' must not have two or more adjacent whitespace characters.
  Line 45: Attributes of plugin with key 'ktlint' are not sorted correctly. Required order: id, version(.ref)
  Line 45: Entries are not sorted alphabetically in section '[plugins]'. Found key 'ktlint' where 'shadowJar' was expected.
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
apacheHttpCore = { group = "org.apache.httpcomponents", name = "httpcore", version = "4.4.16" }
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
