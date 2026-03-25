# OSGi Feature Maven Plugin

Maven plugin for the complete OSGi feature lifecycle: generating feature JSON from project dependencies, merging features, validating configurations, and building OCI artifact images.

## Goals Overview

| Goal | Phase | Description |
|------|-------|-------------|
| `osgifeature:create-feature` | package | Generate feature.json from resolved bundles, optionally export repo/ and features/ |
| `osgifeature:merge-feature` | package | Merge variables/configurations/extensions from other features |
| `osgifeature:validate-feature` | verify | Validate configurations against component/metatype metadata |
| `osgifeature:attach-feature` | package | Attach feature.json as Maven artifact |

## Quick Start

```xml
<plugin>
  <groupId>org.eclipse.osgi-technology.featurelauncher.tools</groupId>
  <artifactId>feature-maven-plugin</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <executions>
    <execution>
      <goals><goal>create-feature</goal></goals>
      <configuration>
        <include>
          <maven>
            <mavenDependencies>true</mavenDependencies>
          </maven>
        </include>
      </configuration>
    </execution>
  </executions>
</plugin>
```

## Bundle Resolution

Bundles can be resolved from three sources, used in both `<include>` and `<exclude>`:

### Maven Dependencies

```xml
<include>
  <maven>
    <mavenDependencies>true</mavenDependencies>
    <scopes>compile,runtime</scopes>
    <includeDependencyManagement>false</includeDependencyManagement>
  </maven>
</include>
```

### Bndrun Files

```xml
<include>
  <bndruns>
    <bndrun>app.bndrun</bndrun>
  </bndruns>
</include>
```

### Existing Feature Files

```xml
<include>
  <features>
    <features>path/to/other-feature.json</features>
  </features>
</include>
```

## Include / Exclude

The final bundle set is: **include minus exclude**. Both sections support the same three sources.

```xml
<configuration>
  <include>
    <maven><mavenDependencies>true</mavenDependencies></maven>
  </include>
  <exclude>
    <features>
      <features>${project.basedir}/../base/target/features/base.json</features>
    </features>
  </exclude>
</configuration>
```

This generates a feature containing all Maven dependencies EXCEPT those already in the base feature.

## Goals

### `create-feature`

Generates an OSGi feature JSON file. Extracts metadata, variables, and categories from the POM.

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `outputFileName` | `feature.outputFileName` | `feature.json` | Output filename |
| `outputDirectory` | `feature.outputDirectory` | `${project.build.directory}` | Output directory |
| `configByFeature` | `config.by.feature` | `false` | Extract component configs into feature |
| `configByBundle` | `config.by.bundle` | `false` | Configs organized by bundle |
| `configByPid` | `config.by.pid` | `false` | Configs organized by PID |
| `bundleExport` | `bundle.export` | `true` | Export bundles in feature |
| `bundleHashMd5` | `bundle.hash.md5` | `false` | MD5 hash for bundles |
| `bundleHashSha1` | `bundle.hash.sha1` | `false` | SHA-1 hash |
| `bundleHashSha256` | `bundle.hash.sha256` | `false` | SHA-256 hash |
| `bundleHashSha512` | `bundle.hash.sha512` | `false` | SHA-512 hash |
| `mergeFeatures` | — | (empty) | Feature files to merge (variables/configs/extensions) |
| `repoDirectory` | `feature.repoDirectory` | — | If set, export bundles to this directory in Maven GAV layout |
| `featuresDirectory` | `feature.featuresDirectory` | — | If set, copy feature.json into this directory |
| `skip` | `feature.skip` | `false` | Skip execution |

#### POM Metadata Mapping

| POM Element | Feature Field |
|-------------|---------------|
| `<name>` | `name` |
| `<description>` | `description` |
| `<organization><name>` | `vendor` |
| `<licenses>` | `license` (comma-separated) |
| `<scm><url>` | `scm` |
| `<issueManagement><url>` | `docURL` |

#### Variables

Properties prefixed with `osgi.feature.var.` become feature variables with optional type:

