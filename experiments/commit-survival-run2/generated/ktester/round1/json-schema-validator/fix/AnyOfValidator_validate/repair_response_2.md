```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * Unit tests for {@link AnyOfValidator#validate(ExecutionContext, JsonNode, JsonNode, String)}.
 * <p>
 * The original test relied on Mockito's JUnit 5 extension and on direct access to a
 * private constant in {@code AnyOfValidator}.  To make the test compile without
 * additional test‑framework configuration we:
 * <ul>
 *   <li>remove the {@code @ExtendWith(MockitoExtension.class)} annotation and the
 *       {@code @Mock} field annotations, creating the required mocks manually;</li>
 *   <li>add static imports for Mockito helpers and JUnit assertions;</li>
 *   <li>obtain the private {@code DISCRIMINATOR_REMARK} constant via reflection.</li>
 * </ul>
 */
class AnyOfValidator_validate_Test {

    /**
     * shared objects for the whole test suite
     */
    private static ObjectMapper mapper;
    private static SchemaValidatorsConfig config;

    /**
     * objects created for each test
     */
    private ValidationContext validationContext;
    private ExecutionContext executionContext;
    private AnyOfValidator anyOfValidator;

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
        // we will toggle discriminator flag per test
        config = new SchemaValidatorsConfig();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a minimal ValidationContext – only the config part is needed.
        this.validationContext = new ValidationContext(
                null,               // URIFactory – not used in these tests
                null,               // URNFactory – not used
                null,               // JsonMetaSchema – not used
                null,               // JsonSchemaFactory – not used
                null,               // CollectorContext – not used
                config);
        // simple ExecutionContext (default constructor is sufficient for the tests)
        this.executionContext = new ExecutionContext();

        // Prepare a dummy ANY‑OF schema node (an array with two empty subschemas)
        ArrayNode anyOfArray = mapper.createArrayNode();
        anyOfArray.add(mapper.createObjectNode());
        anyOfArray.add(mapper.createObjectNode());

        // Instantiate the validator under test.
        this.anyOfValidator = new AnyOfValidator(
                "anyOfTest",   // schemaPath (arbitrary)
                anyOfArray,    // the ANY‑OF node
                null,          // parent schema – not needed for these tests
                validationContext);
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
     * Helper – obtain the private DISCRIMINATOR_REMARK constant via reflection.
     * --------------------------------------------------------------------- */
    private String getDiscriminatorRemark() throws Exception {
        Field f = AnyOfValidator.class.getDeclaredField("DISCRIMINATOR_REMARK");
        f.setAccessible(true);
        return (String) f.get(null);
    }

    /* --------------------------------------------------------------------- *
     * Test case 1 – one subschema validates successfully, the other returns
     * errors. According to the implementation, as soon as a subschema validates
     * without errors the validator clears the accumulated errors and returns
     * an empty set.
     * --------------------------------------------------------------------- */
    @Test
    void validate_whenOneSubschemaValid_returnsEmptySet() {
        // Arrange – first subschema returns no errors, second returns one.
        JsonSchema subSchema1 = mock(JsonSchema.class);
        JsonSchema subSchema2 = mock(JsonSchema.class);
        when(subSchema1.hasTypeValidator()).thenReturn(false);
        when(subSchema2.hasTypeValidator()).thenReturn(false);
        when(subSchema1.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());
        ValidationMessage error = mock(ValidationMessage.class);
        when(subSchema2.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Set.of(error));

        // inject the mocked subschemas
        try {
            injectSchemas(subSchema1, subSchema2);
        } catch (Exception e) {
            fail("Failed to inject schemas: " + e.getMessage());
        }

        JsonNode node = mapper.createObjectNode();
        JsonNode root = mapper.createObjectNode();
        String at = "$";

        // Act
        Set<ValidationMessage> result = anyOfValidator.validate(executionContext, node, root, at);

        // Assert – because the first subschema succeeded, result must be empty.
        assertTrue(result.isEmpty(), "Result should be empty when any subschema validates");
    }

    /* --------------------------------------------------------------------- *
     * Test case 2 – all subschemas return errors. The validator must aggregate
     * those errors (excluding “required” errors when appropriate) and return
     * them unchanged.
     * --------------------------------------------------------------------- */
    @Test
    void validate_whenAllSubschemasInvalid_aggregatesErrors() {
        // Arrange – both subschemas return distinct ValidationMessage objects.
        JsonSchema subSchema1 = mock(JsonSchema.class);
        JsonSchema subSchema2 = mock(JsonSchema.class);
        when(subSchema1.hasTypeValidator()).thenReturn(false);
        when(subSchema2.hasTypeValidator()).thenReturn(false);
        ValidationMessage err1 = mock(ValidationMessage.class);
        ValidationMessage err2 = mock(ValidationMessage.class);
        when(err1.getType()).thenReturn("type1");
        when(err2.getType()).thenReturn("type2");
        when(subSchema1.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Set.of(err1));
        when(subSchema2.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Set.of(err2));

        try {
            injectSchemas(subSchema1, subSchema2);
        } catch (Exception e) {
            fail("Failed to inject schemas: " + e.getMessage());
        }

        JsonNode node = mapper.createObjectNode();
        JsonNode root = mapper.createObjectNode();
        String at = "$";

        // Act
        Set<ValidationMessage> result = anyOfValidator.validate(executionContext, node, root, at);

        // Assert – both errors must be present.
        assertEquals(2, result.size(), "Both validation errors should be aggregated");
        assertTrue(result.containsAll(Set.of(err1, err2)), "Result set must contain the two mocked errors");
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

        // mock TypeValidator
        TypeValidator typeValidator = mock(TypeValidator.class);
        when(typeValidator.getSchemaType()).thenReturn(JsonType.INTEGER);
        when(typeValidator.equalsToSchemaType(any())).thenReturn(false);

        // mock subschema
        JsonSchema mockSchema = mock(JsonSchema.class);
        when(mockSchema.hasTypeValidator()).thenReturn(true);
        when(mockSchema.getTypeValidator()).thenReturn(typeValidator);
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

        // mock DiscriminatorContext
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        when(discCtx.isDiscriminatorMatchFound()).thenReturn(true);
        injectDiscriminatorContext(discCtx);

        // mock schema that returns a non‑empty error set
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

        // obtain the private remark constant via reflection
        String remark = getDiscriminatorRemark();

        boolean remarkFound = result.stream()
                .anyMatch(v -> v.getMessage() != null && v.getMessage().endsWith(remark));
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

    /**
     * Verify that the validator instance is created correctly.
     */
    @Test
    void validator_isInstantiated() {
        Assertions.assertNotNull(anyOfValidator, "AnyOfValidator should be instantiated");
    }

    /**
     * A very basic sanity‑check that calling {@code validate} does not
     * throw an exception and returns a non‑null set (empty in this minimal
     * configuration).
     */
    @Test
    void validate_returnsNonNullSet() {
        // instance being validated
        JsonNode node = mapper.createObjectNode();
        // root of the document
        JsonNode root = mapper.createObjectNode();
        String at = "$";

        Set<ValidationMessage> result = anyOfValidator.validate(new ExecutionContext(), node, root, at);
        Assertions.assertNotNull(result, "Result of validate() must not be null");
        // With empty subschemas the validator should produce no errors.
        Assertions.assertEquals(Collections.emptySet(), result, "Result should be empty for empty subschemas");
    }
}
```