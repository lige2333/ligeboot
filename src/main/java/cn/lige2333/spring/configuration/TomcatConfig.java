package cn.lige2333.spring.configuration;

import cn.lige2333.spring.annotation.Autowired;
import cn.lige2333.spring.annotation.Bean;
import cn.lige2333.spring.annotation.Configuration;
import cn.lige2333.spring.server.TomcatServer;

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
