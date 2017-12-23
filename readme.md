# APT学习

## APT（Annotation Processing Tool）用来处理java注解的工具

原理： 通过反射注解，做出响应的处理[比如生成java源文件]，自己手动解析注解比较麻烦

开源的APT工具：
	javapote、apt-jelly

javapote的学习：


javapoet里面常用的几个类：

    MethodSpec 代表一个构造函数或方法声明。
    TypeSpec 代表一个类，接口，或者枚举声明。
    FieldSpec 代表一个成员变量，一个字段声明。
    JavaFile包含一个顶级类的Java文件。


for循环的生成方式：

```

MethodSpec main = MethodSpec.methodBuilder("main")

    .addStatement("int total = 0")
    .beginControlFlow("for (int i = 0; i < 10; i++)")
    .addStatement("total += i")
    .endControlFlow()
    .build();

```

还可以简单的直接addCode("for 循环")

demo:

<p>

	private MethodSpec computeRange(String name, int from, int to, String op) {
	  return MethodSpec.methodBuilder(name)
	      .returns(int.class)
	      .addStatement("int result = 0")
	      .beginControlFlow("for (int i = " + from + "; i < " + to + "; i++)")
	      .addStatement("result = result " + op + " i")
	      .endControlFlow()
	      .addStatement("return result")
	      .build();
	}

</p>


## 占位符

	$L 字面常量
    $S for Strings
    $T for Types
    $N for Names(我们自己生成的方法名或者变量名等等)


## API

```

MethodSpec .addJavadoc("XXX") 方法上面添加注释

MethodSpec.constructorBuilder() 构造器

MethodSpec.addAnnotation(Override.class); 方法上面添加注解

TypeSpec.enumBuilder("XXX") 生成一个XXX的枚举

TypeSpec.interfaceBuilder("HelloWorld")生成一个HelloWorld接口 ==！


```

# ClassName 它可以识别任何声明类


```

ClassName hoverboard = ClassName.get("com.mattel", "Hoverboard");

ClassName list = ClassName.get("java.util", "List");

```