package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link AnyOfValidator}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal validation where one subschema succeeds.</li>
 *   <li>Validation when a subschema’s {@link TypeValidator} rejects the node.</li>
 *   <li>OpenAPI‑3 style discriminator handling (both match‑found and no‑match cases).</li>
 *   <li>The {@code walk} method’s behaviour when validation is disabled.</li>
 *   <li>The {@code preloadJsonSchema} method delegating to the base class.</li>
 * </ul>
 *
 * All heavy collaborators are mocked with Mockito so the tests focus on the
 * logic inside {@code AnyOfValidator} without requiring the full schema engine.
 */
@RunWith(MockitoJUnitRunner.class)
public class AnyOfValidatorTest {

    // -------------------------------------------------------------------------
    // Common test fixtures
    // -------------------------------------------------------------------------

    @Mock
    private ValidationContext validationContext;

    @Mock
    private ValidationContext.Config config;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private CollectorContext collectorContext;

    @Mock
    private ValidatorState validatorState;

    @Mock
    private Scope dummyScope; // used for dynamic scopes

    @Mock
    private JsonSchema schema1;

    @Mock
    private JsonSchema schema2;

    @Mock
    private JsonSchema schema3; // used for discriminator tests

    @Mock
    private TypeValidator typeValidator;

    // a simple JSON node used as the instance to validate
    private JsonNode instanceNode;

    // a dummy root node (not used by the validator logic we test)
    private JsonNode rootNode;

    // The validator under test – created in each test method to inject a fresh
    // list of subschemas.
    private AnyOfValidator anyOfValidator;

    @Before
    public void setUp() {
        // -----------------------------------------------------------------
        // Common mock behaviour
        // -----------------------------------------------------------------
        when(validationContext.getConfig()).thenReturn(config);
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(collectorContext.enterDynamicScope()).thenReturn(dummyScope);
        when(collectorContext.exitDynamicScope()).thenReturn(dummyScope);

        // By default the validator is not in walk‑enabled mode.
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);

        // Simple JSON primitives for the instance and root.
        instanceNode = JsonNodeFactory.instance.textNode("sample");
        rootNode = JsonNodeFactory.instance.objectNode();

