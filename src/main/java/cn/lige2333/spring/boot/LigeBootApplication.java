package cn.lige2333.spring.boot;

import cn.lige2333.spring.context.BeanContext;
import cn.lige2333.spring.handler.HandlerManager;
import cn.lige2333.spring.server.TomcatServer;
import cn.lige2333.spring.utils.ClassUtil;


public class LigeBootApplication {
    public static void run(Class<?> cls, String[] args) {
        System.out.println("WelCome to use ligeboot");
        TomcatServer tomcatServer = new TomcatServer(args);
        try {
            //载入类
            BeanContext.initContext(cls.getPackage().getName());
            //启动Tomcat
            tomcatServer.startServer();
            //加载所有的Controller中的路由方法
            HandlerManager.resolveMappingHandler(ClassUtil.getClasses(cls.getPackage().getName()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
