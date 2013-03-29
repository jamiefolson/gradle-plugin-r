package com.jfolson.gradle.r

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.GradleException
import java.io.InputStream
import java.io.OutputStream

class RExec extends DefaultTask {
    File workingDir = project.rootDir
    InputStream standardInput = null
    OutputStream standardOutput = null
    String command = null
    String expression = null
    File file = null
    def args = []

    @TaskAction
    def exec() {
        def myargs = ['R']
        if (command != null){
            myargs.add("CMD")
            myargs.add(command)
        }else if (expression != null){
            myargs.add("-q")
            myargs.add("-e")
            myargs.add(expression)
        }else if (file != null){
            myargs.add("-f")
            myargs.add(file.getAbsolutePath())
        }
        myargs.addAll(args)
        project.exec {
            workingDir workingDir
            commandLine myargs
            if (standardInput != null) {
                standardInput = standardInput
            }
            if (standardOutput != null) {
                standardOutput = standardOutput
            }
        }
    }

    def args(Object... args) {
        for (Object arg : args){
            this.args.add(arg)
        }
    }

    def args(Iterable<?> args) {
        for (Object arg : args){
            this.args.add(arg)
        }
    }

}

