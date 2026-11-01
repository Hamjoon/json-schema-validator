package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.net.URI;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.networknt.schema.ValidationContext.DiscriminatorContext;
import java.lang.reflect.Field;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.urn.URNFactory;

@ExtendWith(MockitoExtension.class)
class AnyOfValidator_validate_Test {

    /**
     * shared objects for the whole test suite
     */
    private static ObjectMapper mapper;

    private static SchemaValidatorsConfig config;

    /**
     * objects created for each test
     */
    @Mock
    private URIFactory uriFactory;

    @Mock
    private URNFactory urnFactory;

    @Mock
    private JsonMetaSchema metaSchema;

    @Mock
    private JsonSchemaFactory jsonSchemaFactory;

    private ValidationContext validationContext;

    private ExecutionContext executionContext;

    /**
     * the validator under test
     */
    private AnyOfValidator anyOfValidator;

    @Mock
    private CollectorContext collectorContext;

    @Mock
    private ValidatorState validatorState;

    // used for both enter/exit scopes
    @Mock
    private CollectorContext.Scope mockScope;

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
        // we will toggle discriminator flag per test
        config = new SchemaValidatorsConfig();
    }

    @BeforeEach
    void setupBeforeEach() {
        // -----------------------------------------------------------------
        // 1. Build a real ValidationContext – we only need the config part,
        //    all other collaborators are mocked because they are not touched
        //    by the AnyOfValidator.validate logic we are exercising.
        // -----------------------------------------------------------------
        validationContext = new ValidationContext(uriFactory, urnFactory, metaSchema, jsonSchemaFactory, config);
        // -----------------------------------------------------------------
        // 2. ExecutionContext – a simple instance with a fresh CollectorContext.
        // -----------------------------------------------------------------
        // uses default CollectorContext
        executionContext = new ExecutionContext();
        // -----------------------------------------------------------------
        // 3. Prepare a dummy ANY‑OF schema node (an array with two subschemas).
        // -----------------------------------------------------------------
        ArrayNode anyOfArray = mapper.createArrayNode();
        // {} – first subschema
        anyOfArray.add(mapper.createObjectNode());
        // {} – second subschema
        anyOfArray.add(mapper.createObjectNode());
        // -----------------------------------------------------------------
        // 4. Mock the ValidationContext#newSchema method so that it returns
        //    our own mock JsonSchema objects (one per array element).
        // -----------------------------------------------------------------
        JsonSchema subSchema1 = mock(JsonSchema.class);
        JsonSchema subSchema2 = mock(JsonSchema.class);
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(// first call → first subschema
        subSchema1).thenReturn(// second call → second subschema
        subSchema2);
        // -----------------------------------------------------------------
        // 5. Instantiate the validator under test.
        // -----------------------------------------------------------------
        anyOfValidator = new AnyOfValidator(// schemaPath (arbitrary)
        "anyOfTest", // the ANY‑OF node
        anyOfArray, // parentSchema – not needed for these tests
        null, validationContext);
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
     * Test case 1 – one subschema validates successfully, the other returns
     * errors. According to the implementation, as soon as a subschema validates
     * without errors the validator clears the accumulated errors and returns
     * an empty set.
     * --------------------------------------------------------------------- */
    @Test
    void validate_whenOneSubschemaValid_returnsEmptySet() {
        // -----------------------------------------------------------------
        // Arrange – first subschema returns no errors, second returns one.
        // -----------------------------------------------------------------
        JsonSchema subSchema1 = validationContext.newSchema("anyOfTest/0", null, null);
        JsonSchema subSchema2 = validationContext.newSchema("anyOfTest/1", null, null);
        when(subSchema1.hasTypeValidator()).thenReturn(false);
        when(subSchema2.hasTypeValidator()).thenReturn(false);
        // first subschema – successful validation
        when(subSchema1.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
        // second subschema – produces a single ValidationMessage
        ValidationMessage error = mock(ValidationMessage.class);
        when(subSchema2.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(error));
        // arbitrary instance node
        JsonNode node = mapper.createObjectNode();
        // arbitrary root node
        JsonNode root = mapper.createObjectNode();
        String at = "$";
        // -----------------------------------------------------------------
        // Act
        // -----------------------------------------------------------------
        Set<ValidationMessage> result = anyOfValidator.validate(executionContext, node, root, at);
        // -----------------------------------------------------------------
        // Assert – because the first subschema succeeded, result must be empty.
        // -----------------------------------------------------------------
        assertTrue(result.isEmpty(), "Result should be empty when any subschema validates");
    }

    /* --------------------------------------------------------------------- *
     * Test case 2 – all subschemas return errors. The validator must aggregate
     * those errors (excluding “required” errors when appropriate) and return
     * them unchanged.
     * --------------------------------------------------------------------- */
    @Test
    void validate_whenAllSubschemasInvalid_aggregatesErrors() {
        // -----------------------------------------------------------------
        // Arrange – both subschemas return distinct ValidationMessage objects.
        // -----------------------------------------------------------------
        JsonSchema subSchema1 = validationContext.newSchema("anyOfTest/0", null, null);
        JsonSchema subSchema2 = validationContext.newSchema("anyOfTest/1", null, null);
        when(subSchema1.hasTypeValidator()).thenReturn(false);
        when(subSchema2.hasTypeValidator()).thenReturn(false);
        ValidationMessage err1 = mock(ValidationMessage.class);
        ValidationMessage err2 = mock(ValidationMessage.class);
        when(err1.getType()).thenReturn("type1");
        when(err2.getType()).thenReturn("type2");
        when(subSchema1.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(err1));
        when(subSchema2.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(err2));
        JsonNode node = mapper.createObjectNode();
        JsonNode root = mapper.createObjectNode();
        String at = "$";
        // -----------------------------------------------------------------
        // Act
        // -----------------------------------------------------------------
        Set<ValidationMessage> result = anyOfValidator.validate(executionContext, node, root, at);
        // -----------------------------------------------------------------
        // Assert – both errors must be present.
        // -----------------------------------------------------------------
        assertEquals(2, result.size(), "Both validation errors should be aggregated");
        assertTrue(result.containsAll(Set.of(err1, err2)), "Result set must contain the two mocked errors");
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
     * 1. OpenAPI3StyleDiscriminators disabled – early success (no type
     *    validator, empty error set).  The validator must return an empty
     *    set immediately.
     * --------------------------------------------------------------------- */
    @Test
    void validate_whenNoTypeValidatorAndEmptyErrors_returnsEmptySet() throws Exception {
        // explicit – default is false
        config.setOpenAPI3StyleDiscriminators(false);
        JsonSchema mockSchema = mock(JsonSchema.class);
        when(mockSchema.hasTypeValidator()).thenReturn(false);
        when(mockSchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
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
        when(mockSchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(schemaError));
        injectSchemas(mockSchema);
        JsonNode node = mapper.createObjectNode();
        JsonNode root = mapper.createObjectNode();
        String at = "#/anyOf/0";
        Set<ValidationMessage> result = anyOfValidator.validate(executionContext, node, root, at);
        assertEquals(2, result.size(), "Should contain the original error and the discriminator remark");
        // the remark is generated via BaseJsonValidator.buildValidationMessage – we verify that at
        // least one of the messages ends with the known suffix.
        boolean remarkFound = result.stream().anyMatch(v -> v.getMessage() != null && v.getMessage().endsWith(AnyOfValidator.DISCRIMINATOR_REMARK));
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
        when(mockSchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
        injectSchemas(mockSchema);
        JsonNode node = mapper.createObjectNode();
        JsonNode root = mapper.createObjectNode();
        String at = "#/anyOf/1";
        Set<ValidationMessage> result = anyOfValidator.validate(executionContext, node, root, at);
        assertTrue(result.isEmpty(), "When discriminator match succeeds without errors, result must be empty");
    }
}