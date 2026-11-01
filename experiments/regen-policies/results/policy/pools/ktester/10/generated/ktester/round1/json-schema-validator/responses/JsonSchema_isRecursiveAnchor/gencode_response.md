```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.walk.DefaultKeywordWalkListenerRunner;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchema#isRecursiveAnchor()}.
 *
 * <p>The tests cover the default value, modifications via reflection,
 * handling of the {@code "$recursiveAnchor"} keyword, inheritance through
 * {@code fromRef()}, and the fact that validator initialization does not
 * affect the flag.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_isRecursiveAnchor_Test {

    /** Mocked validation context used by the factory method. */
    @Mock
    ValidationContext validationContext;

    /** Mocked meta‑schema required by {@link ValidationContext}. */
    @Mock
    JsonMetaSchema metaSchema;

    /** Mocked schema location – the concrete value is irrelevant for the test. */
    @Mock
    SchemaLocation schemaLocation;

    /** Mocked evaluation path – also irrelevant for the test. */
    @Mock
    JsonNodePath evaluationPath;

    /** Jackson mapper used to build {@link JsonNode}s from JSON strings. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** A simple empty JSON object used as a fallback schema node. */
    private static final ObjectNode EMPTY_SCHEMA_NODE = JsonNodeFactory.instance.objectNode();

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Minimal stubbing so that JsonSchema.from() can run without NPEs.
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        when(validationContext.getSchemaResources()).thenReturn(new java.util.concurrent.ConcurrentHashMap<>());
        when(validationContext.getDynamicAnchors()).thenReturn(new java.util.concurrent.ConcurrentHashMap<>());
        // The walk listener runner is created lazily; we provide a harmless stub.
        when(validationContext.getKeywordWalkListenerRunner()).thenReturn(new DefaultKeywordWalkListenerRunner());
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* --------------------------------------------------------------------- *
     * Helper methods
     * --------------------------------------------------------------------- */

    /**
     * Creates a {@link JsonSchema} instance from a JSON string.
     *
     * @param json the schema definition as a JSON string
     * @return a freshly built {@link JsonSchema}
     */
    private JsonSchema createSchemaFromJson(String json) {
        JsonNode node = MAPPER.readTree(json);
        return JsonSchema.from(
                validationContext,
                schemaLocation,
                evaluationPath,
                node,
                null,
                false);
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

    /* --------------------------------------------------------------------- *
     * Test cases – default behaviour
     * --------------------------------------------------------------------- */

    @Test
    void isRecursiveAnchor_ShouldReturnFalse_ForFreshSchema() {
        JsonSchema schema = JsonSchema.from(
                validationContext,
                schemaLocation,
                evaluationPath,
                EMPTY_SCHEMA_NODE,
                null,
                false);
        assertFalse(schema.isRecursiveAnchor(),
                "A freshly created JsonSchema must have recursiveAnchor == false");
    }

    /* --------------------------------------------------------------------- *
     * Test cases – reflection manipulation
     * --------------------------------------------------------------------- */

    @Test
    void isRecursiveAnchor_ShouldReflectReflectionChanges() throws Exception {
        JsonSchema schema = JsonSchema.from(
                validationContext,
                schemaLocation,
                evaluationPath,
                EMPTY_SCHEMA_NODE,
                null,
                false);

        // set to true via reflection
        setRecursiveAnchorViaReflection(schema, true);
        assertTrue(schema.isRecursiveAnchor(),
                "After setting recursiveAnchor to true via reflection the getter must return true");

        // reset to false via reflection
        setRecursiveAnchorViaReflection(schema, false);
        assertFalse(schema.isRecursiveAnchor(),
                "After resetting recursiveAnchor to false via reflection the getter must return false");
    }

    @Test
    void isRecursiveAnchor_IsInstanceSpecific() throws Exception {
        JsonSchema schemaA = JsonSchema.from(
                validationContext,
                schemaLocation,
                evaluationPath,
                EMPTY_SCHEMA_NODE,
                null,
                false);
        JsonSchema schemaB = JsonSchema.from(
                validationContext,
                schemaLocation,
                evaluationPath,
                EMPTY_SCHEMA_NODE,
                null,
                false);

        // modify only schemaA
        setRecursiveAnchorViaReflection(schemaA, true);

        assertTrue(schemaA.isRecursiveAnchor(),
                "Instance A should report true after reflection change");
        assertFalse(schemaB.isRecursiveAnchor(),
                "Instance B must remain false, confirming the field is not static");
    }

    /* --------------------------------------------------------------------- *
     * Test cases – $recursiveAnchor keyword handling
     * --------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] $recursiveAnchor={0}")
    @MethodSource("provideRecursiveAnchorKeywordValues")
    void isRecursiveAnchor_ShouldRespectKeyword(boolean keywordValue) {
        String json = String.format("{\"$recursiveAnchor\": %s}", keywordValue);
        JsonSchema schema = createSchemaFromJson(json);
        assertEquals(keywordValue, schema.isRecursiveAnchor(),
                "The accessor must reflect the value of $recursiveAnchor keyword");
    }

    private static Stream<Boolean> provideRecursiveAnchorKeywordValues() {
        return Stream.of(true, false);
    }

    @Test
    void isRecursiveAnchor_ShouldRemainFalse_WhenKeywordAbsent() {
        String json = "{\"type\":\"object\",\"properties\":{}}";
        JsonSchema schema = createSchemaFromJson(json);
        assertFalse(schema.isRecursiveAnchor(),
                "Absent $recursiveAnchor keyword must leave the flag at its default false");
    }

    @Test
    void isRecursiveAnchor_ShouldIgnoreMalformedKeywordValue() {
        String json = "{\"$recursiveAnchor\": \"notABoolean\"}";
        JsonSchema schema = createSchemaFromJson(json);
        assertFalse(schema.isRecursiveAnchor(),
                "Non‑boolean $recursiveAnchor value must be ignored and default to false");
    }

    /* --------------------------------------------------------------------- *
     * Test cases – inheritance via fromRef()
     * --------------------------------------------------------------------- */

    @Test
    void isRecursiveAnchor_InheritedFromParent_True() {
        JsonSchema parent = createSchemaFromJson("{\"$recursiveAnchor\": true}");
        JsonSchema child = parent.fromRef(parent, mock(JsonNodePath.class));
        assertTrue(child.isRecursiveAnchor(),
                "Child schema obtained via fromRef() must inherit true flag from parent");
    }

    @Test
    void isRecursiveAnchor_InheritedFromParent_False() {
        JsonSchema parent = createSchemaFromJson("{\"$recursiveAnchor\": false}");
        JsonSchema child = parent.fromRef(parent, mock(JsonNodePath.class));
        assertFalse(child.isRecursiveAnchor(),
                "Child schema obtained via fromRef() must inherit false flag from parent");
    }

    /* --------------------------------------------------------------------- *
     * Test cases – validator initialization does not affect the flag
     * --------------------------------------------------------------------- */

    @Test
    void isRecursiveAnchor_UnchangedAfterInitializeValidators_True() {
        JsonSchema schema = createSchemaFromJson("{\"$recursiveAnchor\": true}");
        schema.initializeValidators(); // should be a no‑op for the flag
        assertTrue(schema.isRecursiveAnchor(),
                "initializeValidators() must not flip a true recursiveAnchor flag");
    }

    @Test
    void isRecursiveAnchor_UnchangedAfterInitializeValidators_False() {
        JsonSchema schema = createSchemaFromJson("{\"type\":\"string\"}");
        schema.initializeValidators();
        assertFalse(schema.isRecursiveAnchor(),
                "initializeValidators() must not turn a false recursiveAnchor flag to true");
    }

    /* --------------------------------------------------------------------- *
     * Test cases – building from a JSON string (simulating InputFormat)
     * --------------------------------------------------------------------- */

    @Test
    void isRecursiveAnchor_FromStringInput_True() throws Exception {
        String json = "{ \"$recursiveAnchor\": true }";
        JsonSchema schema = createSchemaFromJson(json);
        assertTrue(schema.isRecursiveAnchor(),
                "Schema built from a JSON string containing $recursiveAnchor:true must report true");
    }

    @Test
    void isRecursiveAnchor_FromStringInput_False() throws Exception {
        String json = "{ \"type\": \"array\" }";
        JsonSchema schema = createSchemaFromJson(json);
        assertFalse(schema.isRecursiveAnchor(),
                "Schema built from a JSON string without $recursiveAnchor must report false");
    }
}
```