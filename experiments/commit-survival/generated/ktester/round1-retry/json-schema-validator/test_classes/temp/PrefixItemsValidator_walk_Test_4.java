package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;

class PrefixItemsValidator_walk_Test extends BaseJsonValidator {

    private PrefixItemsValidator validator;

    //     @Mock
    private JsonSchema parentSchema;

    //     @Mock
    private ValidationContext validationContext;

    //     @Mock
    private SchemaValidatorsConfig config;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * The list of schemas that correspond to the tuple items (the “prefixItems”).
     */
    private final List<JsonSchema> tupleSchema;

    /**
     * Simple concrete implementation of {@link ApplyDefaultsStrategy} used in the tests.
     */
    private static class SimpleApplyDefaultsStrategy extends ApplyDefaultsStrategy {

        private final boolean apply;

        SimpleApplyDefaultsStrategy(boolean apply) {
            // The superclass expects three booleans (object, array, object‑properties defaults).
            // We forward the same flag for all of them.
            super(apply, apply, apply);
            this.apply = apply;
        }

        @Override
        public boolean shouldApplyArrayDefaults() {
            return apply;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a minimal valid "prefixItems" schema node (an array with a single empty schema object).
        ArrayNode schemaNode = MAPPER.createArrayNode();
        // {} – a placeholder schema
        schemaNode.add(MAPPER.createObjectNode());
        // The constructor arguments `parentSchema` and `validationContext` are not used by the
        // `walk` method that is exercised in these tests, so they can safely be null.
        validator = new PrefixItemsValidator("testPath", schemaNode, null, null);
    }

    /**
     * Helper to set a private or protected field via reflection.
     */
    private static void setField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the tuple schema list (used by the test suite via reflection).
     */
    public List<JsonSchema> getTupleSchema() {
        return tupleSchema;
    }

    @Test
    void walk_returnsEmptySetWhenNodeIsNotArray() {
        // Arrange – a non‑array node (NullNode works fine)
        JsonNode nonArrayNode = MAPPER.nullNode();
        ExecutionContext executionContext = new ExecutionContext();
        JsonNode rootNode = MAPPER.createObjectNode();
        String at = "";
        boolean shouldValidateSchema = false;
        // Act
        Set<ValidationMessage> result = validator.walk(executionContext, nonArrayNode, rootNode, at, shouldValidateSchema);
        // Assert
        assertNotNull(result, "Resulting set must not be null");
        assertTrue(result.isEmpty(), "Result should be empty when the node is not an array");
    }

    @Test
    void walk_returnsEmptySetWhenArrayIsEmpty() {
        // Arrange
        ArrayNode emptyArray = MAPPER.createArrayNode();
        ExecutionContext executionContext = new ExecutionContext();
        JsonNode rootNode = MAPPER.createObjectNode();
        String at = "";
        boolean shouldValidateSchema = false;
        // Act
        Set<ValidationMessage> result = validator.walk(executionContext, emptyArray, rootNode, at, shouldValidateSchema);
        // Assert
        assertNotNull(result, "Resulting set must not be null");
        assertTrue(result.isEmpty(), "Result should be empty when the array has no items");
        assertTrue(emptyArray.isArray(), "The test array must still be recognized as an array");
    }

    /* --------------------------------------------------------------
     *  ApplyDefaultsStrategy disabled – array must stay untouched
     * -------------------------------------------------------------- */
    @Test
    void walk_whenApplyDefaultsStrategyIsDisabled_shouldReturnEmptyMessagesAndNotModifyArray() {
        setField(validator, "applyDefaultsStrategy", new SimpleApplyDefaultsStrategy(false));
        ArrayNode array = MAPPER.createArrayNode().add(1).add(2).add(3);
        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> messages = validator.walk(ctx, array, array, "/", true);
        assertTrue(messages.isEmpty(), "No validation messages expected");
        // array must stay exactly the same
        assertEquals("[1,2,3]", array.toString(), "Array content must remain unchanged when defaults are not applied");
    }

    /* --------------------------------------------------------------
     *  Tuple schema empty – walk should iterate zero times
     * -------------------------------------------------------------- */
    @Test
    void walk_whenTupleSchemaIsEmpty_shouldIterateZeroTimes() {
        setField(validator, "applyDefaultsStrategy", new SimpleApplyDefaultsStrategy(true));
        setField(validator, "tupleSchema", Collections.emptyList());
        ArrayNode array = MAPPER.createArrayNode().add("a").add("b");
        Set<ValidationMessage> messages = validator.walk(new ExecutionContext(), array, array, "/", true);
        assertTrue(messages.isEmpty());
        assertEquals("[\"a\",\"b\"]", array.toString(), "Array must stay unchanged when there is no tuple schema");
    }

    @Override
    public Set<ValidationMessage> walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema) {
        // The original implementation walks over the tuple schema and applies defaults.
        // Delegating to the superclass implementation preserves that logic.
        return super.walk(executionContext, node, rootNode, at, shouldValidateSchema);
    }

    @Override
    public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at) {
        // Validation logic is unchanged.
        return super.validate(executionContext, node, rootNode, at);
    }
}
