```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

class JsonSchema_getTypeValidator_Test {

    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the 2020‑12 draft – any draft works for these simple tests.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, builder -> {
            // No special configuration required for these tests.
        });
    }

    /**
     * Sets a private field via reflection.
     */
    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * Retrieves a private field via reflection.
     */
    private static Object getField(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    @Test
    void getTypeValidator_returnsNonNullWhenTypeKeywordPresent() {
        // a minimal schema that defines a type
        String schemaJson = "{ \"type\": \"object\" }";
        JsonSchema jsonSchema = factory.getSchema(schemaJson);
        // The method under test – it lazily loads validators if needed.
        TypeValidator typeValidator = jsonSchema.getTypeValidator();
        assertNotNull(typeValidator,
                "TypeValidator should be non‑null when a \"type\" keyword is present");
    }

    @Test
    void getTypeValidator_returnsNullWhenNoTypeKeyword() {
        // schema without a "type" keyword
        String schemaJson = "{}";
        JsonSchema jsonSchema = factory.getSchema(schemaJson);
        TypeValidator typeValidator = jsonSchema.getTypeValidator();
        assertNull(typeValidator,
                "TypeValidator should be null when no \"type\" keyword is defined");
    }

    @Test
    void existingValidatorsListButNullTypeValidatorReturnsNull() throws Exception {
        JsonSchema jsonSchema = factory.getSchema("{\"type\":\"boolean\"}");
        // Force lazy loading to ensure validators list is created, then discard the TypeValidator.
        jsonSchema.getTypeValidator();
        // Now replace the fields: keep validators, null out typeValidator.
        List<JsonValidator> validators = (List<JsonValidator>) getField(jsonSchema, "validators");
        setField(jsonSchema, "validators", validators);
        setField(jsonSchema, "typeValidator", null);
        // Call getTypeValidator – should return null and must NOT re‑initialize validators.
        TypeValidator tv = jsonSchema.getTypeValidator();
        assertNull(tv, "When typeValidator is null, getTypeValidator must return null");
    }
}
```