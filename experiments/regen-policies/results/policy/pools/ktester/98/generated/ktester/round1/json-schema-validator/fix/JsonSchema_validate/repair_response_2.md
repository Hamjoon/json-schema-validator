```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link JsonSchema#validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)}.
 */
class JsonSchema_validate_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSchema schema;

    private ExecutionContext executionContext;

    private JsonNodePath rootPath;

    /**
     * Result holder returned by {@code executionContext.getResults()}.
     */
    @Mock
    private ValidationResultHolder resultsHolder; // a mock for the inner results object

    @BeforeEach
    void setupBeforeEach() {
        MockitoAnnotations.openMocks(this);
        // a simple object‑type schema that requires a {@code name} property of type {@code string}
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              },
              "required": [ "name" ]
            }
            """;
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        this.schema = factory.getSchema(schemaJson);
        // default execution context – the real implementation does not need any special configuration
        this.executionContext = new ExecutionContext();
        // root instance location
        this.rootPath = new JsonNodePath(PathType.JSON);
    }

    /**
     * Build a simple object‑type schema that requires a {@code name} property of type {@code string}.
     */
    private static JsonSchema createSchema() {
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              },
              "required": [ "name" ]
            }
            """;
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        return factory.getSchema(schemaJson);
    }

    /**
     * Helper to build a mock ValidationContext with configurable discriminator flag.
     */
    private ValidationContext mockValidationContext(boolean discriminatorEnabled) {
        ValidationContext vc = mock(ValidationContext.class);
        SchemaValidatorsConfig config = mock(SchemaValidatorsConfig.class);
        when(config.isDiscriminatorKeywordEnabled()).thenReturn(discriminatorEnabled);
        when(vc.getConfig()).thenReturn(config);
        return vc;
    }

    /**
     * Helper to inject private fields of {@link BaseJsonValidator}.
     */
    private void injectBaseFields(JsonSchema target, JsonNode schemaNode,
                                  ValidationContext validationContext,
                                  JsonSchemaLocation schemaLocation) throws Exception {
        // BaseJsonValidator.schemaNode
        Field nodeField = BaseJsonValidator.class.getDeclaredField("schemaNode");
        nodeField.setAccessible(true);
        nodeField.set(target, schemaNode);
        // BaseJsonValidator.validationContext
        Field ctxField = BaseJsonValidator.class.getDeclaredField("validationContext");
        ctxField.setAccessible(true);
        ctxField.set(target, validationContext);
        // JsonSchema.schemaLocation (package‑private field)
        Field locField = JsonSchema.class.getDeclaredField("schemaLocation");
        locField.setAccessible(true);
        locField.set(target, schemaLocation);
    }

    /**
     * Helper to create a mock {@link DiscriminatorContext}.
     */
    private DiscriminatorContext mockDiscriminatorContext(ObjectNode returnFromPath) {
        DiscriminatorContext dc = mock(DiscriminatorContext.class);
        when(dc.getDiscriminatorForPath(any())).thenReturn(returnFromPath);
        // registerDiscriminator is void – we only verify its invocation
        return dc;
    }

    /**
     * Helper to create a mock {@link JsonValidator}.
     */
    private JsonValidator mockValidator(Set<ValidationMessage> toReturn, RuntimeException toThrow) {
        JsonValidator v = mock(JsonValidator.class);
        if (toThrow != null) {
            when(v.validate(any(), any(), any(), any())).thenThrow(toThrow);
            // still return a set when called inside finally (the real code still gets the set)
            when(v.validate(any(), any(), any(), any())).thenReturn(toReturn);
        } else {
            when(v.validate(any(), any(), any(), any())).thenReturn(toReturn);
        }
        return v;
    }

    @Test
    void validate_validInstance_returnsNoErrors() throws Exception {
        // given
        JsonNode instance = MAPPER.readTree("{ \"name\": \"John\" }");
        // when
        Set<ValidationMessage> messages = schema.validate(executionContext, instance, instance, rootPath);
        // then
        assertTrue(messages.isEmpty(), "A valid instance should produce no validation messages");
    }

    @Test
    void validate_invalidInstance_returnsErrors() throws Exception {
        // given – the {@code name} property is not a string
        JsonNode instance = MAPPER.readTree("{ \"name\": 1 }");
        // when
        Set<ValidationMessage> messages = schema.validate(executionContext, instance, instance, rootPath);
        // then
        assertFalse(messages.isEmpty(), "An invalid instance should produce validation messages");
        // at least one message must refer to the 'type' violation for the 'name' property
        boolean containsTypeError = messages.stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("type"));
        assertTrue(containsTypeError, "Expected a type‑mismatch validation message");
    }

    @Test
    void discriminatorKeywordDisabled_blocksAreSkipped() throws Exception {
        // --------------------------------------------------------------
        // Group: Discriminator keyword disabled – no discriminator handling
        // --------------------------------------------------------------
        ExecutionContext ctx = executionContext;
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(mock(DiscriminatorContext.class));
        // schema without discriminator (empty node)
        ObjectNode schemaNode = MAPPER.createObjectNode();
        // disabled
        ValidationContext vc = mockValidationContext(false);
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
        injectBaseFields(schema, schemaNode, vc, loc);
        // No validators – getValidators() returns empty list
        JsonSchema spy = spy(schema);
        doReturn(Collections.emptyList()).when(spy).getValidators();
        // invoke
        Set<ValidationMessage> msgs = spy.validate(ctx, NullNode.getInstance(),
                NullNode.getInstance(), JsonNodePath.empty());
        // --------------------------------------------------------------
        // Expectations
        // --------------------------------------------------------------
        assertTrue(msgs.isEmpty(), "When discriminator is disabled, no errors should be produced");
        // No interaction with DiscriminatorContext
        verify(ctx, never()).getCurrentDiscriminatorContext();
        // results.setResult must NOT be called because there are no errors
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
    }

    @Test
    void discriminatorEnabledButAbsentInSchema_isIgnored() throws Exception {
        // --------------------------------------------------------------
        // Group: Discriminator enabled – discriminator absent in schema
        // --------------------------------------------------------------
        ExecutionContext ctx = executionContext;
        // null context
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(null);
        // no "discriminator" field
        ObjectNode schemaNode = MAPPER.createObjectNode();
        // enabled
        ValidationContext vc = mockValidationContext(true);
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
        injectBaseFields(schema, schemaNode, vc, loc);
        JsonSchema spy = spy(schema);
        doReturn(Collections.emptyList()).when(spy).getValidators();
        Set<ValidationMessage> msgs = spy.validate(ctx, NullNode.getInstance(),
                NullNode.getInstance(), JsonNodePath.empty());
        assertTrue(msgs.isEmpty(), "No discriminator -> no errors");
        // No registration nor match check
        verify(ctx, never()).getCurrentDiscriminatorContext();
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
    }

    @Test
    void discriminatorEnabledButNoContext_nothingProcessed() throws Exception {
        // --------------------------------------------------------------
        // Group: Discriminator enabled – discriminator present, but ExecutionContext has no DiscriminatorContext
        // --------------------------------------------------------------
        ExecutionContext ctx = executionContext;
        // missing context
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(null);
        // schema with discriminator object
        ObjectNode disc = MAPPER.createObjectNode();
        disc.put("propertyName", "type");
        ObjectNode schemaNode = MAPPER.createObjectNode().set("discriminator", disc);
        ValidationContext vc = mockValidationContext(true);
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
        injectBaseFields(schema, schemaNode, vc, loc);
        JsonSchema spy = spy(schema);
        doReturn(Collections.emptyList()).when(spy).getValidators();
        Set<ValidationMessage> msgs = spy.validate(ctx, NullNode.getInstance(),
                NullNode.getInstance(), JsonNodePath.empty());
        assertTrue(msgs.isEmpty(), "No context -> discriminator handling is skipped");
        // First block should attempt registration but context is null -> no call
        verify(ctx, times(1)).getCurrentDiscriminatorContext();
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
    }

    @Test
    void discriminatorEnabled_contextPresent_notPreviouslyRegistered() throws Exception {
        // --------------------------------------------------------------
        // Group: Discriminator enabled – context present, discriminator not previously registered (null from context)
        // --------------------------------------------------------------
        // returns null for path
        DiscriminatorContext dc = mockDiscriminatorContext(null);
        ExecutionContext ctx = executionContext;
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(dc);
        // discriminator object in schema
        ObjectNode disc = MAPPER.createObjectNode();
        disc.put("propertyName", "type");
        ObjectNode schemaNode = MAPPER.createObjectNode().set("discriminator", disc);
        ValidationContext vc = mockValidationContext(true);
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
        injectBaseFields(schema, schemaNode, vc, loc);
        // spy to verify checkDiscriminatorMatch invocation
        JsonSchema spy = spy(schema);
        doReturn(Collections.emptyList()).when(spy).getValidators();
        // instance with matching property
        ObjectNode instance = MAPPER.createObjectNode().put("type", "cat");
        Set<ValidationMessage> msgs = spy.validate(ctx, instance, instance, JsonNodePath.empty());
        assertTrue(msgs.isEmpty(), "No validator errors -> empty set");
        // First block should register discriminator
        verify(dc, times(1)).registerDiscriminator(loc, disc);
        // Second block should retrieve null, register again, then use current discriminator
        verify(dc, times(2)).registerDiscriminator(loc, disc);
        // checkDiscriminatorMatch is protected in BaseJsonValidator – verify via spy
        verify(spy, times(1)).checkDiscriminatorMatch(eq(dc), eq(disc), eq("cat"), any());
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
    }

    @Test
    void discriminatorEnabled_contextPresent_alreadyRegistered() throws Exception {
        // --------------------------------------------------------------
        // Group: Discriminator enabled – context present, discriminator already registered (non‑null from context)
        // --------------------------------------------------------------
        ObjectNode existingDisc = MAPPER.createObjectNode();
        existingDisc.put("propertyName", "type");
        DiscriminatorContext dc = mockDiscriminatorContext(existingDisc);
        ExecutionContext ctx = executionContext;
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(dc);
        // schema discriminator (different instance but same content)
        ObjectNode schemaDisc = MAPPER.createObjectNode();
        schemaDisc.put("propertyName", "type");
        ObjectNode schemaNode = MAPPER.createObjectNode().set("discriminator", schemaDisc);
        ValidationContext vc = mockValidationContext(true);
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
        injectBaseFields(schema, schemaNode, vc, loc);
        JsonSchema spy = spy(schema);
        doReturn(Collections.emptyList()).when(spy).getValidators();
        ObjectNode instance = MAPPER.createObjectNode().put("type", "dog");
        Set<ValidationMessage> msgs = spy.validate(ctx, instance, instance, JsonNodePath.empty());
        assertTrue(msgs.isEmpty());
        // First block registers (context not null)
        verify(dc, times(1)).registerDiscriminator(loc, schemaDisc);
        // Second block should NOT re‑register because it finds existingDisc
        verify(dc, never()).registerDiscriminator(eq(loc), eq(existingDisc));
        // checkDiscriminatorMatch should be called with the existing discriminator
        verify(spy, times(1)).checkDiscriminatorMatch(eq(dc), eq(existingDisc), eq("dog"), any());
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
    }

    @Test
    void validatorReturnsErrors_marksFailure() throws Exception {
        // --------------------------------------------------------------
        // Group: Validator returns errors – errors collection populated
        // --------------------------------------------------------------
        ExecutionContext ctx = executionContext;
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(null);
        // schema without discriminator
        ObjectNode schemaNode = MAPPER.createObjectNode();
        ValidationContext vc = mockValidationContext(false);
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
        injectBaseFields(schema, schemaNode, vc, loc);
        // mock validator returning a non‑empty set
        ValidationMessage vm = ValidationMessage.builder()
                .type("type")
                .path("$.name")
                .message("msg1")
                .build();
        JsonValidator validator = mockValidator(Set.of(vm), null);
        JsonSchema spy = spy(schema);
        doReturn(List.of(validator)).when(spy).getValidators();
        Set<ValidationMessage> msgs = spy.validate(ctx, NullNode.getInstance(),
                NullNode.getInstance(), JsonNodePath.empty());
        assertEquals(1, msgs.size(), "One error should be returned");
        assertTrue(msgs.contains(vm));
        // failure should be recorded
        verify(resultsHolder, times(1)).setResult(eq(JsonNodePath.empty()), any(), any(), eq(false));
    }

    @Test
    void validatorReturnsEmptySet_noFailure() throws Exception {
        // --------------------------------------------------------------
        // Group: Validator returns empty set – no errors
        // --------------------------------------------------------------
        ExecutionContext ctx = executionContext;
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(null);
        ObjectNode schemaNode = MAPPER.createObjectNode();
        ValidationContext vc = mockValidationContext(false);
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
        injectBaseFields(schema, schemaNode, vc, loc);
        JsonValidator validator = mockValidator(Collections.emptySet(), null);
        JsonSchema spy = spy(schema);
        doReturn(List.of(validator)).when(spy).getValidators();
        Set<ValidationMessage> msgs = spy.validate(ctx, NullNode.getInstance(),
                NullNode.getInstance(), JsonNodePath.empty());
        assertTrue(msgs.isEmpty(), "Empty set should result in no messages");
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
    }

    @Test
    void validatorThrowsException_finallyCollectsResults() {
        // --------------------------------------------------------------
        // Group: Validator throws exception – finally block still processes results
        // --------------------------------------------------------------
        ExecutionContext ctx = executionContext;
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(null);
        when(ctx.getResults()).thenReturn(resultsHolder);
        ObjectNode schemaNode = MAPPER.createObjectNode();
        ValidationContext vc = mockValidationContext(false);
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
        try {
            injectBaseFields(schema, schemaNode, vc, loc);
        } catch (Exception e) {
            fail(e);
        }
        ValidationMessage vm = ValidationMessage.builder()
                .type("type")
                .path("$.field")
                .message("msg2")
                .build();
        RuntimeException boom = new RuntimeException("boom");
        JsonValidator validator = mockValidator(Set.of(vm), boom);
        JsonSchema spy = spy(schema);
        doReturn(List.of(validator)).when(spy).getValidators();
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> spy.validate(ctx, NullNode.getInstance(),
                        NullNode.getInstance(), JsonNodePath.empty()));
        assertEquals("boom", thrown.getMessage());
        // despite the exception the error set should have been built (though not returned)
        // verify that setResult was called because errors were collected before propagation
        verify(resultsHolder, times(1)).setResult(eq(JsonNodePath.empty()), any(), any(), eq(false));
    }

    @Test
    void noValidators_returnsEmptySet() throws Exception {
        // --------------------------------------------------------------
        // Group: No validators – empty validator list
        // --------------------------------------------------------------
        ExecutionContext ctx = executionContext;
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(null);
        ObjectNode schemaNode = MAPPER.createObjectNode();
        ValidationContext vc = mockValidationContext(false);
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
        injectBaseFields(schema, schemaNode, vc, loc);
        JsonSchema spy = spy(schema);
        doReturn(Collections.emptyList()).when(spy).getValidators();
        Set<ValidationMessage> msgs = spy.validate(ctx, NullNode.getInstance(),
                NullNode.getInstance(), JsonNodePath.empty());
        assertTrue(msgs.isEmpty());
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
    }

    @Test
    void validatorReturnsNull_isIgnored() throws Exception {
        // --------------------------------------------------------------
        // Group: Validator returns null – result ignored
        // --------------------------------------------------------------
        ExecutionContext ctx = executionContext;
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(null);
        ObjectNode schemaNode = MAPPER.createObjectNode();
        ValidationContext vc = mockValidationContext(false);
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
        injectBaseFields(schema, schemaNode, vc, loc);
        JsonValidator validator = mockValidator(null, null);
        JsonSchema spy = spy(schema);
        doReturn(List.of(validator)).when(spy).getValidators();
        Set<ValidationMessage> msgs = spy.validate(ctx, NullNode.getInstance(),
                NullNode.getInstance(), JsonNodePath.empty());
        assertTrue(msgs.isEmpty());
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
    }

    @Test
    void multipleValidators_firstHasErrors_secondEmpty() throws Exception {
        // --------------------------------------------------------------
        // Group: Multiple validators – first returns errors, second empty
        // --------------------------------------------------------------
        ExecutionContext ctx = executionContext;
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(null);
        ObjectNode schemaNode = MAPPER.createObjectNode();
        ValidationContext vc = mockValidationContext(false);
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
        injectBaseFields(schema, schemaNode, vc, loc);
        ValidationMessage m1 = ValidationMessage.builder()
                .type("type")
                .path("$.a")
                .message("first")
                .build();
        ValidationMessage m2 = ValidationMessage.builder()
                .type("type")
                .path("$.b")
                .message("second")
                .build();
        JsonValidator v1 = mockValidator(Set.of(m1, m2), null);
        JsonValidator v2 = mockValidator(Collections.emptySet(), null);
        JsonSchema spy = spy(schema);
        doReturn(List.of(v1, v2)).when(spy).getValidators();
        Set<ValidationMessage> msgs = spy.validate(ctx, NullNode.getInstance(),
                NullNode.getInstance(), JsonNodePath.empty());
        assertEquals(2, msgs.size());
        assertTrue(msgs.containsAll(Set.of(m1, m2)));
        verify(resultsHolder, times(1)).setResult(eq(JsonNodePath.empty()), any(), any(), eq(false));
    }

    @Test
    void multipleValidators_bothHaveErrors_unionPerformed() throws Exception {
        // --------------------------------------------------------------
        // Group: Multiple validators – both return errors, union performed
        // --------------------------------------------------------------
        ExecutionContext ctx = executionContext;
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(null);
        ObjectNode schemaNode = MAPPER.createObjectNode();
        ValidationContext vc = mockValidationContext(false);
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
        injectBaseFields(schema, schemaNode, vc, loc);
        ValidationMessage a = ValidationMessage.builder()
                .type("type")
                .path("$.a")
                .message("a")
                .build();
        ValidationMessage b = ValidationMessage.builder()
                .type("type")
                .path("$.b")
                .message("b")
                .build();
        ValidationMessage c = ValidationMessage.builder()
                .type("type")
                .path("$.c")
                .message("c")
                .build();
        JsonValidator v1 = mockValidator(Set.of(a), null);
        JsonValidator v2 = mockValidator(Set.of(b, c), null);
        JsonSchema spy = spy(schema);
        doReturn(List.of(v1, v2)).when(spy).getValidators();
        Set<ValidationMessage> msgs = spy.validate(ctx, NullNode.getInstance(),
                NullNode.getInstance(), JsonNodePath.empty());
        assertEquals(3, msgs.size());
        assertTrue(msgs.containsAll(Set.of(a, b, c)));
        verify(resultsHolder, times(1)).setResult(eq(JsonNodePath.empty()), any(), any(), eq(false));
    }

    @Test
    void discriminatorPropertyMissing_inInstance_callsMatchWithNull() throws Exception {
        // --------------------------------------------------------------
        // Group: Discriminator enabled – discriminator present but property missing in instance
        // --------------------------------------------------------------
        DiscriminatorContext dc = mockDiscriminatorContext(MAPPER.createObjectNode().put("propertyName", "type"));
        ExecutionContext ctx = executionContext;
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(dc);
        ObjectNode schemaDisc = MAPPER.createObjectNode();
        schemaDisc.put("propertyName", "type");
        ObjectNode schemaNode = MAPPER.createObjectNode().set("discriminator", schemaDisc);
        ValidationContext vc = mockValidationContext(true);
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
        injectBaseFields(schema, schemaNode, vc, loc);
        JsonSchema spy = spy(schema);
        doReturn(Collections.emptyList()).when(spy).getValidators();
        // instance without "type" property
        ObjectNode instance = MAPPER.createObjectNode().put("name", "Rex");
        Set<ValidationMessage> msgs = spy.validate(ctx, instance, instance, JsonNodePath.empty());
        assertTrue(msgs.isEmpty());
        // checkDiscriminatorMatch should be called with null value
        verify(spy, times(1)).checkDiscriminatorMatch(eq(dc), eq(schemaDisc), isNull(), any());
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
    }

    @Test
    void discriminatorPropertyNullValue_callsMatchWithNull() throws Exception {
        // --------------------------------------------------------------
        // Group: Discriminator enabled – discriminator property present but value is null node
        // --------------------------------------------------------------
        DiscriminatorContext dc = mockDiscriminatorContext(MAPPER.createObjectNode().put("propertyName", "type"));
        ExecutionContext ctx = executionContext;
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(dc);
        ObjectNode schemaDisc = MAPPER.createObjectNode();
        schemaDisc.put("propertyName", "type");
        ObjectNode schemaNode = MAPPER.createObjectNode().set("discriminator", schemaDisc);
        ValidationContext vc = mockValidationContext(true);
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
        injectBaseFields(schema, schemaNode, vc, loc);
        JsonSchema spy = spy(schema);
        doReturn(Collections.emptyList()).when(spy).getValidators();
        // instance with "type": null
        ObjectNode instance = MMapper.createObjectNode().set("type", NullNode.getInstance());
        Set<ValidationMessage> msgs = spy.validate(ctx, instance, instance, JsonNodePath.empty());
        assertTrue(msgs.isEmpty());
        verify(spy, times(1)).checkDiscriminatorMatch(eq(dc), eq(schemaDisc), isNull(), any());
    }

    @Test
    void discriminatorNonTextualValue_convertedToText() throws Exception {
        // --------------------------------------------------------------
        // Group: Discriminator enabled – discriminator property value non‑textual (number)
        // --------------------------------------------------------------
        DiscriminatorContext dc = mockDiscriminatorContext(MAPPER.createObjectNode().put("propertyName", "type"));
        ExecutionContext ctx = executionContext;
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(dc);
        ObjectNode schemaDisc = MAPPER.createObjectNode();
        schemaDisc.put("propertyName", "type");
        ObjectNode schemaNode = MAPPER.createObjectNode().set("discriminator", schemaDisc);
        ValidationContext vc = mockValidationContext(true);
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
        injectBaseFields(schema, schemaNode, vc, loc);
        JsonSchema spy = spy(schema);
        doReturn(Collections.emptyList()).when(spy).getValidators();
        // instance with numeric discriminator value
        ObjectNode instance = MAPPER.createObjectNode().put("type", 123);
        Set<ValidationMessage> msgs = spy.validate(ctx, instance, instance, JsonNodePath.empty());
        assertTrue(msgs.isEmpty());
        // asText() on numeric node yields "123"
        verify(spy, times(1)).checkDiscriminatorMatch(eq(dc), eq(schemaDisc), eq("123"), any());
    }

    @Test
    void nullSchemaNode_throwsNPE() throws Exception {
        // --------------------------------------------------------------
        // Group: Null schemaNode – NullPointerException when accessing discriminator
        // --------------------------------------------------------------
        ExecutionContext ctx = executionContext;
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(mock(DiscriminatorContext.class));
        // Force schemaNode to null via reflection
        injectBaseFields(schema, null, mockValidationContext(true), mock(JsonSchemaLocation.class));
        JsonSchema spy = spy(schema);
        doReturn(Collections.emptyList()).when(spy).getValidators();
        assertThrows(NullPointerException.class,
                () -> spy.validate(ctx, NullNode.getInstance(),
                        NullNode.getInstance(), JsonNodePath.empty()));
    }

    @Test
    void discriminatorNodeNotObject_throwsClassCastException() throws Exception {
        // --------------------------------------------------------------
        // Group: Discriminator node is not an ObjectNode – ClassCastException
        // --------------------------------------------------------------
        ExecutionContext ctx = executionContext;
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(mock(DiscriminatorContext.class));
        // discriminator is a text node, causing cast to ObjectNode to fail
        ObjectNode schemaNode = MAPPER.createObjectNode()
                .set("discriminator", new TextNode("not-an-object"));
        injectBaseFields(schema, schemaNode, mockValidationContext(true), mock(JsonSchemaLocation.class));
        JsonSchema spy = spy(schema);
        doReturn(Collections.emptyList()).when(spy).getValidators();
        assertThrows(ClassCastException.class,
                () -> spy.validate(ctx, NullNode.getInstance(),
                        NullNode.getInstance(), JsonNodePath.empty()));
    }

    @Test
    void discriminatorMissingPropertyName_throwsNPE() throws Exception {
        // --------------------------------------------------------------
        // Group: Discriminator object missing required "propertyName" – NullPointerException
        // --------------------------------------------------------------
        ExecutionContext ctx = executionContext;
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(mock(DiscriminatorContext.class));
        // discriminator without propertyName
        ObjectNode disc = MAPPER.createObjectNode(); // empty object
        ObjectNode schemaNode = MAPPER.createObjectNode().set("discriminator", disc);
        injectBaseFields(schema, schemaNode, mockValidationContext(true), mock(JsonSchemaLocation.class));
        JsonSchema spy = spy(schema);
        doReturn(Collections.emptyList()).when(spy).getValidators();
        assertThrows(NullPointerException.class,
                () -> spy.validate(ctx, NullNode.getInstance(),
                        NullNode.getInstance(), JsonNodePath.empty()));
    }

    @Test
    void executionContextResultsNull_throwsNPE_onFailure() throws Exception {
        // --------------------------------------------------------------
        // Group: ExecutionContext results null – NullPointerException when marking failure
        // --------------------------------------------------------------
        ExecutionContext ctx = executionContext;
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(null);
        // simulate missing results holder
        when(ctx.getResults()).thenReturn(null);
        ObjectNode schemaNode = MAPPER.createObjectNode();
        ValidationContext vc = mockValidationContext(false);
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
        injectBaseFields(schema, schemaNode, vc, loc);
        ValidationMessage vm = ValidationMessage.builder()
                .type("type")
                .path("$.field")
                .message("msg")
                .build();
        JsonValidator validator = mockValidator(Set.of(vm), null);
        JsonSchema spy = spy(schema);
        doReturn(List.of(validator)).when(spy).getValidators();
        assertThrows(NullPointerException.class,
                () -> spy.validate(ctx, NullNode.getInstance(),
                        NullNode.getInstance(), JsonNodePath.empty()));
    }

    @Test
    void validatorThrowsUncheckedException_finallyStillProcesses() {
        // --------------------------------------------------------------
        // Group: Validator throws unchecked exception and finally block still processes results
        // --------------------------------------------------------------
        ExecutionContext ctx = executionContext;
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(null);
        when(ctx.getResults()).thenReturn(resultsHolder);
        ObjectNode schemaNode = MAPPER.createObjectNode();
        ValidationContext vc = mockValidationContext(false);
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
        try {
            injectBaseFields(schema, schemaNode, vc, loc);
        } catch (Exception e) {
            fail(e);
        }
        ValidationMessage vm = ValidationMessage.builder()
                .type("type")
                .path("$.field")
                .message("msg2")
                .build();
        IllegalStateException ise = new IllegalStateException("boom");
        JsonValidator validator = mockValidator(Set.of(vm), ise);
        JsonSchema spy = spy(schema);
        doReturn(List.of(validator)).when(spy).getValidators();
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> spy.validate(ctx, NullNode.getInstance(),
                        NullNode.getInstance(), JsonNodePath.empty()));
        assertEquals("boom", thrown.getMessage());
        // despite the exception the error set should have been collected and failure recorded
        verify(resultsHolder, times(1)).setResult(eq(JsonNodePath.empty()), any(), any(), eq(false));
    }
}
```