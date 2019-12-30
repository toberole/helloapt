package com.zhouwei;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * Created by zhouwei on 2017/12/23.
 */
@AutoService(Processor.class)
public class DBAPTUtil extends AbstractProcessor {
    private Elements elementUtils;

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        // 获取所有的带有DB注解的类
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(DB.class);

        // dbName,element 用户可以指定数据库的名字
        Map<String, List<Element>> dbs = new HashMap<>();

        ClassName SQLiteOpenHelper_ = ClassName.get("android.database.sqlite", "SQLiteOpenHelper");
        ClassName SQLiteDatabase_ = ClassName.get("android.database.sqlite", "SQLiteDatabase");
        ClassName Context_ = ClassName.get("android.content", "Context");

        //获取dbname
        for (Element e : elements) {
            DB annation = e.getAnnotation(DB.class);
            String dbName = annation.dbName();
            List<Element> es = dbs.get(dbName);
            if (null == es) {
                es = new ArrayList<Element>();
            }
            es.add(e);
        }

        System.out.println("dbs size: "+dbs.size());

        for (Element element : elements) {
            MethodSpec constructor = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(Context_, "context")
                    .addParameter(String.class, "name")
                    .addStatement("super(context, name, null, 1)")
                    .build();

            // 构造sql语句 获取该类上所有成员
            List<? extends Element> members = elementUtils.getAllMembers((TypeElement) element);
            StringBuilder sb = new StringBuilder();
            sb.append("CREATE TABLE IF NOT EXISTS");
            sb.append(" " + element.getSimpleName() + " (");

            // 为类构建对应的表
            for (int i = 0; i < members.size(); i++) {
                Element e = members.get(i);
                createSql(sb, e);
            }

            MethodSpec onCreate = MethodSpec.methodBuilder("onCreate")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(TypeName.VOID)
                    .addParameter(SQLiteDatabase_, "db")
                    .addStatement("String sql = " + "\"" + sb.substring(0, sb.length() - 2) + ")\"")
                    .addStatement("db.execSQL(sql)")
                    .build();


            MethodSpec onUpgrade = MethodSpec.methodBuilder("onUpgrade")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(TypeName.VOID)
                    .addParameter(SQLiteDatabase_, "db")
                    .addParameter(int.class, "oldVersion")
                    .addParameter(int.class, "newVersion")
                    .build();

            TypeSpec type = TypeSpec.classBuilder(element.getSimpleName() + "DB")
                    .addMethod(constructor)
                    .superclass(SQLiteOpenHelper_)
                    .addModifiers(Modifier.PUBLIC)
                    .addMethod(onCreate).addMethod(onUpgrade)
                    .build();


            JavaFile javaFile = JavaFile.builder(getPackageName((TypeElement) element), type).build();
            try {
                javaFile.writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    private void createSql(StringBuilder sb, Element e) {
        String memberName = e.getSimpleName().toString();

        // 获取该元素上的所有注解
        List<? extends AnnotationMirror> annotationMirrors = e.getAnnotationMirrors();
        if (annotationMirrors.size() == 1) {
            AnnotationMirror annotationMirror = annotationMirrors.get(0);
            String annotationName = annotationMirror.getAnnotationType().asElement().getSimpleName().toString();
            //(name===Property  type=String  name=dada  name===Primarykey  )
            if (Property.class.getSimpleName().equals(annotationName)) {
                // 处理Property注解上面的值
                Map<? extends ExecutableElement, ? extends AnnotationValue> values = annotationMirror.getElementValues();
                // annotationMirror 封装的是注解的 属性和值的键值对
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> en : values.entrySet()) {
                    ExecutableElement ee = en.getKey();
                    AnnotationValue av = en.getValue();
                    String annotationPramName = ee.getSimpleName().toString();// type
                    String annotationPramValue = av.getValue().toString().trim();//String
                    if ("type".equals(annotationPramName)) {
                        if ("String".equals(annotationPramValue + "")) {
                            sb.append(memberName + " TEXT,");
                        }
                    }
                }
            } else if (Primarykey.class.getSimpleName().equals(annotationName)) {
                // 处理Primarykey注解上面的值
                Map<? extends ExecutableElement, ? extends AnnotationValue> values = annotationMirror.getElementValues();
                // annotationMirror 封装的是注解的 属性和值的键值对
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> en : values.entrySet()) {
                    ExecutableElement ee = en.getKey();
                    AnnotationValue av = en.getValue();
                    String annotationPramName = ee.getSimpleName().toString();// type
                    String annotationPramValue = av.getValue().toString().trim();//String
                    if ("type".equals(annotationPramName)) {
                        if ("String".equals(annotationPramValue + "")) {
                            sb.append(memberName + " TEXT,");
                        } else if ("int".equals(annotationPramValue + "")) {
                            sb.append(memberName + " INTEGER,");
                        }
                    }
                }
            } else if (AutoIncrement.class.getSimpleName().equals(annotationName)) {
                sb.append(memberName + " INTEGER,");
            }
        }
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elementUtils = processingEnv.getElementUtils();
    }

    private String getPackageName(TypeElement typeElement) {
        return elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(DIActivity.class.getCanonicalName());
    }
}
