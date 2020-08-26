package cn.lige2333.spring.context;

import cn.lige2333.spring.annotation.Autowired;
import cn.lige2333.spring.annotation.Component;
import cn.lige2333.spring.annotation.Controller;
import cn.lige2333.spring.utils.ClassUtil;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BeanContext {
    private static ConcurrentHashMap<String, Object> context = new ConcurrentHashMap<>();

    private static Map<String, Class<?>> classMap;

    /**
     * 载入class
     */
    public static void initContext(String path) {
        //保存名字与字节码的映射
        classMap = ClassUtil.getClasses(path).stream()
                .filter(aClass -> aClass.isAnnotationPresent(Component.class)|| aClass.isAnnotationPresent(Controller.class))
                .collect(Collectors.toMap(Class::getSimpleName, Function.identity()));
    }

    /**
     * 根据名字获取Bean
     */
    public static Object getBean(String beanName) {
        //进入容器寻找实例对象
        Object o = context.get(beanName);
        //如果对象不存在则新建
        if (o == null) {
            initClass(ClassUtil.toUpperCaseFirstOne(beanName));
            o = context.get(beanName);
        }
        return o;
    }

    /**
     * 为实例对象注入属性
     */
    private static void initField(Object object) throws IllegalAccessException {
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

    /**
     * 根据名字生成实例对象
     */
    private static void initClass(String beanName) {
        //获取字节码
        Class<?> clazz = classMap.get(beanName);
        initClass(clazz);
    }

    /**
     * 根据class生成实例对象
     */
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

}
