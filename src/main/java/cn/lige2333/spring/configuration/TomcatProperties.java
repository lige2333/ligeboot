package cn.lige2333.spring.configuration;

import cn.lige2333.spring.annotation.ConfigurationProperties;

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
