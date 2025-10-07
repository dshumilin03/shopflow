package com.shopflow.user.service.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.user.service.logging.annotation.Sensitive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
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

        Map<String, Object> sanitized = new LinkedHashMap<>();
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        for (int i = 0; i < args.length; i++) {
            sanitized.put("arg" + i, sanitizeValue(args[i], visited));
        }

        try {
            return MAPPER.writeValueAsString(sanitized);
        } catch (Exception e) {
            log.debug("Failed to serialize sanitized args", e);
            return sanitized.toString();
        }
    }

    private Object sanitizeValue(Object value, Set<Object> visited) {
        if (value == null) {
            return "<null>";
        }

        if (visited.contains(value)) {
            return "<recursion>";
        }
        visited.add(value);

        if (value instanceof String s) {
            if (s.length() > MAX_STRING_LENGTH) {
                return s.substring(0, MAX_STRING_LENGTH) + "...";
            }
            return s;
        }

        if (isPrimitiveOrWrapper(value.getClass())) {
            return value;
        }

        if (value.getClass().isEnum()) {
            return ((Enum<?>) value).name();
        }


        if (value.getClass().isArray()) {
            return sanitizeArray(value, visited);
        }

        if (value instanceof Collection<?> col) {
            return sanitizeCollection(col, visited);
        }

        if (value instanceof Map<?, ?> map) {
            return sanitizeMap(map, visited);
        }

        // TODO test it
        if (value instanceof Pageable pageable) {
            return Map.of(
                    "page", pageable.getPageNumber(),
                    "size", pageable.getPageSize(),
                    "sort", pageable.getSort().toString()
            );
        }

        if (value.getClass().getPackageName().startsWith("java.")) {
            return value.toString();
        }

        return sanitizeObject(value, visited);
    }

    private List<Object> sanitizeArray(Object array, Set<Object> visited) {
        int length = java.lang.reflect.Array.getLength(array);
        List<Object> result = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            Object element = java.lang.reflect.Array.get(array, i);
            result.add(sanitizeValue(element, visited));
        }

        return result;
    }

    private List<Object> sanitizeCollection(Collection<?> collection, Set<Object> visited) {
        List<Object> result = new ArrayList<>();
        for (Object item : collection) {
            result.add(sanitizeValue(item, visited));
        }
        return result;
    }

    private Map<Object, Object> sanitizeMap(Map<?, ?> map, Set<Object> visited) {
        Map<Object, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(entry.getKey(), sanitizeValue(entry.getValue(), visited));
        }
        return result;
    }

    private Map<String, Object> sanitizeObject(Object obj, Set<Object> visited) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        Class<?> clazz = obj.getClass();

        for (Field field : clazz.getDeclaredFields()) {

            if (field.isSynthetic() || field.getName().startsWith("this$") || field.getName().equals("serialVersionUID")) {
                continue;
            }

            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                if (field.isAnnotationPresent(Sensitive.class)) {
                    sanitized.put(field.getName(), MASK);
                } else {
                    sanitized.put(field.getName(), sanitizeValue(value, visited));
                }
            } catch (Exception e) {
                sanitized.put(field.getName(), "<error>");
            }
        }
        return sanitized;
    }

    private boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive()
                || clazz.equals(Boolean.class)
                || clazz.equals(Byte.class)
                || clazz.equals(Character.class)
                || clazz.equals(Short.class)
                || clazz.equals(Integer.class)
                || clazz.equals(Long.class)
                || clazz.equals(Float.class)
                || clazz.equals(Double.class);
    }
}
