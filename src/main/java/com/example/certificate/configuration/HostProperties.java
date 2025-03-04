package com.example.certificate.configuration;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "hosts-properties")
public class HostProperties {
    private List<HostItem> items;

    public List<HostItem> getItems() {
        return items;
    }

    public void setItems(List<HostItem> items) {
        this.items = items;
    }
}