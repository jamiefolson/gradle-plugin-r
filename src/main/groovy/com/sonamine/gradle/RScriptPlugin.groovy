package com.sonamine.gradle

import org.gradle.api.Project
import org.gradle.api.Plugin

class RScriptPluginExtension {
  def String sourceDir = "."
  def String targetDir = "."
}

class RScriptPlugin implements Plugin<Project> {
  void apply(Project project) {
    project.extensions.create('rpackage',RScriptPluginExtension)
    project.rpackage.sourceDir = "src"
    project.rpackage.targetDir = project.buildDir
    project.task('roxygenize',type:Exec) {
        commandLine = ['R','-e','library(roxygen2,quietly=TRUE,verbose=FALSE);roxygenize(package.dir="'+project.rpackage.sourceDir+'",roxygen.dir="'+project.rpackage.targetDir+'")']
        workingDir = project.file('./')
    }
    project.task('check',type:Exec,dependsOn: project.roxygenize) {
      commandLine = ['R','CMD','CHECK']
      workingDir = project.file(project.rpackage.targetDir)
    }
    project.task('package',type:Exec, dependsOn: project.check) {
      commandLine = ['R','CMD','PACKAGE']
      workingDir = project.file(project.rpackage.targetDir)
    }
    project.task('install',type:Exec, dependsOn: project.rpackage) {
      commandLine = ['R','CMD','INSTALL']
      workingDir = project.file(project.rpackage.targetDir)
    }
  }
}
