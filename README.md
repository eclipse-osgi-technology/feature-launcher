# OSGi Feature Launcher
This repository contains an implementation of the Feature Launcher specification


## Project structure

The project is decomposed into many modules, most of which are designed to enforce the overall dependency rules associated with the launcher. There are also modules which partially duplicate each others' functions, but with a greater or lesser dependency fanout.

### OSGi API dependencies

In order to be able to launch an OSGi framework we must couple to its API, however if the OSGi API is on the class path then it can interfere with the correct execution of the framework. The Feature Launcher modules therefore avoid any dependency on the OSGi framework and framework launch APIs, unless explicitly required.

