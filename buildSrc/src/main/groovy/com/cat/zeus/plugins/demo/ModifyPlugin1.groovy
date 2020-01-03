package com.cat.zeus.plugins.demo

import org.gradle.api.Plugin
import org.gradle.api.Project


class ModifyPlugin1 implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.android.registerTransform(new ModifyTransform(project))
    }
}