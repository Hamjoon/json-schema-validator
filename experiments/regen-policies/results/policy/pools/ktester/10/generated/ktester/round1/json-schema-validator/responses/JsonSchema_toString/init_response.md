```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_toString_Test {

    @Mock
    ValidationContext validationContext;
    @Mock
    SchemaLocation schemaLocation;
    @Mock
    JsonNodePath evaluationPath;
    @Mock
    JsonNode schemaNode;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Stub the behaviour needed for the toString() method
        // evaluationPath is rendered inside quotes, we control its toString()
        when(evaluationPath.toString()).thenReturn("$");

        // schemaNode is rendered as its JSON representation
        when(schemaNode.toString()).thenReturn("{\"type\":\"string\"}");
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    @Test
    void toString_returnsExpectedRepresentation() {
        // JsonSchema has a package‑private static factory method `from`
        // which is accessible because this test resides in the same package.
        JsonSchema jsonSchema = JsonSchema.from(
                validationContext,
                schemaLocation,
                evaluationPath,
                schemaNode,
                null,               // no parent schema
                false               // suppressSubSchemaRetrieval
        );

        String expected = "\"$\" : {\"type\":\"string\"}";
        assertEquals(expected, jsonSchema.toString(),
                "JsonSchema.toString() should format the evaluation path and schema node correctly");
    }
}
```