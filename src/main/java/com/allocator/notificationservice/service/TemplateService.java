package com.allocator.notificationservice.service;

import java.util.Map;

public interface TemplateService {
    String render(String template, Map<String, Object> variables);
}
