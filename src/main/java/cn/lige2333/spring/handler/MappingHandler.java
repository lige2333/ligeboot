package cn.lige2333.spring.handler;

import cn.lige2333.spring.context.BeanContext;
import cn.lige2333.spring.utils.ClassUtil;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
