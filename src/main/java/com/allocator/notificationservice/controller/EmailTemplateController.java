package com.allocator.notificationservice.controller;

import com.allocator.notificationservice.dto.TemplateRequest;
import com.allocator.notificationservice.model.EmailTemplate;
import com.allocator.notificationservice.repository.EmailTemplateRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/templates")
@RequiredArgsConstructor
public class EmailTemplateController {

    private final EmailTemplateRepository repository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmailTemplate createTemplate(@Valid @RequestBody TemplateRequest request) {
        EmailTemplate template = EmailTemplate.builder()
                .name(request.getName())
                .subjectTemplate(request.getSubjectTemplate())
                .bodyTemplate(request.getBodyTemplate())
                .build();
        return repository.save(template);
    }

    @GetMapping
    public List<EmailTemplate> getAllTemplates() {
        return repository.findAll();
    }

    @GetMapping("/{name}")
    public EmailTemplate getTemplate(@PathVariable String name) {
        return repository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Template not found"));
    }
}
