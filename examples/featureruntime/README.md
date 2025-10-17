# Feature Runtime

The Feature Runtime runs inside the OSGi environment.

This example corresponds to the demo-2 in the OCX 2024 and can be watched [here](https://youtu.be/fukpqKdASas?si=InepGTb3nJ3RPAbh&t=859).

## Overview

We will launch for OSGi runtime using the Feature Launcher, and so define the bundles we want to launch the framework with in `gogo.json`


### Project Structure

#### `src/main/resources/features/gogo.json`  
There are quite a few bundles here, highlights:

The Bundles we need to add for a Feature Runtime:
  - `org.eclipse.osgi-technology.featurelauncher:common:1.0.0-SNAPSHOT`
  - `org.eclipse.osgi-technology.featurelauncher:common.osgi:1.0.0-SNAPSHOT`
  - `org.eclipse.osgi-technology.featurelauncher:runtime:1.0.0-SNAPSHOT`
  - `org.eclipse.osgi-technology.featurelauncher.repository:common.osgi:1.0.0-SNAPSHOT`
  - `org.eclipse.osgi-technology.featurelauncher.repository:spi:1.0.0-SNAPSHOT`

Bundles for the Maven Repository:
  - `org.eclipse.osgi-technology.featurelauncher.repository:artifact.maven:1.0.0-SNAPSHOT`
  - `org.apache.maven.resolver:maven-resolver-api:2.0.1`
  - `org.apache.maven.resolver:maven-resolver-connector-basic:2.0.1`
  - `org.apache.maven.resolver:maven-resolver-impl:2.0.1`
  - `org.apache.maven.resolver:maven-resolver-named-locks:2.0.1`
  - `org.apache.maven.resolver:maven-resolver-spi:2.0.1`
  - `org.apache.maven.resolver:maven-resolver-transport-file:2.0.1`
  - `org.apache.maven.resolver:maven-resolver-util:2.0.1`

Bundles to track configuration changes:
  - `org.apache.felix:org.apache.felix.cm.json:2.0.6`
  - `org.apache.felix:org.apache.felix.configadmin:1.9.26`

Demo-2 Command adds some example commands to the Gogo console:
  - `examples.featurelauncher.demo2:demo2-command:0.0.1-SNAPSHOT`

### The Launching Process

Is very similar to the `featurelauncher` example, the main difference is here we are configuring a local repository to find the demo-2 bundle artifacts.

## Getting Started

### Prerequisites

- Java 17
- Maven

### Build

```bash
mvn install
```

We want to install the artifacts into the local maven repo so that the Repository can load the demo2 bundles.

### Run

```bash
cd demo-2
mvn exec:java
```

### Demo 2 Command

As defined in the `gogo.json` file, bundle `"examples.featurelauncher:demo2-command:0.0.1-SNAPSHOT"` is installed.
This defines three Gogo commands
  - `list`
  - `addfeature`
  - `removefeature`

These interact with the FeatureRuntime to implement the commands.
  - `list` uses the FeatureRuntime `getInstalledFeatures()` method
  - `addfeature` uses the FeatureRuntime `install` method
  - `removefeature` uses the FeatureRuntime `remove` method

Once the Demo is running, we will see the Gogo console again:
```
____________________________
Welcome to Apache Felix Gogo

g!
```

If you use the `lb` command you will see all the features installed by the Feature **Launcher**.
Nothing has been installed by the Feature **Runtime** yet.

### Add Feature a.json
On the Gogo console type:
`addfeature a.json`

This will use the Feature **Runtime** to add the feature defined in a.json
```
Hello World from A
examples.featurelauncher.demo-2:a-feature:1.0 ::
  Installed Bundles:
    examples.featurelauncher:demo2-a:0.0.1-SNAPSHOT
  Installed Configurations:
```

This feature defines the  `examples.featurelauncher:demo2-a:0.0.1-SNAPSHOT` which you can also see by using the `lb` command.

### Add Feature b.json
On the Gogo console type:
`addfeature b.json`

This will add the feature defined in b.json
```
Greetings Tim from B
examples.featurelauncher.demo-2:b-feature:1.0 ::
  Installed Bundles:
    examples.featurelauncher:demo2-b:0.0.1-SNAPSHOT
  Installed Configurations:
```

Again you can use `lb` to see the newly installed bundle in the list.

### Remove Feature
Using the feature id we can remove it
```
removefeature examples.featurelauncher.demo-2:a-feature:1.0
```

If you then use `lb` you can see the `demo2-a` bundle has been removed.

### Add Feature Configuration
A major part of the dynamism of OSGi come from the Configurations, feature documents can also specify configurations.
```
addfeature b-config.json
```

This will install the configuration defined in b-config.json
```
  Installed Bundles:
  Installed Configurations:
    demo.config.b
      
      name=OSGi Summit

g! Greetings OSGi Summit from B
```

The demo-2 bundle's configuration has been set with `name=OSGi Summit`, so we see the updated *"Greetings OSGi Summit from B"*, rather than Tim which it was previously.

### Update Feature and Configuration
We can also update a bundle and apply a configuration at the same time.
```
addfeature a-with-config.json
```

This will update the bundle and apply configuration
```
Hello OCX 2024 from A
examples.featurelauncher.demo-2:a-feature:1.0 ::
  Installed Bundles:
    examples.featurelauncher:demo2-a:0.0.1-SNAPSHOT
  Installed Configurations:
    demo.config.a
      
      name=OCX 2024
```

Here demo2-a is installed with the `name=OCX 2024` configuration resulting in *`Hello OCX 2024 from A`*, rather than World which the default value is.
