package cn.lige2333.spring.boot;

import cn.lige2333.spring.annotation.Component;
import cn.lige2333.spring.annotation.Configuration;
import cn.lige2333.spring.context.BeanContext;
import cn.lige2333.spring.handler.HandlerManager;
import cn.lige2333.spring.server.TomcatServer;
import cn.lige2333.spring.utils.ClassUtil;

import java.util.List;
import java.util.stream.Collectors;


public class LigeBootApplication {



    public static void run(Class<?> cls, String[] args) {
        System.out.println("WelCome to use ligeboot");
        try {
            //载入配置
            BeanContext.classes = ClassUtil.getClasses(cls.getPackage().getName());
            List<Class<?>> configs = BeanContext.classes.stream().filter(aClass -> aClass.isAnnotationPresent(Configuration.class)).collect(Collectors.toList());
            configs.forEach(aClass -> {
                String name = ClassUtil.toLowerCaseFirstOne(aClass.getSimpleName());
                BeanContext.getBean(name);
            });
            //启动Tomcat
            TomcatServer tomcatServer = (TomcatServer)BeanContext.getBean("tomcatServer");
            tomcatServer.startServer();
            //加载所有的Controller中的路由方法
            HandlerManager.resolveMappingHandler(ClassUtil.getClasses(cls.getPackage().getName()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
