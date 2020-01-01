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

javassist:
ClassPool：javassist的类池，使用ClassPool 类可以跟踪和控制所操作的类,它的工作方式与 JVM 类装载器非常相似。
CtClass： CtClass提供了检查类数据（如字段和方法）以及在类中添加新字段、方法和构造函数、以及改变类、父类和接口的方法。不过，Javassist 并未提供删除类中字段、方法或者构造函数的任何方法。
CtField：用来访问域
CtMethod ：用来访问方法
CtConstructor：用来访问构造器
insertClassPath:为ClassPool添加搜索路径，否则ClassPool 无法找打对应的类
eg:
    classPool.insertClassPath(new ClassClassPath(String.class));
    classPool.insertClassPath(new ClassClassPath(Person.class));
    classPool.insertClassPath("/Users/feifei/Desktop/1");

classPool.get(className);加载一个类
classPool.makeClass(className);创建一个类
CtClass.addField();添加属性
CtClass.addMethod(); 添加方法
eg:
    CtField ageField = new CtField(CtClass.intType,"age",stuClass);
    stuClass.addField(ageField);
    CtMethod setMethod = CtMethod.make("public void setAge(int age) { this.age = age;}",stuClass);
    stuClass.addMethod(getMethod);

Class<?>clazz = stuClass.toClass();将CtCLass对象转化为JVM对象

// 创建一个类，并写入到本地文件
public static void testCreateClass(){
    System.out.println("testCreateClass");
    //创建ClassPool
    ClassPool classPool = ClassPool.getDefault();

    //添加类路径
//        classPool.insertClassPath(new ClassClassPath(this.getClass()));
    classPool.insertClassPath(new ClassClassPath(String.class));
    //创建类
    CtClass stuClass = classPool.makeClass("com.feifei.Student");

    //加载类
    //classPool.get(className)
    try {
        //添加属性
        CtField idField = new CtField(CtClass.longType,"id",stuClass);
        stuClass.addField(idField);

        CtField nameField = new CtField(classPool.get("java.lang.String"),"name",stuClass);
        stuClass.addField(nameField);

        CtField ageField = new CtField(CtClass.intType,"age",stuClass);
        stuClass.addField(ageField);


        //添加方法
        CtMethod getMethod = CtMethod.make("public int getAge(){return this.age;}",stuClass);
        CtMethod setMethod = CtMethod.make("public void setAge(int age) { this.age = age;}",stuClass);

        stuClass.addMethod(getMethod);
        stuClass.addMethod(setMethod);

        //toClass 将CtClass 转换为java.lang.class
        Class<?>clazz = stuClass.toClass();
        System.out.println("testCreateClass clazz:"+clazz);

        System.out.println("testCreateClas ------ 属性列表 -----");
        Field[] fields = clazz.getDeclaredFields();
        for(Field field:fields){
            System.out.println("testCreateClass"+field.getType()+"\t"+field.getName());
        }

        System.out.println("testCreateClass ------ 方法列表 -----");

        Method[] methods = clazz.getDeclaredMethods();
        for(Method method:methods){
            System.out.println("feifei  "+method.getReturnType()+"\t"+method.getName()+"\t"+ Arrays.toString(method.getParameterTypes()));
        }

        stuClass.writeFile("/Users/feifei/Desktop/1");
    } catch (Exception e){
        e.printStackTrace();
    }finally {
        //将stuClass 从ClassPool 移除
        if(stuClass != null){
            stuClass.detach();
        }
    }
}


package com.example.myjavassist;

public class Person {
}

// 修改一个类的父类
public static void testSetSuperClass(){

    System.out.println("testSetSuperClass");
    //创建ClassPool
    ClassPool classPool = ClassPool.getDefault();


    try {
        //添加类路径
        classPool.insertClassPath(new ClassClassPath(String.class));
        classPool.insertClassPath(new ClassClassPath(Person.class));
        classPool.insertClassPath("/Users/feifei/Desktop/1");

        // 加载类
        //创建类
        CtClass stuClass = classPool.get("com.feifei.Student");
        CtClass personClass = classPool.get("com.example.myjavassist.Person");

        if(stuClass.isFrozen()){
            stuClass.freeze();
        }
        stuClass.setSuperclass(personClass);

        //toClass 将CtClass 转换为java.lang.class
        Class<?>clazz = stuClass.toClass();
        System.out.println("testSetSuperClass ------ 属性列表 -----");
        Field[] fields = clazz.getDeclaredFields();
        for(Field field:fields){
            System.out.println("testCreateClass"+field.getType()+"\t"+field.getName());
        }

        System.out.println("testSetSuperClass ------ 方法列表 -----");

        Method[] methods = clazz.getDeclaredMethods();
        for(Method method:methods){
            System.out.println("testSetSuperClass  "+method.getReturnType()+"\t"+method.getName()+"\t"+ Arrays.toString(method.getParameterTypes()));
        }

        stuClass.writeFile("/Users/feifei/Desktop/1");
        personClass.writeFile("/Users/feifei/Desktop/1");

    } catch (Exception e) {
        e.printStackTrace();
    } finally {

    }
}


