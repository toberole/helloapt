package com.cat.zeus.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project

class FirstPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        System.out.println("FirstPlugin ========================")
        System.out.println("插件!!!!!!!!!!")
        System.out.println("FirstPlugin ========================")
    }
}