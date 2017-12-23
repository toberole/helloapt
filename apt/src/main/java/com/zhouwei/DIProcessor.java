package com.zhouwei;


import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

@AutoService(Processor.class)
public class DIProcessor extends AbstractProcessor {

    private Elements elementUtils;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(DIActivity.class.getCanonicalName());
    }

    /**
     * public final class DIMainActivity extends MainActivity {
     * public static void bindView(MainActivity activity) {
     * activity.tv = (android.widget.TextView) activity.findViewById(R.id.tv);
     * }
     * }
     *
     * @param set
     * @param roundEnvironment
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        // 获取所有的使用了DIActivity注解的元素
        // 如果注解使用在类上 ，那么获取到的这个元素就是个类
        // 如果注解使用在属性上，那么获取到的这个元素就是属性
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(DIActivity.class);
        for (Element element : elements) {
            // 因为DIActivity注解是被定义只能在类上使用的注解，所以这里可以将element转换为TypeElement
            // 如果DIActivity注解可以使用在类、属性等元素上 这里可用通过判断做出相应的逻辑处理
            TypeElement typeElement = (TypeElement) element;
            List<? extends Element> members = elementUtils.getAllMembers(typeElement);
            MethodSpec.Builder method = MethodSpec.methodBuilder("bindView")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(TypeName.VOID)
                    .addParameter(ClassName.get(typeElement.asType()), "activity");
            for (Element item : members) {
                DIView diView = item.getAnnotation(DIView.class);
                if (diView != null) {
                    // 添加生成的语句
                    // method.addCode() 不会自动导入包 代码需要自己格式化
                    // 但是如果使用addStatement API自动导包有冲突 那还得注意
                    // addStatement()可以格式化代码
                    method.addStatement(String.format("activity.%s = (%s) activity.findViewById(%s)", item.getSimpleName(), ClassName.get(item.asType()).toString(), diView.value()));
                }
            }
            // 生成一个类
            // "DI" + element.getSimpleName()即为生成的类的名字
            TypeSpec typeSpec = TypeSpec.classBuilder("DI" + element.getSimpleName())
                    .superclass(TypeName.get(typeElement.asType()))// 父类
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addMethod(method.build())
                    .build();
            JavaFile javaFile = JavaFile.builder(getPackageName(typeElement), typeSpec).build();
            try {
                javaFile.writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private String getPackageName(TypeElement typeElement) {
        return elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elementUtils = processingEnv.getElementUtils();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_7;
    }
}