// 方法重命名、复制方法、新建方法，添加方法体
public class Calculator {
    public void getSum(long n) {
        long sum = 0;
        for (int i = 0; i < n; i++) {
            sum += i;
        }
        System.out.println("n="+n+",sum="+sum);
    }
}
public static void testInsertMethod(){

    ClassPool pool = ClassPool.getDefault();
    CtClass ctClass = null;
    try {
        ctClass = pool.get("com.example.myjavassist.Calculator");

        //获取类中现有的方法
        String getSumName = "getSum";
        CtMethod methodOld = ctClass.getDeclaredMethod(getSumName);


        String methodNewName = getSumName+"$impl";
        //修改原有方法的方法名
        methodOld.setName(methodNewName);


        //创建一个新的方法getSumName,并将旧方法 复制成新方法中.
        CtMethod newMethod = CtNewMethod.copy(methodOld,getSumName,ctClass,null);

        //设置新newMethod的方法体
        StringBuffer body = new StringBuffer();
        body.append("{\nlong start = System.currentTimeMillis();\n");
        // 调用原有代码，类似于method();($$)表示所有的参数
        body.append(methodNewName + "($$);\n");
        body.append("System.out.println(\"Call to method " + methodNewName
                + " took \" +\n (System.currentTimeMillis()-start) + " + "\" ms.\");\n");
        body.append("}");

        newMethod.setBody(body.toString());

        //为类新添加方法
        ctClass.addMethod(newMethod);

        Calculator calculator =(Calculator)ctClass.toClass().newInstance();
        calculator.getSum(10000);

        //将类输出到文件
        ctClass.writeFile("/Users/feifei/Desktop/1");

    } catch (Exception e){
        e.printStackTrace();
    }
    finally {
        if(ctClass!=null){
            ctClass.detach();
        }
    }
}


每个Transform其实都是一个gradle task，Android编译器中的TaskManager将每个Transform串连起来，
第一个Transform接收来自javac编译的结果，以及已经拉取到在本地的第三方依赖（jar. aar），
还有resource资源，注意，这里的resource并非android项目中的res资源，而是asset目录下的资源。
这些编译的中间产物，在Transform组成的链条上流动，每个Transform节点可以对class进行处理再传递
给下一个Transform。我们常见的混淆，Desugar等逻辑，它们的实现如今都是封装在一个个Transform中，
而我们自定义的Transform，会插入到这个Transform链条的最前面。
Transform其实可以有两种输入:
一种是消费型的，当前Transform需要将消费型型输出给下一个Transform，
另一种是引用型的，当前Transform可以读取这些输入，而不需要输出给下一个Transform，
比如Instant Run就是通过这种方式，检查两次编译之间的diff的。

JVM平台上，处理字节码的框架最常见的就三个，ASM，Javasist，AspectJ。
我尝试过Javasist，而AspectJ也稍有了解，最终选择ASM，因为使用它可以更底层地处理字节码的每条命令，处理速度、内存占用，也优于其他两个框架。

ASM:
ASM设计了两种API类型，一种是Tree API, 一种是基于Visitor API(visitor pattern)，

Tree API将class的结构读取到内存，构建一个树形结构，然后需要处理Method、Field等元素时，到树形结构中定位到某个元素，进行操作，然后把操作再写入新的class文件。

Visitor API则将通过接口的方式，分离读class和写class的逻辑，一般通过一个ClassReader负责读取class字节码，然后ClassReader通过一个ClassVisitor接口，将字节码的每个细节按顺序通过接口的方式，传递给ClassVisitor（你会发现ClassVisitor中有多个visitXXXX接口），这个过程就像ClassReader带着ClassVisitor游览了class字节码的每一个指令。

上面这两种解析文件结构的方式在很多处理结构化数据时都常见，一般得看需求背景选择合适的方案，而我们的需求是这样的，出于某个目的，寻找class文件中的一个hook点，进行字节码修改，这种背景下，我们选择Visitor API的方式比较合适。





public class CreateJavaExtension {
    def str = "动态生成Java类的字符串"
}
































