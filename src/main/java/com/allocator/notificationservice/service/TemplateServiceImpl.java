package com.allocator.notificationservice.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TemplateServiceImpl implements TemplateService {

    @Override
    public String render(String template, Map<String, Object> variables) {
        if (template == null || variables == null) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue().toString();
            result = result.replace("${" + entry.getKey() + "}", value);
        }
        return result;
    }
}
