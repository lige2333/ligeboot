package cn.lige2333.spring.servlet;

import cn.lige2333.spring.handler.HandlerManager;
import cn.lige2333.spring.handler.MappingHandler;

import javax.servlet.*;
import java.io.IOException;

public class DispatcherServlet implements Servlet {
    public void init(ServletConfig servletConfig) throws ServletException {

    }

    public ServletConfig getServletConfig() {
        return null;
    }

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

    public String getServletInfo() {
        return null;
    }

    public void destroy() {

    }
}
