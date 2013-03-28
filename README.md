This is a gradle plugin for building and installing R packages.  It also supports roxygen2, which is run prior to building the package.

This plugin is available on maven with group: `com.jfolson`, artifact: `gradle-plugin-r`, version: `0.1`, or it can be built and installed with gradle 1.2 or greater:

    gradle build install

The name of the package is assumed to be the name of the gradle project with the package source found in the 'src' directory, but both can configured with the `rpackage.packageName` and `rpackage.packageDir`, respectively.  For example:

```
buildscript {
    repositories {
      mavenCentral()
    }
    dependencies {
        classpath 'com.jfolson:gradle-plugin-r:0.1-SNAPSHOT'
    }
}

apply plugin: 'rpackage'

rpackage {
  srcDir = 'pkg'
  buildDir = 'build/pkg'
  name = 'MyPackage'
}
```

To see an example, check out [rQuery](https://github.com/jamiefolson/rQuery).

The following R tasks are available: DESCRIPTION, roxygenize, buildRPackage, checkRPackage, and installRPackage.  These do not need to be called directly though, as the following dependencies are setup:

  * install -> installRPackage -> build -> buildRPackage -> compile -> roxygenize -> DESCRIPTION
  * test -> checkRPackage -> build

