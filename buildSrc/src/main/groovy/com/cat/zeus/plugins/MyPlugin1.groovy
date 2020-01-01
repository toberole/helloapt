package com.cat.zeus.plugins

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.Status
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project

class MyPlugin1 extends Transform implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def android = project.extensions.getByType(AppExtension)
        android.registerTransform(this)
    }

    @Override
    String getName() {
        return MyPlugin1.simpleName
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<QualifiedContent.ContentType> getOutputTypes() {
        return super.getOutputTypes()
    }

    @Override
    Set<? super QualifiedContent.Scope> getReferencedScopes() {
        return super.getReferencedScopes()
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
    boolean isCacheable() {
        return true
    }


    /**
     * 在transform方法中，我们将每个jar包和class文件复制到dest路径，
     * 这个dest路径就是下一个Transform的输入数据，而在复制时，
     * 我们就可以做一些狸猫换太子，偷天换日的事情了，先将jar包和class文件的字节码做一些修改，
     * 再进行复制即可，至于怎么修改字节码，就要借助我们后面介绍的ASM了
     */
    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        // 当前是否是增量编译
        boolean isIncremental = transformInvocation.isIncremental()
        // 消费型 可以从中获取jar包和class文件夹，注意需要输出给下一个transform
        Collection<TransformInput> inputs = transformInvocation.getInputs()
        // 引用型从输入 无需输出
        Collection<TransformInput> referencedInputs = transformInvocation.getReferencedInputs()
        // TransformOutputProvider管理输出路径，如果消费型输入为空 那么outputProvider == null
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider()

        // 非增量编译 删除所有的output cache
        if (!isIncremental) {
            outputProvider.deleteAll()
        }

        /**
         * 如果是增量编译，则要检查每个文件的Status，Status分四种
         * NOTCHANGED: 当前文件不需处理，甚至复制操作都不用；
         * ADDED、CHANGED: 正常处理，输出给下一个任务；
         * REMOVED: 移除outputProvider获取路径对应的文件。
         */

        // 提供增量的特性。
        for (TransformInput input : inputs) {
            for (JarInput jarInput : input.getJarInputs()) {
                Status status = jarInput.getStatus()
                def dest = transformInvocation.outputProvider.getContentLocation(
                        jarInput.file.absolutePath, jarInput.contentTypes,
                        jarInput.scopes, Format.JAR
                )

                System.out.println("jarInput.file.absolutePath: " + jarInput.file.absolutePath)
                System.out.println("jarInput dest: " + dest)
                boolean emptyRun = false
                if (isIncremental && !emptyRun) {
                    switch (status) {
                        case Status.NOTCHANGED:
                            break
                        case Status.REMOVED:
                            if (dest.exists()) {
                                FileUtils.forceDelete(dest)
                            }
                            break
                        case Status.ADDED:
                        case Status.CHANGED:
                            // transformJar(jarInput.getFile(), dest, status);
                            break
                    }
                } else {
                    // transformJar(jarInput.getFile(), dest, status);
                }
                // TODO 修改字节码
                // 将修改过的字节码copy到dest，就可以实现编译期间干预字节码的目的了
                // FileUtils.copyFile(jarInput.getFile(), dest)
            }

            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                File dest = outputProvider.getContentLocation(directoryInput.getName(),
                        directoryInput.getContentTypes(), directoryInput.getScopes(),
                        Format.DIRECTORY)
                FileUtils.forceMkdir(dest)
                boolean emptyRun = false
                if (isIncremental && !emptyRun) {
                    String srcDirPath = directoryInput.getFile().getAbsolutePath()
                    String destDirPath = dest.getAbsolutePath()
                    Map<File, Status> fileStatusMap = directoryInput.getChangedFiles()
                    for (Map.Entry<File, Status> changedFile : fileStatusMap.entrySet()) {
                        Status status = changedFile.getValue()
                        File inputFile = changedFile.getKey()
                        String destFilePath = inputFile.getAbsolutePath().replace(srcDirPath, destDirPath);
                        File destFile = new File(destFilePath)
                        switch (status) {
                            case NOTCHANGED:
                                break
                            case REMOVED:
                                if (destFile.exists()) {
                                    FileUtils.forceDelete(destFile)
                                }
                                break
                            case ADDED:
                            case CHANGED:
                                // FileUtils.touch(destFile)
                                // transformSingleFile(inputFile, destFile, srcDirPath);
                                break
                        }
                    }
                } else {
                    //transformDir(directoryInput.getFile(), dest);
                }
                // TODO 修改字节码
                // 将修改过的字节码copy到dest，就可以实现编译期间干预字节码的目的了
                //FileUtils.copyDirectory(directoryInput.getFile(), dest)
            }
        }

        /**
         * 支持并发编译，并发编译的实现并不复杂，只需要将上面处理单个jar/class的逻辑，并发处理，最后阻塞等待所有任务结束即可。
         * private WaitableExecutor waitableExecutor = WaitableExecutor.useGlobalSharedThreadPool();
         *
         *
         * //异步并发处理jar/class
         * waitableExecutor.execute(() -> {*     bytecodeWeaver.weaveJar(srcJar, destJar);
         *     return null;
         * });
         * waitableExecutor.execute(() -> {
         *     bytecodeWeaver.weaveSingleClassToFile(file, outputFile, inputDirPath);
         *     return null;
         * });
         *
         *
         * //等待所有任务结束
         * waitableExecutor.waitForTasksWithQuickFail(true);
         */
    }
}