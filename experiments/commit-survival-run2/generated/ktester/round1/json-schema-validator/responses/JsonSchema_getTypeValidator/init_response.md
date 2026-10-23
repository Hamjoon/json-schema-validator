```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link JsonSchema#getTypeValidator()}.
 *
 * The tests verify that:
 * <ul>
 *   <li>When a {@code JsonSchema} instance is created the {@code typeValidator}
 *       field is {@code null} (i.e. {@code hasTypeValidator()} returns {@code false}).</li>
 *   <li>After invoking {@code initializeValidators()} the field is populated
 *       (i.e. {@code hasTypeValidator()} returns {@code true}) and the getter
 *       returns a non‑null {@link TypeValidator} instance.</li>
 * </ul>
 *
 * The {@link JsonSchema} constructor is private, therefore the package‑private
 * static factory method {@code JsonSchema.from(...)} is used to create the
 * instance. All external collaborators are mocked so that the test focuses
 * solely on the behaviour of {@code getTypeValidator()}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getTypeValidator_Test {

    /** Shared mapper for building {@link JsonNode} objects from JSON strings. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** A simple schema containing a {@code type} keyword – this triggers creation of a {@link TypeValidator}. */
    private static final String SCHEMA_WITH_TYPE = "{ \"type\": \"string\" }";

    /** Mocked {@link ValidationContext} used for all test instances. */
    @Mock
    private ValidationContext validationContext;

    /** The {@link JsonSchema} instance under test. */
    private JsonSchema jsonSchema;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // --------------------------------------------------------------------
        // 1️⃣  Prepare the mocked ValidationContext
        // --------------------------------------------------------------------
        // The constructor of JsonSchema accesses getMetaSchema() and getConfig().
        // For the purpose of this test we can safely return null for both.
        when(validationContext.getMetaSchema()).thenReturn(null);
        when(validationContext.getConfig()).thenReturn(null);

        // --------------------------------------------------------------------
        // 2️⃣  Build a JsonNode representing a schema that contains a "type"
        // --------------------------------------------------------------------
        JsonNode schemaNode = MAPPER.readTree(SCHEMA_WITH_TYPE);

        // --------------------------------------------------------------------
        // 3️⃣  Create the JsonSchema instance via the package‑private factory.
        // --------------------------------------------------------------------
        // The factory method is package‑private; because this test class lives in
        // the same package we can call it directly.
        jsonSchema = JsonSchema.from(
                validationContext,
                "test-schema",                         // schemaPath
                new URI("http://example.com/schema"),   // currentUri
                schemaNode,                             // schemaNode
                null,                                   // parent
                false                                   // suppressSubSchemaRetrieval
        );
        // At this point the schema has not yet run its validator initialisation.
        // The typeValidator field should therefore be null.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    // ------------------------------------------------------------------------
    //  Test cases
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("getTypeValidator() returns null before validators are initialised")
    void testGetTypeValidatorInitiallyNull() {
        // Pre‑condition – the schema has not been initialised yet.
        assertFalse(jsonSchema.hasTypeValidator(),
                "hasTypeValidator() should be false before initialiseValidators()");

        // The getter should return null.
        assertNull(jsonSchema.getTypeValidator(),
                "getTypeValidator() should return null before initialiseValidators()");
    }

    @Test
    @DisplayName("getTypeValidator() returns a non‑null TypeValidator after initialiseValidators()")
    void testGetTypeValidatorAfterInitialize() {
        // Initialise all validators – this is the public method that populates
        // the internal TypeValidator (if the schema defines a "type" keyword).
        jsonSchema.initializeValidators();

        // After initialisation the schema must report that it has a TypeValidator.
        assertTrue(jsonSchema.hasTypeValidator(),
                "hasTypeValidator() should be true after initialiseValidators()");

        // The getter must now return a non‑null instance.
        TypeValidator typeValidator = jsonSchema.getTypeValidator();
        assertNotNull(typeValidator, "getTypeValidator() should return a non‑null instance after initialiseValidators()");

        // (Optional) Verify that the returned validator knows its type.
        // TypeValidator exposes a getType() method in the library – we can
        // assert that it matches the schema definition ("string").
        // This check is defensive; if the method ever changes the test will
        // still pass because we already verified non‑null above.
        // Uncomment the following lines if getType() is available:
        // assertEquals("string", typeValidator.getType(), "The TypeValidator should represent the 'string' type");
    }
}
```