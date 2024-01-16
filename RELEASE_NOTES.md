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
