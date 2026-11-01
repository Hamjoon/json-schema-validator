package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class JsonSchema_hasRequiredValidator_Test {

    /**
     * Mocks needed for the package‑private factory method **
     */
    @Mock
    ValidationContext validationContext;

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonNode // a dummy schema – its content is irrelevant for this test
    schemaNode;

    @Mock
    JsonSchema // no parent is required for the scenarios below
    parentSchema;

    /**
     * The instance under test – created once per test method **
     */
    JsonSchema jsonSchema;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Use the package‑private *from* factory to obtain a real JsonSchema instance.
        // All arguments are mocks – the constructor logic only stores them; the
        // validator list is lazily initialised and will not be touched by the
        // *hasRequiredValidator* method.
        jsonSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, parentSchema, false);
        // Ensure the requiredValidator field is *null* before each test case.
        setRequiredValidator(null);
    }

    /**
     * Helper that uses reflection to set the private {@code requiredValidator}
     * field of {@link JsonSchema}. This is the only way to inject a value
     * without invoking any private methods of the class under test.
     */
    private void setRequiredValidator(JsonValidator validator) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("requiredValidator");
        f.setAccessible(true);
        f.set(jsonSchema, validator);
    }

    /**
     * Helper that reads the private {@code requiredValidator} field – useful
     *    for assertions in the “real schema” tests. **
     */
    private JsonValidator getRequiredValidator(JsonSchema schema) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("requiredValidator");
        f.setAccessible(true);
        return (JsonValidator) f.get(schema);
    }

    /**
     * Helper that builds a {@link JsonSchema} from a JSON string using the
     *    public {@link JsonSchemaFactory}. **
     */
    private JsonSchema buildSchemaFromJson(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        // Use the default 2020‑12 version – the exact version is irrelevant for the
        // required‑keyword handling.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        return factory.getSchema(node);
    }

    /**
     * Data provider for schemas that contain the {@code required} keyword. **
     */
    static Stream<String> schemaWithRequiredProvider() {
        return Stream.of(// single required property
        "{ \"type\": \"object\", \"required\": [\"name\"] }", // required together with other keywords
        "{ \"type\": \"object\", \"properties\": { \"id\": {\"type\": \"string\"} }, \"required\": [\"id\"] }");
    }

    /**
     * Data provider for schemas that do **not** contain the {@code required} keyword. **
     */
    static Stream<String> schemaWithoutRequiredProvider() {
        return Stream.of(// object without required
        "{ \"type\": \"object\", \"properties\": { \"age\": { \"type\": \"integer\" } } }", // primitive type
        "{ \"type\": \"string\" }");
    }

    @Test
    void hasRequiredValidator_ReturnsFalse_WhenValidatorIsNull() {
        // The field was cleared in {@code setupBeforeEach()}, so we expect false.
        assertFalse(jsonSchema.hasRequiredValidator(), "hasRequiredValidator() should return false when no required validator is present");
    }

    @Test
    void hasRequiredValidator_ReturnsTrue_WhenValidatorIsPresent() throws Exception {
        // Prepare a simple mock validator – its behaviour is irrelevant.
        JsonValidator dummyValidator = mock(JsonValidator.class);
        setRequiredValidator(dummyValidator);
        assertTrue(jsonSchema.hasRequiredValidator(), "hasRequiredValidator() should return true when a required validator is set");
    }

    @ParameterizedTest
    @MethodSource("schemaWithRequiredProvider")
    void hasRequiredValidator_True_WhenSchemaContainsRequired(String schemaJson) throws Exception {
        JsonSchema schema = buildSchemaFromJson(schemaJson);
        assertTrue(schema.hasRequiredValidator(), "Schema should report a required validator when the 'required' keyword is present");
        // Sanity check – the private field must indeed be non‑null.
        assertNotNull(getRequiredValidator(schema), "Internal requiredValidator field should be initialised");
    }

    @ParameterizedTest
    @MethodSource("schemaWithoutRequiredProvider")
    void hasRequiredValidator_False_WhenSchemaLacksRequired(String schemaJson) throws Exception {
        JsonSchema schema = buildSchemaFromJson(schemaJson);
        assertFalse(schema.hasRequiredValidator(), "Schema should report no required validator when the 'required' keyword is absent");
        assertNull(getRequiredValidator(schema), "Internal requiredValidator field should remain null");
    }

    @Test
    void hasRequiredValidator_True_AfterManualInjectionEvenIfSchemaLacksRequired() throws Exception {
        // Build a schema without the required keyword.
        JsonSchema schema = buildSchemaFromJson("{ \"type\": \"object\" }");
        assertFalse(schema.hasRequiredValidator(), "Pre‑condition: the schema must not have a required validator initially");
        // Inject a dummy validator via reflection.
        JsonValidator dummy = mock(JsonValidator.class);
        Field f = JsonSchema.class.getDeclaredField("requiredValidator");
        f.setAccessible(true);
        f.set(schema, dummy);
        assertTrue(schema.hasRequiredValidator(), "After injection, hasRequiredValidator() must return true");
    }

    @Test
    void hasRequiredValidator_False_AfterClearingValidator() throws Exception {
        // Build a schema that initially creates a required validator.
        JsonSchema schema = buildSchemaFromJson("{ \"type\": \"object\", \"required\": [\"id\"] }");
        assertTrue(schema.hasRequiredValidator(), "Pre‑condition: schema should initially have a required validator");
        // Clear the private field.
        Field f = JsonSchema.class.getDeclaredField("requiredValidator");
        f.setAccessible(true);
        f.set(schema, null);
        assertFalse(schema.hasRequiredValidator(), "After clearing the field, hasRequiredValidator() must return false");
    }

    @Test
    void hasRequiredValidator_True_AfterWithConfigThatDoesNotDisableRequired() throws Exception {
        // Build a schema with a required keyword.
        JsonSchema schema = buildSchemaFromJson("{ \"type\": \"object\", \"required\": [\"code\"] }");
        assertTrue(schema.hasRequiredValidator(), "Pre‑condition: required validator must be present before config change");
        // Apply a config that changes an unrelated validator (typeLoose).
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setTypeLoose(true);
        JsonSchema changed = schema.withConfig(cfg);
        // The original instance is immutable – withConfig returns a new instance.
        // Both should still report a required validator.
        assertTrue(changed.hasRequiredValidator(), "withConfig that does not affect 'required' must keep the validator");
        assertTrue(schema.hasRequiredValidator(), "Original schema instance must remain unchanged");
    }
}
