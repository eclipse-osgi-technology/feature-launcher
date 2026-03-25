# OSGi Feature Generator

Core library for generating OSGi Feature JSON files from bundle metadata. Used by the [feature-maven-plugin](../feature-maven-plugin/README.md).

## What it does

1. Extracts metadata from OSGi bundle JARs (Maven coordinates, manifest, DS components, Metatype, checksums)
2. Generates feature.json using Pebble templates
3. Optionally generates component configuration files
4. Copies bundles to a Maven repository layout (GAV structure)

## Public API

### `FeatureGenerator` (interface)

Entry point for feature generation:

```java
File generate(Setting setting, FeatureMetadata metadata,
              List<FeatureBundleMetadata> bundles, Map<String, Object> variables)
```

### Records

| Record | Purpose |
|--------|---------|
| `Setting` | Output directory, filename, bundle settings, config settings |
| `FeatureMetadata` | Feature ID, name, description, vendor, license, SCM, categories |
| `FeatureBundleMetadata` | Bundle ID, JAR file, properties |
| `FeatureBundleSetting` | Export flag, hash settings |
| `HashSetting` | MD5, SHA-1, SHA-256, SHA-512 toggle |
| `ConfigSetting` | Generate configs by feature, by bundle, or by PID |

## Templates

Pebble templates in `src/main/resources/templates/`:

| Template | Purpose |
|----------|---------|
| `feature.peb` | Main feature.json structure |
| `common.peb` | Shared macros for component/config rendering |
| `bundleConfigs.peb` | Bundle-level configuration output |
| `configs.peb` | Component-level configuration output |
| `json.peb` | JSON utility macros (string arrays, value formatting) |

## Dependencies

- [Pebble Templates](https://pebbletemplates.io/) 3.2.4 — template engine
- [bnd Reporter](https://bnd.bndtools.org/) — bundle metadata extraction (components, metatype, checksums, manifest)
- OSGi Feature Service API 1.0.0

## License

[Eclipse Public License 2.0](https://www.eclipse.org/legal/epl-2.0/)
