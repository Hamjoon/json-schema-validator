package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.walk.DefaultKeywordWalkListenerRunner;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchema_isRecursiveAnchor_Test {

    /**
     * Mocked validation context used by the factory method.
     */
    @Mock
    ValidationContext validationContext;

    /**
     * Mocked meta‑schema required by {@link ValidationContext}.
     */
    @Mock
    JsonMetaSchema metaSchema;

    /**
     * Mocked schema location – the concrete value is irrelevant for the test.
     */
    @Mock
    SchemaLocation schemaLocation;

    /**
     * Mocked evaluation path – also irrelevant for the test.
     */
    @Mock
    JsonNodePath evaluationPath;

    /**
     * A simple JSON object used as the schema node.
     */
    private static final ObjectNode SCHEMA_NODE = JsonNodeFactory.instance.objectNode();

    /**
     * Jackson mapper used to build {@link JsonNode}s from JSON strings.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * A simple empty JSON object used as a fallback schema node.
     */
    private static final ObjectNode EMPTY_SCHEMA_NODE = JsonNodeFactory.instance.objectNode();

    @BeforeEach
    void setupBeforeEach() {
        // Stub the validation context so that the constructor logic can execute without NPEs.
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        // The factory method asks the context for an ID; return {@code null} to trigger the
        // default branch where {@code recursiveAnchor} stays {@code false}.
        when(validationContext.resolveSchemaId(SCHEMA_NODE)).thenReturn(null);
        // The maps used for schema resources / dynamic anchors are not inspected by the
        // {@code isRecursiveAnchor()} method, but they must be non‑null to avoid
        // {@code NullPointerException}s inside the constructor.
        when(validationContext.getSchemaResources()).thenReturn(new java.util.concurrent.ConcurrentHashMap<>());
        when(validationContext.getDynamicAnchors()).thenReturn(new java.util.concurrent.ConcurrentHashMap<>());
    }

    /**
     * Creates a {@link JsonSchema} instance from a JSON string.
     *
     * @param json the schema definition as a JSON string
     * @return a freshly built {@link JsonSchema}
     */
    private JsonSchema createSchemaFromJson(String json) {
        JsonNode node = MAPPER.readTree(json);
        return JsonSchema.from(validationContext, schemaLocation, evaluationPath, node, null, false);
    }

    /**
     * Uses reflection to set the private {@code recursiveAnchor} field.
     *
     * @param schema the {@link JsonSchema} instance to manipulate
     * @param value  the boolean value to assign
     */
    private void setRecursiveAnchorViaReflection(JsonSchema schema, boolean value) throws Exception {
        Field field = JsonSchema.class.getDeclaredField("recursiveAnchor");
        field.setAccessible(true);
        field.setBoolean(schema, value);
    }

    private static Stream<Boolean> provideRecursiveAnchorKeywordValues() {
        return Stream.of(true, false);
    }

    /**
     * Verify that a newly created {@link JsonSchema} reports {@code false}
     * for {@code isRecursiveAnchor()}.
     */
    @Test
    void isRecursiveAnchor_ShouldReturnFalse_ForFreshSchema() {
        // Create the schema via the public factory method.
        JsonSchema schema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, SCHEMA_NODE, // no parent schema
        null, // do not suppress sub‑schema retrieval
        false);
        // The accessor should return the default value.
        assertFalse(schema.isRecursiveAnchor(), "A freshly created JsonSchema must have recursiveAnchor == false");
    }

    /**
     * Verify that the accessor consistently returns {@code false} even when the
     * schema is created with a parent schema (which itself does not have a recursive
     * anchor).
     */
    @Test
    void isRecursiveAnchor_ShouldReturnFalse_WhenParentSchemaProvided() {
        // Parent schema – also created via the factory method.
        JsonSchema parent = JsonSchema.from(validationContext, schemaLocation, evaluationPath, SCHEMA_NODE, null, false);
        // Child schema with the parent supplied.
        JsonSchema child = JsonSchema.from(validationContext, schemaLocation, evaluationPath, SCHEMA_NODE, parent, false);
        // Both parent and child must report false.
        assertFalse(parent.isRecursiveAnchor(), "Parent schema should not have a recursive anchor");
        assertFalse(child.isRecursiveAnchor(), "Child schema should not have a recursive anchor");
    }

    @Test
    void isRecursiveAnchor_ShouldReflectReflectionChanges() throws Exception {
        JsonSchema schema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, EMPTY_SCHEMA_NODE, null, false);
        // set to true via reflection
        setRecursiveAnchorViaReflection(schema, true);
        assertTrue(schema.isRecursiveAnchor(), "After setting recursiveAnchor to true via reflection the getter must return true");
        // reset to false via reflection
        setRecursiveAnchorViaReflection(schema, false);
        assertFalse(schema.isRecursiveAnchor(), "After resetting recursiveAnchor to false via reflection the getter must return false");
    }

    @Test
    void isRecursiveAnchor_IsInstanceSpecific() throws Exception {
        JsonSchema schemaA = JsonSchema.from(validationContext, schemaLocation, evaluationPath, EMPTY_SCHEMA_NODE, null, false);
        JsonSchema schemaB = JsonSchema.from(validationContext, schemaLocation, evaluationPath, EMPTY_SCHEMA_NODE, null, false);
        // modify only schemaA
        setRecursiveAnchorViaReflection(schemaA, true);
        assertTrue(schemaA.isRecursiveAnchor(), "Instance A should report true after reflection change");
        assertFalse(schemaB.isRecursiveAnchor(), "Instance B must remain false, confirming the field is not static");
    }

    @ParameterizedTest(name = "[{index}] $recursiveAnchor={0}")
    @MethodSource("provideRecursiveAnchorKeywordValues")
    void isRecursiveAnchor_ShouldRespectKeyword(boolean keywordValue) {
        String json = String.format("{\"$recursiveAnchor\": %s}", keywordValue);
        JsonSchema schema = createSchemaFromJson(json);
        assertEquals(keywordValue, schema.isRecursiveAnchor(), "The accessor must reflect the value of $recursiveAnchor keyword");
    }

    @Test
    void isRecursiveAnchor_ShouldRemainFalse_WhenKeywordAbsent() {
        String json = "{\"type\":\"object\",\"properties\":{}}";
        JsonSchema schema = createSchemaFromJson(json);
        assertFalse(schema.isRecursiveAnchor(), "Absent $recursiveAnchor keyword must leave the flag at its default false");
    }

    @Test
    void isRecursiveAnchor_ShouldIgnoreMalformedKeywordValue() {
        String json = "{\"$recursiveAnchor\": \"notABoolean\"}";
        JsonSchema schema = createSchemaFromJson(json);
        assertFalse(schema.isRecursiveAnchor(), "Non‑boolean $recursiveAnchor value must be ignored and default to false");
    }

    @Test
    void isRecursiveAnchor_InheritedFromParent_True() {
        JsonSchema parent = createSchemaFromJson("{\"$recursiveAnchor\": true}");
        JsonSchema child = parent.fromRef(parent, mock(JsonNodePath.class));
        assertTrue(child.isRecursiveAnchor(), "Child schema obtained via fromRef() must inherit true flag from parent");
    }

    @Test
    void isRecursiveAnchor_InheritedFromParent_False() {
        JsonSchema parent = createSchemaFromJson("{\"$recursiveAnchor\": false}");
        JsonSchema child = parent.fromRef(parent, mock(JsonNodePath.class));
        assertFalse(child.isRecursiveAnchor(), "Child schema obtained via fromRef() must inherit false flag from parent");
    }

    @Test
    void isRecursiveAnchor_UnchangedAfterInitializeValidators_True() {
        JsonSchema schema = createSchemaFromJson("{\"$recursiveAnchor\": true}");
        // should be a no‑op for the flag
        schema.initializeValidators();
        assertTrue(schema.isRecursiveAnchor(), "initializeValidators() must not flip a true recursiveAnchor flag");
    }

    @Test
    void isRecursiveAnchor_UnchangedAfterInitializeValidators_False() {
        JsonSchema schema = createSchemaFromJson("{\"type\":\"string\"}");
        schema.initializeValidators();
        assertFalse(schema.isRecursiveAnchor(), "initializeValidators() must not turn a false recursiveAnchor flag to true");
    }

    @Test
    void isRecursiveAnchor_FromStringInput_True() throws Exception {
        String json = "{ \"$recursiveAnchor\": true }";
        JsonSchema schema = createSchemaFromJson(json);
        assertTrue(schema.isRecursiveAnchor(), "Schema built from a JSON string containing $recursiveAnchor:true must report true");
    }

    @Test
    void isRecursiveAnchor_FromStringInput_False() throws Exception {
        String json = "{ \"type\": \"array\" }";
        JsonSchema schema = createSchemaFromJson(json);
        assertFalse(schema.isRecursiveAnchor(), "Schema built from a JSON string without $recursiveAnchor must report false");
    }
}
