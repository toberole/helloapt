package com.cat.zeus.plugins

import com.android.build.api.transform.*
import com.android.ide.common.internal.WaitableExecutor
import com.cat.zeus.plugins.exts.MypluginExtension
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

class MyPlugin4 extends BasePlugin {
    MypluginExtension mypluginExtension

    @Override
    void apply(Project project) {
        super.apply(project)
        mypluginExtension = project.extensions.create('AppMypluginExtension', MypluginExtension)
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        // 获取脚本配置的参数
        System.out.println("AppMypluginExtension: " + project.AppMypluginExtension.test)
        System.out.println("AppMypluginExtension: " + mypluginExtension.test)

        // Transform 的 inputs 分为两种类型
        // 一种是目录
        // 一种是 jar包[依赖的jar包或者是自建的moudle]
        transformInvocation.inputs.each { TransformInput input ->
            // 1) 对类型为"目录"的 input 进行遍历
            input.directoryInputs.each { DirectoryInput dirInput ->
                // 在MainActivity的onCreate()方法之前注入代码
                MyInject.injectOnCreate(dirInput.file.absolutePath, project)
                // 获取 output 目录
                // getContentLocation方法相当于创建一个对应名称表示的目录
                def dest = transformInvocation.outputProvider.getContentLocation(
                        dirInput.name, dirInput.contentTypes,
                        dirInput.scopes, Format.DIRECTORY
                )
                // 将 input 的目录复制到 output 指定目录
                FileUtils.copyDirectory(dirInput.file, dest)
            }

            // 2) 对类型为 jar 文件的 input 进行遍历
            input.jarInputs.each { JarInput jarInput ->
                // jar 文件一般是第三方依赖库jar包
                // 重命名输出文件（同目录 copyFile 会冲突）
                def jarName = jarInput.name
                def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())

                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }
                // 生成输出路径
                def dest = transformInvocation.outputProvider.getContentLocation(
                        jarName + md5Name, jarInput.contentTypes,
                        jarInput.scopes, Format.JAR
                )
                // 将输入内容复制到输出
                FileUtils.copyFile(jarInput.file, dest)
            }
        }
    }

    /**
     * 核心方法，具体如何处理输入和输出
     * @param inputs 为传过来的输入流，两种格式，一种jar包格式，一种目录格式
     * @param outputProvider 获取到输出目录，最后将修改的文件复制到输出目录，
     * 作为下一个Transdorm的输入，以供下一个Transfrom使用，
     * 这一步必须执行，不让编译会报错
     */
}

//public void transform(TransformInvocation invocation) {
//    for (TransformInput input : invocation.getInputs()) {
//        input.getJarInputs().parallelStream().forEach(jarInput -> {
//                File src = jarInput.getFile() ;
//                JarFile jarFile = new JarFile(file) ;
//                Enumeration<JarEntry> entries = jarFile.entries() ;
//        while (entries.hasMoreElements()) {
//            JarEntry entry = entries.nextElement();
//            //处理
//        }
//    }
//}

//private WaitableExecutor waitableExecutor = WaitableExecutor.useGlobalSharedThreadPool();
////异步并发处理jar/class
//waitableExecutor.execute(() -> {
//        bytecodeWeaver.weaveJar(srcJar, destJar);
//return null;
//});
//waitableExecutor.execute(() -> {
//        bytecodeWeaver.weaveSingleClassToFile(file, outputFile, inputDirPath);
//return null;
//});
////等待所有任务结束
//waitableExecutor.waitForTasksWithQuickFail(true);