        // -----------------------------------------------------------------
        // Helper that builds an AnyOfValidator with the supplied subschemas.
        // -----------------------------------------------------------------
    }

    /**
     * Utility: creates an {@link AnyOfValidator} that contains the given
     * {@link JsonSchema} mocks as its subschemas.
     */
    private AnyOfValidator buildValidator(JsonSchema... schemas) {
        // Build a dummy schema node (an array) whose size matches the number of
        // supplied schema mocks. The content of the array is irrelevant because
        // we stub {@code validationContext.newSchema(...)} to return our mocks.
        ArrayNode schemaNode = JsonNodeFactory.instance.arrayNode();
        for (int i = 0; i < schemas.length; i++) {
            schemaNode.addObject(); // placeholder element
        }

        // Stub the factory method that creates the subschemas.
        for (int i = 0; i < schemas.length; i++) {
            when(validationContext.newSchema(anyString(), eq(schemaNode.get(i)), any()))
                    .thenReturn(schemas[i]);
        }

        // Build the validator.
        AnyOfValidator validator = new AnyOfValidator("anyOf", schemaNode, mock(JsonSchema.class), validationContext);
        return validator;
    }

    // -------------------------------------------------------------------------
    // 1. Normal validation – one subschema succeeds, others fail.
    // -------------------------------------------------------------------------

    @Test
    public void validate_ShouldReturnEmptySet_WhenAtLeastOneSubschemaValid() {
        // -----------------------------------------------------------------
        // Arrange
        // -----------------------------------------------------------------
        // schema1 succeeds (no errors)
        when(schema1.hasTypeValidator()).thenReturn(false);
        when(schema1.validate(executionContext, instanceNode, rootNode, "$")).thenReturn(Collections.emptySet());

        // schema2 fails (returns a required‑field error)
        ValidationMessage error = ValidationMessage.builder()
                .type(ValidatorTypeCode.REQUIRED.getValue())
                .path("$")
                .message("required property missing")
                .build();
        when(schema2.hasTypeValidator()).thenReturn(false);
        when(schema2.validate(executionContext, instanceNode, rootNode, "$"))
                .thenReturn(Collections.singleton(error));

        anyOfValidator = buildValidator(schema1, schema2);

        // OpenAPI‑3 style discriminators are disabled for this test.
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);

        // -----------------------------------------------------------------
        // Act
        // -----------------------------------------------------------------
        Set<ValidationMessage> result = anyOfValidator.validate(executionContext, instanceNode, rootNode, "$");

        // -----------------------------------------------------------------
        // Assert
        // -----------------------------------------------------------------
        // Because schema1 succeeded, all errors must be cleared and the
        // result set must be empty.
        assertTrue("Result should be empty when at least one subschema validates", result.isEmpty());

        // Verify that both subschemas were invoked.
        verify(schema1).validate(executionContext, instanceNode, rootNode, "$");
        verify(schema2).validate(executionContext, instanceNode, rootNode, "$");
    }

    // -------------------------------------------------------------------------
    // 2. TypeValidator mismatch – the validator should add a type‑error message.
    // -------------------------------------------------------------------------

    @Test
    public void validate_ShouldAddTypeError_WhenSubschemaTypeValidatorRejectsNode() {
        // -----------------------------------------------------------------
        // Arrange
        // -----------------------------------------------------------------
        // schema1 has a TypeValidator that expects a NUMBER, but the instance is a STRING.
        when(schema1.hasTypeValidator()).thenReturn(true);
        when(schema1.getTypeValidator()).thenReturn(typeValidator);
        when(typeValidator.getSchemaType()).thenReturn(JsonType.NUMBER);
        // The type does NOT match the node.
        when(typeValidator.equalsToSchemaType(instanceNode)).thenReturn(false);

        // The validator builds a message containing the expected type name.
        anyOfValidator = buildValidator(schema1);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);

        // -----------------------------------------------------------------
        // Act
        // -----------------------------------------------------------------
        Set<ValidationMessage> result = anyOfValidator.validate(executionContext, instanceNode, rootNode, "$");

        // -----------------------------------------------------------------
        // Assert
        // -----------------------------------------------------------------
        assertEquals("Exactly one validation message should be produced", 1, result.size());
        ValidationMessage msg = result.iterator().next();
        assertEquals("The type name in the message must match the expected schema type",
                JsonType.NUMBER.toString(), msg.getMessage());

        // Ensure that the schema’s own validation logic was never called because
        // the type check short‑circuits it.
        verify(schema1, never()).validate(any(), any(), any(), anyString());
    }

    // -------------------------------------------------------------------------
    // 3. OpenAPI‑3 discriminator – match found with errors.
    // -------------------------------------------------------------------------

    @Test
    public void validate_ShouldAppendDiscriminatorRemark_WhenDiscriminatorMatchFoundButSubschemaHasErrors() {
        // -----------------------------------------------------------------
        // Arrange
        // -----------------------------------------------------------------
        // Enable discriminator handling.
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);

        // Build a discriminator context that pretends a match has been found.
        ValidationContext.DiscriminatorContext discriminatorContext = new ValidationContext.DiscriminatorContext();
        discriminatorContext.setDiscriminatorMatchFound(true);
        // Force the validator to use this context (the constructor creates a new
        // one, so we replace it via reflection for the test).
        anyOfValidator = buildValidator(schema3);
        // Inject the prepared context via reflection (since the field is final).
        try {
            java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("discriminatorContext");
            f.setAccessible(true);
            f.set(anyOfValidator, discriminatorContext);
        } catch (Exception e) {
            fail("Failed to inject discriminator context: " + e);
        }

        // schema3 validates and returns a non‑empty error set.
        ValidationMessage subError = ValidationMessage.builder()
                .type(ValidatorTypeCode.MINIMUM.getValue())
                .path("$")
                .message("value too small")
                .build();
        when(schema3.hasTypeValidator()).thenReturn(false);
        when(schema3.validate(executionContext, instanceNode, rootNode, "$"))
                .thenReturn(Collections.singleton(subError));

        // -----------------------------------------------------------------
        // Act
        // -----------------------------------------------------------------
        Set<ValidationMessage> result = anyOfValidator.validate(executionContext, instanceNode, rootNode, "$");

        // -----------------------------------------------------------------
        // Assert
        // -----------------------------------------------------------------
        // The result must contain the original sub‑error plus the discriminator remark.
        assertEquals("Two messages expected (sub‑error + discriminator remark)", 2, result.size());

        boolean foundRemark = result.stream()
                .anyMatch(m -> m.getMessage().contains(AnyOfValidator.DISCRIMINATOR_REMARK));
        assertTrue("Discriminator remark must be present", foundRemark);
    }

    // -------------------------------------------------------------------------
    // 4. walk() when validation is disabled – should delegate walk() to each subschema.
    // -------------------------------------------------------------------------

    @Test
    public void walk_ShouldDelegateToSubschemas_WhenShouldValidateSchemaIsFalse() {
        // -----------------------------------------------------------------
        // Arrange
        // -----------------------------------------------------------------
        anyOfValidator = buildValidator(schema1, schema2);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);

        // The subschemas’ walk() method should be called with shouldValidate = false.
        when(schema1.walk(executionContext, instanceNode, rootNode, "$", false)).thenReturn(new LinkedHashSet<>());
        when(schema2.walk(executionContext, instanceNode, rootNode, "$", false)).thenReturn(new LinkedHashSet<>());

        // -----------------------------------------------------------------
        // Act
        // -----------------------------------------------------------------
        Set<ValidationMessage> result = anyOfValidator.walk(executionContext, instanceNode, rootNode, "$", false);

        // -----------------------------------------------------------------
        // Assert
        // -----------------------------------------------------------------
        // walk() must return an empty set (the validator does not collect errors when it only walks).
        assertTrue(result.isEmpty());

        // Verify that each subschema received the walk call with the correct flag.
        verify(schema1).walk(executionContext, instanceNode, rootNode, "$", false);
        verify(schema2).walk(executionContext, instanceNode, rootNode, "$", false);
    }

    // -------------------------------------------------------------------------
    // 5. preloadJsonSchema() – ensure the base‑class helper is invoked.
    // -------------------------------------------------------------------------

    @Test
    public void preloadJsonSchema_ShouldInvokeBaseClassPreloadForAllSubschemas() {
        // -----------------------------------------------------------------
        // Arrange
        // -----------------------------------------------------------------
        // We create a spy of AnyOfValidator so we can verify the protected call.
        anyOfValidator = Mockito.spy(buildValidator(schema1, schema2));
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);

        // -----------------------------------------------------------------
        // Act
        // -----------------------------------------------------------------
        anyOfValidator.preloadJsonSchema();

        // -----------------------------------------------------------------
        // Assert
        // -----------------------------------------------------------------
        // The protected method preloadJsonSchemas(List) is defined in BaseJsonValidator.
        // Using a spy we can verify that it was called with the exact list we
        // supplied during construction.
        Mockito.verify(anyOfValidator, times(1)).preloadJsonSchemas(Mockito.argThat(list ->
                list.size() == 2 && list.containsAll(Arrays.asList(schema1, schema2))
        ));
    }
}
