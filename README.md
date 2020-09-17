# 手写一个Springboot

自己手写了一个SpringBoot，通过maven作为项目管理器，实现了通过启动器启动，Tomcat继承，SpringMVC路由分发以及Spring容器的IOC和DI

## 1、启动器

启动器的模仿比较简单，主要就是写一个run静态方法，用户可以通过main方法直接调用该静态方法进行容器的启动。

```java
public class SpringBootApplication {
  public static void run(Class<?> cls, String[] args) {
      System.out.println("WelCome to use Springboot");
      //进行Springboot启动的加载
  }
}
```

可以通过手动创建微服务入口启动微服务

```java
public class mainApplication {
    public static void main(String[] args) {
        SpringBootApplication.run(mainApplication.class,args);
    }
}
```

## 2、Tomcat集成

集成tomcat的思路就是在微服务启动时运行Tomcat Server，并且设置守护线程防止tomcat中途退出。

集成tomcat通过引入jar包：

```
<dependency>
    <groupId>org.apache.tomcat.embed</groupId>
    <artifactId>tomcat-embed-core</artifactId>
    <version>8.5.23</version>
</dependency>
```

然后创建TomcatServer类：

```java
public class TomcatServer {
    private Tomcat tomcat = new Tomcat();


    public void startServer() throws LifecycleException {
        //实例化tomcat
        tomcat.start();
        //实例化context容器
        Context context = new StandardContext();
        context.setPath("");
        context.addLifecycleListener(new Tomcat.FixContextListener());

        tomcat.getHost().addChild(context);

        //设置守护线程防止tomcat中途退出
        Thread awaitThread = new Thread("tomcat_await_thread."){
            @Override
            public void run() {
                TomcatServer.this.tomcat.getServer().await();
            }
        };
        //设置为非守护线程
        awaitThread.setDaemon(false);
        awaitThread.start();
    }
}
```

最后在程序启动时运行tomcatServer

```java
public class SpringBootApplication {
  public static void run(Class<?> cls, String[] args) {
      System.out.println("WelCome to use Springboot");
      //运行tomcat
    TomcatServer tomcatServer = new TomcatServer(args);
            tomcatServer.startServer();
  }
}
```

##3、IOC和DI

IOC和DI指的是Spring容器对于对象的管理，IOC代表了控制反转，也就是对象不再通过new进行创建，而是交给Spring容器统一创建，这样可以降低耦合性，DI指的是在对象在需要被使用时，要将其从Spring容器中取出注入到使用方。

可以通过注解+反射完成Spring容器的简单编写。

###3.1 BeanContext

写一个Spring容器：

```java
public class BeanContext {
  //储存对象的Map，key为对象名字，value为对象
    private static ConcurrentHashMap<String, Object> context = new ConcurrentHashMap<>();
  //容器启动时，将所有在该包下的字节码文件加载入该集合
		public static List<Class<?>> classes = new ArrayList<>();
}
```

准备好对应的注解：

@Autowired

```java
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Autowired {
}
```

@Component

```java
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Component {
}
```

然后实现对应的从容器中获取对象的getBean方法

```java
 /**
   * 根据名字获取Bean
   */
public static Object getBean(String beanName) {
    //进入容器寻找实例对象
    Object o = context.get(beanName);
    //如果对象不存在则新建
    if (o == null) {
        initClass(beanName);
        o = context.get(beanName);
    }
    return o;
}
```

如果要新建对象，我们需要获取对象的字节码，并通过反射创建对象与注入属性，字节码文件会在容器启动时加载进classes集合，此处根据反射获取是否存在@Component注解以决定是否需要加载该对象。

```java
private static void initClass(String beanName) {
    Map<String, Class<?>> classMap = classes.stream()
            .filter(aClass -> aClass.isAnnotationPresent(Component.class))
            .collect(Collectors.toMap(clazz -> ClassUtil.toLowerCaseFirstOne(clazz.getSimpleName()), Function.identity()));
    //获取字节码
    Class<?> clazz = classMap.get(beanName);
    initClass(clazz);
}
```

