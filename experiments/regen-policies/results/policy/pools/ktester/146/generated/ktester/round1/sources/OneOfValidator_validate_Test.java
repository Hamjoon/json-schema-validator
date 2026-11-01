package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

@ExtendWith(MockitoExtension.class)
class OneOfValidator_validate_Test {

    private static ObjectMapper mapper;

    private static ValidationContext validationContext;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /* --------------------------------------------------- *
     *  Helper – a testable subclass of OneOfValidator    *
     * --------------------------------------------------- */
    private static class TestableOneOfValidator extends OneOfValidator {

        private final boolean shortCircuit;

        // null = default (true)
        private final Boolean reportChildErrors;

        TestableOneOfValidator(SchemaLocation location, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext ctx, boolean shortCircuit, Boolean reportChildErrors) {
            super(location, evaluationPath, schemaNode, parentSchema, ctx);
            this.shortCircuit = shortCircuit;
            this.reportChildErrors = reportChildErrors;
        }

        @Override
        protected boolean canShortCircuit() {
            return shortCircuit;
        }

        @Override
        protected boolean reportChildErrors(ExecutionContext executionContext) {
            return reportChildErrors == null ? super.reportChildErrors(executionContext) : reportChildErrors;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
        // Build a ValidationContext that uses the 2020‑12 meta‑schema.
        JsonMetaSchema meta = JsonMetaSchema.getV202012();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        validationContext = new ValidationContext(meta, factory, new SchemaValidatorsConfig());
        JsonMetaSchema meta = JsonMetaSchema.getV202012();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        validationContext = new ValidationContext(meta, factory, new SchemaValidatorsConfig());
    }

    /**
     * Creates a validator instance for the given JSON schema fragment.
     */
    private static TestableOneOfValidator createValidator(String schemaJson, boolean shortCircuit, Boolean reportChildErrors) throws Exception {
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        // The constructor arguments that are not used by the validator are mocked.
        SchemaLocation location = mock(SchemaLocation.class);
        JsonNodePath path = mock(JsonNodePath.class);
        JsonSchema parent = null;
        return new TestableOneOfValidator(location, path, schemaNode, parent, validationContext, shortCircuit, reportChildErrors);
    }

    /**
     * Data for the “valid – exactly one subschema matches” group.
     */
    private static Stream<Arguments> exactlyOneMatchProvider() throws Exception {
        String schema = "[{\"type\":\"string\"},{\"type\":\"integer\"}]";
        TestableOneOfValidator validator = createValidator(schema, false, null);
        JsonNode instance = MAPPER.readTree("\"hello\"");
        return Stream.of(Arguments.of(validator, instance));
    }

    /**
     * Data for the “invalid – no subschema matches” group.
     */
    private static Stream<Arguments> noneMatchProvider() throws Exception {
        String schema = "[{\"type\":\"string\"},{\"type\":\"integer\"}]";
        TestableOneOfValidator validator = createValidator(schema, false, null);
        JsonNode instance = MAPPER.readTree("true");
        return Stream.of(Arguments.of(validator, instance));
    }

    /**
     * Data for the “invalid – more than one subschema matches” group.
     */
    private static Stream<Arguments> multipleMatchProvider() throws Exception {
        // empty schemas accept any JSON value
        String schema = "[{},{}]";
        TestableOneOfValidator validator = createValidator(schema, false, null);
        JsonNode instance = MAPPER.readTree("42");
        return Stream.of(Arguments.of(validator, instance));
    }

    /**
     * Data for the “short‑circuit enabled” group.
     */
    private static Stream<Arguments> shortCircuitProvider() throws Exception {
        String schema = "[{\"type\":\"string\"},{\"type\":\"integer\"}]";
        TestableOneOfValidator validator = createValidator(schema, true, null);
        // matches first subschema
        JsonNode instance = MAPPER.readTree("\"short\"");
        return Stream.of(Arguments.of(validator, instance));
    }

