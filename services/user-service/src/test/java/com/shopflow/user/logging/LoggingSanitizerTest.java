package com.shopflow.user.logging;

import com.shopflow.user.service.logging.LoggingSanitizer;
import com.shopflow.user.service.logging.annotation.Sensitive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LoggingSanitizerTest {

    private final LoggingSanitizer sanitizer = new LoggingSanitizer();

    static class TestDto {
        String email = "john@example.com";
        @Sensitive
        String password = "secret123";
        Nested nested = new Nested();
    }

    static class Nested {
        @Sensitive
        String token = "abcdef";
        String info = "visible";
    }

    @Test
    @DisplayName("Masking fields, annotated with @Sensitive")
    void masksSensitiveFields() {
        TestDto dto = new TestDto();

        String json = sanitizer.sanitize(new Object[]{dto});

        assertThat(json).contains("\"password\":\"***\"");
        assertThat(json).contains("\"token\":\"***\"");
        assertThat(json).contains("\"email\":\"john@example.com\"");
    }

    @Test
    @DisplayName("Cuts Long strings")
    void truncatesLongStrings() {
        String longText = "a".repeat(500);

        String json = sanitizer.sanitize(new Object[]{longText});

        assertThat(json).contains("...");
        assertThat(json.length()).isLessThan(300);
    }

    @Test
    @DisplayName("Returns <null> for null values")
    void handlesNullValues() {
        String json = sanitizer.sanitize(new Object[]{null});

        assertThat(json).contains("<null>");
    }

    @Test
    @DisplayName("Works with Collections and Map's")
    void handlesCollectionsAndMaps() {
        List<String> list = List.of("one", "two");
        Map<String, Object> map = Map.of("key", "value");
        Set<String> set = Set.of("angry", "bird");

        String json = sanitizer.sanitize(new Object[]{list, map, set});

        String[] resultArr = new String[] {"one", "two", "key", "value", "angry", "bird"};
        for (String element : resultArr) {
            assertThat(json).contains(element);
        }
    }

    @Test
    @DisplayName("Serializes complex objects")
    void SerializesComplexObjects() {
        Object weird = new Object() {
            @Sensitive
            String password = "hidden";
        };

        String json = sanitizer.sanitize(new Object[]{weird});

        assertThat(json).contains("***");
        assertThat(json).doesNotContain("hidden");
    }

    @Test
    @DisplayName("Primitives are not corrupt")
    void primitivesAreNotCorrupt() {
        String json = sanitizer.sanitize(new Object[]{"abc", 123, true});

        assertThat(json).contains("abc", "123", "true");
    }

    @Test
    @DisplayName("Serializes DTO's without @Sensitive")
    void serializesDTOWithoutSensitive() {
        Object dto = new Object() {
            String legitField = "legit";
        };

        String json = sanitizer.sanitize(new Object[]{dto});

        assertThat(json).contains("legit");
        assertThat(json).doesNotContain("***");
   }


    @Test
    @DisplayName("Handles deep nested structures correctly")
    void handlesDeepNestedStructures() {
        record Inner(String name) {}
        record Wrapper(java.util.List<java.util.Map<String, Inner>> data) {}

        var inner = new Inner("deep");
        var map = java.util.Map.of("key", inner);
        var list = java.util.List.of(map);
        var wrapper = new Wrapper(list);

        String json = sanitizer.sanitize(new Object[]{wrapper});

        assertThat(json).contains("deep");
        assertThat(json).contains("key");
    }

    @Test
    @DisplayName("Handles unserializable fields safely")
    void handlesUnserializableFields() {
        class UnserializableDTO {
            String name = "test";
            Thread thread = new Thread();
        }

        Object dto = new UnserializableDTO();
        String json = sanitizer.sanitize(new Object[]{dto});

        assertThat(json).contains("test");
        assertThat(json).contains("<error>");
    }

    @Test
    @DisplayName("Handles arrays as collections")
    void handlesArrays() {
        Object[] input = new Object[]{new String[]{"a", "b", "c"}};

        String json = sanitizer.sanitize(input);

        assertThat(json).contains("a").contains("b").contains("c");
    }
}
