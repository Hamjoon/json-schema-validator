```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validateAndCollect_Test {

    /** Shared objects for the whole test class */
    private static ObjectMapper mapper;

    /** Objects created for each test */
    private ExecutionContext executionContext;
    private JsonSchema jsonSchema;

    /** Mocks required by the JsonSchema constructor */
    @Mock
    private ValidationContext validationContext;          // mock of ValidationContext
    @Mock
    private JsonMetaSchema metaSchema;                    // returned by validationContext.getMetaSchema()
    @Mock
    private SchemaValidatorsConfig config;                // returned by validationContext.getConfig()

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // ---------- 1. Prepare ExecutionContext ----------
        executionContext = new ExecutionContext(); // simple ctor, uses default CollectorContext

        // ---------- 2. Prepare a minimal schema (empty object) ----------
        ObjectNode schemaNode = mapper.createObjectNode(); // "{}"

        // ---------- 3. Stub the ValidationContext behaviour ----------
        // getMetaSchema() is used inside the JsonSchema constructor
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        // getConfig() is optional – we let it return null to avoid keywordWalkListenerRunner creation
        when(validationContext.getConfig()).thenReturn(null);

        // ---------- 4. Create the JsonSchema instance ----------
        // The static factory method is package‑private, but the test lives in the same package,
        // so we can call it directly.
        URI dummyUri = URI.create("http://example.com/dummy-schema");
        String schemaPath = "#";
        jsonSchema = JsonSchema.from(validationContext,
                                     schemaPath,
                                     dummyUri,
                                     schemaNode,
                                     null,               // no parent schema
                                     false);             // do not suppress sub‑schema retrieval
        assertNotNull(jsonSchema, "JsonSchema instance should be created");
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to release for this simple test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    /** 
     * Basic sanity test: validateAndCollect should return a non‑null ValidationResult
     * when invoked with a simple ExecutionContext and a JSON instance.
     */
    @Test
    void testValidateAndCollectReturnsResult() throws Exception {
        // Arrange – a trivial JSON document to validate
        JsonNode dataNode = mapper.readTree("{\"foo\":\"bar\"}");

        // Act
        ValidationResult result = jsonSchema.validateAndCollect(executionContext, dataNode);

        // Assert
        assertNotNull(result, "validateAndCollect should never return null");
        // The result may contain no messages for an empty schema; we just ensure the call succeeds.
        assertTrue(result.getMessages().isEmpty() ||
                   result.getMessages().stream().allMatch(m -> m != null),
                   "All validation messages, if any, should be non‑null");
    }

    /**
     * Verify that passing a {@code null} JSON node triggers a {@link NullPointerException}
     * (the current implementation of JsonSchema.validateAndCollect does not guard against it).
     */
    @Test
    void testValidateAndCollectWithNullNode() {
        // Act & Assert
        assertThrows(NullPointerException.class,
                     () -> jsonSchema.validateAndCollect(executionContext, null),
                     "validateAndCollect should throw NPE when the JSON node is null");
    }
}
```