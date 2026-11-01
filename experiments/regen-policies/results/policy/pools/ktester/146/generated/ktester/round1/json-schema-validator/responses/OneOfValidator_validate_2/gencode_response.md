```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.mockito.InOrder;

/**
 * Comprehensive unit tests for {@link OneOfValidator#validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)}.
 *
 * The tests use a combination of real schemas (via {@link JsonSchemaFactory}) for simple
 * scenarios and heavily mocked collaborators for the more intricate discriminator‑related
 * paths.  The {@link TestableOneOfValidator} subclass gives us direct access to the
 * protected {@code validate} method and lets us inject mocked {@link JsonSchema}s,
 * a mock {@link ValidationContext} and a mock parent schema.
 */
class OneOfValidatorValidateTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /* --------------------------------------------------------------------- *
     * Helper classes and methods                                            *
     * --------------------------------------------------------------------- */

    /**
     * Simple subclass exposing the protected {@code validate} method and allowing
     * injection of the {@link ValidationContext}, parent schema and the list of
     * child schemas.
     */
    private static class TestableOneOfValidator extends OneOfValidator {
        private final ValidationContext validationContext;
        private final JsonSchema parentSchema;
        private final List<JsonSchema> schemas;
        private final boolean canShortCircuit;

        TestableOneOfValidator(ValidationContext vc, JsonSchema parent, List<JsonSchema> schemas,
                               boolean canShortCircuit) {
            this.validationContext = vc;
            this.parentSchema = parent;
            this.schemas = schemas;
            this.canShortCircuit = canShortCircuit;
        }

        @Override
        protected boolean canShortCircuit() {
            return canShortCircuit;
        }

        @Override
        public ValidationContext getValidationContext() {
            return validationContext;
        }

        @Override
        public JsonSchema getParentSchema() {
            return parentSchema;
        }

        // expose the protected method for the test class
        Set<ValidationMessage> invokeValidate(ExecutionContext ctx, JsonNode node,
                                            JsonNode rootNode, JsonNodePath path,
                                            boolean walk) {
            return super.validate(ctx, node, rootNode, path, walk);
        }
    }

    /** Mock {@link ExecutionContext} with the required behaviour. */
    private ExecutionContext mockExecutionContext(boolean failFast, boolean discriminatorEnabled) {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.isFailFast()).thenReturn(failFast);
        // remember the value set by the validator so we can assert restoration later
        doAnswer(invocation -> {
            boolean v = invocation.getArgument(0);
            when(ctx.isFailFast()).thenReturn(v);
            return null;
        }).when(ctx).setFailFast(anyBoolean());

        // config
        ValidationContext vc = mock(ValidationContext.class);
        ValidationContext.Config config = mock(ValidationContext.Config.class);
        when(config.isDiscriminatorKeywordEnabled()).thenReturn(discriminatorEnabled);
        when(vc.getConfig()).thenReturn(config);
        when(ctx.getValidationContext()).thenReturn(vc);

        // execution config (locale)
        ExecutionContext.ExecutionConfig execConfig = mock(ExecutionContext.ExecutionConfig.class);
        when(execConfig.getLocale()).thenReturn(Locale.ENGLISH);
        when(ctx.getExecutionConfig()).thenReturn(execConfig);

        // discriminator context – a simple mutable stub
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        when(discCtx.isActive()).thenReturn(discriminatorEnabled);
        when(discCtx.isDiscriminatorMatchFound()).thenReturn(false);
        when(discCtx.isDiscriminatorIgnore()).thenReturn(false);
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(discCtx);
        // enter/leave do nothing
        doNothing().when(ctx).enterDiscriminatorContext(any(), any());
        doNothing().when(ctx).leaveDiscriminatorContextImmediately(any());

        return ctx;
    }

    /** Helper to create a mock {@link JsonSchema} that returns the supplied error set. */
    private JsonSchema mockSchema(Set<ValidationMessage> errors, boolean walkSupported) {
        JsonSchema schema = mock(JsonSchema.class);
        if (walkSupported) {
            when(schema.walk(any(), any(), any(), any(), eq(true))).thenReturn(errors);
        } else {
            when(schema.validate(any(), any(), any(), any())).thenReturn(errors);
        }
        // needed for discriminator handling
        ObjectNode node = MAPPER.createObjectNode();
        when(schema.getSchemaNode()).thenReturn(node);
        return schema;
    }

    /** Simple factory for a ValidationMessage set containing a single dummy error. */
    private Set<ValidationMessage> singleError(String type, String message) {
        ValidationMessage vm = ValidationMessage.builder()
                .type(type)
                .message(message)
                .build();
        return Collections.singleton(vm);
    }

    /* --------------------------------------------------------------------- *
     * Test cases                                                            *
     * --------------------------------------------------------------------- */

    @Test
    void noDiscriminatorExactlyOneSchemaValid_returnsEmptySet() {
        // two schemas: first valid (empty errors), second invalid (non‑empty)
        JsonSchema s0 = mockSchema(Collections.emptySet(), false);
        JsonSchema s1 = mockSchema(singleError("type", "not a string"), false);
        List<JsonSchema> schemas = List.of(s0, s1);

        ExecutionContext ctx = mockExecutionContext(false, false);
        ValidationContext vc = ctx.getValidationContext();
        TestableOneOfValidator validator = new TestableOneOfValidator(vc, null, schemas, false);

        JsonNode node = MAPPER.createObjectNode().put("any", "value"); // content irrelevant
        Set<ValidationMessage> msgs = validator.invokeValidate(ctx, node, node,
                new JsonNodePath(JsonNodePath.PathType.JSON), false);
        assertTrue(msgs.isEmpty(), "Exactly one matching schema should produce no errors");
    }

    @Test
    void noDiscriminatorNoSchemaValid_returnsOneOfError() {
        JsonSchema s0 = mockSchema(singleError("type", "error0"), false);
        JsonSchema s1 = mockSchema(singleError("type", "error1"), false);
        List<JsonSchema> schemas = List.of(s0, s1);

        ExecutionContext ctx = mockExecutionContext(false, false);
        ValidationContext vc = ctx.getValidationContext();
        TestableOneOfValidator validator = new TestableOneOfValidator(vc, null, schemas, false);

        JsonNode node = MAPPER.createObjectNode(); // any node
        Set<ValidationMessage> msgs = validator.invokeValidate(ctx, node, node,
                new JsonNodePath(JsonNodePath.PathType.JSON), true);
        assertEquals(1, msgs.size(), "One error expected when no schema validates");
        ValidationMessage vm = msgs.iterator().next();
        assertEquals("oneOf", vm.getType(), "Message type should be 'oneOf'");
        assertTrue(vm.getMessage().contains("0"), "Argument should indicate 0 valid schemas");
    }

    @Test
    void noDiscriminatorMultipleValid_shortCircuitAllowed() {
        JsonSchema s0 = mockSchema(Collections.emptySet(), false);
        JsonSchema s1 = mockSchema(Collections.emptySet(), false);
        JsonSchema s2 = mockSchema(singleError("type", "error2"), false);
        List<JsonSchema> schemas = List.of(s0, s1, s2);

        ExecutionContext ctx = mockExecutionContext(true, false);
        ValidationContext vc = ctx.getValidationContext();
        // canShortCircuit = true
        TestableOneOfValidator validator = new TestableOneOfValidator(vc, null, schemas, true);

        JsonNode node = MAPPER.createObjectNode();
        Set<ValidationMessage> msgs = validator.invokeValidate(ctx, node, node,
                new JsonNodePath(JsonNodePath.PathType.JSON), false);
        assertEquals(1, msgs.size(), "One error expected when >1 schema matches");
        ValidationMessage vm = msgs.iterator().next();
        assertEquals("oneOf.indexes", vm.getType(), "Message key should be oneOf.indexes");
        assertTrue(vm.getMessage().contains("2"), "Argument should indicate 2 valid schemas");
        assertTrue(vm.getMessage().contains("0, 1") || vm.getMessage().contains("1, 0"),
                "Message should list indexes 0 and 1");
    }

    @Test
    void noDiscriminatorMultipleValid_shortCircuitNotAllowed() {
        JsonSchema s0 = mockSchema(Collections.emptySet(), false);
        JsonSchema s1 = mockSchema(Collections.emptySet(), false);
        JsonSchema s2 = mockSchema(Collections.emptySet(), false);
        List<JsonSchema> schemas = List.of(s0, s1, s2);

        ExecutionContext ctx = mockExecutionContext(false, false);
        ValidationContext vc = ctx.getValidationContext();
        // canShortCircuit = false
        TestableOneOfValidator validator = new TestableOneOfValidator(vc, null, schemas, false);

        JsonNode node = MAPPER.createObjectNode();
        Set<ValidationMessage> msgs = validator.invokeValidate(ctx, node, node,
                new JsonNodePath(JsonNodePath.PathType.JSON), true);
        assertEquals(1, msgs.size(), "One error expected when >1 schema matches");
        ValidationMessage vm = msgs.iterator().next();
        assertEquals("oneOf.indexes", vm.getType());
        assertTrue(vm.getMessage().contains("3"));
        assertTrue(vm.getMessage().contains("0, 1, 2"));
    }

    @Test
    void discriminatorEnabled_matchingDiscriminator_returnsEmptySet() {
        // ----- mock discriminator validator -----
        DiscriminatorValidator discValidator = mock(DiscriminatorValidator.class);
        when(discValidator.getKeyword()).thenReturn("discriminator");
        when(discValidator.getPropertyName()).thenReturn("type");
        when(discValidator.getMapping()).thenReturn(Collections.emptyMap());

        // parent schema returns the discriminator validator
        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getValidators()).thenReturn(List.of(discValidator));

        // ----- mock child schema that has $ref matching discriminator value -----
        JsonSchema child = mock(JsonSchema.class);
        ObjectNode childNode = MAPPER.createObjectNode().put("$ref", "A");
        when(child.getSchemaNode()).thenReturn(childNode);
        when(child.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        // ----- execution context with discriminator enabled -----
        ExecutionContext ctx = mockExecutionContext(true, true);
        // make the discriminator context report a match immediately
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        when(discCtx.isActive()).thenReturn(true);
        when(discCtx.isDiscriminatorMatchFound()).thenReturn(true);
        when(discCtx.isDiscriminatorIgnore()).thenReturn(false);
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(discCtx);

        ValidationContext vc = ctx.getValidationContext();

        TestableOneOfValidator validator = new TestableOneOfValidator(vc, parent,
                List.of(child), false);

        // node with discriminator property "type":"A"
        JsonNode node = MAPPER.createObjectNode().put("type", "A");
        Set<ValidationMessage> msgs = validator.invokeValidate(ctx, node, node,
                new JsonNodePath(JsonNodePath.PathType.JSON), false);
        assertTrue(msgs.isEmpty(), "Matching discriminator with a valid schema should yield no errors");
    }

    @Test
    void discriminatorEnabled_noMatchProducesSpecialError() {
        // discriminator validator present
        DiscriminatorValidator discValidator = mock(DiscriminatorValidator.class);
        when(discValidator.getKeyword()).thenReturn("discriminator");
        when(discValidator.getPropertyName()).thenReturn("type");
        when(discValidator.getMapping()).thenReturn(Collections.emptyMap());

        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getValidators()).thenReturn(List.of(discValidator));

        // child schema with $ref that does NOT match discriminator value
        JsonSchema child = mock(JsonSchema.class);
        ObjectNode childNode = MAPPER.createObjectNode().put("$ref", "B");
        when(child.getSchemaNode()).thenReturn(childNode);
        when(child.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        ExecutionContext ctx = mockExecutionContext(true, true);
        // discriminator context reports NO match and NOT ignored
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        when(discCtx.isActive()).thenReturn(true);
        when(discCtx.isDiscriminatorMatchFound()).thenReturn(false);
        when(discCtx.isDiscriminatorIgnore()).thenReturn(false);
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(discCtx);

        ValidationContext vc = ctx.getValidationContext();

        TestableOneOfValidator validator = new TestableOneOfValidator(vc, parent,
                List.of(child), false);

        JsonNode node = MAPPER.createObjectNode().put("type", "UNKNOWN");
        Set<ValidationMessage> msgs = validator.invokeValidate(ctx, node, node,
                new JsonNodePath(JsonNodePath.PathType.JSON), false);
        assertEquals(1, msgs.size(), "Special discriminator‑no‑match error expected");
        ValidationMessage vm = msgs.iterator().next();
        assertTrue(vm.getMessage().contains("based on the provided discriminator"),
                "Message should mention discriminator no‑alternative");
    }

    @Test
    void executionContextFailFastFlagRestoredAfterValidate() {
        ExecutionContext ctx = mockExecutionContext(true, false); // start true
        // simple schemas – both invalid so we get an error
        JsonSchema s0 = mockSchema(singleError("type", "e0"), false);
        JsonSchema s1 = mockSchema(singleError("type", "e1"), false);
        ValidationContext vc = ctx.getValidationContext();

        TestableOneOfValidator validator = new TestableOneOfValidator(vc, null,
                List.of(s0, s1), false);

        JsonNode node = MAPPER.createObjectNode();
        validator.invokeValidate(ctx, node, node,
                new JsonNodePath(JsonNodePath.PathType.JSON), false);

        // after the call the mock should have been asked to set the flag back to true
        verify(ctx).setFailFast(true);
        assertTrue(ctx.isFailFast(), "Fail‑fast flag must be restored to its original value");
    }

    @Test
    void canShortCircuitThrowsException_propagates() {
        JsonSchema s0 = mockSchema(Collections.emptySet(), false);
        JsonSchema s1 = mockSchema(Collections.emptySet(), false);
        List<JsonSchema> schemas = List.of(s0, s1);

        ExecutionContext ctx = mockExecutionContext(false, false);
        ValidationContext vc = ctx.getValidationContext();

        // subclass that throws from canShortCircuit()
        OneOfValidator validator = new OneOfValidator() {
            @Override
            protected boolean canShortCircuit() {
                throw new RuntimeException("boom");
            }

            @Override
            public ValidationContext getValidationContext() {
                return vc;
            }

            @Override
            public JsonSchema getParentSchema() {
                return null;
            }

            @Override
            public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node,
                                                   JsonNode rootNode, JsonNodePath instanceLocation) {
                return super.validate(executionContext, node, rootNode, instanceLocation);
            }
        };
        // inject schemas via reflection (field is private final)
        try {
            java.lang.reflect.Field f = OneOfValidator.class.getDeclaredField("schemas");
            f.setAccessible(true);
            f.set(validator, schemas);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }

        JsonNode node = MAPPER.createObjectNode();
        assertThrows(RuntimeException.class,
                () -> validator.validate(ctx, node, node,
                        new JsonNodePath(JsonNodePath.PathType.JSON), false),
                "Exception from canShortCircuit() should propagate");
    }

    @Test
    void reportChildErrorsThrowsException_propagates() {
        JsonSchema s0 = mockSchema(singleError("type", "e0"), false);
        List<JsonSchema> schemas = List.of(s0);

        ExecutionContext ctx = mockExecutionContext(false, false);
        ValidationContext vc = ctx.getValidationContext();

        // subclass that throws from reportChildErrors()
        OneOfValidator validator = new OneOfValidator() {
            @Override
            protected boolean reportChildErrors(ExecutionContext executionContext) {
                throw new IllegalArgumentException("bad");
            }

            @Override
            public ValidationContext getValidationContext() {
                return vc;
            }

            @Override
            public JsonSchema getParentSchema() {
                return null;
            }

            @Override
            public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node,
                                                   JsonNode rootNode, JsonNodePath instanceLocation) {
                return super.validate(executionContext, node, rootNode, instanceLocation);
            }
        };
        // inject schemas via reflection
        try {
            java.lang.reflect.Field f = OneOfValidator.class.getDeclaredField("schemas");
            f.setAccessible(true);
            f.set(validator, schemas);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }

        JsonNode node = MAPPER.createObjectNode();
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(ctx, node, node,
                        new JsonNodePath(JsonNodePath.PathType.JSON), false),
                "Exception from reportChildErrors() should propagate");
    }

    @Test
    void nullExecutionContext_throwsNPE() {
        JsonSchema s0 = mockSchema(Collections.emptySet(), false);
        List<JsonSchema> schemas = List.of(s0);
        ValidationContext vc = mock(ValidationContext.class);
        TestableOneOfValidator validator = new TestableOneOfValidator(vc, null, schemas, false);
        JsonNode node = MAPPER.createObjectNode();
        assertThrows(NullPointerException.class,
                () -> validator.invokeValidate(null, node, node,
                        new JsonNodePath(JsonNodePath.PathType.JSON), false));
    }

    @Test
    void nullNodeWithDiscriminatorEnabled_throwsNPE() {
        // set up discriminator enabled but node is null
        DiscriminatorValidator discValidator = mock(DiscriminatorValidator.class);
        when(discValidator.getKeyword()).thenReturn("discriminator");
        when(discValidator.getPropertyName()).thenReturn("type");
        when(discValidator.getMapping()).thenReturn(Collections.emptyMap());

        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getValidators()).thenReturn(List.of(discValidator));

        ExecutionContext ctx = mockExecutionContext(false, true);
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        when(discCtx.isActive()).thenReturn(true);
        when(discCtx.isDiscriminatorMatchFound()).thenReturn(false);
        when(discCtx.isDiscriminatorIgnore()).thenReturn(false);
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(discCtx);

        ValidationContext vc = ctx.getValidationContext();

        JsonSchema child = mockSchema(Collections.emptySet(), false);
        TestableOneOfValidator validator = new TestableOneOfValidator(vc, parent,
                List.of(child), false);

        assertThrows(NullPointerException.class,
                () -> validator.invokeValidate(ctx, null, null,
                        new JsonNodePath(JsonNodePath.PathType.JSON), false));
    }

    @Test
    void nullValidationContext_throwsNPE() {
        // validator with null validationContext
        TestableOneOfValidator validator = new TestableOneOfValidator(null, null,
                Collections.emptyList(), false);
        ExecutionContext ctx = mockExecutionContext(false, false);
        JsonNode node = MAPPER.createObjectNode();
        assertThrows(NullPointerException.class,
                () -> validator.invokeValidate(ctx, node, node,
                        new JsonNodePath(JsonNodePath.PathType.JSON), false));
    }

    @Test
    void parentSchemaMissing_throwsNPE() {
        // validator where getParentSchema() returns null but discriminator enabled
        ExecutionContext ctx = mockExecutionContext(false, true);
        ValidationContext vc = ctx.getValidationContext();

        JsonSchema s0 = mockSchema(Collections.emptySet(), false);
        TestableOneOfValidator validator = new TestableOneOfValidator(vc, null,
                List.of(s0), false);

        JsonNode node = MAPPER.createObjectNode().put("type", "A");
        assertThrows(NullPointerException.class,
                () -> validator.invokeValidate(ctx, node, node,
                        new JsonNodePath(JsonNodePath.PathType.JSON), false));
    }

    @Test
    void discriminatorValidatorTypeMismatch_throwsClassCastException() {
        // validator present with keyword "discriminator" but not an instance of DiscriminatorValidator
        JsonValidator bogus = mock(JsonValidator.class);
        when(bogus.getKeyword()).thenReturn("discriminator");

        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getValidators()).thenReturn(List.of(bogus));

        ExecutionContext ctx = mockExecutionContext(false, true);
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        when(discCtx.isActive()).thenReturn(true);
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(discCtx);

        ValidationContext vc = ctx.getValidationContext();

        JsonSchema child = mockSchema(Collections.emptySet(), false);
        TestableOneOfValidator validator = new TestableOneOfValidator(vc, parent,
                List.of(child), false);

        JsonNode node = MAPPER.createObjectNode().put("type", "A");
        assertThrows(ClassCastException.class,
                () -> validator.invokeValidate(ctx, node, node,
                        new JsonNodePath(JsonNodePath.PathType.JSON), false));
    }

    @Test
    void discriminatorPropertyNameNull_throwsNPE() {
        // discriminator validator with null property name
        DiscriminatorValidator discValidator = mock(DiscriminatorValidator.class);
        when(discValidator.getKeyword()).thenReturn("discriminator");
        when(discValidator.getPropertyName()).thenReturn(null); // null!
        when(discValidator.getMapping()).thenReturn(Collections.emptyMap());

        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getValidators()).thenReturn(List.of(discValidator));

        ExecutionContext ctx = mockExecutionContext(false, true);
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        when(discCtx.isActive()).thenReturn(true);
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(discCtx);

        ValidationContext vc = ctx.getValidationContext();

        JsonSchema child = mockSchema(Collections.emptySet(), false);
        TestableOneOfValidator validator = new TestableOneOfValidator(vc, parent,
                List.of(child), false);

        JsonNode node = MAPPER.createObjectNode().put("type", "A");
        assertThrows(NullPointerException.class,
                () -> validator.invokeValidate(ctx, node, node,
                        new JsonNodePath(JsonNodePath.PathType.JSON), false));
    }

    @Test
    void discriminatorMappingNull_throwsNPE() {
        // discriminator validator with null mapping
        DiscriminatorValidator discValidator = mock(DiscriminatorValidator.class);
        when(discValidator.getKeyword()).thenReturn("discriminator");
        when(discValidator.getPropertyName()).thenReturn("type");
        when(discValidator.getMapping()).thenReturn(null); // null!

        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getValidators()).thenReturn(List.of(discValidator));

        ExecutionContext ctx = mockExecutionContext(false, true);
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        when(discCtx.isActive()).thenReturn(true);
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(discCtx);

        ValidationContext vc = ctx.getValidationContext();

        JsonSchema child = mockSchema(Collections.emptySet(), false);
        TestableOneOfValidator validator = new TestableOneOfValidator(vc, parent,
                List.of(child), false);

        JsonNode node = MAPPER.createObjectNode().put("type", "A");
        assertThrows(NullPointerException.class,
                () -> validator.invokeValidate(ctx, node, node,
                        new JsonNodePath(JsonNodePath.PathType.JSON), false));
    }

    @Test
    void schemaNodeNull_throwsNPE() {
        // child schema returns null for getSchemaNode()
        DiscriminatorValidator discValidator = mock(DiscriminatorValidator.class);
        when(discValidator.getKeyword()).thenReturn("discriminator");
        when(discValidator.getPropertyName()).thenReturn("type");
        when(discValidator.getMapping()).thenReturn(Collections.emptyMap());

        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getValidators()).thenReturn(List.of(discValidator));

        JsonSchema child = mock(JsonSchema.class);
        when(child.getSchemaNode()).thenReturn(null); // null node
        when(child.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        ExecutionContext ctx = mockExecutionContext(false, true);
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        when(discCtx.isActive()).thenReturn(true);
        when(discCtx.isDiscriminatorMatchFound()).thenReturn(false);
        when(discCtx.isDiscriminatorIgnore()).thenReturn(false);
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(discCtx);

        ValidationContext vc = ctx.getValidationContext();

        TestableOneOfValidator validator = new TestableOneOfValidator(vc, parent,
                List.of(child), false);

        JsonNode node = MAPPER.createObjectNode().put("type", "A");
        assertThrows(NullPointerException.class,
                () -> validator.invokeValidate(ctx, node, node,
                        new JsonNodePath(JsonNodePath.PathType.JSON), false));
    }

    @Test
    void enterDiscriminatorContextThrows_propagates() {
        // set up discriminator enabled
        DiscriminatorValidator discValidator = mock(DiscriminatorValidator.class);
        when(discValidator.getKeyword()).thenReturn("discriminator");
        when(discValidator.getPropertyName()).thenReturn("type");
        when(discValidator.getMapping()).thenReturn(Collections.emptyMap());

        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getValidators()).thenReturn(List.of(discValidator));

        ExecutionContext ctx = mockExecutionContext(false, true);
        // make enterDiscriminatorContext throw
        doThrow(new IllegalStateException("boom")).when(ctx)
                .enterDiscriminatorContext(any(), any());

        ValidationContext vc = ctx.getValidationContext();

        JsonSchema child = mockSchema(Collections.emptySet(), false);
        TestableOneOfValidator validator = new TestableOneOfValidator(vc, parent,
                List.of(child), false);

        JsonNode node = MAPPER.createObjectNode().put("type", "A");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.invokeValidate(ctx, node, node,
                        new JsonNodePath(JsonNodePath.PathType.JSON), false));
        assertEquals("boom", ex.getMessage());
    }
}
```