```xml
<properties>
  <osgi.feature.var.String_appName>MyApp</osgi.feature.var.String_appName>
  <osgi.feature.var.Integer_port>8080</osgi.feature.var.Integer_port>
  <osgi.feature.var.Boolean_debug>false</osgi.feature.var.Boolean_debug>
  <osgi.feature.var.Double_timeout>30.5</osgi.feature.var.Double_timeout>
</properties>
```

Format: `osgi.feature.var.[Type_]name` — supported types: `String`, `Integer`, `Double`, `Float`, `Boolean`. Without type prefix and `_`, defaults to String.

#### Categories

Properties prefixed with `osgi.feature.category` become categories:

```xml
<properties>
  <osgi.feature.category.1>web</osgi.feature.category.1>
  <osgi.feature.category.2>persistence</osgi.feature.category.2>
</properties>
```

#### Example with Hashes and Merge

```xml
<execution>
  <goals><goal>create-feature</goal></goals>
  <configuration>
    <include>
      <maven><mavenDependencies>true</mavenDependencies></maven>
    </include>
    <bundleHashSha256>true</bundleHashSha256>
    <mergeFeatures>
      <mergeFeature>${project.basedir}/../base/target/base-feature.json</mergeFeature>
    </mergeFeatures>
  </configuration>
</execution>
```

#### Example with Repo and Features Export

```xml
<execution>
  <goals><goal>create-feature</goal></goals>
  <configuration>
    <include>
      <maven><mavenDependencies>true</mavenDependencies></maven>
    </include>
    <repoDirectory>${project.build.directory}/repo</repoDirectory>
    <featuresDirectory>${project.build.directory}/features</featuresDirectory>
  </configuration>
</execution>
```

Output:
```text
target/
├── repo/
│   └── org/osgi/.../bundle-1.0.0.jar  (Maven GAV layout)
├── features/
│   └── feature.json
└── feature.json  (original output)
```

### `merge-feature`

Merges variables, configurations, and extensions from source features into a target feature. Bundles are NOT merged. Last-wins on key conflicts.

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `targetFeature` | `feature.merge.targetFeature` | — | Target feature (required) |
| `mergeFeatures` | — | — | Source features to merge (required) |
| `mergeOutputFile` | `feature.merge.outputFile` | same as target | Output file |
| `mergeSkip` | `feature.merge.skip` | `false` | Skip merge |

```xml
<execution>
  <goals><goal>merge-feature</goal></goals>
  <configuration>
    <targetFeature>${project.build.directory}/feature.json</targetFeature>
    <mergeFeatures>
      <mergeFeature>../base/target/base-feature.json</mergeFeature>
      <mergeFeature>../config/target/config-feature.json</mergeFeature>
    </mergeFeatures>
    <mergeOutputFile>${project.build.directory}/merged-feature.json</mergeOutputFile>
  </configuration>
</execution>
```

### `validate-feature`

Validates feature configurations against DS component and Metatype metadata from bundles.

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `featureFile` | `feature.validate.featureFile` | `${project.build.directory}/feature.json` | Feature to validate |
| `failOnError` | `feature.validate.failOnError` | `false` | Fail build on errors |
| `validateSkip` | `feature.validate.skip` | `false` | Skip validation |

**Checks performed:**
- Configuration PID matches a component in resolved bundles
- Property keys match Metatype attribute IDs
- Required Metatype attributes are present in configuration

```xml
<execution>
  <goals><goal>validate-feature</goal></goals>
  <configuration>
    <failOnError>true</failOnError>
    <include>
      <maven><mavenDependencies>true</mavenDependencies></maven>
    </include>
  </configuration>
</execution>
```

### `attach-feature`

Attaches feature.json as a Maven artifact for deployment.

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `featureFile` | `feature.file` | `${project.build.directory}/feature.json` | File to attach |
| `classifier` | `feature.classifier` | `feature` | Artifact classifier |
| `type` | `feature.type` | `json` | Artifact type |

## License

[Eclipse Public License 2.0](https://www.eclipse.org/legal/epl-2.0/)
