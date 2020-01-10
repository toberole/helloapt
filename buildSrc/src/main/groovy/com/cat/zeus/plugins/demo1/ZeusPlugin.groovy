package com.cat.zeus.plugins.demo1

import org.gradle.api.Plugin
import org.gradle.api.Project


class ZeusPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.android.registerTransform(new ZeusTransform(project))
    }
}