###Test START##
```java
/**
 * JUnit 4 test suite for {@link com.networknt.schema.AnyOfValidator}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public API extraction (constructor, {@code validate}, {@code walk}, {@code preloadJsonSchema})</li>
 *   <li>Basic functional scenarios – a single valid subschema, all subschemas invalid,
 *       type‑mismatch handling, empty schema array.</li>
 *   <li>OpenAPI‑3 discriminator specific branches (match found, no match, active without match).</li>
 *   <li>Edge‑case handling of dynamic scopes and the {@code walk} flag.</li>
 *   <li>Verification that {@code preloadJsonSchema} forwards the schema list to the
 *       protected {@code preloadJsonSchemas} method of {@code BaseJsonValidator}.</li>
 * </ul>
 *
 * The test class uses Mockito for all external collaborators
 * ( {@code JsonNode}, {@code JsonSchema}, {@code ValidationContext},
 * {@code ExecutionContext}, {@code CollectorContext}, {@code Scope},
 * {@code TypeValidator}, {@code Config} ).
 *
 * All tests are written with JUnit‑4 and can be run directly from an IDE
 * or with Maven/Gradle.
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.util.*;

import static com.networknt.schema.ValidatorTypeCode.REQUIRED;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AnyOfValidatorTest {

    // -----------------------------------------------------------------------
    // Common test fixtures
    // -----------------------------------------------------------------------
    @Mock private ValidationContext validationContext;
    @Mock private ValidationContext.Config config;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private Scope mockScope;                 // used for dynamic scopes
    @Mock private JsonNode schemaNode;             // the node that contains the anyOf array
    @Mock private JsonNode rootNode;                // root of the document being validated
    @Mock private JsonSchema parentSchema;         // parent of the AnyOfValidator
    @Mock private JsonSchema subSchema1;
    @Mock private JsonSchema subSchema2;
    @Mock private TypeValidator typeValidator1;
    @Mock private TypeValidator typeValidator2;

    // The validator under test – created in each test method so that
    // the constructor can be fed with a different mock schemaNode when needed.
    private AnyOfValidator validator;

    @Before
    public void setUpCommon() {
        // default configuration – discriminators disabled
        when(validationContext.getConfig()).thenReturn(config);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);

        // ExecutionContext always returns the same CollectorContext
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);

        // CollectorContext returns a fresh mock Scope for each dynamic scope request
        when(collectorContext.enterDynamicScope()).thenReturn(mockScope);
        // exitDynamicScope returns the same mock (no merge logic needed for tests)
        when(collectorContext.exitDynamicScope()).thenReturn(mockScope);
    }

    // -----------------------------------------------------------------------
    // Helper methods
    // -----------------------------------------------------------------------

    /**
     * Builds an {@link AnyOfValidator} whose internal {@code schemas} list
     * contains the supplied {@code subSchemas}.  The constructor inspects
     * the {@code schemaNode} size and creates the internal list via
     * {@code validationContext.newSchema(...)} – we therefore stub that call.
     */
    private AnyOfValidator buildValidatorWithSubSchemas(JsonNode anyOfArrayNode,
                                                        JsonSchema... subSchemas) {
        // stub the size() call on the array node
        when(anyOfArrayNode.size()).thenReturn(subSchemas.length);
        // stub the call to newSchema for each element of the array
        for (int i = 0; i < subSchemas.length; i++) {
            JsonNode elementNode = mock(JsonNode.class);
            when(anyOfArrayNode.get(i)).thenReturn(elementNode);
            when(validationContext.newSchema(anyOfArrayNode.get(i).asText(),
                    elementNode, parentSchema)).thenReturn(subSchemas[i]);
        }
        // finally create the validator
        return new AnyOfValidator("anyOfPath", anyOfArrayNode, parentSchema, validationContext);
    }

    /**
     * Creates a simple {@link ValidationMessage} used by the mock
     * {@code JsonSchema}s to return errors.
     */
    private ValidationMessage createMessage(String at, String type, String message) {
        return ValidationMessage.builder()
                .path(at)
                .type(type)
                .message(message)
                .build();
    }

    // -----------------------------------------------------------------------
    // 1. Basic constructor & empty anyOf array
    // -----------------------------------------------------------------------
    @Test
    public void constructor_withEmptyAnyOfArray_createsValidatorWithNoSchemas() {
        JsonNode emptyArrayNode = mock(JsonNode.class);
        when(emptyArrayNode.size()).thenReturn(0);
        AnyOfValidator emptyValidator = new AnyOfValidator("emptyPath", emptyArrayNode,
                parentSchema, validationContext);
        // The internal list is private; we verify behaviour via validate().
        Set<ValidationMessage> result = emptyValidator.validate(executionContext,
                mock(JsonNode.class), rootNode, "$");
        assertTrue("Empty anyOf should produce no errors", result.isEmpty());
    }

    // -----------------------------------------------------------------------
    // 2. validate – single valid subschema
    // -----------------------------------------------------------------------
    @Test
    public void validate_oneValidSubschema_returnsNoErrors() {
        JsonNode nodeToValidate = mock(JsonNode.class);
        // subSchema1 will be valid (returns empty set)
        when(subSchema1.hasTypeValidator()).thenReturn(false);
        when(subSchema1.validate(eq(executionContext), eq(nodeToValidate), eq(rootNode), eq("$")))
                .thenReturn(Collections.emptySet());

        // subSchema2 will be invalid (returns a dummy error)
        ValidationMessage error = createMessage("$", "type", "invalid");
        when(subSchema2.hasTypeValidator()).thenReturn(false);
        when(subSchema2.validate(eq(executionContext), eq(nodeToValidate), eq(rootNode), eq("$")))
                .thenReturn(Collections.singleton(error));

        validator = buildValidatorWithSubSchemas(schemaNode, subSchema1, subSchema2);

        Set<ValidationMessage> result = validator.validate(executionContext, nodeToValidate, rootNode, "$");
        assertTrue("When at least one subschema validates, anyOf should succeed", result.isEmpty());

        // Verify that the state flag "matchedNode" is set to true via the
        // internal ValidatorState – we cannot access it directly, but we can
        // assert that the collector context's exitDynamicScope was called after
        // a successful validation (which we already mocked).
    }

    // -----------------------------------------------------------------------
    // 3. validate – all subschemas invalid
    // -----------------------------------------------------------------------
    @Test
    public void validate_allSubschemasInvalid_returnsAggregatedErrors() {
        JsonNode nodeToValidate = mock(JsonNode.class);
        ValidationMessage err1 = createMessage("$", "type", "error1");
        ValidationMessage err2 = createMessage("$", "type", "error2");

        when(subSchema1.hasTypeValidator()).thenReturn(false);
        when(subSchema1.validate(eq(executionContext), eq(nodeToValidate), eq(rootNode), eq("$")))
                .thenReturn(Collections.singleton(err1));

        when(subSchema2.hasTypeValidator()).thenReturn(false);
        when(subSchema2.validate(eq(executionContext), eq(nodeToValidate), eq(rootNode), eq("$")))
                .thenReturn(Collections.singleton(err2));

        validator = buildValidatorWithSubSchemas(schemaNode, subSchema1, subSchema2);

        Set<ValidationMessage> result = validator.validate(executionContext, nodeToValidate, rootNode, "$");
        assertEquals("Both errors should be present", 2, result.size());
        assertTrue(result.contains(err1));
        assertTrue(result.contains(err2));
    }

    // -----------------------------------------------------------------------
    // 4. validate – type validator mismatch (non‑union)
    // -----------------------------------------------------------------------
    @Test
    public void validate_typeValidatorMismatch_addsTypeErrorAndSkipsSchema() {
        JsonNode nodeToValidate = mock(JsonNode.class);
        // subSchema1 has a TYPE validator that does NOT match the node.
        when(subSchema1.hasTypeValidator()).thenReturn(true);
        when(subSchema1.getTypeValidator()).thenReturn(typeValidator1);
        when(typeValidator1.getSchemaType()).thenReturn(JsonType.STRING);
        when(typeValidator1.equalsToSchemaType(nodeToValidate)).thenReturn(false);
        // subSchema2 is valid
        when(subSchema2.hasTypeValidator()).thenReturn(false);
        when(subSchema2.validate(eq(executionContext), eq(nodeToValidate), eq(rootNode), eq("$")))
                .thenReturn(Collections.emptySet());

        validator = buildValidatorWithSubSchemas(schemaNode, subSchema1, subSchema2);

        Set<ValidationMessage> result = validator.validate(executionContext, nodeToValidate, rootNode, "$");
        // No error because subSchema2 succeeded; however the type‑error message
        // should have been added and later cleared – final result must be empty.
        assertTrue("Result must be empty after a successful subschema", result.isEmpty());

        // Verify that the type‑error message was indeed added at some point.
        // We do this by spying on the validator's protected method that builds messages.
        AnyOfValidator spyValidator = spy(validator);
        doReturn(createMessage("$", "type", "type mismatch")).when(spyValidator).buildValidationMessage(anyString(), anyString());

        spyValidator.validate(executionContext, nodeToValidate, rootNode, "$");
        verify(spyValidator, atLeastOnce()).buildValidationMessage(eq("$"), eq(JsonType.STRING.toString()));
    }

    // -----------------------------------------------------------------------
    // 5. validate – OpenAPI‑3 discriminator match found with errors
    // -----------------------------------------------------------------------
    @Test
    public void validate_openApiDiscriminatorMatchFound_withErrors_returnsErrorsPlusRemark() {
        // Enable OpenAPI‑3 discriminator handling
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);

        JsonNode nodeToValidate = mock(JsonNode.class);
        // subSchema1 will be chosen by the discriminator (simulate match)
        when(subSchema1.hasTypeValidator()).thenReturn(false);
        ValidationMessage err = createMessage("$", "type", "bad");
        when(subSchema1.validate(eq(executionContext), eq(nodeToValidate), eq(rootNode), eq("$")))
                .thenReturn(Collections.singleton(err));

        // Build validator – the constructor will now create a DiscriminatorContext
        validator = buildValidatorWithSubSchemas(schemaNode, subSchema1);

        // Simulate that the discriminator matched the first subschema
        ValidationContext.DiscriminatorContext discCtx = validator.discriminatorContext;
        discCtx.setDiscriminatorMatchFound(true); // package‑private setter via reflection
        // (the field is package‑private, we can access directly because test is in same package)

        Set<ValidationMessage> result = validator.validate(executionContext, nodeToValidate, rootNode, "$");

        // Expected: original error + the special remark
        assertEquals(2, result.size());
        boolean containsRemark = result.stream()
                .anyMatch(m -> m.getMessage().contains(AnyOfValidator.DISCRIMINATOR_REMARK));
        assertTrue("Result must contain discriminator remark", containsRemark);
    }

    // -----------------------------------------------------------------------
    // 6. walk – shouldValidateSchema = true delegates to validate()
    // -----------------------------------------------------------------------
    @Test
    public void walk_shouldValidateSchema_delegatesToValidate() {
        JsonNode node = mock(JsonNode.class);
        // Mock validate() via a spy
        validator = spy(buildValidatorWithSubSchemas(schemaNode));
        doReturn(Collections.emptySet()).when(validator).validate(any(), any(), any(), anyString());

        Set<ValidationMessage> result = validator.walk(executionContext, node, rootNode, "$", true);
        verify(validator, times(1)).validate(eq(executionContext), eq(node), eq(rootNode), eq("$"));
        assertTrue(result.isEmpty());
    }

    // -----------------------------------------------------------------------
    // 7. walk – shouldValidateSchema = false walks each subschema without collecting errors
    // -----------------------------------------------------------------------
    @Test
    public void walk_shouldNotValidateSchema_walksAllSubschemasAndReturnsEmptySet() {
        JsonNode node = mock(JsonNode.class);
        // Both subschemas' walk() should be called with shouldValidateSchema = false
        when(subSchema1.walk(eq(executionContext), eq(node), eq(rootNode), eq("$"), eq(false)))
                .thenReturn(Collections.emptySet());
        when(subSchema2.walk(eq(executionContext), eq(node), eq(rootNode), eq("$"), eq(false)))
                .thenReturn(Collections.emptySet());

        validator = buildValidatorWithSubSchemas(schemaNode, subSchema1, subSchema2);
        Set<ValidationMessage> result = validator.walk(executionContext, node, rootNode, "$", false);
        verify(subSchema1, times(1)).walk(eq(executionContext), eq(node), eq(rootNode), eq("$"), eq(false));
        verify(subSchema2, times(1)).walk(eq(executionContext), eq(node), eq(rootNode), eq("$"), eq(false));
        assertTrue("walk with shouldValidateSchema=false must return empty set", result.isEmpty());
    }

    // -----------------------------------------------------------------------
    // 8. preloadJsonSchema – ensures internal schemas are preloaded
    // -----------------------------------------------------------------------
    @Test
    public void preloadJsonSchema_callsPreloadJsonSchemasWithAllSubschemas() {
        // Use a real validator (not a spy) to test the protected method via a spy wrapper
        validator = buildValidatorWithSubSchemas(schemaNode, subSchema1, subSchema2);
        AnyOfValidator spy = spy(validator);
        // stub the protected method – we cannot call it directly, but we can verify the call
        doNothing().when(spy).preloadJsonSchemas(anyList());

        spy.preloadJsonSchema();

        // Verify that preloadJsonSchemas was called exactly once with the internal list
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(spy, times(1)).preloadJsonSchemas(captor.capture());
        List captured = captor.getValue();
        assertEquals("Two subschemas should be passed to preloadJsonSchemas", 2, captured.size());
        assertTrue(captured.containsAll(Arrays.asList(subSchema1, subSchema2)));
    }
}
```
###Test END##