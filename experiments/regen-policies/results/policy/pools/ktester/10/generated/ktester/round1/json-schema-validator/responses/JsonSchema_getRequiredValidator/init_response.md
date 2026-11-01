**Generated test class**

```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getRequiredValidator_Test {

    /*** Mocks needed for constructing a {@link JsonSchema} instance ***/
    @Mock
    private JsonMetaSchema metaSchema;               // required by ValidationContext
    @Mock
    private JsonSchemaFactory jsonSchemaFactory;     // required by ValidationContext
    @Mock
    private SchemaLocation schemaLocation;           // required by JsonSchema.from(...)
    @Mock
    private JsonNodePath evaluationPath;             // required by JsonSchema.from(...)

    private ValidationContext validationContext;
    private JsonNode emptySchemaNode;                 // a minimal schema (no “required” keyword)

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a very small ValidationContext – the internal maps can be empty.
        validationContext = new ValidationContext(
                metaSchema,
                jsonSchemaFactory,
                new SchemaValidatorsConfig()
        );

        // An empty JSON object node represents a schema without any keywords.
        emptySchemaNode = JsonMapperFactory.getMapper().createObjectNode();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /**
     * The {@code requiredValidator} field of {@link JsonSchema} is initialised only
     * when the schema contains a {@code "required"} keyword.  With an empty schema
     * the field stays {@code null}.  This test verifies that {@code getRequiredValidator()}
     * correctly returns {@code null} in that situation.
     */
    @Test
    void getRequiredValidator_returnsNullWhenSchemaHasNoRequiredKeyword() {
        // --------------------------------------------------------------------
        // GIVEN – a JsonSchema created from an empty schema definition
        // --------------------------------------------------------------------
        JsonSchema schema = JsonSchema.from(
                validationContext,
                schemaLocation,
                evaluationPath,
                emptySchemaNode,
                null,               // no parent schema
                false               // do not suppress sub‑schema retrieval
        );

        // --------------------------------------------------------------------
        // WHEN – the accessor is invoked
        // --------------------------------------------------------------------
        JsonValidator validator = schema.getRequiredValidator();

        // --------------------------------------------------------------------
        // THEN – the result must be null because the schema does not define
        //        any required properties
        // --------------------------------------------------------------------
        assertNull(validator, "Expected requiredValidator to be null for a schema without a \"required\" keyword");
    }
}
```