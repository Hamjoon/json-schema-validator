```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Unit tests for {@link JsonSchema#getValidationContext()}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getValidationContext_Test {

    @Mock
    private ValidationContext validationContextMock;

    @Mock
    private JsonMetaSchema metaSchemaMock;

    @Mock
    private JsonNode schemaNodeMock;

    @Mock
    private JsonNodePath evaluationPathMock;

    private JsonSchema jsonSchema;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // ----- stub behaviour of ValidationContext and its collaborators -----
        // meta‑schema related calls
        when(validationContextMock.getMetaSchema()).thenReturn(metaSchemaMock);
        when(metaSchemaMock.readAnchor(any())).thenReturn(null);
        when(metaSchemaMock.readDynamicAnchor(any())).thenReturn(null);

        // map getters – use real concurrent maps to avoid NPEs on putIfAbsent
        when(validationContextMock.getSchemaResources()).thenReturn(new ConcurrentHashMap<>());
        when(validationContextMock.getDynamicAnchors()).thenReturn(new ConcurrentHashMap<>());
        when(validationContextMock.getSchemaReferences()).thenReturn(new ConcurrentHashMap<>());

        // other methods used in the constructor
        when(validationContextMock.resolveSchemaId(any())).thenReturn(null);

        // ----- create a JsonSchema instance via the package‑private factory method -----
        // SchemaLocation can be constructed with null arguments (the constructor does not enforce non‑null)
        SchemaLocation schemaLocation = new SchemaLocation(null, null);

        jsonSchema = JsonSchema.from(
                validationContextMock,
                schemaLocation,
                evaluationPathMock,
                schemaNodeMock,
                null,               // parent schema – none for this test
                false               // suppressSubSchemaRetrieval
        );
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
    void getValidationContext_returnsSameInstance() {
        ValidationContext returned = jsonSchema.getValidationContext();
        assertSame(validationContextMock, returned,
                "JsonSchema#getValidationContext should return the ValidationContext supplied at construction");
    }

    @Test
    void getValidationContext_multipleCalls_returnSameInstance() {
        ValidationContext first = jsonSchema.getValidationContext();
        ValidationContext second = jsonSchema.getValidationContext();
        assertSame(first, second,
                "Multiple invocations of getValidationContext should return the identical instance");
    }
}
```