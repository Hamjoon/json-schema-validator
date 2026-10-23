```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link JsonSchema#hasRequiredValidator()}.
 *
 * <p>The test suite is organised in two groups:</p>
 *
 * <ul>
 *   <li><b>hasRequiredValidator</b> – verifies the behaviour of the method when the
 *   internal {@code requiredValidator} field is manipulated directly (via reflection)
 *   or left untouched.</li>
 *   <li><b>hasRequiredValidator_initialization</b> – exercises the full
 *   initialisation path (calling {@link JsonSchema#initializeValidators()}) with
 *   various schema definitions, parent/child relationships and a reset‑via‑reflection
 *   scenario.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_hasRequiredValidator_Test {

    /** Shared objects for the whole test suite */
    private static ObjectMapper mapper;

    /** Objects created for each test */
    private ValidationContext validationContext;

    @BeforeAll
    static void setupBeforeAll() {
        // Jackson mapper is cheap and thread‑safe – initialise once.
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        /*
         * Build a minimal but functional ValidationContext.  All the
         * arguments have sensible defaults that are sufficient for the
         * validator initialisation performed in the tests.
         */
        URIFactory uriFactory = new URIFactory();               // default implementation
        URNFactory urnFactory = new URNFactory();               // default implementation
        JsonMetaSchema metaSchema = JsonMetaSchema.getV7();     // a concrete meta‑schema
        JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(); // default factory
        SchemaValidatorsConfig config = new SchemaValidatorsConfig(); // empty config

        validationContext = new ValidationContext(uriFactory, urnFactory,
                metaSchema, schemaFactory, config);
    }

    /* --------------------------------------------------------------
     * Helper utilities
     * -------------------------------------------------------------- */

    /** Sets the private {@code requiredValidator} field via reflection. */
    private static void setRequiredValidator(JsonSchema schema, JsonValidator validator) throws Exception {
        Field field = JsonSchema.class.getDeclaredField("requiredValidator");
        field.setAccessible(true);
        field.set(schema, validator);
    }

    /** Returns the current value of the private {@code requiredValidator} field (for debugging). */
    @SuppressWarnings("unused")
    private static JsonValidator getRequiredValidator(JsonSchema schema) throws Exception {
        Field field = JsonSchema.class.getDeclaredField("requiredValidator");
        field.setAccessible(true);
        return (JsonValidator) field.get(schema);
    }

    /* --------------------------------------------------------------
     * Group: hasRequiredValidator
     * -------------------------------------------------------------- */

    /**
     * Covers the two basic scenarios described in the original test‑case JSON:
     *
     * <ul>
     *   <li>A newly created {@link JsonSchema} instance – no validator has been assigned
     *       yet, so the method must return {@code false}.</li>
     *   <li>After injecting a non‑null {@link JsonValidator} (a Mockito mock) via reflection,
     *       the method must recognise the presence of the validator and return {@code true}.</li>
     * </ul>
     */
    @ParameterizedTest(name = "{index} ⇒ {2}")
    @MethodSource("provideHasRequiredValidatorDirectManipulation")
    void hasRequiredValidator_directManipulation(String description,
                                                JsonSchema schema,
                                                boolean expected) throws Exception {
        assertEquals(expected, schema.hasRequiredValidator(),
                "hasRequiredValidator() did not match expectation: " + description);
    }

    private static Stream<Arguments> provideHasRequiredValidatorDirectManipulation() throws Exception {
        // 1️⃣  Fresh schema – no required validator.
        JsonSchema freshSchema = JsonSchema.from(
                createValidationContext(),
                "fresh",
                new URI("urn:test:fresh"),
                // an empty schema node
                new ObjectMapper().readTree("{}"),
                null,
                false);
        // Do NOT call initialiseValidators(); we want the field to stay null.

        // 2️⃣  Schema where we inject a mock validator.
        JsonSchema injectedSchema = JsonSchema.from(
                createValidationContext(),
                "injected",
                new URI("urn:test:injected"),
                new ObjectMapper().readTree("{}"),
                null,
                false);
        JsonValidator mockValidator = mock(JsonValidator.class);
        setRequiredValidator(injectedSchema, mockValidator);

        return Stream.of(
                Arguments.of("Fresh instance – no required validator assigned",
                        freshSchema, false),
                Arguments.of("After injecting a non‑null JsonValidator via reflection",
                        injectedSchema, true)
        );
    }

    /* --------------------------------------------------------------
     * Group: hasRequiredValidator_initialization
     * -------------------------------------------------------------- */

    /**
     * Parameterised test that exercises {@link JsonSchema#initializeValidators()}
     * with a variety of schema definitions, including parent/child relationships
     * and a reset‑via‑reflection scenario.
     */
    @ParameterizedTest(name = "{index} ⇒ {4}")
    @MethodSource("provideInitializationScenarios")
    void hasRequiredValidator_initialization(String description,
                                             String parentSchemaJson,
                                             String schemaJson,
                                             boolean resetRequiredValidator,
                                             boolean expected) throws Exception {
        // --------------------------------------------------------------
        // 1️⃣  Build the *parent* schema if one is supplied.
        // --------------------------------------------------------------
        JsonSchema parent = null;
        if (parentSchemaJson != null) {
            JsonNode parentNode = mapper.readTree(parentSchemaJson);
            parent = JsonSchema.from(validationContext,
                    "parent",
                    new URI("urn:test:parent"),
                    parentNode,
                    null,
                    false);
            parent.initializeValidators(); // ensure any parent validator is instantiated
        }

        // --------------------------------------------------------------
        // 2️⃣  Build the *target* schema (either root or child).
        // --------------------------------------------------------------
        JsonNode schemaNode = mapper.readTree(schemaJson);
        JsonSchema target;
        if (parent != null) {
            // Child schema – use the public factory method.
            target = parent.createChildSchema("child", schemaNode);
        } else {
            target = JsonSchema.from(validationContext,
                    "root",
                    new URI("urn:test:root"),
                    schemaNode,
                    null,
                    false);
        }
        target.initializeValidators();

        // --------------------------------------------------------------
        // 3️⃣  Optionally reset the requiredValidator field via reflection.
        // --------------------------------------------------------------
        if (resetRequiredValidator) {
            setRequiredValidator(target, null);
        }

        // --------------------------------------------------------------
        // 4️⃣  Assertion.
        // --------------------------------------------------------------
        assertEquals(expected, target.hasRequiredValidator(),
                "Unexpected hasRequiredValidator() result for scenario: " + description);
    }

    private static Stream<Arguments> provideInitializationScenarios() {
        return Stream.of(
                // 1️⃣ object with required → true
                Arguments.of(
                        "Object schema with a non‑empty \"required\" array creates RequiredValidator",
                        null,
                        "{\"type\":\"object\",\"required\":[\"name\"]}",
                        false,
                        true),

                // 2️⃣ object without required → false
                Arguments.of(
                        "Object schema without \"required\" keyword must not create RequiredValidator",
                        null,
                        "{\"type\":\"object\"}",
                        false,
                        false),

                // 3️⃣ non‑object type with required → false
                Arguments.of(
                        "\"required\" on a non‑object type (string) is ignored; no RequiredValidator",
                        null,
                        "{\"type\":\"string\",\"required\":[\"len\"]}",
                        false,
                        false),

                // 4️⃣ object with required then reset → false
                Arguments.of(
                        "After creating a RequiredValidator, resetting the field via reflection should return false",
                        null,
                        "{\"type\":\"object\",\"required\":[\"id\"]}",
                        true, // resetRequiredValidator
                        false),

                // 5️⃣ child schema without its own required (parent has) → false
                Arguments.of(
                        "Child schema without its own \"required\" must not inherit parent’s RequiredValidator",
                        "{\"type\":\"object\",\"required\":[\"parentProp\"]}",
                        "{\"type\":\"object\"}",
                        false,
                        false),

                // 6️⃣ child schema with its own required (parent also has) → true
                Arguments.of(
                        "Both parent and child define \"required\"; child gets its own RequiredValidator",
                        "{\"type\":\"object\",\"required\":[\"parentProp\"]}",
                        "{\"type\":\"object\",\"required\":[\"childProp\"]}",
                        false,
                        true)
        );
    }

    /* --------------------------------------------------------------
     * Utility to create a fresh ValidationContext for static method sources.
     * -------------------------------------------------------------- */
    private static ValidationContext createValidationContext() {
        URIFactory uriFactory = new URIFactory();
        URNFactory urnFactory = new URNFactory();
        JsonMetaSchema metaSchema = JsonMetaSchema.getV7();
        JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance();
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        return new ValidationContext(uriFactory, urnFactory, metaSchema, schemaFactory, config);
    }
}
```