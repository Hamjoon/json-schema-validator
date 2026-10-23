```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TypeValidator#getSchemaType()}.
 *
 * The tests verify that the {@code schemaType} field is correctly derived from the
 * supplied schema node when a {@link TypeValidator} instance is created.
 *
 * No private members are accessed; only the public constructor and the getter
 * under test are used.
 */
@ExtendWith(MockitoExtension.class)
class TypeValidator_getSchemaType_Test {

    private ValidationContext validationContext;
    private JsonSchema parentSchema;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a minimal but valid ValidationContext.
        URIFactory uriFactory = org.mockito.Mockito.mock(URIFactory.class);
        URNFactory urnFactory = org.mockito.Mockito.mock(URNFactory.class);
        JsonMetaSchema metaSchema = org.mockito.Mockito.mock(JsonMetaSchema.class);
        JsonSchemaFactory jsonSchemaFactory = org.mockito.Mockito.mock(JsonSchemaFactory.class);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig(); // default configuration

        validationContext = new ValidationContext(uriFactory, urnFactory, metaSchema,
                jsonSchemaFactory, config);

        // Parent schema is not used by getSchemaType(), so a mock is sufficient.
        parentSchema = org.mockito.Mockito.mock(JsonSchema.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void getSchemaType_returnsStringWhenSchemaSpecifiesString() {
        // GIVEN a schema node that defines type "string"
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.put("type", "string");

        // WHEN a TypeValidator is constructed
        TypeValidator validator = new TypeValidator("testPath", schemaNode, parentSchema, validationContext);

        // THEN the schema type reported by the validator matches JsonType.STRING
        assertEquals(JsonType.STRING, validator.getSchemaType(),
                "Expected schema type to be STRING for a schema node with type \"string\"");
    }

    @Test
    void getSchemaType_returnsIntegerWhenSchemaSpecifiesInteger() {
        // GIVEN a schema node that defines type "integer"
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.put("type", "integer");

        // WHEN a TypeValidator is constructed
        TypeValidator validator = new TypeValidator("testPath", schemaNode, parentSchema, validationContext);

        // THEN the schema type reported by the validator matches JsonType.INTEGER
        assertEquals(JsonType.INTEGER, validator.getSchemaType(),
                "Expected schema type to be INTEGER for a schema node with type \"integer\"");
    }

    @Test
    void getSchemaType_returnsUnionWhenSchemaSpecifiesArrayOfTypes() {
        // GIVEN a schema node that defines a union type ["string","null"]
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.putArray("type")
                .add("string")
                .add("null");

        // WHEN a TypeValidator is constructed
        TypeValidator validator = new TypeValidator("testPath", schemaNode, parentSchema, validationContext);

        // THEN the validator reports UNION as the schema type
        assertEquals(JsonType.UNION, validator.getSchemaType(),
                "Expected schema type to be UNION for a schema node with an array of types");
    }

    @Test
    void getSchemaType_returnsNullWhenSchemaNodeHasNoTypeKeyword() {
        // GIVEN a schema node without a "type" keyword
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode(); // empty object

        // WHEN a TypeValidator is constructed
        TypeValidator validator = new TypeValidator("testPath", schemaNode, parentSchema, validationContext);

        // THEN the schema type is null (the TypeValidator does not set a type)
        assertNull(validator.getSchemaType(),
                "Expected schema type to be null when the schema node does not contain a \"type\" keyword");
    }
}
```