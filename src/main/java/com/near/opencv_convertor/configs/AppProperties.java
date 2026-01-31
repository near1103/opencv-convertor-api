package com.near.opencv_convertor.configs;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private Cors cors = new Cors();
    private Uploads uploads = new Uploads();

    @Getter @Setter
    public static class Cors {
        private String[] allowedOrigins;
    }

    @Getter @Setter
    public static class Uploads {
        private String path;
        private String url;
    }
}
