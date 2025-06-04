# OSGi Feature Launcher
This repository contains an implementation of the Feature Launcher specification


## Project structure

The project is decomposed into many modules, most of which are designed to enforce the overall dependency rules associated with the launcher. There are also modules which partially duplicate each others' functions, but with a greater or lesser dependency fanout.

### Main Artifacts

The main parts of the OSGi Feature Launcher are:
- Artifact Repository
    - A means of accessing the installable bytes for bundles in a Feature
    - Out of the box there are two repositories
        - MavenArtifactRepository
            - `org.eclipse.osgi-technology.featurelauncher.repository:artifact.maven`
        - LiteRepository (local repository in Maven format)
            - `org.eclipse.osgi-technology.featurelauncher.repository:artifact.lite`
- Feature Launcher
  - A Feature Launcher obtains an OSGi Framework instance and installs a Feature into it. 
    - `org.eclipse.osgi-technology.featurelauncher.launch:launcher`
- Feature Runtime
  - A Feature Runtime is an OSGi service capable of installing Features into the running OSGi framework, removing installed Features from the OSGi framework, and updating an installed Feature with a new Feature definition.
    - `org.eclipse.osgi-technology.featurelauncher:runtime`

### OSGi API dependencies

In order to be able to launch an OSGi framework we must couple to its API, however if the OSGi API is on the class path then it can interfere with the correct execution of the framework. The Feature Launcher modules therefore avoid any dependency on the OSGi framework and framework launch APIs, unless explicitly required.

### Examples

In the examples dir are two examples based off the demo given in the OCX 2024 Feature Launcher talk.
  - [Feature Launcher](examples/featurelauncher/README.md)
  - [Feature Runtime](examples/featureruntime/README.md)

## Resources

- [YouTube: Getting Started with the OSGi Feature Launcher - OCX 2024](https://www.youtube.com/watch?v=fukpqKdASas)
- [Feature Launcher Service Specification](https://osgi.github.io/osgi/cmpn/service.feature.launcher.html)
- [Feature Service Specification](https://osgi.github.io/osgi/cmpn/service.feature.html)
