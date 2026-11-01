```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#getValidationContext()}.
 *
 * The test creates a {@link JsonSchema} instance via the package‑private
 * {@code JsonSchema.from(...)} factory method, providing a mocked
 * {@link ValidationContext}. The expectation is that {@code getValidationContext()}
 * returns exactly the same instance that was supplied to the constructor.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getValidationContext_Test {

    @Mock
    private ValidationContext mockValidationContext;
    @Mock
    private JsonMetaSchema mockMetaSchema;
    @Mock
    private JsonSchemaFactory mockFactory;
    @Mock
    private JsonNode mockSchemaNode;
    @Mock
    private JsonNodePath mockEvaluationPath;

    private JsonSchema jsonSchema;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Stub the ValidationContext behaviour that is exercised by the JsonSchema constructor.
        when(mockValidationContext.getMetaSchema()).thenReturn(mockMetaSchema);
        when(mockValidationContext.getJsonSchemaFactory()).thenReturn(mockFactory);
        when(mockValidationContext.getSchemaResources()).thenReturn(new ConcurrentHashMap<>());
        when(mockValidationContext.getDynamicAnchors()).thenReturn(new ConcurrentHashMap<>());
        when(mockValidationContext.resolveSchemaId(any())).thenReturn(null);

        // The meta‑schema is queried for anchors; we return null to keep the flow simple.
        when(mockMetaSchema.readAnchor(any())).thenReturn(null);
        when(mockMetaSchema.readDynamicAnchor(any())).thenReturn(null);

        // A minimal SchemaLocation – both arguments may be null for the purpose of this test.
        SchemaLocation schemaLocation = new SchemaLocation(null, null);

        // Build the JsonSchema instance using the package‑private factory method.
        jsonSchema = JsonSchema.from(
                mockValidationContext,
                schemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                null,               // no parent schema
                false               // suppressSubSchemaRetrieval
        );
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; Mockito handles mock lifecycle.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    @Test
    void getValidationContext_returnsSameInstance() {
        ValidationContext returned = jsonSchema.getValidationContext();
        assertSame(mockValidationContext, returned,
                "JsonSchema#getValidationContext should return the ValidationContext supplied at construction");
    }

    @Test
    void getValidationContext_multipleCallsReturnSameInstance() {
        ValidationContext first = jsonSchema.getValidationContext();
        ValidationContext second = jsonSchema.getValidationContext();
        assertSame(first, second,
                "Multiple invocations of getValidationContext should return the identical instance");
    }
}
```