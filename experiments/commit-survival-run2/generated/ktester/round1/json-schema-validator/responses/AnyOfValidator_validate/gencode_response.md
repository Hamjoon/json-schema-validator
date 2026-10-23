```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.ValidationContext.DiscriminatorContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnyOfValidator_validate_Test {

    /** shared objects for the whole test suite */
    private static ObjectMapper mapper;
    private static SchemaValidatorsConfig config;

    /** objects created for each test */
    @Mock private URIFactory uriFactory;
    @Mock private URNFactory urnFactory;
    @Mock private JsonMetaSchema metaSchema;
    @Mock private JsonSchemaFactory jsonSchemaFactory;

    /** Mocks required for the execution path */
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private ValidatorState validatorState;
    @Mock private CollectorContext.Scope mockScope;   // used for both enter/exit scopes

    /** the validator under test – created once per test */
    private AnyOfValidator anyOfValidator;

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
        config = new SchemaValidatorsConfig();               // we will toggle discriminator flag per test
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // -----------------------------------------------------------------
        // 1. Build a *real* ValidationContext – only the config part matters.
        // -----------------------------------------------------------------
        ValidationContext validationContext = new ValidationContext(
                uriFactory,
                urnFactory,
                metaSchema,
                jsonSchemaFactory,
                config);

        // -----------------------------------------------------------------
        // 2. Create the validator under test.  The constructor will try to
        //    build its internal 'schemas' list – we will replace it later
        //    with the mocks that belong to each test case.
        // -----------------------------------------------------------------
        // anyOf node with a single empty object – content does not matter
        anyOfValidator = new AnyOfValidator(
                "anyOfTest",
                mapper.createArrayNode().add(mapper.createObjectNode()),
                null,
                validationContext);

        // -----------------------------------------------------------------
        // 3. Prepare generic mocks for ExecutionContext / CollectorContext /
        //    ValidatorState.  Individual tests can further customise these.
        // -----------------------------------------------------------------
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(collectorContext.enterDynamicScope()).thenReturn(mockScope);
        when(collectorContext.exitDynamicScope()).thenReturn(mockScope);
        // these mock methods are void – just ignore
        doNothing().when(mockScope).mergeWith(any());
        doNothing().when(validatorState).setMatchedNode(anyBoolean());

        // By default the state reports “no matched node” and walk disabled.
        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);
    }

    /* --------------------------------------------------------------------- *
     * Helper – replace the private 'schemas' list with a list of mocks.
     * --------------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private void injectSchemas(JsonSchema... schemas) throws Exception {
        Field f = AnyOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        List<JsonSchema> list = new ArrayList<>(Arrays.asList(schemas));
        f.set(anyOfValidator, list);
    }

    /* --------------------------------------------------------------------- *
     * Helper – inject a mock DiscriminatorContext (used when the config flag
     * is true).  The field is private, so we use reflection.
     * --------------------------------------------------------------------- */
    private void injectDiscriminatorContext(DiscriminatorContext ctx) throws Exception {
        Field f = AnyOfValidator.class.getDeclaredField("discriminatorContext");
        f.setAccessible(true);
        f.set(anyOfValidator, ctx);
    }

    /* --------------------------------------------------------------------- *
     * 1. OpenAPI3StyleDiscriminators disabled – early success (no type
     *    validator, empty error set).  The validator must return an empty
     *    set immediately.
     * --------------------------------------------------------------------- */
    @Test
    void validate_whenNoTypeValidatorAndEmptyErrors_returnsEmptySet() throws Exception {
        config.setOpenAPI3StyleDiscriminators(false); // explicit – default is false

        JsonSchema mockSchema = mock(JsonSchema.class);
        when(mockSchema.hasTypeValidator()).thenReturn(false);
        when(mockSchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        injectSchemas(mockSchema);

        JsonNode node = mapper.createObjectNode();
        JsonNode root = mapper.createObjectNode();
        String at = "$.foo";

        Set<ValidationMessage> result = anyOfValidator.validate(executionContext, node, root, at);
        assertTrue(result.isEmpty(), "Result must be empty when a subschema validates without errors");
    }

    /* --------------------------------------------------------------------- *
     * 2. Type validator mismatch – schema expects INTEGER but node is STRING.
     *    The validator should add a type‑mismatch message and return it.
     * --------------------------------------------------------------------- */
    @Test
    void validate_whenTypeValidatorMismatch_returnsTypeError() throws Exception {
        config.setOpenAPI3StyleDiscriminators(false);

        // ---- mock TypeValidator -------------------------------------------------
        TypeValidator typeValidator = mock(TypeValidator.class);
        // any non‑UNION type
        when(typeValidator.getSchemaType()).thenReturn(JsonType.INTEGER);
        // node type does NOT match
        when(typeValidator.equalsToSchemaType(any())).thenReturn(false);

        // ---- mock subschema ----------------------------------------------------
        JsonSchema mockSchema = mock(JsonSchema.class);
        when(mockSchema.hasTypeValidator()).thenReturn(true);
        when(mockSchema.getTypeValidator()).thenReturn(typeValidator);
        // validate() will NOT be called because of early continue

        injectSchemas(mockSchema);

        // a textual node – the actual content does not matter
        JsonNode node = mapper.readTree("\"string\"");
        JsonNode root = mapper.createObjectNode();
        String at = "$.bar";

        Set<ValidationMessage> result = anyOfValidator.validate(executionContext, node, root, at);
        assertEquals(1, result.size(), "Exactly one type‑mismatch ValidationMessage should be returned");
    }

    /* --------------------------------------------------------------------- *
     * 3. OpenAPI3StyleDiscriminators enabled – discriminator match found
     *    *with* errors.  The returned set must contain the schema errors plus
     *    the DISCRIMINATOR_REMARK message.
     * --------------------------------------------------------------------- */
    @Test
    void validate_whenDiscriminatorMatchFoundWithErrors_returnsErrorsPlusRemark() throws Exception {
        config.setOpenAPI3StyleDiscriminators(true);

        // ---- mock DiscriminatorContext -----------------------------------------
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        when(discCtx.isDiscriminatorMatchFound()).thenReturn(true);
        injectDiscriminatorContext(discCtx);

        // ---- mock schema that returns a non‑empty error set --------------------
        ValidationMessage schemaError = mock(ValidationMessage.class);
        JsonSchema mockSchema = mock(JsonSchema.class);
        when(mockSchema.hasTypeValidator()).thenReturn(false);
        when(mockSchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Set.of(schemaError));

        injectSchemas(mockSchema);

        JsonNode node = mapper.createObjectNode();
        JsonNode root = mapper.createObjectNode();
        String at = "#/anyOf/0";

        Set<ValidationMessage> result = anyOfValidator.validate(executionContext, node, root, at);
        assertEquals(2, result.size(), "Should contain the original error and the discriminator remark");
        // the remark is generated via BaseJsonValidator.buildValidationMessage – we verify that at
        // least one of the messages ends with the known suffix.
        boolean remarkFound = result.stream()
                .anyMatch(v -> v.getMessage() != null && v.getMessage().endsWith(AnyOfValidator.DISCRIMINATOR_REMARK));
        assertTrue(remarkFound, "Result must contain the discriminator‑remark message");
    }

    /* --------------------------------------------------------------------- *
     * 4. OpenAPI3StyleDiscriminators enabled – discriminator match found
     *    *without* errors.  The method should return an empty set.
     * --------------------------------------------------------------------- */
    @Test
    void validate_whenDiscriminatorMatchFoundWithoutErrors_returnsEmptySet() throws Exception {
        config.setOpenAPI3StyleDiscriminators(true);

        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        when(discCtx.isDiscriminatorMatchFound()).thenReturn(true);
        injectDiscriminatorContext(discCtx);

        JsonSchema mockSchema = mock(JsonSchema.class);
        when(mockSchema.hasTypeValidator()).thenReturn(false);
        when(mockSchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        injectSchemas(mockSchema);

        JsonNode node = mapper.createObjectNode();
        JsonNode root = mapper.createObjectNode();
        String at = "#/anyOf/1";

        Set<ValidationMessage> result = anyOfValidator.validate(executionContext, node, root, at);
        assertTrue(result.isEmpty(), "When discriminator match succeeds without errors, result must be empty");
    }

    /* --------------------------------------------------------------------- *
     * 5. After loop – a valid subschema clears required‑type errors.
     *    First subschema returns a REQUIRED‑type error, second validates
     *    successfully and sets the matched‑node flag.
     * --------------------------------------------------------------------- */
    @Test
    void validate_whenValidSubschemaClearsRequiredErrors_returnsEmptySet() throws Exception {
        config.setOpenAPI3StyleDiscriminators(false);

        // ---- first subschema – produces a REQUIRED error -----------------------
        ValidationMessage requiredErr = mock(ValidationMessage