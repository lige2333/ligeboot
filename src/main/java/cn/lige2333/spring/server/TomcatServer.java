package cn.lige2333.spring.server;

import cn.lige2333.spring.servlet.DispatcherServlet;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;

public class TomcatServer {
    private Tomcat tomcat = new Tomcat();


    public void startServer() throws LifecycleException {
        //实例化tomcat
        tomcat.start();
        //实例化context容器
        Context context = new StandardContext();
        context.setPath("");
        context.addLifecycleListener(new Tomcat.FixContextListener());
        DispatcherServlet dispatcherServlet = new DispatcherServlet();
        Tomcat.addServlet(context,"dispatcherServlet",dispatcherServlet).setAsyncSupported(true);

        //添加URL映射
        context.addServletMappingDecoded("/","dispatcherServlet");
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

    public void setport(Integer port){
        tomcat.setPort(port);
    }

}
