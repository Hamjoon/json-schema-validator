```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_hasTypeValidator_Test {

    private static ObjectMapper mapper;

    @Mock
    ValidationContext validationContext;          // mocked validation context
    @Mock
    JsonMetaSchema metaSchema;                    // mocked meta‑schema (returned by the context)

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // The JsonSchema constructor accesses validationContext.getMetaSchema()
        // and validationContext.getConfig(). We stub the needed calls here.
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getConfig()).thenReturn(null); // config is optional for this test
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to clean up for these unit tests
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release
    }

    @Test
    void hasTypeValidator_returnsTrueWhenSchemaDefinesType() throws Exception {
        // GIVEN – a schema node that contains a top‑level "type" keyword
        JsonNode schemaNode = mapper.readTree("{\"type\":\"string\"}");

        // WHEN – a JsonSchema instance is created and validators are initialised
        JsonSchema jsonSchema = JsonSchema.from(
                validationContext,
                "#",                                   // schemaPath
                URI.create("http://example.com/schema"), // currentUri
                schemaNode,
                null,                                 // parent
                false                                 // suppressSubSchemaRetrieval
        );
        jsonSchema.initializeValidators();

        // THEN – the type validator should be present
        assertTrue(jsonSchema.hasTypeValidator(),
                "hasTypeValidator() should return true when a 'type' keyword is defined");
    }

    @Test
    void hasTypeValidator_returnsFalseWhenSchemaDoesNotDefineType() throws Exception {
        // GIVEN – a schema node without a top‑level "type" keyword
        JsonNode schemaNode = mapper.readTree("{\"properties\":{\"name\":{\"type\":\"string\"}}}");

        // WHEN – a JsonSchema instance is created and validators are initialised
        JsonSchema jsonSchema = JsonSchema.from(
                validationContext,
                "#",
                URI.create("http://example.com/schema"),
                schemaNode,
                null,
                false
        );
        jsonSchema.initializeValidators();

        // THEN – the type validator should be absent
        assertFalse(jsonSchema.hasTypeValidator(),
                "hasTypeValidator() should return false when no top‑level 'type' keyword exists");
    }
}
```