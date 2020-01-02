package com.cat.zeus.plugins

import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 需要在transform加入复制文件的代码，作用是将class复制到dest的目录，
 * 也就是说应用自定义transform后需要自己处理复制class文件的流程。
 * 否则的话会出现打出来的包classes.dex是0字节
 *
 * 不管对class处理不处理，都要copy file
 */
class BasePlugin extends Transform implements Plugin<Project> {
    AppExtension android
    Project project

    @Override
    void apply(Project project) {
        System.out.println("****** " + this.class.simpleName + " ******")
        this.project = project
        android = project.extensions.getByType(AppExtension)
        android.registerTransform(this)
    }

    @Override
    String getName() {
        return this.class.simpleName
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        /**
         * ContentType是一个接口，默认有一个枚举类型DefaultContentType实现了ContentType，
         * 包含有CLASSES和RESOURCES类型。
         *
         * CLASSES类型表示的是在jar包或者文件夹中的.class文件。
         * RESOURCES类型表示的是标准的Java源文件。
         */
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    /**
     * 只是想查看输入的内容,那么只需在getScopes()返回一个空集合，
     * 在getReferencedScopes()返回想要接收的范围
     */
    Set<? super QualifiedContent.Scope> getReferencedScopes() {
        return super.getReferencedScopes()
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
    }
}