package com.allocator.authservice.config;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.ConsoleAppender;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

// @Configuration("authLoggingConfig")
public class LoggingConfig {

    @PostConstruct
    public void configureJsonLogging() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("{\n" +
                "  \"timestamp\": \"%d{yyyy-MM-dd'T'HH:mm:ss.SSSZ}\",\n" +
                "  \"level\": \"%level\",\n" +
                "  \"logger\": \"%logger\",\n" +
                "  \"message\": \"%message\",\n" +
                "  \"userId\": \"%mdc{userId:-}\",\n" +
                "  \"traceId\": \"%mdc{traceId:-}\",\n" +
                "  \"serviceName\": \"%mdc{serviceName:-}\",\n" +
                "  \"operation\": \"%mdc{operation:-}\",\n" +
                "  \"failureReason\": \"%mdc{failureReason:-}\",\n" +
                "  \"exception\": \"%ex{full}%n\"\n" +
                "}");
        encoder.start();

        ConsoleAppender consoleAppender = new ConsoleAppender();
        consoleAppender.setContext(loggerContext);
        consoleAppender.setEncoder(encoder);
        consoleAppender.start();

        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.detachAndStopAllAppenders();
        rootLogger.addAppender(consoleAppender);
    }
}

