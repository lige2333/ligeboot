package cn.lige2333.spring.context;

import cn.lige2333.spring.annotation.*;
import cn.lige2333.spring.utils.ClassUtil;
import org.apache.commons.lang3.math.NumberUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BeanContext {
    private static ConcurrentHashMap<String, Object> context = new ConcurrentHashMap<>();

    public static List<Class<?>> classes = new ArrayList<>();

    public static ResourceBundle resourceBundle = ResourceBundle.getBundle("application");

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

    /**
     * 为实例对象注入属性
     */
    private static void initField(Object object) throws IllegalAccessException, InvocationTargetException {
        //获取字节码
        Class<?> clazz = object.getClass();
        //获取所有属性
        Field[] fields = clazz.getDeclaredFields();
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

    /**
     * 根据名字生成实例对象
     */
    private static void initClass(String beanName) {
        Map<String, Class<?>> classMap = classes.stream()
                .filter(aClass -> aClass.isAnnotationPresent(Component.class) || aClass.isAnnotationPresent(Controller.class)
                        || aClass.isAnnotationPresent(Configuration.class) || aClass.isAnnotationPresent(ConfigurationProperties.class))
                .collect(Collectors.toMap(clazz -> ClassUtil.toLowerCaseFirstOne(clazz.getSimpleName()), Function.identity()));
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
