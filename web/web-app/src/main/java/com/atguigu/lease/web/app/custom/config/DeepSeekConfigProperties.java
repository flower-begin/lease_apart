package com.atguigu.lease.web.app.custom.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "deepseek.api")
public class DeepSeekConfigProperties {

    private String key;
    private String url = "https://api.deepseek.com/v1/chat/completions";
    private String model = "deepseek-chat";

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}
