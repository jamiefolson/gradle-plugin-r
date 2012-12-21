package com.sonamine.gradle

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.plugins.BasePlugin

class RPackagePluginExtension {
  def String packageDir = "src"
  def String packageName = null
  def String buildDir = null
}

class RPackagePlugin implements Plugin<Project> {
  void apply(Project project) {
    project.apply(plugin: BasePlugin.class)
    project.extensions.create("rpackage",RPackagePluginExtension.class)
    project.gradle.projectsEvaluated {
      if (project.rpackage.packageName == null) {
        project.rpackage.packageName = project.name
      }
      if (project.rpackage.buildDir == null) {
        project.rpackage.buildDir = "${project.buildDir}/${project.rpackage.packageName}"
      }
    }
    project.task('DESCRIPTION') {
//    Should properly setup inputs/outputs
//    BUT, this setup won't correctly update things
//      project.gradle.projectsEvaluated({
//          inputs.file "${project.buildFile}"
//          outputs.file "${project.projectDir}/${project.rpackage.packageDir}/DESCRIPTION"
//      })
      doFirst {
        def lines = []
        println "Setting DESCRIPTION for package ${project}"
        project.file("${project.rpackage.packageDir}/DESCRIPTION").eachLine { 
          line ->
          if (!line.startsWith("Package") && !line.startsWith("Version")) {
            lines.add(line)
          }
        }
        project.file("${project.rpackage.packageDir}/DESCRIPTION").withWriter { out ->
          out.writeLine("Package: ${project.rpackage.packageName}")
          out.writeLine("Version: ${project.version}")
          lines.each() { line ->
            out.writeLine(line)
          }
        }
      }
    }

    project.task('roxygenize',type:Exec, dependsOn: project.tasks.DESCRIPTION) {
      project.gradle.projectsEvaluated({
        inputs.dir "${project.projectDir}/${project.rpackage.packageDir}"
        outputs.dir "${project.rpackage.buildDir}/"
        })
      doFirst {
        commandLine = ['R','-e',
          "library(roxygen2,quietly=TRUE,verbose=FALSE);"+
          "roxygenize(package.dir='${project.rpackage.packageDir}',"+
          "roxygen.dir='${project.rpackage.buildDir}',"+
          "roclets = c(\"collate\", \"namespace\", \"rd\", \"testthat\"))"]
        workingDir project.projectDir
      }
    }
      
    if (project.tasks.findByPath('compile')==null) {
      project.task('compile') << {
        println "Compiling the project"
      }
    }
    project.compile.dependsOn project.roxygenize
   
    project.task('buildRPackage',type:Exec, dependsOn: project.compile) {
      project.gradle.projectsEvaluated({
        inputs.dir "${project.rpackage.buildDir}"
        outputs.file "${project.buildDir}/${project.rpackage.packageName}_${project.version}.tar.gz"
        })
      doFirst {
        commandLine = ['R','CMD','build',
                    "${project.rpackage.buildDir}"]
        workingDir project.buildDir
        println commandLine.join(" ")
      }
    }
  
    if (project.tasks.findByPath('build')==null) {
      project.task('build') << {
        println "Building the project"
      }
    }
    project.build.dependsOn project.buildRPackage
    
    project.task('checkRPackage',type:Exec, dependsOn: project.build) {
      project.gradle.projectsEvaluated({
        inputs.dir "${project.rpackage.buildDir}/"
        outputs.dir "${project.buildDir}/${project.rpackage.packageName}.Rcheck/"
        })
      doFirst {
        commandLine = ['R','CMD','check',"-o",
                    "${project.buildDir}",
                    "${project.rpackage.buildDir}"]
        workingDir project.projectDir
        println commandLine.join(" ")
      }
    }
 
    if (project.tasks.findByPath('test')==null) {
      project.task('test') << {
        println "Check the validity of the build"
      }
    }
    project.test.dependsOn project.checkRPackage

    project.assemble.dependsOn project.build

    if (project.tasks.findByPath('install')==null) {
      project.task('install', dependsOn: project.build) << {
        println "Install any installable files"
      }else {
        project.install.dependsOn: project.build
      }
    }

    project.task('installRPackage',type:Exec) {
      project.gradle.projectsEvaluated({
        inputs.file "${project.buildDir}/${project.rpackage.packageName}_${project.version}.tar.gz"
        outputs.file "${project.buildDir}/${project.rpackage.packageName}_${project.version}.tar.gz"
        })
      doFirst {
        commandLine = ['R','CMD','INSTALL',"${project.rpackage.packageName}_${project.version}.tar.gz"]
        workingDir = project.file(project.buildDir)
        println commandLine.join(" ")
      }
    }
    project.install.dependsOn project.installRPackage
  }
}