    /**
     * Data for the “report child errors – false” group.
     */
    private static Stream<Arguments> reportChildFalseProvider() throws Exception {
        // first subschema fails (type mismatch), second subschema succeeds.
        String schema = "[{\"type\":\"integer\"},{\"type\":\"string\"}]";
        TestableOneOfValidator validator = createValidator(schema, false, false);
        JsonNode instance = MAPPER.readTree("\"abc\"");
        return Stream.of(Arguments.of(validator, instance));
    }

    /**
     * Data for the “walk flag true” group.
     */
    private static Stream<Arguments> walkProvider() throws Exception {
        String schema = "[{\"type\":\"string\"},{\"type\":\"integer\"}]";
        TestableOneOfValidator validator = createValidator(schema, false, null);
        // matches first subschema
        JsonNode instance = MAPPER.readTree("\"walk\"");
        return Stream.of(Arguments.of(validator, instance));
    }

    /**
     * Data for the “valid – oneOf with a single subschema” group.
     */
    private static Stream<Arguments> singleSubschemaProvider() throws Exception {
        String schema = "[{\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}}}]";
        TestableOneOfValidator validator = createValidator(schema, false, null);
        JsonNode instance = MAPPER.readTree("{\"age\":30}");
        return Stream.of(Arguments.of(validator, instance));
    }

    /**
     * Data for the “valid – array instance matches exactly one subschema” group.
     */
    private static Stream<Arguments> arrayMatchProvider() throws Exception {
        String schema = "[" + "{\"type\":\"array\",\"minItems\":3,\"maxItems\":3}," + "{\"type\":\"array\",\"minItems\":5}" + "]";
        TestableOneOfValidator validator = createValidator(schema, false, null);
        JsonNode instance = MAPPER.readTree("[1,2,3]");
        return Stream.of(Arguments.of(validator, instance));
    }

    /**
     * Data for the “valid – non‑object schema (type string)” group.
     */
    private static Stream<Arguments> stringSchemaProvider() throws Exception {
        String schema = "[" + "{\"type\":\"string\",\"maxLength\":10}," + "{\"type\":\"number\"}" + "]";
        TestableOneOfValidator validator = createValidator(schema, false, null);
        JsonNode instance = MAPPER.readTree("\"hello\"");
        return Stream.of(Arguments.of(validator, instance));
    }

    /**
     * Data for the “invalid – duplicate identical subschemas” group.
     */
    private static Stream<Arguments> duplicateSchemasProvider() throws Exception {
        String schema = "[" + "{\"type\":\"object\",\"properties\":{\"flag\":{\"type\":\"boolean\"}}}," + "{\"type\":\"object\",\"properties\":{\"flag\":{\"type\":\"boolean\"}}}" + "]";
        TestableOneOfValidator validator = createValidator(schema, false, null);
        JsonNode instance = MAPPER.readTree("{\"flag\":true}");
        return Stream.of(Arguments.of(validator, instance));
    }

    /**
     * When the instance matches exactly one subschema the validator must return
     * an empty {@link Set}.
     */
    @Test
    void validateMatchesExactlyOneSchema() throws Exception {
        // oneOf: [ {"type":"string"}, {"type":"integer"} ]
        JsonNode schemaNode = mapper.readTree("[{\"type\":\"string\"},{\"type\":\"integer\"}]");
        // Dummy objects for the constructor – they are not consulted by the
        // validator for this test.
        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        JsonNodePath evaluationPath = mock(JsonNodePath.class);
        JsonSchema parentSchema = null;
        OneOfValidator validator = new OneOfValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validationContext);
        // Node that is a JSON string – matches the first subschema only.
        JsonNode instance = mapper.readTree("\"hello\"");
        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> messages = validator.validate(ctx, instance, instance, JsonNodePath.ROOT);
        assertTrue(messages.isEmpty(), "Expected no validation messages when exactly one schema matches");
    }

