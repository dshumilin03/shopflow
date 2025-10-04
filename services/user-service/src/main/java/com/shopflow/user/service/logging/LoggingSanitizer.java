package com.shopflow.user.service.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.user.service.logging.annotation.Sensitive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.*;

@Component
@Slf4j
public class LoggingSanitizer {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_STRING_LENGTH = 200;
    private static final String MASK = "***";

    public String sanitize(Object[] args) {

        if (args == null || args.length == 0) {
            return "[]";
        }

        Map<String, Object> sanitzed = new LinkedHashMap<>();

        for (int i = 0; i < args.length; i++) {
            sanitzed.put("arg" + i, sanitizeValue(args[i]));
        }

        try {
            return MAPPER.writeValueAsString(sanitzed);
        } catch (Exception e) {
            log.debug("Failed to serialize sanitized args", e);
            return sanitzed.toString();
        }
    }

    private Object sanitizeValue(Object value) {
        if (value == null) return "<null>";

        if (value instanceof String s) {
            if (s.length() > MAX_STRING_LENGTH) {
                return s.substring(0, MAX_STRING_LENGTH) + "...";
            }
            return s;
        }

        if (isPrimitiveOrWrapper(value.getClass())) {
            return value;
        }

        if (value instanceof Collection<?> col) {
            return sanitizeCollection(col);
        }

        if (value instanceof Map<?, ?> map) {
            return sanitizeMap(map);
        }

        return sanitizeObject(value);
    }

    private List<Object> sanitizeCollection(Collection<?> collection) {
        List<Object> result = new ArrayList<>();
        for (Object item : collection) {
            result.add(sanitizeValue(item));
        }
        return result;
    }

    private Map<Object, Object> sanitizeMap(Map<?, ?> map) {
        Map<Object, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(entry.getKey(), sanitizeValue(entry.getValue()));
        }
        return result;
    }

    private Map<String, Object> sanitizeObject(Object obj) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        Class<?> clazz = obj.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                if (field.isAnnotationPresent(Sensitive.class)) {
                    sanitized.put(field.getName(), MASK);
                } else {
                    sanitized.put(field.getName(), sanitizeValue(value));
                }
            } catch (Exception e) {
                sanitized.put(field.getName(), "<error>");
            }
        }
        return sanitized;
    }

    private boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz.equals(Boolean.class) ||
                clazz.equals(Byte.class) ||
                clazz.equals(Character.class) ||
                clazz.equals(Short.class) ||
                clazz.equals(Integer.class) ||
                clazz.equals(Long.class) ||
                clazz.equals(Float.class) ||
                clazz.equals(Double.class);
    }
}
