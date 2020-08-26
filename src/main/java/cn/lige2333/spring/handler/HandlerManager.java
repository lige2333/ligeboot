package cn.lige2333.spring.handler;

import cn.lige2333.spring.annotation.Controller;
import cn.lige2333.spring.annotation.RequestMapping;
import cn.lige2333.spring.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

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
