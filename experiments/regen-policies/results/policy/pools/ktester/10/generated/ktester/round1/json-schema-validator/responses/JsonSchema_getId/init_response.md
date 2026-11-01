```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.PathType;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.ValidationContext;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.JsonMetaSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.JsonNodePath;
import com.networknt.schema.AbsoluteIri;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getId_Test {

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    AbsoluteIri absoluteIri;

    private ValidationContext validationContext;
    private ObjectMapper objectMapper;
    private JsonNodePath evaluationPath;

    @BeforeEach
    void setupBeforeEach() {
        // Use the V7 meta‑schema and a default factory for the ValidationContext
        validationContext = new ValidationContext(
                JsonMetaSchema.getV7(),
                JsonSchemaFactory.getInstance(VersionFlag.V7),
                new SchemaValidatorsConfig());

        objectMapper = new ObjectMapper();
        evaluationPath = new JsonNodePath(PathType.SCHEMA);

        // Common stubbing for the SchemaLocation used by JsonSchema
        when(schemaLocation.resolve(anyString())).thenReturn("resolved");
        when(schemaLocation.toString()).thenReturn("schemaLocation");
    }

    @AfterEach
    void teardownAfterEach() {
        // No explicit cleanup required for these tests
    }

    @Test
    void testGetIdWhenIdIsPresent() {
        // Schema node containing a $id property
        ObjectNode schemaNode = objectMapper.createObjectNode();
        schemaNode.put("$id", "http://example.com/schema-id");

        // No fragment – hasNoFragment will be true
        when(schemaLocation.getFragment()).thenReturn(null);
        when(schemaLocation.getAbsoluteIri()).thenReturn(null); // not used when $id is present

        JsonSchema jsonSchema = JsonSchema.from(validationContext, schemaLocation,
                evaluationPath, schemaNode, null, false);

        assertEquals("http://example.com/schema-id", jsonSchema.getId(),
                "The id returned by getId() should be the $id value from the schema");
    }

    @Test
    void testGetIdWhenNoIdAndNoFragment() {
        // Schema node without a $id property
        ObjectNode schemaNode = objectMapper.createObjectNode();

        // No fragment – triggers the “no $id but schema resource” branch
        when(schemaLocation.getFragment()).thenReturn(null);
        // Provide a non‑null absolute IRI – this value becomes the id
        when(schemaLocation.getAbsoluteIri()).thenReturn(absoluteIri);
        when(absoluteIri.toString()).thenReturn("http://example.com/abs");

        JsonSchema jsonSchema = JsonSchema.from(validationContext, schemaLocation,
                evaluationPath, schemaNode, null, false);

        assertEquals("http://example.com/abs", jsonSchema.getId(),
                "When $id is missing and there is no fragment, getId() should return the absolute IRI");
    }

    @Test
    void testGetIdWhenNoIdAndFragmentPresent() {
        // Schema node without a $id property
        ObjectNode schemaNode = objectMapper.createObjectNode();

        // Simulate a non‑empty fragment – hasNoFragment will be false
        JsonNodePath fragmentPath = mock(JsonNodePath.class);
        when(schemaLocation.getFragment()).thenReturn(fragmentPath);
        when(schemaLocation.getAbsoluteIri()).thenReturn(null); // irrelevant in this branch

        JsonSchema jsonSchema = JsonSchema.from(validationContext, schemaLocation,
                evaluationPath, schemaNode, null, false);

        assertNull(jsonSchema.getId(),
                "When $id is missing and a fragment is present, getId() should be null");
    }
}
```