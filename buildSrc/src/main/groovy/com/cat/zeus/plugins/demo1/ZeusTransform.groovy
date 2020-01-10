package com.cat.zeus.plugins.demo1

import com.android.SdkConstants
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import javassist.*
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.Project

import javax.xml.crypto.dsig.TransformException
import java.text.MessageFormat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class ZeusTransform extends Transform {
    private static ThreadPoolExecutor executor

    ExtraInfo extraInfo
    def pool = ClassPool.default
    def project

    ZeusTransform(Project project) {
        this.project = project
        this.extraInfo = project.extensions.create('ExtraInfo', ExtraInfo)
        int count = Runtime.getRuntime().availableProcessors() * 2 + 1
        executor = new ThreadPoolExecutor(count, count,
                30, java.util.concurrent.TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>())
        executor.allowCoreThreadTimeOut(true)
    }

    @Override
    String getName() {
        return ZeusTransform.simpleName
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

        int size = transformInvocation.inputs.size()
        CountDownLatch countDownLatch = new CountDownLatch(size)

        transformInvocation.inputs.each {
            executor.execute(new Runnable() {
                @Override
                void run() {
                    it.jarInputs.each {
                        System.out.println("jarInput path: " + it.file.absolutePath)
                        pool.insertClassPath(it.file.absolutePath)
                        handleJarInput(it, transformInvocation.outputProvider)
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
                        FileUtils.copyDirectory(it.file, dest)
                    }

                    countDownLatch.countDown()
                }
            })
        }

        countDownLatch.await(30, TimeUnit.SECONDS)
    }

    private void handleJarInput(JarInput jarInput, TransformOutputProvider outputProvider) {
        if (jarInput.file.getAbsolutePath().endsWith(".jar")) {
            def jarName = jarInput.name
            def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
            if (jarName.endsWith(".jar")) {
                jarName = jarName.substring(0, jarName.length() - 4)
            }

            File tmpFile = new File(jarInput.file.getParent() + File.separator + "classes_zeus.jar")
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
            FileUtils.copyFile(tmpFile, dest)
            tmpFile.delete()
        }
    }

    private boolean checkClassFile(String name) {
        return (name.startsWith(extraInfo.packageName) && name.endsWith(".class") && !name.startsWith("R\$")
                && !"R.class".equals(name) && !"BuildConfig.class".equals(name))
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
                addCode(method, ctClass)
            }
        }
    }

    private void insertCode(CtClass ctClass, String fileName) {
        CtMethod[] methods = ctClass.getDeclaredMethods()
        if (methods != null) {
            for (int i = 0; i < methods.length; i++) {
                CtMethod method = methods[i]
                addCode(method, ctClass)
            }
            ctClass.writeFile(fileName)
            ctClass.detach()
        }
    }

    private void addCode(CtMethod method, CtClass ctClass) {
        if (!isNative(method) && !isEmpty(method)) {
            String name = method.getName()
            method.addLocalVariable("startTime", CtClass.longType)
            method.addLocalVariable("endTime", CtClass.longType)
            method.insertBefore("startTime = System.currentTimeMillis();")
            method.insertAfter("endTime = System.currentTimeMillis();")

            method.addLocalVariable("n", CtClass.longType)
            method.insertAfter("n = endTime - startTime;")
            String wthreshold = extraInfo.wthreshold + ""
            String ethreshold = extraInfo.ethreshold + ""

            String is = """android.util.Log.i("{0}","{1}#{2} total time: " + n);"""
            is = MessageFormat.format(
                    is, extraInfo.tag, ctClass.simpleName, name
            )
            String ws = """android.util.Log.w("{0}","{1}#{2} total time: " + n);"""
            ws = MessageFormat.format(
                    ws, extraInfo.tag, ctClass.simpleName, name
            )
            String es = """android.util.Log.e("{0}","{1}#{2} total time: " + n);"""
            es = MessageFormat.format(
                    es, extraInfo.tag, ctClass.simpleName, name
            )

            StringBuffer sb = new StringBuffer()
            sb.append("if (n > " + ethreshold + ") {")
                    .append(es)
                    .append("} else if (n > " + wthreshold + ") {")
                    .append(ws)
                    .append("} else {")
                    .append(is)
                    .append("}")
            method.insertAfter(sb.toString())

            if (extraInfo.tryCatch) {
                addTryCatch(method, ctClass)
            }
        }
    }

    private void addTryCatch(CtMethod origin_Method, CtClass c) {
        if (c.isFrozen()) {
            c.defrost()
        }
        String new_method_name = origin_Method.getName() + "_" + "Zeus_" + System.currentTimeMillis()
        CtMethod ctMethod_New = CtNewMethod.copy(origin_Method, new_method_name, c, null)
        c.addMethod(ctMethod_New)
        int methodParameterLen = origin_Method.getParameterTypes().length
        StringBuffer sb = new StringBuffer()
        sb.append("{try{")
        if (!origin_Method.getReturnType().getName().contains("void")) {
            sb.append("return ")
        }
        sb.append(new_method_name)
        sb.append("(")
        for (int i = 0; i < methodParameterLen; i++) {
            sb.append("\$" + (i + 1))
            if (i != methodParameterLen - 1) {
                sb.append(",")
            }
        }

        String s = """android.util.Log.e("{0}","Exception: " + ex);"""
        s = MessageFormat.format(s, extraInfo.tag)

        sb.append(");}catch(Exception ex){ ")
                .append(s)
                .append("ex.printStackTrace();}")

        if (!origin_Method.getReturnType().getName().contains("void")) {
            sb.append("return ")
            String result = getReturnValue(origin_Method.getReturnType().getName())
            sb.append(result)
            sb.append(";")
        }
        sb.append("}")
        origin_Method.setBody(sb.toString())
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

    static String getReturnValue(String type) {
        String result = "null"
        switch (type) {
            case "int":
                result = "0"
                break
            case "long":
                result = "0l"
                break
            case "double":
                result = "0d"
                break
            case "float":
                result = "0f"
                break
            case "boolean":
                result = "true"
                break
            case "char":
                result = "\'a\'"
                break
            case "short":
                result = "0"
                break
            case "byte":
                result = "0"
                break
            default:
                break
        }
        return result
    }
}