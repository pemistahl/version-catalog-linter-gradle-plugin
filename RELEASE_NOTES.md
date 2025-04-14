## Version Catalog Linter 1.0.5 (released on Apr 14 2025)

### Bug Fixes

- The previous release did not correctly identify all possible formats of BOM declarations.
  This has been fixed.

## Version Catalog Linter 1.0.4 (released on Apr 11 2025)

### Bug Fixes

- This plugin now allows to specify plugins without versions in version catalogs.
  Since Gradle 8.8 this is allowed, so the `VersionCatalogChecker` won't throw
  an error anymore in this case. (#27)

- This plugin now identifies BOM declarations correctly. If a library without
  a version is found, it will be checked whether there is a BOM declaration with
  the same group as in the library artifact. If there is no declaration,
  the `VersionCatalogChecker` will report an error. (#25)

## Version Catalog Linter 1.0.3 (released on Jan 31 2024)

### Bug Fixes

- The Gradle tasks failed when a single-line comment was part of a version catalog.
  This has been fixed. (#5)

## Version Catalog Linter 1.0.2 (released on Jan 16 2024)

### Bug Fixes

- Libraries declared using string notation now throw a validation error
  in the `VersionCatalogChecker` asking to use table notation instead.
  This change makes the behavior of the checker consistent with the behavior
  of the `VersionCatalogFormatter` which already converts string notation
  to table notation.

- The `VersionCatalogFormatter` inserted section labels into the version catalog
  even when a section did not contain any data. This has been fixed.

## Version Catalog Linter 1.0.1 (released on Jan 09 2024)

### Improvements

- The shadowJar plugin has been applied to produce
  artifacts with all dependencies included for convenience.

## Version Catalog Linter 1.0.0 (released on Jan 07 2024)

This is the very first release. Enjoy!
