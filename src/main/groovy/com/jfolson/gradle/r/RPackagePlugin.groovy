package com.jfolson.gradle.r

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.GradleException

class RPackageBaseExtension {
  def File srcDir
  def File buildDir
}

class RPackageProjectExtension extends RPackageBaseExtension {
	def String name
}

class RPackagePlugin implements Plugin<Project> {
  void apply(Project project) {
    project.apply(plugin: BasePlugin.class)
    project.extensions.create("rpackage",RPackageProjectExtension.class)
	project.rpackage.conventionMapping.map("name") { project.name }
	project.rpackage.conventionMapping.map("srcDir") { project.file("src") }
	project.rpackage.conventionMapping.map("buildDir") { 
		project.file("${project.buildDir}/${project.rpackage.name}") }
	
    project.task('DESCRIPTION') {
		extensions.create("rpackage",RPackageBaseExtension.class)
		rpackage.conventionMapping.map("srcDir") { project.rpackage.srcDir }
		rpackage.conventionMapping.map("buildDir") { project.rpackage.buildDir }
//    Should properly setup inputs/outputs
//    BUT, this setup won't correctly update things
//      project.gradle.projectsEvaluated({
//          inputs.file "${project.buildFile}"
//          outputs.file "${project.projectDir}/${project.rpackage.srcDir}/DESCRIPTION"
//      })
      doFirst {
        def lines = []
        println "Setting DESCRIPTION for package ${project.rpackage.name}"
        new File("${rpackage.srcDir}/DESCRIPTION").eachLine { 
          line ->
          if (!line.startsWith("Package") && !line.startsWith("Version")) {
            lines.add(line)
          }
        }
        new File("${rpackage.srcDir}/DESCRIPTION").withWriter { out ->
          out.writeLine("Package: ${project.rpackage.name}")
          out.writeLine("Version: ${project.version}")
          lines.each() { line ->
            out.writeLine(line)
          }
        }
      }
    }

    project.task('roxygenize',type:Exec, dependsOn: project.tasks.DESCRIPTION) {
		extensions.create("rpackage",RPackageBaseExtension.class)
		rpackage.conventionMapping.map("srcDir") { project.rpackage.srcDir }
		rpackage.conventionMapping.map("buildDir") { project.rpackage.buildDir }
      project.gradle.projectsEvaluated({
        inputs.dir "${rpackage.srcDir}"
        outputs.dir "${rpackage.buildDir}/"
        })
      doFirst {
        commandLine = ['R','-e',
          "library(roxygen2,quietly=TRUE,verbose=FALSE);"+
          "roxygenize(package.dir='${project.relativePath(rpackage.srcDir)}',"+
          "roxygen.dir='${project.relativePath(rpackage.buildDir)}',"+
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
		extensions.create("rpackage",RPackageBaseExtension.class)
		rpackage.conventionMapping.map("srcDir") { project.rpackage.srcDir }
		rpackage.conventionMapping.map("buildDir") { project.rpackage.buildDir }

      project.gradle.projectsEvaluated({
        inputs.dir "${rpackage.buildDir}"
        outputs.file "${project.buildDir}/${project.rpackage.name}_${project.version}.tar.gz"
        })
      doFirst {
        commandLine = ['R','CMD','build',
                    "${rpackage.buildDir}"]
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
		extensions.create("rpackage",RPackageBaseExtension.class)
		rpackage.conventionMapping.map("srcDir") { project.rpackage.srcDir }
		rpackage.conventionMapping.map("buildDir") { project.rpackage.buildDir }
      project.gradle.projectsEvaluated({
        inputs.dir "${rpackage.buildDir}/"
        outputs.dir "${project.buildDir}/${project.rpackage.name}.Rcheck/"
        })
      doFirst {
        commandLine = ['R','CMD','check',"-o",
                    "${project.buildDir}",
                    "${rpackage.buildDir}"]
        workingDir project.projectDir
        println commandLine.join(" ")
      }
    }
 
    if (project.tasks.findByPath('test')==null) {
      project.task('test') << {
        println "Check the validity of the build"
      }
    }
    //project.test.dependsOn project.checkRPackage

    //project.assemble.dependsOn project.build

    if (project.tasks.findByPath('install')==null) {
      project.task('install', dependsOn: project.build) << {
        println "Install any installable files"
      }
    }else {
        project.install.dependsOn project.build
    }

	project.task('skeletonCheck'){
		extensions.create("rpackage",RPackageBaseExtension.class)
		rpackage.conventionMapping.map("srcDir") { project.rpackage.srcDir }
		rpackage.conventionMapping.map("buildDir") { project.rpackage.buildDir }
		doFirst {
			if (rpackage.srcDir.exists()) {
				throw new GradleException("Package source directory not empty for skeleton!")
			}
		}
	}
	
    project.task('skeletonGenerate',type:Exec, dependsOn: project.skeletonCheck) {
	  doFirst {
		def tmpdir = project.file(".skeleton")
		if (!tmpdir.exists()){
			tmpdir.mkdir()
		}
        commandLine = ['R','-e',
          "package.skeleton(\"${project.rpackage.name}\",path=\".skeleton\",force=TRUE)"]
        workingDir project.projectDir
      }
    }
	project.task('skeleton',type:Copy, dependsOn: project.skeletonGenerate) {
		extensions.create("rpackage",RPackageBaseExtension.class)
		rpackage.conventionMapping.map("srcDir") { project.rpackage.srcDir }
		rpackage.conventionMapping.map("buildDir") { project.rpackage.buildDir }
		project.gradle.projectsEvaluated({
			from project.file(".skeleton/${project.rpackage.name}")
			exclude "man/*"
			into rpackage.srcDir
		})
	  }
	
	project.task('installRPackage',type:Exec, dependsOn: project.buildRPackage) {
		project.gradle.projectsEvaluated({
		  inputs.file "${project.buildDir}/${project.rpackage.name}_${project.version}.tar.gz"
		  outputs.file "${project.buildDir}/${project.rpackage.name}_${project.version}.tar.gz"
		  })
		doFirst {
		  commandLine = ['R','CMD','INSTALL',"${project.rpackage.name}_${project.version}.tar.gz"]
		  workingDir = project.file(project.buildDir)
		  println commandLine.join(" ")
		}
	  }
    project.install.dependsOn project.installRPackage
  }
}
