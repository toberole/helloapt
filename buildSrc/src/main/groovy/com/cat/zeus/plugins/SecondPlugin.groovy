package com.cat.zeus.plugins

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project

class SecondPlugin extends Transform implements Plugin<Project> {
    @Override
    void apply(Project project) {
        System.out.println("SecondPlugin ========================")
        System.out.println("插件!!!!!!!!!!")
        System.out.println("SecondPlugin ========================")

        def android = project.extensions.getByType(AppExtension)
        android.registerTransform(this)
    }

    @Override
    String getName() {
        return SecondPlugin.simpleName
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        System.out.println("******** start transform ********")

        transformInvocation.inputs.each { input ->
            System.out.println("***********************")

            input.directoryInputs.each { dirInput ->
                System.out.println(dirInput.file.path)

                // 将input目录复制到output目录 否则运行时候会报错classnotfound
                def dest = transformInvocation.outputProvider.getContentLocation(
                        dirInput.name, dirInput.contentTypes, dirInput.scopes, Format.DIRECTORY
                )

                System.out.println("dirInput dest: " + dest)

                FileUtils.copyDirectory(dirInput.file, dest)
            }

            input.jarInputs.each { jarInput ->
                // 重命名输出文件【同目录copyfile会冲突】
                def jarName = jarInput.name
                def md5Name = DigestUtils.md5Hex(jarInput.file.absolutePath)
                System.out.println("jarName: " + jarName)
                System.out.println("md5Name: " + md5Name)
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }

                def dest = transformInvocation.outputProvider.getContentLocation(jarName + md5Name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                System.out.println("jarInput dest: " + dest)
                FileUtils.copyFile(jarInput.file, dest)
            }
        }

        System.out.println("******** end transform ********")
    }
}