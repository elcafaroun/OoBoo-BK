package io.c4us.masterbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.default-user")
public class DefaultUserProperties {

    private String name;
    private String email;
    private String phone;
    private String password;
    private String profile;
    private String structure;

}
