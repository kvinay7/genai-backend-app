package com.example.backend.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import jakarta.annotation.PostConstruct;
import net.logstash.logback.encoder.LogstashEncoder;
import net.logstash.logback.fieldnames.LogstashFieldNames;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LogbackConfig {

    @PostConstruct
    public void configureLogback() {

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();

        LogstashEncoder encoder = new LogstashEncoder();
        encoder.setContext(context);

        LogstashFieldNames fieldNames = new LogstashFieldNames();
        fieldNames.setTimestamp("timestamp");
        fieldNames.setMessage("message");
        fieldNames.setLevel("level");

        encoder.setFieldNames(fieldNames);
        encoder.start();

        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(context);
        consoleAppender.setName("consoleAppender");
        consoleAppender.setEncoder(encoder);
        consoleAppender.start();

        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.INFO);
        rootLogger.addAppender(consoleAppender);
    }
}
