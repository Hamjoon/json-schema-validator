```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#getId()}.
 *
 * The tests cover the three branches that initialise the {@code id} field
 * inside the {@link JsonSchema} constructor:
 *
 * 1. The schema node contains a non‑fragment $id.
 * 2. The schema node contains a $id with a fragment (the id must be {@code null}).
 * 3. The schema node does not contain a $id and the {@link SchemaLocation}
 *    has no fragment – the id is derived from the location’s absolute IRI
 *    (or an empty string when the IRI is {@code null}).
 *
 * The {@link JsonSchema} instances are created via the package‑private
 * {@code JsonSchema.from(...)} factory method so that the private constructor
 * can be exercised without using reflection.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getId_Test {

    @Mock JsonMetaSchema metaSchema;
    @Mock JsonSchemaFactory schemaFactory;
    @Mock SchemaValidatorsConfig config;

    ValidationContext validationContext;
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;
    @Mock JsonNode schemaNode;

    @BeforeEach
    void setupBeforeEach() {
        // a real ValidationContext with a real (empty) map – we will spy it
        validationContext = new ValidationContext(metaSchema, schemaFactory, new SchemaValidatorsConfig(),
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>());
        validationContext = spy(validationContext);

        // default stubbing – most tests override these
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(metaSchema.readAnchor(any())).thenReturn(null);
        when(metaSchema.readDynamicAnchor(any())).thenReturn(null);
        when(schemaLocation.getFragment()).thenReturn(null);
        when(schemaLocation.getAbsoluteIri()).thenReturn(null);
        when(schemaLocation.toString()).thenReturn("mockLocation");
    }

    @Test
    void getId_returnsIdFromSchemaNodeWhenIdHasNoFragment() {
        // $id present and does NOT contain a fragment
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn("http://example.com/my-schema");

        JsonSchema schema = JsonSchema.from(validationContext, schemaLocation, evaluationPath,
                schemaNode, null, false);

        assertEquals("http://example.com/my-schema", schema.getId(),
                "The id should be the $id value when it has no fragment");
    }

    @Test
    void getId_isNullWhenSchemaNodeIdContainsFragment() {
        // $id contains a fragment – the constructor should set id to null
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn("http://example.com/my-schema#frag");
        // when a fragment is present the constructor calls schemaLocation.resolve(id)
        // we do not need the real result – just ensure resolve() returns a non‑null location
        when(schemaLocation.resolve(anyString())).thenReturn(schemaLocation);

        JsonSchema schema = JsonSchema.from(validationContext, schemaLocation, evaluationPath,
                schemaNode, null, false);

        assertNull(schema.getId(),
                "The id must be null when the $id contains a fragment (anchor)");
    }

    @Test
    void getId_derivedFromSchemaLocationWhenNoIdInSchemaNode() {
        // No $id in the schema node
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn(null);
        // schemaLocation has no fragment (already stubbed) and its absolute IRI is null
        // therefore the constructor should set id to the empty string
        when(schemaLocation.getAbsoluteIri()).thenReturn(null);

        JsonSchema schema = JsonSchema.from(validationContext, schemaLocation, evaluationPath,
                schemaNode, null, false);

        assertEquals("", schema.getId(),
                "When there is no $id and no absolute IRI, the id should be the empty string");
    }
}
```