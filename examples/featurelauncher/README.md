# Feature Launcher

The Feature Launcher obtains an OSGi Framework instance and installs a Feature into it.

This example corresponds to the demo-1 in the OCX 2024 and can be watched [here](https://youtu.be/fukpqKdASas?si=InepGTb3nJ3RPAbh&t=859).

## Overview

The key concept here is that the Feature Launcher runs **outside** of OSGi, and launches the OSGi framework with the feature.

### Project Structure

#### `src/main/resources/features/gogo.json`  
  Contains the feature to be installed by the Feature Launcher. In this case, it's the Gogo command line tool.

#### `src/main/java/demo1/API_Launcher.java`  
  Main application class that demonstrates how to use the Feature Launcher API.

### The Launching Process

A brief description of the API_Launcher.java code

- The standard Java ServiceLoader infrastructure is used to obtain an instance of a Feature Launcher
    - The Feature Launcher will locate and instantiate the OSGi framework
    - In this example, in the maven pom.xml we place `org.apache.felix.framework` OSGi framework on the `runtime` path
- Creates an `ArtifactRepository` to find artifacts defined in the feature configuration
- Configures the launcher with
    - Feature configuration data (`demo-1/src/main/resources/features/gogo.json`)
    - The created ArtifactRepository to load the features
- Launches the framework


## Getting Started

### Prerequisites

- Java 17
- Maven

### Build

```bash
mvn package
```

### Run

```bash
cd demo-1
mvn exec:java
```

You should now see the Gogo console:
```
____________________________
Welcome to Apache Felix Gogo

g!
```

To list the installed bundles, type ```lb``` to see the minimal Gogo bundles defined in the `gogo.json` feature.
```
g! lb
START LEVEL 1
ID|State      |Level|Name
0|Active     |    0|System Bundle (7.0.5)|7.0.5
1|Active     |    1|Apache Felix Gogo Command (1.1.2)|1.1.2
2|Active     |    1|Apache Felix Gogo Shell (1.1.4)|1.1.4
3|Active     |    1|Apache Felix Gogo Runtime (1.1.6)|1.1.6
```