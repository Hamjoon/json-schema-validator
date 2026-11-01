package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AnyOfValidator_preloadJsonSchema_Test {

    @Mock
    private ValidationContext validationContext;

    @Mock
    private SchemaLocation schemaLocation;

    @Mock
    private JsonNodePath evaluationPath;

    @Mock
    private JsonSchema parentSchema;

    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations
        mocks = MockitoAnnotations.openMocks(this);
        // The validator checks the configuration flag – provide a default config
        when(validationContext.getConfig()).thenReturn(new SchemaValidatorsConfig());
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Release Mockito resources
        mocks.close();
    }

    /**
     * Helper to set the private {@code schemas} field via reflection.
     */
    private static void setSchemasField(AnyOfValidator validator, List<JsonSchema> list) throws Exception {
        Field f = AnyOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        f.set(validator, list);
    }

    @Test
    void preloadJsonSchema_ShouldInvokePreloadOnAllContainedSchemas() {
        // ----- Arrange -------------------------------------------------------
        // Create two mock JsonSchema instances that will be returned by the
        // ValidationContext when the AnyOfValidator constructor creates its
        // internal schema list.
        JsonSchema mockSchema1 = mock(JsonSchema.class);
        JsonSchema mockSchema2 = mock(JsonSchema.class);
        // The AnyOfValidator constructor calls validationContext.newSchema(...)
        // for each element of the supplied schemaNode (an array). Stub the method
        // to return the two mocks in order.
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), any(JsonNode.class), any(JsonSchema.class))).thenReturn(mockSchema1, mockSchema2);
        // Build a simple JSON array node containing two empty objects – this
        // represents the "anyOf" schema definition.
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode schemaArray = mapper.createArrayNode();
        schemaArray.add(mapper.createObjectNode());
        schemaArray.add(mapper.createObjectNode());
        // Construct the validator under test.
        AnyOfValidator validator = new AnyOfValidator(schemaLocation, evaluationPath, schemaArray, parentSchema, validationContext);
        // ----- Act -----------------------------------------------------------
        validator.preloadJsonSchema();
        // ----- Assert --------------------------------------------------------
        // Verify that each mock schema had its preloadJsonSchema() method
        // invoked exactly once by the validator.
        verify(mockSchema1, times(1)).preloadJsonSchema();
        verify(mockSchema2, times(1)).preloadJsonSchema();
    }

    /**
     * Helper to retrieve the private {@code schemas} field via reflection.
     */
    @SuppressWarnings("unchecked")
    private static List<JsonSchema> getSchemasField(AnyOfValidator validator) throws Exception {
        Field f = AnyOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        return (List<JsonSchema>) f.get(validator);
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("preloadJsonSchema - empty schema list")
    void preloadJsonSchema_EmptySchemaList() throws Exception {
        // Arrange – construct validator with an empty array node
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode emptyArray = mapper.createArrayNode();
        AnyOfValidator validator = new AnyOfValidator(schemaLocation, evaluationPath, emptyArray, parentSchema, validationContext);
        // Spy to verify the protected delegate call
        AnyOfValidator spyValidator = Mockito.spy(validator);
        doNothing().when(spyValidator).preloadJsonSchemas(anyList());
        // Act
        spyValidator.preloadJsonSchema();
        // Assert – internal list is empty and delegate called once with empty list
        List<JsonSchema> internal = getSchemasField(spyValidator);
        assertTrue(internal.isEmpty(), "Internal schemas list should be empty");
        verify(spyValidator, times(1)).preloadJsonSchemas(eq(internal));
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("preloadJsonSchema - non‑empty schema list")
    void preloadJsonSchema_NonEmptySchemaList() throws Exception {
        // Arrange
        JsonSchema mock1 = mock(JsonSchema.class);
        JsonSchema mock2 = mock(JsonSchema.class);
        List<JsonSchema> list = new ArrayList<>(Arrays.asList(mock1, mock2));
        AnyOfValidator validator = new AnyOfValidator(schemaLocation, evaluationPath, new ArrayNode(JsonNodeFactory.instance), parentSchema, validationContext);
        setSchemasField(validator, list);
        AnyOfValidator spy = Mockito.spy(validator);
        doNothing().when(spy).preloadJsonSchemas(anyList());
        // Act
        spy.preloadJsonSchema();
        // Assert – list unchanged and delegate called with same instance
        List<JsonSchema> after = getSchemasField(spy);
        assertSame(list, after, "Schemas list instance must stay the same");
        assertEquals(2, after.size(), "List should still contain two elements");
        assertTrue(after.containsAll(Arrays.asList(mock1, mock2)));
        verify(spy, times(1)).preloadJsonSchemas(eq(list));
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("preloadJsonSchema - single schema list")
    void preloadJsonSchema_SingleSchemaList() throws Exception {
        JsonSchema mock = mock(JsonSchema.class);
        List<JsonSchema> list = new ArrayList<>(Collections.singletonList(mock));
        AnyOfValidator validator = new AnyOfValidator(schemaLocation, evaluationPath, new ArrayNode(JsonNodeFactory.instance), parentSchema, validationContext);
        setSchemasField(validator, list);
        AnyOfValidator spy = Mockito.spy(validator);
        doNothing().when(spy).preloadJsonSchemas(anyList());
        spy.preloadJsonSchema();
        List<JsonSchema> after = getSchemasField(spy);
        assertEquals(1, after.size());
        assertSame(mock, after.get(0));
        verify(spy, times(1)).preloadJsonSchemas(eq(list));
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("preloadJsonSchema - list containing null element")
    void preloadJsonSchema_ListWithNullElement() throws Exception {
        JsonSchema mock = mock(JsonSchema.class);
        List<JsonSchema> list = new ArrayList<>(Arrays.asList(mock, null));
        AnyOfValidator validator = new AnyOfValidator(schemaLocation, evaluationPath, new ArrayNode(JsonNodeFactory.instance), parentSchema, validationContext);
        setSchemasField(validator, list);
        AnyOfValidator spy = Mockito.spy(validator);
        doNothing().when(spy).preloadJsonSchemas(anyList());
        spy.preloadJsonSchema();
        List<JsonSchema> after = getSchemasField(spy);
        assertEquals(2, after.size());
        assertNull(after.get(1));
        verify(spy, times(1)).preloadJsonSchemas(eq(list));
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("preloadJsonSchema - duplicate schemas in list")
    void preloadJsonSchema_DuplicateSchemas() throws Exception {
        JsonSchema mock = mock(JsonSchema.class);
        List<JsonSchema> list = new ArrayList<>(Arrays.asList(mock, mock));
        AnyOfValidator validator = new AnyOfValidator(schemaLocation, evaluationPath, new ArrayNode(JsonNodeFactory.instance), parentSchema, validationContext);
        setSchemasField(validator, list);
        AnyOfValidator spy = Mockito.spy(validator);
        doNothing().when(spy).preloadJsonSchemas(anyList());
        spy.preloadJsonSchema();
        List<JsonSchema> after = getSchemasField(spy);
        assertEquals(2, after.size());
        assertSame(mock, after.get(0));
        assertSame(mock, after.get(1));
        verify(spy, times(1)).preloadJsonSchemas(eq(list));
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("preloadJsonSchema - multiple invocations (idempotence)")
    void preloadJsonSchema_MultipleInvocations() throws Exception {
        JsonSchema mock1 = mock(JsonSchema.class);
        JsonSchema mock2 = mock(JsonSchema.class);
        List<JsonSchema> list = new ArrayList<>(Arrays.asList(mock1, mock2));
        AnyOfValidator validator = new AnyOfValidator(schemaLocation, evaluationPath, new ArrayNode(JsonNodeFactory.instance), parentSchema, validationContext);
        setSchemasField(validator, list);
        AnyOfValidator spy = Mockito.spy(validator);
        doNothing().when(spy).preloadJsonSchemas(anyList());
        // First call
        spy.preloadJsonSchema();
        // Second call
        spy.preloadJsonSchema();
        verify(spy, times(2)).preloadJsonSchemas(eq(list));
        // List must stay unchanged after both calls
        List<JsonSchema> after = getSchemasField(spy);
        assertEquals(2, after.size());
        assertTrue(after.containsAll(Arrays.asList(mock1, mock2)));
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("preloadJsonSchema - exception propagation (RuntimeException)")
    void preloadJsonSchema_ExceptionPropagation_RuntimeException() {
        // Subclass that throws from the protected delegate
        AnyOfValidator validator = new AnyOfValidator(schemaLocation, evaluationPath, new ArrayNode(JsonNodeFactory.instance), parentSchema, validationContext) {

            @Override
            protected void preloadJsonSchemas(List<JsonSchema> schemas) {
                throw new RuntimeException("forced");
            }
        };
        RuntimeException ex = assertThrows(RuntimeException.class, validator::preloadJsonSchema, "RuntimeException should propagate unchanged");
        assertEquals("forced", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("preloadJsonSchema - list mutation by preloadJsonSchemas")
    void preloadJsonSchema_ListMutationByDelegate() throws Exception {
        JsonSchema mock1 = mock(JsonSchema.class);
        JsonSchema mock2 = mock(JsonSchema.class);
        JsonSchema added = mock(JsonSchema.class);
        List<JsonSchema> initial = new ArrayList<>(Arrays.asList(mock1, mock2));
        AnyOfValidator validator = new AnyOfValidator(schemaLocation, evaluationPath, new ArrayNode(JsonNodeFactory.instance), parentSchema, validationContext) {

            @Override
            protected void preloadJsonSchemas(List<JsonSchema> schemas) {
                // Simulate a delegate that adds a new schema
                schemas.add(added);
            }
        };
        setSchemasField(validator, initial);
        validator.preloadJsonSchema();
        List<JsonSchema> after = getSchemasField(validator);
        assertEquals(3, after.size(), "List size should increase to three");
        assertTrue(after.containsAll(Arrays.asList(mock1, mock2, added)));
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("preloadJsonSchema - null internal schemas list")
    void preloadJsonSchema_NullInternalSchemas() throws Exception {
        AnyOfValidator validator = new AnyOfValidator(schemaLocation, evaluationPath, new ArrayNode(JsonNodeFactory.instance), parentSchema, validationContext);
        // Force the private field to null
        setSchemasField(validator, null);
        assertThrows(NullPointerException.class, validator::preloadJsonSchema, "Calling preloadJsonSchema with null internal list should throw NPE");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("preloadJsonSchema - unmodifiable schemas list")
    void preloadJsonSchema_UnmodifiableSchemasList() throws Exception {
        JsonSchema mock1 = mock(JsonSchema.class);
        List<JsonSchema> unmodifiable = Collections.unmodifiableList(new ArrayList<>(Collections.singletonList(mock1)));
        AnyOfValidator validator = new AnyOfValidator(schemaLocation, evaluationPath, new ArrayNode(JsonNodeFactory.instance), parentSchema, validationContext) {

            @Override
            protected void preloadJsonSchemas(List<JsonSchema> schemas) {
                // Attempt to modify – should trigger UnsupportedOperationException
                schemas.add(mock(JsonSchema.class));
            }
        };
        setSchemasField(validator, unmodifiable);
        assertThrows(UnsupportedOperationException.class, validator::preloadJsonSchema, "Modifying an unmodifiable list should result in UnsupportedOperationException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("preloadJsonSchema - error propagation (AssertionError)")
    void preloadJsonSchema_ErrorPropagation_AssertionError() {
        AnyOfValidator validator = new AnyOfValidator(schemaLocation, evaluationPath, new ArrayNode(JsonNodeFactory.instance), parentSchema, validationContext) {

            @Override
            protected void preloadJsonSchemas(List<JsonSchema> schemas) {
                throw new AssertionError("forced error");
            }
        };
        AssertionError err = assertThrows(AssertionError.class, validator::preloadJsonSchema, "AssertionError should propagate unchanged");
        assertEquals("forced error", err.getMessage());
    }
}
