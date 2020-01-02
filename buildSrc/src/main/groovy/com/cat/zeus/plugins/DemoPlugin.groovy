package com.cat.zeus.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project

class DemoPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        System.out.println("DemoPlugin ========================")
        System.out.println("插件!!!!!!!!!!")
        System.out.println("DemoPlugin ========================")

        project.extensions.create("test", Test)

        System.out.println("s1: " + project['test'].s1)
        System.out.println("s2: " + project['test'].s2)
    }
}

class Test {
    String s1
    String s2
}