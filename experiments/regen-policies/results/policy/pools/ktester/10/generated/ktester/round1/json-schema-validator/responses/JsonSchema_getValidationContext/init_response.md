```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getValidationContext_Test {

    /** Shared objects that can be reused for all tests */
    private static JsonMetaSchema metaSchema;
    private static JsonSchemaFactory schemaFactory;
    private static SchemaValidatorsConfig config;

    /** Objects that are created per‑test */
    private ValidationContext validationContext;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a concrete meta‑schema (draft‑2019‑09) – this is sufficient for the test.
        metaSchema = JsonMetaSchema.getInstance(SpecVersion.VersionFlag.V201909);
        schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        config = new SchemaValidatorsConfig();
    }

    @BeforeEach
    void setupBeforeEach(@Mock SchemaLocation schemaLocation,
                         @Mock JsonNodePath evaluationPath,
                         @Mock JsonNode schemaNode) {
        /*
         * For the purpose of testing getValidationContext() we only need a
         * ValidationContext instance that is stored inside the JsonSchema.
         * The other constructor arguments can be simple mocks because the
         * method under test does not interact with them.
         */
        validationContext = new ValidationContext(metaSchema, schemaFactory, config);
        // The JsonSchema instance is created via the public static factory method.
        JsonSchema jsonSchema = JsonSchema.from(validationContext,
                                                schemaLocation,
                                                evaluationPath,
                                                schemaNode,
                                                null,               // no parent schema
                                                false);             // do not suppress sub‑schema retrieval
        // Store the created schema for the test method to use.
        this.jsonSchemaUnderTest = jsonSchema;
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to clean up for this simple test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up at the class level.
    }

    /** Instance under test – created in @BeforeEach */
    private JsonSchema jsonSchemaUnderTest;

    @Test
    void getValidationContext_returnsTheSameInstanceProvidedAtConstruction() {
        // Act
        ValidationContext returnedContext = jsonSchemaUnderTest.getValidationContext();

        // Assert – the returned context must be the exact same object that was
        // supplied to the JsonSchema constructor.
        assertSame(validationContext, returnedContext,
                "JsonSchema#getValidationContext should return the ValidationContext used during construction");
    }
}
```