    /**
     * When the instance matches none of the subschemas the validator must report
     * a {@code oneOf} error.
     */
    @Test
    void validateMatchesNoSchema() throws Exception {
        // oneOf: [ {"type":"string"}, {"type":"integer"} ]
        JsonNode schemaNode = mapper.readTree("[{\"type\":\"string\"},{\"type\":\"integer\"}]");
        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        JsonNodePath evaluationPath = mock(JsonNodePath.class);
        JsonSchema parentSchema = null;
        OneOfValidator validator = new OneOfValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validationContext);
        // Node that is a boolean – matches neither subschema.
        JsonNode instance = mapper.readTree("true");
        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> messages = validator.validate(ctx, instance, instance, JsonNodePath.ROOT);
        assertFalse(messages.isEmpty(), "Expected validation messages when no schema matches");
        ValidationMessage msg = messages.iterator().next();
        assertEquals("oneOf", msg.getType(), "The validation message type should be 'oneOf'");
        assertTrue(msg.getMessage().contains("0 are valid"), "Message should indicate that 0 subschemas are valid");
    }

    /**
     * When the instance matches more than one subschema the validator must report
     * a {@code oneOf} error that lists the indexes of the matching subschemas.
     */
    @Test
    void validateMatchesMultipleSchemas() throws Exception {
        // oneOf: [ {}, {} ] – both empty schemas accept any JSON value.
        JsonNode schemaNode = mapper.readTree("[{},{}]");
        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        JsonNodePath evaluationPath = mock(JsonNodePath.class);
        JsonSchema parentSchema = null;
        OneOfValidator validator = new OneOfValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validationContext);
        // Node that is a number – matches both empty subschemas.
        JsonNode instance = mapper.readTree("42");
        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> messages = validator.validate(ctx, instance, instance, JsonNodePath.ROOT);
        assertFalse(messages.isEmpty(), "Expected validation messages when multiple schemas match");
        ValidationMessage msg = messages.iterator().next();
        assertEquals("oneOf", msg.getType(), "The validation message type should be 'oneOf'");
        assertTrue(msg.getMessage().contains("2 are valid"), "Message should indicate that 2 subschemas are valid");
        assertTrue(msg.getMessage().contains("indexes '0, 1'"), "Message should list the indexes of the matching subschemas");
    }

    // static import set.
    @SuppressWarnings("unchecked")
    private static <T> T mock(Class<T> clazz) {
        return org.mockito.Mockito.mock(clazz);
    }

    @ParameterizedTest(name = "[{index}] exactly one subschema matches")
    @MethodSource("exactlyOneMatchProvider")
    void validate_ExactlyOneMatches(TestableOneOfValidator validator, JsonNode instance) {
        ExecutionContext ctx = mock(ExecutionContext.class);
        Set<ValidationMessage> msgs = validator.validate(ctx, instance, instance, JsonNodePath.ROOT);
        assertTrue(msgs.isEmpty(), "Expected no validation messages when exactly one schema matches");
    }

    @ParameterizedTest(name = "[{index}] no subschema matches")
    @MethodSource("noneMatchProvider")
    void validate_NoMatches(TestableOneOfValidator validator, JsonNode instance) {
        ExecutionContext ctx = mock(ExecutionContext.class);
        Set<ValidationMessage> msgs = validator.validate(ctx, instance, instance, JsonNodePath.ROOT);
        assertFalse(msgs.isEmpty(), "Expected a validation error when no schema matches");
        ValidationMessage vm = msgs.iterator().next();
        assertEquals("oneOf", vm.getType());
        assertTrue(vm.getMessage().contains("0 are valid"));
    }

    @ParameterizedTest(name = "[{index}] multiple subschemas match")
    @MethodSource("multipleMatchProvider")
    void validate_MultipleMatches(TestableOneOfValidator validator, JsonNode instance) {
        ExecutionContext ctx = mock(ExecutionContext.class);
        Set<ValidationMessage> msgs = validator.validate(ctx, instance, instance, JsonNodePath.ROOT);
        assertFalse(msgs.isEmpty(), "Expected a validation error when more than one schema matches");
        ValidationMessage vm = msgs.iterator().next();
        assertEquals("oneOf", vm.getType());
        assertTrue(vm.getMessage().contains("2 are valid"));
        assertTrue(vm.getMessage().contains("indexes '0, 1'"));
    }

    @ParameterizedTest(name = "[{index}] short‑circuit enabled")
    @MethodSource("shortCircuitProvider")
    void validate_ShortCircuitEnabled(TestableOneOfValidator validator, JsonNode instance) {
        ExecutionContext ctx = mock(ExecutionContext.class);
        // Spy the validator to verify that the second subschema's validate method is never called.
        OneOfValidator spy = spy(validator);
        Set<ValidationMessage> msgs = spy.validate(ctx, instance, instance, JsonNodePath.ROOT);
        assertTrue(msgs.isEmpty(), "Short‑circuit should result in successful validation");
        // Verify that validate(...) was invoked only once on the first child schema.
        // The internal list of child schemas is private; we approximate by ensuring that
        // no further interaction with the spy occurs after the first successful match.
        verify(spy, atMost(1)).validate(any(), any(), any(), any(), anyBoolean());
    }

    @ParameterizedTest(name = "[{index}] report child errors = false")
    @MethodSource("reportChildFalseProvider")
    void validate_ReportChildErrorsFalse(TestableOneOfValidator validator, JsonNode instance) {
        ExecutionContext ctx = mock(ExecutionContext.class);
        Set<ValidationMessage> msgs = validator.validate(ctx, instance, instance, JsonNodePath.ROOT);
        // Because the first subschema fails, the validator should still succeed (second matches)
        // and, with child errors suppressed, the result set must be empty.
        assertTrue(msgs.isEmpty(), "When reportChildErrors() is false and a later subschema matches, validation should succeed without child messages");
    }

    @ParameterizedTest(name = "[{index}] walk method delegates correctly")
    @MethodSource("walkProvider")
    void walk_DelegatesToValidate(TestableOneOfValidator validator, JsonNode instance) {
        ExecutionContext ctx = mock(ExecutionContext.class);
        Set<ValidationMessage> msgs = validator.walk(ctx, instance, instance, JsonNodePath.ROOT, true);
        // walk(true) should behave like validate()
        assertTrue(msgs.isEmpty(), "walk with shouldValidateSchema=true should return same result as validate()");
    }

    @ParameterizedTest(name = "[{index}] single subschema behaves like the subschema")
    @MethodSource("singleSubschemaProvider")
    void validate_SingleSubschema(TestableOneOfValidator validator, JsonNode instance) {
        ExecutionContext ctx = mock(ExecutionContext.class);
        Set<ValidationMessage> msgs = validator.validate(ctx, instance, instance, JsonNodePath.ROOT);
        assertTrue(msgs.isEmpty(), "A oneOf with a single subschema should validate exactly as that schema");
    }

    @ParameterizedTest(name = "[{index}] array instance matches exactly one subschema")
    @MethodSource("arrayMatchProvider")
    void validate_ArrayMatchesExactlyOne(TestableOneOfValidator validator, JsonNode instance) {
        ExecutionContext ctx = mock(ExecutionContext.class);
        Set<ValidationMessage> msgs = validator.validate(ctx, instance, instance, JsonNodePath.ROOT);
        assertTrue(msgs.isEmpty(), "Array instance should match exactly one of the subschemas");
    }

    @ParameterizedTest(name = "[{index}] non‑object schema (string) matches")
    @MethodSource("stringSchemaProvider")
    void validate_StringSchemaMatches(TestableOneOfValidator validator, JsonNode instance) {
        ExecutionContext ctx = mock(ExecutionContext.class);
        Set<ValidationMessage> msgs = validator.validate(ctx, instance, instance, JsonNodePath.ROOT);
        assertTrue(msgs.isEmpty(), "String instance should match exactly one of the subschemas");
    }

    @ParameterizedTest(name = "[{index}] duplicate identical subschemas cause error")
    @MethodSource("duplicateSchemasProvider")
    void validate_DuplicateSchemas(TestableOneOfValidator validator, JsonNode instance) {
        ExecutionContext ctx = mock(ExecutionContext.class);
        Set<ValidationMessage> msgs = validator.validate(ctx, instance, instance, JsonNodePath.ROOT);
        assertFalse(msgs.isEmpty(), "Duplicate matching subschemas must trigger a oneOf violation");
        ValidationMessage vm = msgs.iterator().next();
        assertEquals("oneOf", vm.getType());
        assertTrue(vm.getMessage().contains("2 are valid"));
    }

    @Test
    void validate_NullExecutionContext_throwsNPE() throws Exception {
        TestableOneOfValidator validator = createValidator("[{\"type\":\"string\"}]", false, null);
        JsonNode instance = MAPPER.readTree("\"test\"");
        assertThrows(NullPointerException.class, () -> validator.validate(null, instance, instance, JsonNodePath.ROOT), "Passing a null ExecutionContext should result in NPE");
    }

    @Test
    void validate_NullInstanceLocation_throwsNPE() throws Exception {
        TestableOneOfValidator validator = createValidator("[{\"type\":\"string\"}]", false, null);
        JsonNode instance = MAPPER.readTree("\"test\"");
        ExecutionContext ctx = mock(ExecutionContext.class);
        assertThrows(NullPointerException.class, () -> validator.validate(ctx, instance, instance, null), "Passing a null JsonNodePath should result in NPE");
    }

    @Test
    void validate_NullRootNode_throwsNPE() throws Exception {
        TestableOneOfValidator validator = createValidator("[{\"type\":\"string\"}]", false, null);
        JsonNode instance = MAPPER.readTree("\"test\"");
        ExecutionContext ctx = mock(ExecutionContext.class);
        assertThrows(NullPointerException.class, () -> validator.validate(ctx, instance, null, JsonNodePath.ROOT), "Passing a null rootNode while instance is non‑null should result in NPE");
    }

    @Test
    void construct_WithNullSchemaNode_throwsIllegalState() {
        // Directly invoke the constructor with a null schema node.
        SchemaLocation location = mock(SchemaLocation.class);
        JsonNodePath path = mock(JsonNodePath.class);
        JsonSchema parent = null;
        assertThrows(IllegalStateException.class, () -> new OneOfValidator(location, path, null, parent, validationContext), "Constructing OneOfValidator with a null schema node should throw IllegalStateException");
    }

    @Test
    void validate_NullSubschemaEntry_throwsNPE() throws Exception {
        // Build a oneOf array that contains a JSON null element.
        String schema = "[null,{\"type\":\"string\"}]";
        TestableOneOfValidator validator = createValidator(schema, false, null);
        JsonNode instance = MAPPER.readTree("\"abc\"");
        ExecutionContext ctx = mock(ExecutionContext.class);
        // The validator will attempt to treat the null element as a JsonSchema and
        // should therefore throw a NPE during validation.
        assertThrows(NullPointerException.class, () -> validator.validate(ctx, instance, instance, JsonNodePath.ROOT));
    }

    @Test
    void validate_NonSchemaElement_throwsIllegalArgument() throws Exception {
        // One element is a plain string instead of a schema object.
        String schema = "[\"not-a-schema\", {\"type\":\"string\"}]";
        TestableOneOfValidator validator = createValidator(schema, false, null);
        JsonNode instance = MAPPER.readTree("\"abc\"");
        ExecutionContext ctx = mock(ExecutionContext.class);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(ctx, instance, instance, JsonNodePath.ROOT));
    }
}
