package com.cat.zeus.plugins.demo

import com.android.SdkConstants
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.internal.pipeline.TransformManager
import com.cat.zeus.plugins.exts.ExtraInfo
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import javassist.Modifier
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.apache.commons.io.IOUtils

import java.text.MessageFormat

import javax.xml.crypto.dsig.TransformException
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class ModifyTransform extends Transform {
    ExtraInfo extraInfo
    def pool = ClassPool.default
    def project

    ModifyTransform(Project project) {
        this.project = project
        this.extraInfo = project.extensions.create('ExtraInfo', ExtraInfo)
    }

    @Override
    String getName() {
        return ModifyTransform.simpleName
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

        project.android.bootClasspath.each {
            pool.appendClassPath(it.absolutePath)
            pool.appendClassPath(project.android.bootClasspath[0].toString())
        }

        transformInvocation.inputs.each {
            it.jarInputs.each {
                System.out.println("jarInput path: " + it.file.absolutePath)
                pool.insertClassPath(it.file.absolutePath)
                def jarName = it.name
                def md5Name = DigestUtils.md5Hex(it.file.getAbsolutePath())
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }

                handleJarInput(it, transformInvocation.outputProvider)

//                def dest = transformInvocation.outputProvider.getContentLocation(
//                        jarName + md5Name,
//                        it.contentTypes,
//                        it.scopes, Format.JAR
//                )
//                FileUtils.copyFile(it.file, dest)
            }


            it.directoryInputs.each {
                def preFileName = it.file.absolutePath
                pool.insertClassPath(preFileName)

                findTarget(it.file, preFileName)

                def dest = transformInvocation.outputProvider.getContentLocation(
                        it.name,
                        it.contentTypes,
                        it.scopes,
                        Format.DIRECTORY)

                System.out.println("copy directory: " + it.file.absolutePath)
                System.out.println("dest directory: " + dest.absolutePath)

                FileUtils.copyDirectory(it.file, dest)
            }
        }
    }

    private void handleJarInput(JarInput jarInput, TransformOutputProvider outputProvider) {
        if (jarInput.file.getAbsolutePath().endsWith(".jar")) {
            def path = jarInput.file.getAbsolutePath()
            System.out.println("***** handleJarInput path: " + path)

            def jarName = jarInput.name
            System.out.println("***** jarName: " + jarName)

            def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
            if (jarName.endsWith(".jar")) {
                jarName = jarName.substring(0, jarName.length() - 4)
            }

            File tmpFile = new File(jarInput.file.getParent() + File.separator + "classes_temp.jar")
            //避免上次的缓存被重复插入
            if (tmpFile.exists()) {
                tmpFile.delete()
            }

            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tmpFile))
            JarFile jarFile = new JarFile(jarInput.file)

            Enumeration<JarEntry> enumeration = jarFile.entries()
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = enumeration.nextElement()
                String entryName = jarEntry.getName()

                InputStream inputStream = jarFile.getInputStream(jarEntry)

                ZipEntry zipEntry = new ZipEntry(entryName)
                jarOutputStream.putNextEntry(zipEntry)

                def zipEntryName = zipEntry.name
                zipEntryName = zipEntryName.replace(File.separator, ".")
                boolean b = checkClassFile(zipEntryName)

                if (b) {
                    zipEntryName = zipEntryName.substring(0, zipEntryName.length() - SdkConstants.DOT_CLASS.length())
                    System.out.println("***** zipEntry name *****: " + zipEntryName)
                    CtClass ctClass = pool.get(zipEntryName)
                    if (!ctClass.isInterface()) {
                        insertCode(ctClass)
                        byte[] code = ctClass.toBytecode()
                        jarOutputStream.write(code)
                        ctClass.detach()
                    } else {
                        byte[] bytes = IOUtils.toByteArray(inputStream)
                        jarOutputStream.write(bytes)
                        inputStream.close()
                    }
                } else {
                    byte[] bytes = IOUtils.toByteArray(inputStream)
                    jarOutputStream.write(bytes)
                    inputStream.close()
                }
                jarOutputStream.closeEntry()
            }

            jarOutputStream.close()

            def dest = outputProvider.getContentLocation(jarName + md5Name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
            System.out.println("copyFile dest: " + dest)
            System.out.println("tmpFile len: " + tmpFile.length())

            FileUtils.copyFile(tmpFile, dest)
            tmpFile.delete()
        }
    }

    private boolean checkClassFile(String name) {
        return (name.startsWith(extraInfo.packageName) && name.endsWith(".class") && !name.startsWith("R\$")
                && !"R.class".equals(name) && !"BuildConfig.class".equals(name))

//        return (name.endsWith(".class") && !name.startsWith("R\$")
//                && !"R.class".equals(name) && !"BuildConfig.class".equals(name))
    }

    private void modify(File dir, String fileName) {
        def filePath = dir.absolutePath

        if (!filePath.endsWith(SdkConstants.DOT_CLASS)) {
            return
        }
        if (filePath.contains('R$') || filePath.contains('R.class')
                || filePath.contains("BuildConfig.class")) {
            return
        }

        def className = filePath.replace(fileName, "")
                .replace("\\", ".")
                .replace("/", ".")
        def name = className.replace(SdkConstants.DOT_CLASS, "")
                .substring(1)

        System.out.println("+++++++++++++ fileName: " + fileName)
        System.out.println("+++++++++++++ name: " + name)

        if (extraInfo.packageName != null && extraInfo.packageName.trim() != "") {
            if (name != null && name.trim() != "" && name.startsWith(extraInfo.packageName)) {
                CtClass ctClass = pool.get(name)
                if (ctClass.isFrozen()) {
                    ctClass.defrost()
                }
                insertCode(ctClass, fileName)
            }
        }
    }

    private insertCode(CtClass ctClass) {
        CtMethod[] methods = ctClass.getDeclaredMethods()
        if (methods != null && methods.length > 0) {
            for (int i = 0; i < methods.length; i++) {
                CtMethod method = methods[i]
                if (!isNative(method) && !isEmpty(method)) {
                    String name = method.getName()
                    method.addLocalVariable("startTime", CtClass.longType)
                    method.addLocalVariable("endTime", CtClass.longType)
                    method.insertBefore("startTime = System.currentTimeMillis();")
                    method.insertAfter("endTime = System.currentTimeMillis();")
                    String s = """android.util.Log.i("xxxx-plugin","{0} total time: " + (endTime - startTime));"""
                    method.insertAfter(MessageFormat.format(s, name))
                }
            }
        }

        System.out.println("insertCode end ......")
    }

    private void insertCode(CtClass ctClass, String fileName) {
        CtMethod[] methods = ctClass.getDeclaredMethods()
        if (methods != null) {
            for (int i = 0; i < methods.length; i++) {
                CtMethod method = methods[i]
                if (!isNative(method) && !isEmpty(method)) {
                    String name = method.getName()
                    method.addLocalVariable("startTime", CtClass.longType)
                    method.addLocalVariable("endTime", CtClass.longType)
                    method.insertBefore("startTime = System.currentTimeMillis();")
                    method.insertAfter("endTime = System.currentTimeMillis();")
                    String s = """android.util.Log.i("xxxx-plugin","{0} total time: " + (endTime - startTime));"""
                    method.insertAfter(MessageFormat.format(s, name))
                }
            }
        }

        ctClass.writeFile(fileName)
        ctClass.detach()

        System.out.println("insertCode end ......")
    }

    private void findTarget(File dir, String fileName) {
        if (dir.isDirectory()) {
            dir.listFiles().each {
                findTarget(it, fileName)
            }
        } else {
            modify(dir, fileName)
        }
    }

    private static boolean isNative(CtMethod method) {
        return Modifier.isNative(method.getModifiers())
    }

    private static boolean isEmpty(CtMethod method) {
        return method.isEmpty()
    }
}