然后通过字节码生成实例对象

```java
private static void initClass(Class<?> clazz) {
    try {
        //生成实例对象
        Object o = clazz.getDeclaredConstructor().newInstance();
        //注入属性
        initField(o);
        //放入容器
        context.put(ClassUtil.toLowerCaseFirstOne(clazz.getSimpleName()), o);
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

属性的注入需要根据@Autowired注解进行判断，如果该属性依赖的对象没有生成，则通过递归调用对象的初始化方法，直到该对象所依赖的所有对象被生成为止。

```java
private static void initField(Object object) throws IllegalAccessException, InvocationTargetException {
    //获取字节码
    Class<?> clazz = object.getClass();
    //获取所有属性
    Field[] fields = clazz.getDeclaredFields();
       for (Field field : fields) {
            //如果打上注解则注入
            if (field.isAnnotationPresent(Autowired.class)) {
                Object o = context.get(field.getName());
                //判断需要注入的Bean是否创建，如果未创建则创建
                if (o == null) {
                    o = getBean(field.getName());
                }
                //注入对象
                field.setAccessible(true);
                field.set(object, o);
            }  
        }
}
```

### 3.2 当前包下所有类的获取

可以通过启动器中传入的启动器字节码获取启动器的包名并加载出所有包中的类的字节码。

```
public class SpringBootApplication {
  public static void run(Class<?> cls, String[] args) {
      System.out.println("WelCome to use Springboot");
      //载入配置（上文提到的classes集合）
      BeanContext.classes = ClassUtil.getClasses(cls.getPackage().getName());
  }
}
```

ClassUtils

```java
public static List<Class<?>> getClasses(String packageName) {

    // 第一个class类的集合
    List<Class<?>> classes = new ArrayList<Class<?>>();
    // 是否循环迭代
    boolean recursive = true;
    // 获取包的名字 并进行替换
    String packageDirName = packageName.replace('.', '/');
    // 定义一个枚举的集合 并进行循环来处理这个目录下的things
    Enumeration<URL> dirs;
    try {
        dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
        // 循环迭代下去
        while (dirs.hasMoreElements()) {
            // 获取下一个元素
            URL url = dirs.nextElement();
            // 得到协议的名称
            String protocol = url.getProtocol();
            // 如果是以文件的形式保存在服务器上
            if ("file".equals(protocol)) {
                // 获取包的物理路径
                String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                // 以文件的方式扫描整个包下的文件 并添加到集合中
                findAndAddClassesInPackageByFile(packageName, filePath, recursive, classes);
            } 
            } catch (IOException e) {
            e.printStackTrace();
        }

        return classes;
}
public static void findAndAddClassesInPackageByFile(String packageName, String packagePath, final boolean recursive,
                                                        List<Class<?>> classes) {
        // 获取此包的目录 建立一个File
        File dir = new File(packagePath);
        // 如果不存在或者 也不是目录就直接返回
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        // 如果存在 就获取包下的所有文件 包括目录
        // 自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
        File[] dirfiles = dir.listFiles(file -> (recursive && file.isDirectory()) || (file.getName().endsWith(".class")));
        // 循环所有文件
        for (File file : dirfiles) {
            // 如果是目录 则继续扫描
            if (file.isDirectory()) {
                findAndAddClassesInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(), recursive,
                        classes);
            } else {
                // 如果是java类文件 去掉后面的.class 只留下类名
                String className = file.getName().substring(0, file.getName().length() - 6);
                try {
                    // 添加到集合中去
                    classes.add(Class.forName(packageName + '.' + className));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }
```

这样就可以获得包下所有的字节码文件了。

# 4、配置类的加载

如果需要创建一些自定义对象的话，Springboot中可以通过定义@Configuration的配置类，相应的，我们可以通过配置文件对于一些Bean的属性进行配置，通过@ConfigurationProperties来实现从配置文件中读取Bean所需要的属性，最后通过@Bean来实例化对象。如果我们需要对一些类中的属性进行自定义配置的话，会通过@Value加上配置名称，并且在配置文件中用对应的配置名称进行配置，这些都可以通过反射+注解来实现。

@Configuration

```java
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Configuration {
}
```

@ConfigurationProperties

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConfigurationProperties {
    String value();
}
```

@Bean

```java
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Bean {
}
```

@Value

```java
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Value {
    String value();
}
```

在beanContext加载时加上@Configuration和@ConfigurationProperties注解的扫描

```java
private static void initClass(String beanName) {
    Map<String, Class<?>> classMap = classes.stream()
            .filter(aClass -> aClass.isAnnotationPresent(Component.class) || aClass.isAnnotationPresent(Controller.class)
                    || aClass.isAnnotationPresent(Configuration.class) || aClass.isAnnotationPresent(ConfigurationProperties.class))
            .collect(Collectors.toMap(clazz -> ClassUtil.toLowerCaseFirstOne(clazz.getSimpleName()), Function.identity()));
    //获取字节码
    Class<?> clazz = classMap.get(beanName);
    initClass(clazz);
}
```

读取配置文件

```
public static ResourceBundle resourceBundle = ResourceBundle.getBundle("application");
```

注入属性时区分是否为配置类，如果是，则按照配置类中的@Bean注解将对象注入容器

```java
private static void initField(Object object) throws IllegalAccessException, InvocationTargetException {
    //获取字节码
    Class<?> clazz = object.getClass();
    //获取所有属性
    Field[] fields = clazz.getDeclaredFields();
    //根据@ConfigurationProperties的name.属性名的规则读取配置文件
    if (clazz.isAnnotationPresent(ConfigurationProperties.class)) {
        for (Field field : fields) {
            //如果配置文件有key则注入
            if (resourceBundle.containsKey(clazz.getAnnotation(ConfigurationProperties.class).value() + "." + field.getName())) {
                String value = resourceBundle.getString(clazz.getAnnotation(ConfigurationProperties.class).value() + "." + field.getName());
                field.setAccessible(true);
                if (NumberUtils.isCreatable(value) && field.getType().equals(Integer.class)) {
                    Integer val = NumberUtils.toInt(value);
                    field.set(object, val);
                }else {
                    field.set(object, value);
                }

            }
        }
    } else {
        for (Field field : fields) {
            //如果打上注解则注入
            if (field.isAnnotationPresent(Autowired.class)) {
                Object o = context.get(field.getName());
                //判断需要注入的Bean是否创建，如果未创建则创建
                if (o == null) {
                    o = getBean(field.getName());
                }
                //注入对象
                field.setAccessible(true);
                field.set(object, o);
            }
            //读取@Value注解所配置的属性
            if (field.isAnnotationPresent(Value.class)) {
                //判断是否配置文件中配置了
                if (resourceBundle.containsKey(field.getAnnotation(Value.class).value())) {
                    String value = resourceBundle.getString(field.getAnnotation(Value.class).value());
                    field.setAccessible(true);
                    if (NumberUtils.isCreatable(value) && field.getType().equals(Integer.class)) {
                        Integer val = NumberUtils.toInt(value);
                        field.set(object, val);
                    }else {
                        field.set(object, value);
                    }
                }
            }
        }
        //加载配置类中被@bean标记的方法所反悔的对象，放入容器中
        if (clazz.isAnnotationPresent(Configuration.class)) {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(Bean.class)) {
                    Parameter[] parameters = method.getParameters();
                    Object[] paramList = new Object[parameters.length];
                    for (int i = 0; i < parameters.length; i++) {
                        paramList[i] = getBean(ClassUtil.toLowerCaseFirstOne(parameters[i].getType().getSimpleName()));
                    }
                    Object returnObj = method.invoke(object, paramList);
                    context.put(method.getName(), returnObj);
                }
            }
        }
    }

}
```

至此，tomcat可以使用配置类进行配置

创建自定义端口属性

```java
@ConfigurationProperties("tomcat")
public class TomcatProperties {
    private Integer port;

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }
}
```

tomcat配置类

```java
@Configuration
public class TomcatConfig {

    @Autowired
    private TomcatProperties tomcatProperties;

    @Bean
    public TomcatServer tomcatServer(TomcatProperties tomcatProperties) {
        TomcatServer tomcatServer = new TomcatServer();
        tomcatServer.setport(tomcatProperties.getPort());
        return tomcatServer;
    }
}
```

在微服务启动时加载配置类

```java
public class SpringBootApplication {

    public static void run(Class<?> cls, String[] args) {
        System.out.println("WelCome to use springboot");
        try {
            //载入配置
            BeanContext.classes = ClassUtil.getClasses(cls.getPackage().getName());
            List<Class<?>> configs = BeanContext.classes.stream().filter(aClass -> aClass.isAnnotationPresent(Configuration.class)).collect(Collectors.toList());
            configs.forEach(aClass -> {
                String name = ClassUtil.toLowerCaseFirstOne(aClass.getSimpleName());
                BeanContext.getBean(name);
            });
            //直接从容器中获取Tomcat对象
            TomcatServer tomcatServer = (TomcatServer)BeanContext.getBean("tomcatServer");
            tomcatServer.startServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
```

配置文件加入端口信息

```properties
tomcat.port=8000
```

至此，IOC、DI、配置文件配置的功能全部完成

# 5、SpringMVC路由分发

SpringMVC主要通过DispatcherServlet进行路由分发，tomcat统一将请求发送给DispatcherServlet，再由DispatcherServlet将这些请求派发给具体的Mapping Handler，处理完毕后返回。

![img](https://img-blog.csdnimg.cn/20190608165851950.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMxNzQ5ODM1,size_16,color_FFFFFF,t_70)

SpringMVC中一般使用@Controller注解标注控制器并将其交由Spring容器进行管理，通过@RequestMapping注解在方法上标注路由以将方法作为MappingHandler，将方法的参数通过@RequestParam标注作为请求的参数。

新增注解@Controller、@RequestMapping、@RequestParam

```java
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Controller {
}
```

```java
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMapping {
    String value();
}
```

```java
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestParam {
    String value();
}
```

创建MappingHandler类，用来保存每个路由的信息

```java
/**
 * 此处保存单个路由的信息
 * uri：路由的路径
 * method：该路由所对应的controller的方法
 * param：该路由所需要的参数
 * controller：该路由对应的controller的字节码
 *
 * */
public class MappingHandler {
    private String uri;
    private Method method;
    private String[] param;
    private Class<?> controller;

    public MappingHandler(String uri, Method method, String[] param, Class<?> controller) {
        this.uri = uri;
        this.method = method;
        this.param = param;
        this.controller = controller;
    }

    public boolean handle(ServletRequest servletRequest, ServletResponse servletResponse) throws InvocationTargetException, IllegalAccessException, IOException {
        //判断请求的uri，如果和当前handler记录的不同则直接返回，相同则继续
        String requestURI = ((HttpServletRequest) servletRequest).getRequestURI();
        if(!this.uri.equals(requestURI)){
            return false;
        }
        //从容器中获取controller实例对象
        Object controller = BeanContext.getBean(ClassUtil.toLowerCaseFirstOne(this.controller.getSimpleName()));
        //将request参数与参数列表一一对应
        Object[] parameters = new Object[param.length];
        for (int i = 0; i < param.length; i++) {
            parameters[i]  = servletRequest.getParameter(param[i]);
        }
        //调用方法，获得返回信息
        Object response = method.invoke(controller, parameters);
        //传给前端
        servletResponse.getWriter().write(response.toString());
        return true;
    }

}
```

然后创建HandlerManager用来管理MappingHandler的运行

```java
public class HandlerManager {
    public static List<MappingHandler> mappingHandlerList = new ArrayList<>();

    public static void resolveMappingHandler(List<Class<?>> classList) {
        //获取有controller注解的类的字节码并生成MappingHandler对象
        classList.stream().filter(aClass -> aClass.isAnnotationPresent(Controller.class)).forEach(HandlerManager::genMappingHandler);
    }

    public static void genMappingHandler(Class<?> clazz) {
        //获取controller下的方法
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            //如果没有@requestmapping注解则直接返回
            if (!method.isAnnotationPresent(RequestMapping.class)) {
                return;
            }
            //获取注解中的路由
            String uri = method.getDeclaredAnnotation(RequestMapping.class).value();
            List<String> paramList = new ArrayList<>();
            //根据@requestparam注解判断参数
            for (Parameter parameter : method.getParameters()) {
                if(parameter.isAnnotationPresent(RequestParam.class)){
                    //将参数加入参数列表
                    paramList.add(parameter.getDeclaredAnnotation(RequestParam.class).value());
                }
            }
            String[] params = paramList.toArray(new String[paramList.size()]);
            //生成mappinghandler
            MappingHandler mappingHandler = new MappingHandler(uri, method, params, clazz);
            mappingHandlerList.add(mappingHandler);
        }
    }
}
```

再创建DispatcherServlet，进行MappingHandler的匹配。

```java
public class DispatcherServlet implements Servlet {
    public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        //遍历所有mappinghandler，分别检查路由是否对应，执行对应路由所对应的方法
        for (MappingHandler handler : HandlerManager.mappingHandlerList) {
            try {
                if (handler.handle(servletRequest, servletResponse)) {
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
```

最后在TomcatServer中补充Dispatcherservlet相关信息

```java
DispatcherServlet dispatcherServlet = new DispatcherServlet();
Tomcat.addServlet(context,"dispatcherServlet",dispatcherServlet).setAsyncSupported(true);

//添加URL映射
context.addServletMappingDecoded("/","dispatcherServlet");
```

至此，当用户访问了相关路由后，dispatcherServlet会根据地址自行匹配，并且找到对应的Controller，创建其实例对象并且执行@RequestMapping标注的方法。

# 6、测试

来到最激动人心的测试环节了，我们根据我们设定的功能，创建一个从Controller到Service的简单测试。

实体类Order

```java
@Data
public class Order {
    private String id;
    private String value;
    private String paid;
}
```

OrderController

```java
@Controller
public class OrderController {

    @Autowired
    private OrderService orderService;

    @RequestMapping("/test")
    public String orderControl(@RequestParam("id")String id,@RequestParam("value")String value){
        Order order = orderService.initOrder(id, value);
        return JSON.toJSONString(order);
    }
}
```

OrderService

```java
@Component
public class OrderService {

    @Autowired
    private PayService payService;

    public Order initOrder(String id, String value) {
        Order order = new Order(id, value);
        payService.payOrder(order);
        return order;
    }
}
```

PayService

```java
@Component
public class PayService {
    @Value("pay.value")
    private String paid;

    public void payOrder(Order order) {
        order.setPaid(paid);

    }
}
```

properties

```properties
tomcat.port=8000
pay.value=yes
```

启动MainApplication，tomcat启动在8000端口，根据路由/test，带上参数id和value，最终地址如下：http://localhost:8000/test?id=1&value=1

根据配置和运行逻辑，返回的String为Order对象的JSON字符串，id和value都是传入的属性：“1”、paid为配置文件中注入的value：“yes”。

最终访问结果

```json
{"id":"1","paid":"yes","value":"1"}
```

访问成功，一切尽在意料之中。

# 7、问题和回顾

以上是简单模仿了一下Springboot微服务框架的一部分功能，当然还存在许多问题尚未解决。

1、没有解决循环依赖，当循环依赖出现时，程序会直接stackoverflow

2、目前对象只支持首字母小写的命名，无法通过命名创建不同的对象

3、SpringMVC的参数只支持String，尚未支持传入对象解析。

4、容器的加载目前是懒加载，即Bean被使用时才会加载，而Spring容器可以区分启动时加载和懒加载，区分单例和多例

5、尚不支持其它框架的集成，如Mybatis等，也没有实现自动配置。

主要通过这个实现理解一下Spring容器的IOC和DI的底层实现方式以及MVC实现思路，代码已经上传github，地址为https://github.com/lige2333/ligeboot
