
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSchema schema;

    private ExecutionContext executionContext;

    private JsonNodePath rootPath;

    /**
     * Result holder returned by {@code executionContext.getResults()}.
     */
    @Mock
    private ValidationResultHolder // a mock for the inner results object
    resultsHolder;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the objects that will be reused by each test method.
        this.schema = createSchema();
        // default config
        this.executionContext = new ExecutionContext();
        // root instance location
        this.rootPath = new JsonNodePath(PathType.JSON);
        // a completely empty schema – we will set the discriminator node manually where needed
        String emptySchema = "{}";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        this.schema = factory.getSchema(emptySchema);
        // default executionContext behaviour
        when(executionContext.getResults()).thenReturn(resultsHolder);
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
    private void injectBaseFields(JsonSchema target, JsonNode schemaNode, ValidationContext validationContext, JsonSchemaLocation schemaLocation) throws Exception {
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
        // given
        // name is not a string
        JsonNode instance = MAPPER.readTree("{ \"name\": 1 }");
        // when
        Set<ValidationMessage> messages = schema.validate(executionContext, instance, instance, rootPath);
        // then
        assertFalse(messages.isEmpty(), "An invalid instance should produce validation messages");
        // At least one message must refer to the 'type' violation for the 'name' property.
        boolean containsTypeError = messages.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("type"));
        assertTrue(containsTypeError, "Expected a type‑mismatch validation message");
    }

    @Test
    void discriminatorKeywordDisabled_blocksAreSkipped() throws Exception {
        // --------------------------------------------------------------
        // Group: Discriminator keyword disabled – no discriminator handling
        // --------------------------------------------------------------
        // mock injected by @Mock
        ExecutionContext ctx = executionContext;
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(mock(DiscriminatorContext.class));
        // schema without discriminator (empty node)
        ObjectNode schemaNode = MAPPER.createObjectNode();
        // disabled
        ValidationContext vc = mockValidationContext(false);
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
        injectBaseFields(schema, schemaNode, vc, loc);
        // No validators – getValidators() returns empty list
        JsonSchema spy = Mockito.spy(schema);
        doReturn(Collections.emptyList()).when(spy).getValidators();
        // invoke
        Set<ValidationMessage> msgs = spy.validate(ctx, NullNode.getInstance(), NullNode.getInstance(), JsonNodePath.empty());
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
        JsonSchema spy = Mockito.spy(schema);
        doReturn(Collections.emptyList()).when(spy).getValidators();
        Set<ValidationMessage> msgs = spy.validate(ctx, NullNode.getInstance(), NullNode.getInstance(), JsonNodePath.empty());
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
        JsonSchema spy = Mockito.spy(schema);
        doReturn(Collections.emptyList()).when(spy).getValidators();
        Set<ValidationMessage> msgs = spy.validate(ctx, NullNode.getInstance(), NullNode.getInstance(), JsonNodePath.empty());
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
        JsonSchema spy = Mockito.spy(schema);
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
        JsonSchema spy = Mockito.spy(schema);
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
        ValidationMessage vm = ValidationMessage.builder().type("type").path("$.name").message("msg1").build();
        JsonValidator validator = mockValidator(Set.of(vm), null);
        JsonSchema spy = Mockito.spy(schema);
        doReturn(List.of(validator)).when(spy).getValidators();
        Set<ValidationMessage> msgs = spy.validate(ctx, NullNode.getInstance(), NullNode.getInstance(), JsonNodePath.empty());
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
        JsonSchema spy = Mockito.spy(schema);
        doReturn(List.of(validator)).when(spy).getValidators();
        Set<ValidationMessage> msgs = spy.validate(ctx, NullNode.getInstance(), NullNode.getInstance(), JsonNodePath.empty());
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
        ObjectNode schemaNode = MAPPER.createObjectNode();
        ValidationContext vc = mockValidationContext(false);
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
        try {
            injectBaseFields(schema, schemaNode, vc, loc);
        } catch (Exception e) {
            fail(e);
        }
        ValidationMessage vm = ValidationMessage.builder().type("type").path("$.field").message("msg2").build();
        RuntimeException boom = new RuntimeException("boom");
        JsonValidator validator = mockValidator(Set.of(vm), boom);
        JsonSchema spy = Mockito.spy(schema);
        doReturn(List.of(validator)).when(spy).getValidators();
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> spy.validate(ctx, NullNode.getInstance(), NullNode.getInstance(), JsonNodePath.empty()));
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
        JsonSchema spy = Mockito.spy(schema);
        doReturn(Collections.emptyList()).when(spy).getValidators();
        Set<ValidationMessage> msgs = spy.validate(ctx, NullNode.getInstance(), NullNode.getInstance(), JsonNodePath.empty());
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
        JsonSchema spy = Mockito.spy(schema);
        doReturn(List.of(validator)).when(spy).getValidators();
        Set<ValidationMessage> msgs = spy.validate(ctx, NullNode.getInstance(), NullNode.getInstance(), JsonNodePath.empty());
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
        ValidationMessage m1 = ValidationMessage.builder().type("type").path("$.a").message("first").build();
        ValidationMessage m2 = ValidationMessage.builder().type("type").path("$.b").message("second").build();
        JsonValidator v1 = mockValidator(Set.of(m1, m2), null);
        JsonValidator v2 = mockValidator(Collections.emptySet(), null);
        JsonSchema spy = Mockito.spy(schema);
        doReturn(List.of(v1, v2)).when(spy).getValidators();
        Set<ValidationMessage> msgs = spy.validate(ctx, NullNode.getInstance(), NullNode.getInstance(), JsonNodePath.empty());
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
        ValidationMessage a = ValidationMessage.builder().type("type").path("$.a").message("a").build();
        ValidationMessage b = ValidationMessage.builder().type("type").path("$.b").message("b").build();
        ValidationMessage c = ValidationMessage.builder().type("type").path("$.c").message("c").build();
        JsonValidator v1 = mockValidator(Set.of(a), null);
        JsonValidator v2 = mockValidator(Set.of(b, c), null);
        JsonSchema spy = Mockito.spy(schema);
        doReturn(List.of(v1, v2)).when(spy).getValidators();
        Set<ValidationMessage> msgs = spy.validate(ctx, NullNode.getInstance(), NullNode.getInstance(), JsonNodePath.empty());
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
        JsonSchema spy = Mockito.spy(schema);
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
        JsonSchema spy = Mockito.spy(schema);
        doReturn(Collections.emptyList()).when(spy).getValidators();
        // instance with "type": null
        ObjectNode instance = MAPPER.createObjectNode().set("type", NullNode.getInstance());
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
        JsonSchema spy = Mockito.spy(schema);
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
        JsonSchema spy = Mockito.spy(schema);
        doReturn(Collections.emptyList()).when(spy).getValidators();
        assertThrows(NullPointerException.class, () -> spy.validate(ctx, NullNode.getInstance(), NullNode.getInstance(), JsonNodePath.empty()));
    }

    @Test
    void discriminatorNodeNotObject_throwsClassCastException() throws Exception {
        // --------------------------------------------------------------
        // Group: Discriminator node is not an ObjectNode – ClassCastException
        // --------------------------------------------------------------
        ExecutionContext ctx = executionContext;
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(mock(DiscriminatorContext.class));
        // discriminator is a text node, causing cast to ObjectNode to fail
        ObjectNode schemaNode = MAPPER.createObjectNode().set("discriminator", new TextNode("not-an-object"));
        injectBaseFields(schema, schemaNode, mockValidationContext(true), mock(JsonSchemaLocation.class));
        JsonSchema spy = Mockito.spy(schema);
        doReturn(Collections.emptyList()).when(spy).getValidators();
        assertThrows(ClassCastException.class, () -> spy.validate(ctx, NullNode.getInstance(), NullNode.getInstance(), JsonNodePath.empty()));
    }

    @Test
    void discriminatorMissingPropertyName_throwsNPE() throws Exception {
        // --------------------------------------------------------------
        // Group: Discriminator object missing required "propertyName" – NullPointerException
        // --------------------------------------------------------------
        ExecutionContext ctx = executionContext;
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(mock(DiscriminatorContext.class));
        // discriminator without propertyName
        // empty object
        ObjectNode disc = MAPPER.createObjectNode();
        ObjectNode schemaNode = MAPPER.createObjectNode().set("discriminator", disc);
        injectBaseFields(schema, schemaNode, mockValidationContext(true), mock(JsonSchemaLocation.class));
        JsonSchema spy = Mockito.spy(schema);
        doReturn(Collections.emptyList()).when(spy).getValidators();
        assertThrows(NullPointerException.class, () -> spy.validate(ctx, NullNode.getInstance(), NullNode.getInstance(), JsonNodePath.empty()));
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
        ValidationMessage vm = ValidationMessage.builder().type("type").path("$.field").message("msg").build();
        JsonValidator validator = mockValidator(Set.of(vm), null);
        JsonSchema spy = Mockito.spy(schema);
        doReturn(List.of(validator)).when(spy).getValidators();
        assertThrows(NullPointerException.class, () -> spy.validate(ctx, NullNode.getInstance(), NullNode.getInstance(), JsonNodePath.empty()));
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
        ValidationMessage vm = ValidationMessage.builder().type("type").path("$.field").message("msg2").build();
        IllegalStateException ise = new IllegalStateException("boom");
        JsonValidator validator = mockValidator(Set.of(vm), ise);
        JsonSchema spy = Mockito.spy(schema);
        doReturn(List.of(validator)).when(spy).getValidators();
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> spy.validate(ctx, NullNode.getInstance(), NullNode.getInstance(), JsonNodePath.empty()));
        assertEquals("boom", thrown.getMessage());
        // despite the exception, the error set should have been collected and failure recorded
        verify(resultsHolder, times(1)).setResult(eq(JsonNodePath.empty()), any(), any(), eq(false));
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:8: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:16: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:17: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:35: error: cannot find symbol
    private ValidationResultHolder // a mock for the inner results object
            ^
  symbol:   class ValidationResultHolder
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:86: error: cannot find symbol
    private void injectBaseFields(JsonSchema target, JsonNode schemaNode, ValidationContext validationContext, JsonSchemaLocation schemaLocation) throws Exception {
                                                                                                               ^
  symbol:   class JsonSchemaLocation
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:15: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:18: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:20: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:34: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:45: error: cannot find symbol
        this.rootPath = new JsonNodePath(PathType.JSON);
                                                 ^
  symbol:   variable JSON
  location: class PathType
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:51: error: cannot find symbol
        when(executionContext.getResults()).thenReturn(resultsHolder);
        ^
  symbol:   method when(JsonNodeResults)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:76: error: cannot find symbol
        ValidationContext vc = mock(ValidationContext.class);
                               ^
  symbol:   method mock(Class<ValidationContext>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:77: error: cannot find symbol
        SchemaValidatorsConfig config = mock(SchemaValidatorsConfig.class);
                                        ^
  symbol:   method mock(Class<SchemaValidatorsConfig>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:78: error: cannot find symbol
        when(config.isDiscriminatorKeywordEnabled()).thenReturn(discriminatorEnabled);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:79: error: cannot find symbol
        when(vc.getConfig()).thenReturn(config);
        ^
  symbol:   method when(SchemaValidatorsConfig)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:105: error: cannot find symbol
        DiscriminatorContext dc = mock(DiscriminatorContext.class);
                                  ^
  symbol:   method mock(Class<DiscriminatorContext>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:106: error: cannot find symbol
        when(dc.getDiscriminatorForPath(any())).thenReturn(returnFromPath);
                                        ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:115: error: cannot find symbol
        JsonValidator v = mock(JsonValidator.class);
                          ^
  symbol:   method mock(Class<JsonValidator>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:117: error: cannot find symbol
            when(v.validate(any(), any(), any(), any())).thenThrow(toThrow);
                            ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:117: error: cannot find symbol
            when(v.validate(any(), any(), any(), any())).thenThrow(toThrow);
                                   ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:117: error: cannot find symbol
            when(v.validate(any(), any(), any(), any())).thenThrow(toThrow);
                                          ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:117: error: cannot find symbol
            when(v.validate(any(), any(), any(), any())).thenThrow(toThrow);
                                                 ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:119: error: cannot find symbol
            when(v.validate(any(), any(), any(), any())).thenReturn(toReturn);
                            ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:119: error: cannot find symbol
            when(v.validate(any(), any(), any(), any())).thenReturn(toReturn);
                                   ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:119: error: cannot find symbol
            when(v.validate(any(), any(), any(), any())).thenReturn(toReturn);
                                          ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:119: error: cannot find symbol
            when(v.validate(any(), any(), any(), any())).thenReturn(toReturn);
                                                 ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:121: error: cannot find symbol
            when(v.validate(any(), any(), any(), any())).thenReturn(toReturn);
                            ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:121: error: cannot find symbol
            when(v.validate(any(), any(), any(), any())).thenReturn(toReturn);
                                   ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:121: error: cannot find symbol
            when(v.validate(any(), any(), any(), any())).thenReturn(toReturn);
                                          ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:121: error: cannot find symbol
            when(v.validate(any(), any(), any(), any())).thenReturn(toReturn);
                                                 ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:157: error: cannot find symbol
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(mock(DiscriminatorContext.class));
        ^
  symbol:   method when(DiscriminatorContext)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:157: error: cannot find symbol
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(mock(DiscriminatorContext.class));
                                                              ^
  symbol:   method mock(Class<DiscriminatorContext>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:162: error: cannot find symbol
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
        ^
  symbol:   class JsonSchemaLocation
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:162: error: cannot find symbol
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
                                      ^
  symbol:   class JsonSchemaLocation
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:165: error: cannot find symbol
        JsonSchema spy = Mockito.spy(schema);
                         ^
  symbol:   variable Mockito
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:166: error: cannot find symbol
        doReturn(Collections.emptyList()).when(spy).getValidators();
        ^
  symbol:   method doReturn(List<Object>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:168: error: cannot find symbol
        Set<ValidationMessage> msgs = spy.validate(ctx, NullNode.getInstance(), NullNode.getInstance(), JsonNodePath.empty());
                                                                                                                    ^
  symbol:   method empty()
  location: class JsonNodePath
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:174: error: cannot find symbol
        verify(ctx, never()).getCurrentDiscriminatorContext();
                    ^
  symbol:   method never()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:176: error: cannot find symbol
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
                              ^
  symbol:   method never()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:176: error: cannot find symbol
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
                                                 ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:176: error: cannot find symbol
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
                                                        ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:176: error: cannot find symbol
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
                                                               ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:176: error: cannot find symbol
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
                                                                      ^
  symbol:   method anyBoolean()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:186: error: cannot find symbol
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(null);
        ^
  symbol:   method when(DiscriminatorContext)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:191: error: cannot find symbol
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
        ^
  symbol:   class JsonSchemaLocation
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:191: error: cannot find symbol
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
                                      ^
  symbol:   class JsonSchemaLocation
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:193: error: cannot find symbol
        JsonSchema spy = Mockito.spy(schema);
                         ^
  symbol:   variable Mockito
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:194: error: cannot find symbol
        doReturn(Collections.emptyList()).when(spy).getValidators();
        ^
  symbol:   method doReturn(List<Object>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:195: error: cannot find symbol
        Set<ValidationMessage> msgs = spy.validate(ctx, NullNode.getInstance(), NullNode.getInstance(), JsonNodePath.empty());
                                                                                                                    ^
  symbol:   method empty()
  location: class JsonNodePath
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:198: error: cannot find symbol
        verify(ctx, never()).getCurrentDiscriminatorContext();
                    ^
  symbol:   method never()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:199: error: cannot find symbol
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
                              ^
  symbol:   method never()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:199: error: cannot find symbol
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
                                                 ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:199: error: cannot find symbol
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
                                                        ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:199: error: cannot find symbol
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
                                                               ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:199: error: cannot find symbol
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
                                                                      ^
  symbol:   method anyBoolean()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:209: error: cannot find symbol
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(null);
        ^
  symbol:   method when(DiscriminatorContext)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:215: error: cannot find symbol
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
        ^
  symbol:   class JsonSchemaLocation
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:215: error: cannot find symbol
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
                                      ^
  symbol:   class JsonSchemaLocation
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:217: error: cannot find symbol
        JsonSchema spy = Mockito.spy(schema);
                         ^
  symbol:   variable Mockito
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:218: error: cannot find symbol
        doReturn(Collections.emptyList()).when(spy).getValidators();
        ^
  symbol:   method doReturn(List<Object>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:219: error: cannot find symbol
        Set<ValidationMessage> msgs = spy.validate(ctx, NullNode.getInstance(), NullNode.getInstance(), JsonNodePath.empty());
                                                                                                                    ^
  symbol:   method empty()
  location: class JsonNodePath
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:222: error: cannot find symbol
        verify(ctx, times(1)).getCurrentDiscriminatorContext();
                    ^
  symbol:   method times(int)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:223: error: cannot find symbol
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
                              ^
  symbol:   method never()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:223: error: cannot find symbol
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
                                                 ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:223: error: cannot find symbol
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
                                                        ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:223: error: cannot find symbol
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
                                                               ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:223: error: cannot find symbol
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
                                                                      ^
  symbol:   method anyBoolean()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:234: error: cannot find symbol
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(dc);
        ^
  symbol:   method when(DiscriminatorContext)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:240: error: cannot find symbol
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
        ^
  symbol:   class JsonSchemaLocation
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:240: error: cannot find symbol
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
                                      ^
  symbol:   class JsonSchemaLocation
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:243: error: cannot find symbol
        JsonSchema spy = Mockito.spy(schema);
                         ^
  symbol:   variable Mockito
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:244: error: cannot find symbol
        doReturn(Collections.emptyList()).when(spy).getValidators();
        ^
  symbol:   method doReturn(List<Object>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:247: error: cannot find symbol
        Set<ValidationMessage> msgs = spy.validate(ctx, instance, instance, JsonNodePath.empty());
                                                                                        ^
  symbol:   method empty()
  location: class JsonNodePath
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:250: error: cannot find symbol
        verify(dc, times(1)).registerDiscriminator(loc, disc);
                   ^
  symbol:   method times(int)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:252: error: cannot find symbol
        verify(dc, times(2)).registerDiscriminator(loc, disc);
                   ^
  symbol:   method times(int)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:254: error: cannot find symbol
        verify(spy, times(1)).checkDiscriminatorMatch(eq(dc), eq(disc), eq("cat"), any());
                    ^
  symbol:   method times(int)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:254: error: cannot find symbol
        verify(spy, times(1)).checkDiscriminatorMatch(eq(dc), eq(disc), eq("cat"), any());
                                                      ^
  symbol:   method eq(DiscriminatorContext)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:254: error: cannot find symbol
        verify(spy, times(1)).checkDiscriminatorMatch(eq(dc), eq(disc), eq("cat"), any());
                                                              ^
  symbol:   method eq(ObjectNode)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:254: error: cannot find symbol
        verify(spy, times(1)).checkDiscriminatorMatch(eq(dc), eq(disc), eq("cat"), any());
                                                                        ^
  symbol:   method eq(String)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:254: error: cannot find symbol
        verify(spy, times(1)).checkDiscriminatorMatch(eq(dc), eq(disc), eq("cat"), any());
                                                                                   ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:255: error: cannot find symbol
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
                              ^
  symbol:   method never()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:255: error: cannot find symbol
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
                                                 ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:255: error: cannot find symbol
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
                                                        ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:255: error: cannot find symbol
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
                                                               ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:255: error: cannot find symbol
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
                                                                      ^
  symbol:   method anyBoolean()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:267: error: cannot find symbol
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(dc);
        ^
  symbol:   method when(DiscriminatorContext)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:273: error: cannot find symbol
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
        ^
  symbol:   class JsonSchemaLocation
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:273: error: cannot find symbol
        JsonSchemaLocation loc = mock(JsonSchemaLocation.class);
                                      ^
  symbol:   class JsonSchemaLocation
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:275: error: cannot find symbol
        JsonSchema spy = Mockito.spy(schema);
                         ^
  symbol:   variable Mockito
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:276: error: cannot find symbol
        doReturn(Collections.emptyList()).when(spy).getValidators();
        ^
  symbol:   method doReturn(List<Object>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:278: error: cannot find symbol
        Set<ValidationMessage> msgs = spy.validate(ctx, instance, instance, JsonNodePath.empty());
                                                                                        ^
  symbol:   method empty()
  location: class JsonNodePath
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:281: error: cannot find symbol
        verify(dc, times(1)).registerDiscriminator(loc, schemaDisc);
                   ^
  symbol:   method times(int)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:283: error: cannot find symbol
        verify(dc, never()).registerDiscriminator(eq(loc), eq(existingDisc));
                   ^
  symbol:   method never()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:283: error: cannot find symbol
        verify(dc, never()).registerDiscriminator(eq(loc), eq(existingDisc));
                                                           ^
  symbol:   method eq(ObjectNode)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:285: error: cannot find symbol
        verify(spy, times(1)).checkDiscriminatorMatch(eq(dc), eq(existingDisc), eq("dog"), any());
                    ^
  symbol:   method times(int)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:285: error: cannot find symbol
        verify(spy, times(1)).checkDiscriminatorMatch(eq(dc), eq(existingDisc), eq("dog"), any());
                                                      ^
  symbol:   method eq(DiscriminatorContext)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:285: error: cannot find symbol
        verify(spy, times(1)).checkDiscriminatorMatch(eq(dc), eq(existingDisc), eq("dog"), any());
                                                              ^
  symbol:   method eq(ObjectNode)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:285: error: cannot find symbol
        verify(spy, times(1)).checkDiscriminatorMatch(eq(dc), eq(existingDisc), eq("dog"), any());
                                                                                ^
  symbol:   method eq(String)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:285: error: cannot find symbol
        verify(spy, times(1)).checkDiscriminatorMatch(eq(dc), eq(existingDisc), eq("dog"), any());
                                                                                           ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:286: error: cannot find symbol
        verify(resultsHolder, never()).setResult(any(), any(), any(), anyBoolean());
                              ^
  symbol:   method never()
  location: class JsonSchema_validate_Test
100 errors
only showing the first 100 errors, of 265 total; use -Xmaxerrs if you would like to see more

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'dependent classes': 'class `com.networknt.schema.ExecutionContext`:\n\t\nclass `com.networknt.schema.JsonNodePath`:\n\t\nclass `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;\n\t\tcom.fasterxml.jackson.databind.JsonNode com.networknt.schema.BaseJsonValidator.schemaNode;\n\trelated functions:\n\t\tmethod `protected static void registerAndMergeDiscriminator(DiscriminatorContext, ObjectNode, JsonSchema, JsonNodePath)` returns `void`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)`, api document: \n     * Rolls up all nested and compatible discriminators to the root discriminator of the type. Detects attempts to redefine\n     * the <code>propertyName</code> or mappings.\n     *\n     * @param currentDiscriminatorContext the currently active {@link DiscriminatorContext}\n     * @param discriminator               the discriminator to use for the check\n     * @param schema                      the value of the <code>discriminator/propertyName</code> field\n     * @param instanceLocation                          the logging prefix\n     ;\nclass `com.networknt.schema.ValidationMessageHandler`:\n\tdependent fields:\n\t\tcom.networknt.schema.SchemaLocation com.networknt.schema.ValidationMessageHandler.schemaLocation;\n\trelated functions:\n\t\tmethod `protected JsonNode getMessageNode(String, JsonNode, JsonSchema, String)` returns `com.fasterxml.jackson.databind.JsonNode`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)`;\nclass `com.networknt.schema.AllOfValidator`:\n\trelated functions:\n\t\tmethod `protected Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)`;\nclass `com.networknt.schema.PropertiesValidator`:\n\trelated functions:\n\t\tmethod `protected Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)`;\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public JsonSchema getRefSchema(JsonNodePath)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)`;\n\t\tmethod `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)`;\nclass `com.networknt.schema.OneOfValidator`:\n\trelated functions:\n\t\tmethod `protected Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)`;\nclass `com.networknt.schema.PrefixItemsValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)`;\nclass `com.networknt.schema.UnevaluatedPropertiesValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


