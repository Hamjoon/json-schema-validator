```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.jackson.JacksonUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchema#getRequiredValidator()}.
 *
 * <p>The tests cover the following scenarios:
 *
 * <ul>
 *   <li>Default state – no {@code "required"} keyword → {@code null}</li>
 *   <li>Schema contains a {@code "required"} keyword → non‑null validator</li>
 *   <li>Schema without {@code "required"} but with other keywords → {@code null}</li>
 *   <li>Repeated calls return the same validator instance</li>
 *   <li>Explicit empty {@code "required"} array still creates a validator</li>
 *   <li>$ref handling – only the resolved schema gets a validator</li>
 * </ul>
 *
 * The test class lives in the same package as {@link JsonSchema} so that the
 * package‑private static factory method {@code JsonSchema.from(...)} can be used.
 */
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
     * Helper that creates a {@link JsonSchema} from a JSON string and
     * triggers validator initialisation.
     */
    private JsonSchema createSchema(String json) {
        try {
            JsonNode node = JsonMapperFactory.getMapper().readTree(json);
            JsonSchema schema = JsonSchema.from(
                    validationContext,
                    schemaLocation,
                    evaluationPath,
                    node,
                    null,               // no parent schema
                    false               // do not suppress sub‑schema retrieval
            );
            // The factory method does not automatically initialise validators.
            schema.initializeValidators();
            return schema;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The {@code requiredValidator} field of {@link JsonSchema} is initialised only
     * when the schema contains a {@code "required"} keyword.  With an empty schema
     * the field stays {@code null}.  This test verifies that {@code getRequiredValidator()}
     * correctly returns {@code null} in that situation.
     */
    @Test
    void getRequiredValidator_returnsNullWhenSchemaHasNoRequiredKeyword() {
        // GIVEN – a JsonSchema created from an empty schema definition
        JsonSchema schema = JsonSchema.from(
                validationContext,
                schemaLocation,
                evaluationPath,
                emptySchemaNode,
                null,
                false
        );
        // No explicit call to initialiseValidators – the empty schema does not need it.

        // WHEN – the accessor is invoked
        JsonValidator validator = schema.getRequiredValidator();

        // THEN – the result must be null because the schema does not define any required properties
        assertNull(validator,
                "Expected requiredValidator to be null for a schema without a \"required\" keyword");
    }

    /**
     * A schema that defines a required property list should cause the JsonSchema
     * instance to create and store a RequiredValidator.
     */
    @Test
    void getRequiredValidator_returnsNonNullWhenRequiredKeywordPresent() {
        // GIVEN – a schema with a non‑empty required array
        String schemaJson = "{ \"type\": \"object\", \"required\": [\"name\"] }";
        JsonSchema schema = createSchema(schemaJson);

        // WHEN
        JsonValidator validator = schema.getRequiredValidator();

        // THEN
        assertNotNull(validator, "RequiredValidator should be created when \"required\" keyword is present");
        assertTrue(validator instanceof RequiredValidator,
                "Validator instance should be of type RequiredValidator");
    }

    /**
     * A schema that does not contain a {@code "required"} keyword (even if it has
     * other validators such as a type validator) must leave the requiredValidator
     * field unset.
     */
    @Test
    void getRequiredValidator_returnsNullWhenRequiredKeywordAbsentButOtherKeywordsPresent() {
        // GIVEN – a schema with a type keyword but no required keyword
        String schemaJson = "{ \"type\": \"object\", \"properties\": { \"age\": { \"type\": \"integer\" } } }";
        JsonSchema schema = createSchema(schemaJson);

        // WHEN
        JsonValidator validator = schema.getRequiredValidator();

        // THEN
        assertNull(validator,
                "RequiredValidator must remain null when schema lacks a \"required\" keyword");
    }

    /**
     * Calling {@code getRequiredValidator()} multiple times on the same
     * {@link JsonSchema} must return the exact same {@link RequiredValidator}
     * instance (i.e., the method does not create a new instance on each call).
     */
    @Test
    void getRequiredValidator_returnsSameInstanceOnRepeatedCalls() {
        // GIVEN
        String schemaJson = "{ \"type\": \"object\", \"required\": [\"id\"] }";
        JsonSchema schema = createSchema(schemaJson);

        // WHEN
        JsonValidator firstCall = schema.getRequiredValidator();
        JsonValidator secondCall = schema.getRequiredValidator();

        // THEN
        assertNotNull(firstCall, "First call should return a non‑null validator");
        assertSame(firstCall, secondCall,
                "Repeated calls must return the same RequiredValidator instance");
    }

    /**
     * An explicit but empty {@code "required"} array still triggers the creation
     * of a {@link RequiredValidator} (it will simply validate that no properties are required).
     */
    @Test
    void getRequiredValidator_returnsNonNullWhenRequiredArrayIsEmpty() {
        // GIVEN
        String schemaJson = "{ \"type\": \"object\", \"required\": [] }";
        JsonSchema schema = createSchema(schemaJson);

        // WHEN
        JsonValidator validator = schema.getRequiredValidator();

        // THEN
        assertNotNull(validator,
                "RequiredValidator should be created even when the \"required\" array is empty");
        assertTrue(validator instanceof RequiredValidator,
                "Validator should be of type RequiredValidator");
    }

    /**
     * When a schema contains a {@code $ref} that points to another schema which defines
     * required properties, the {@link RequiredValidator} is created only for the resolved
     * (referenced) {@link JsonSchema} instance. The schema that only holds the $ref
     * must not have a {@code requiredValidator} of its own.
     */
    @Test
    void getRequiredValidator_refHandlingCreatesValidatorOnlyOnResolvedSchema() {
        // GIVEN – a root schema that only contains a $ref and a components section
        String rootSchemaJson = """
                {
                  "$ref": "#/components/schemas/RefSchema",
                  "components": {
                    "schemas": {
                      "RefSchema": {
                        "type": "object",
                        "required": ["code"]
                      }
                    }
                  }
                }
                """;

        JsonSchema rootSchema = createSchema(rootSchemaJson);

        // The root schema itself must not have a requiredValidator
        assertNull(rootSchema.getRequiredValidator(),
                "Root schema that only contains $ref should have null requiredValidator");

        // Resolve the referenced schema – the library stores it under the path
        // "components/schemas/RefSchema".  Using JsonNodePath.of() creates the
        // appropriate path object.
        JsonSchema referenced = rootSchema.getSubSchema(JsonNodePath.of("components/schemas/RefSchema"));
        assertNotNull(referenced,
                "Referenced schema should be reachable via getSubSchema with the correct path");

        // The referenced schema must have a non‑null RequiredValidator
        JsonValidator refValidator = referenced.getRequiredValidator();
        assertNotNull(refValidator,
                "Referenced schema that defines \"required\" must have a non‑null RequiredValidator");
        assertTrue(refValidator instanceof RequiredValidator,
                "Validator of the referenced schema should be of type RequiredValidator");
    }
}
```