```java
package com.networknt.schema;

import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_isRecursiveAnchor_Test {

    private static JsonSchemaFactory factory;
    private static SchemaValidatorsConfig config;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a factory for Draft‑2020‑12 which supports the $recursiveAnchor keyword
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        // Use the default validator configuration
        config = SchemaValidatorsConfig.builder().build();
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required at the moment
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required at the moment
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources
        factory = null;
        config = null;
    }

    @Test
    void isRecursiveAnchor_true_whenKeywordPresent() throws Exception {
        // given: a schema that defines $recursiveAnchor:true
        String schemaData = "{\n"
                + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n"
                + "  \"$id\": \"http://example.org/recursive-schema.json\",\n"
                + "  \"$recursiveAnchor\": true,\n"
                + "  \"type\": \"object\",\n"
                + "  \"properties\": {\n"
                + "    \"name\": { \"type\": \"string\" }\n"
                + "  }\n"
                + "}";

        JsonSchema schema = factory.getSchema(schemaData, config);

        // when
        boolean result = schema.isRecursiveAnchor();

        // then
        assertTrue(result, "Expected isRecursiveAnchor() to return true when $recursiveAnchor is present and true");
    }

    @Test
    void isRecursiveAnchor_false_whenKeywordAbsent() throws Exception {
        // given: a schema without the $recursiveAnchor keyword
        String schemaData = "{\n"
                + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n"
                + "  \"$id\": \"http://example.org/non-recursive-schema.json\",\n"
                + "  \"type\": \"object\",\n"
                + "  \"properties\": {\n"
                + "    \"name\": { \"type\": \"string\" }\n"
                + "  }\n"
                + "}";

        JsonSchema schema = factory.getSchema(schemaData, config);

        // when
        boolean result = schema.isRecursiveAnchor();

        // then
        assertFalse(result, "Expected isRecursiveAnchor() to return false when $recursiveAnchor is not defined");
    }
}
```