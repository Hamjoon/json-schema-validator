package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_2_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // real instance created by the factory
    private JsonSchema schema;

    // spy on the real instance
    private JsonSchema spySchema;

    // a simple JSON document to validate
    private JsonNode sampleDataNode;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // 1️⃣  Build a minimal, valid JSON‑Schema (an empty object is a valid schema).
        String schemaJson = "{}";
        // 2️⃣  Obtain a JsonSchema instance from the public factory.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        schema = factory.getSchema(schemaJson);
        // 3️⃣  Spy on the instance so we can verify the delegation.
        spySchema = Mockito.spy(schema);
        // 4️⃣  Prepare a simple JSON document that will be passed to validate().
        sampleDataNode = MAPPER.readTree("{\"any\":\"value\"}");
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the spy to avoid cross‑test interference.
        if (spySchema != null) {
            Mockito.reset(spySchema);
        }
    }

    /**
     * Parses a JSON literal (object, array, string, number, boolean or null).
     */
    private static JsonNode readJson(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON supplied to test", e);
        }
    }

    /**
     * Creates a {@link JsonSchema} from a JSON‑Schema string.
     */
    private static JsonSchema createSchema(String schema) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        return factory.getSchema(schema);
    }

    @Test
    void validate_ShouldDelegateToValidateWithDefaultOutputFormat() {
        // Arrange – stub the overloaded method that the simple overload should call.
        Set<ValidationMessage> stubbedResult = Collections.emptySet();
        doReturn(stubbedResult).when(spySchema).validate(eq(sampleDataNode), eq(OutputFormat.DEFAULT));
        // Act – call the method under test.
        Set<ValidationMessage> result = spySchema.validate(sampleDataNode);
        // Assert – the delegating method was invoked exactly once with the expected arguments.
        verify(spySchema, times(1)).validate(eq(sampleDataNode), eq(OutputFormat.DEFAULT));
        // And the result is exactly what the stub returned.
        assertSame(stubbedResult, result, "The result should be the stubbed empty set");
    }

    @Test
    void validate_ValidInput_SatisfiesSchema() {
        // schema: {"type":"object","required":["name"],"properties":{"name":{"type":"string"}}}
        String schemaStr = """
            {
              "type": "object",
              "required": ["name"],
              "properties": {
                "name": { "type": "string" }
              }
            }""";
        JsonSchema s = createSchema(schemaStr);
        JsonNode data = readJson("{\"name\":\"John Doe\"}");
        Set<ValidationMessage> msgs = s.validate(data);
        assertTrue(msgs.isEmpty(), "Valid input should produce no validation messages");
    }

    @Test
    void validate_MissingRequiredProperty_ProducesError() {
        String schemaStr = """
            {
              "type": "object",
              "required": ["name"],
              "properties": {
                "name": { "type": "string" }
              }
            }""";
        JsonSchema s = createSchema(schemaStr);
        JsonNode data = readJson("{}");
        Set<ValidationMessage> msgs = s.validate(data);
        assertEquals(1, msgs.size(), "Exactly one validation message expected");
        ValidationMessage vm = msgs.iterator().next();
        assertEquals("required", vm.getCode());
        assertTrue(vm.getMessage().contains("name"));
    }

    @Test
    void validate_TypeMismatch_RootNodeNotObject() {
        String schemaStr = """
            {
              "type": "object",
              "required": ["name"],
              "properties": {
                "name": { "type": "string" }
              }
            }""";
        JsonSchema s = createSchema(schemaStr);
        JsonNode data = readJson("[1,2,3]");
        Set<ValidationMessage> msgs = s.validate(data);
        assertEquals(1, msgs.size());
        ValidationMessage vm = msgs.iterator().next();
        assertEquals("type", vm.getCode());
        assertTrue(vm.getMessage().toLowerCase().contains("array"));
    }

    @Test
    void validate_NullRootNode_ThrowsNPE() {
        String schemaStr = "{}";
        JsonSchema s = createSchema(schemaStr);
        assertThrows(NullPointerException.class, () -> s.validate((JsonNode) null));
    }

    @Test
    void validate_SchemaWithNoValidators_AlwaysSucceeds() {
        // Empty schema -> no validators.
        JsonSchema s = createSchema("{}");
        JsonNode data = readJson("\"any string\"");
        Set<ValidationMessage> msgs = s.validate(data);
        assertTrue(msgs.isEmpty(), "Empty schema should accept any JSON value");
    }

    @Test
    void validate_EnumValidation_ValueNotInEnum() {
        String schemaStr = """
            {
              "enum": ["GREEN","BLUE"]
            }""";
        JsonSchema s = createSchema(schemaStr);
        JsonNode data = readJson("\"RED\"");
        Set<ValidationMessage> msgs = s.validate(data);
        assertEquals(1, msgs.size());
        ValidationMessage vm = msgs.iterator().next();
        assertEquals("enum", vm.getCode());
        assertTrue(vm.getMessage().contains("RED"));
    }

    @Test
    void validate_MultipleErrors_MissingRequiredAndTypeMismatch() {
        String schemaStr = """
            {
              "type": "object",
              "required": ["name"],
              "properties": {
                "name": { "type": "string" }
              }
            }""";
        JsonSchema s = createSchema(schemaStr);
        JsonNode data = readJson("123");
        Set<ValidationMessage> msgs = s.validate(data);
        // Expect two messages – type and required (required may be omitted because type fails first,
        // but the library aggregates both, so we accept 1 or 2).
        assertTrue(msgs.size() >= 1 && msgs.size() <= 2, "Expected 1‑2 validation messages, got " + msgs.size());
        boolean hasType = msgs.stream().anyMatch(m -> "type".equals(m.getCode()));
        boolean hasRequired = msgs.stream().anyMatch(m -> "required".equals(m.getCode()));
        assertTrue(hasType, "Should contain a type error");
        assertTrue(hasRequired, "Should contain a required‑property error");
    }

    @Test
    void validate_BooleanSchemaFalse_AlwaysFails() {
        // literal boolean false schema
        JsonSchema s = createSchema("false");
        JsonNode data = readJson("\"any\"");
        Set<ValidationMessage> msgs = s.validate(data);
        assertEquals(1, msgs.size());
        ValidationMessage vm = msgs.iterator().next();
        assertEquals("false", vm.getCode());
    }

    @Test
    void validate_BooleanSchemaTrue_AlwaysSucceeds() {
        // literal boolean true schema
        JsonSchema s = createSchema("true");
        JsonNode data = readJson("null");
        Set<ValidationMessage> msgs = s.validate(data);
        assertTrue(msgs.isEmpty(), "True schema must accept any instance");
    }

    @Test
    void validate_PatternValidation_StringDoesNotMatchRegex() {
        String schemaStr = """
            {
              "type": "string",
              "pattern": "^[A-Z]+$"
            }""";
        JsonSchema s = createSchema(schemaStr);
        JsonNode data = readJson("\"abc123\"");
        Set<ValidationMessage> msgs = s.validate(data);
        assertEquals(1, msgs.size());
        ValidationMessage vm = msgs.iterator().next();
        assertEquals("pattern", vm.getCode());
    }

    @Test
    void validate_NumberRange_ValueOutOfBounds() {
        String schemaStr = """
            {
              "type": "number",
              "maximum": 100
            }""";
        JsonSchema s = createSchema(schemaStr);
        JsonNode data = readJson("150");
        Set<ValidationMessage> msgs = s.validate(data);
        assertEquals(1, msgs.size());
        ValidationMessage vm = msgs.iterator().next();
        assertEquals("maximum", vm.getCode());
    }

    @Test
    void validate_ArrayLength_TooFewItems() {
        String schemaStr = """
            {
              "type": "array",
              "minItems": 3,
              "items": { "type": "integer" }
            }""";
        JsonSchema s = createSchema(schemaStr);
        JsonNode data = readJson("[1,2]");
        Set<ValidationMessage> msgs = s.validate(data);
        assertEquals(1, msgs.size());
        ValidationMessage vm = msgs.iterator().next();
        assertEquals("minItems", vm.getCode());
    }

    @Test
    void validate_OneOfValidation_NoneMatch() {
        String schemaStr = """
            {
              "oneOf": [
                { "const": "yes" },
                { "const": "no" }
              ]
            }""";
        JsonSchema s = createSchema(schemaStr);
        JsonNode data = readJson("\"maybe\"");
        Set<ValidationMessage> msgs = s.validate(data);
        assertEquals(1, msgs.size());
        ValidationMessage vm = msgs.iterator().next();
        assertEquals("oneOf", vm.getCode());
    }

    @Test
    void validate_AnyOfValidation_AtLeastOneMatches() {
        String schemaStr = """
            {
              "anyOf": [
                { "type": "number", "minimum": 0 },
                { "type": "string", "pattern": "^[a-z]+$" }
              ]
            }""";
        JsonSchema s = createSchema(schemaStr);
        JsonNode data = readJson("42");
        Set<ValidationMessage> msgs = s.validate(data);
        assertTrue(msgs.isEmpty(), "AnyOf with a matching alternative should succeed");
    }

    @Test
    void validate_NotValidation_ValueMatchesProhibitedSchema() {
        String schemaStr = """
            {
              "not": { "const": "forbidden" }
            }""";
        JsonSchema s = createSchema(schemaStr);
        JsonNode data = readJson("\"forbidden\"");
        Set<ValidationMessage> msgs = s.validate(data);
        assertEquals(1, msgs.size());
        ValidationMessage vm = msgs.iterator().next();
        assertEquals("not", vm.getCode());
    }

    @Test
    void validate_ConditionalValidation_IfThenClauseEnforced() {
        String schemaStr = """
            {
              "type": "object",
              "properties": {
                "type": { "enum": ["premium","basic"] },
                "discount": { "type": "number", "minimum": 0 }
              },
              "if": {
                "properties": { "type": { "const": "premium" } },
                "required": ["type"]
              },
              "then": {
                "required": ["discount"]
              }
            }""";
        JsonSchema s = createSchema(schemaStr);
        JsonNode data = readJson("{\"type\":\"premium\",\"discount\":-5}");
        Set<ValidationMessage> msgs = s.validate(data);
        assertEquals(1, msgs.size());
        ValidationMessage vm = msgs.iterator().next();
        assertEquals("minimum", vm.getCode());
    }

    @Test
    void validate_AdditionalPropertiesFalse_ExtraPropertyError() {
        String schemaStr = """
            {
              "type": "object",
              "required": ["name"],
              "properties": {
                "name": { "type": "string" }
              },
              "additionalProperties": false
            }""";
        JsonSchema s = createSchema(schemaStr);
        JsonNode data = readJson("{\"name\":\"John\",\"age\":30}");
        Set<ValidationMessage> msgs = s.validate(data);
        assertEquals(1, msgs.size());
        ValidationMessage vm = msgs.iterator().next();
        assertEquals("additionalProperties", vm.getCode());
    }

    @Test
    void validate_FormatValidation_InvalidEmail() {
        String schemaStr = """
            {
              "type": "string",
              "format": "email"
            }""";
        JsonSchema s = createSchema(schemaStr);
        JsonNode data = readJson("\"not-an-email\"");
        Set<ValidationMessage> msgs = s.validate(data);
        assertEquals(1, msgs.size());
        ValidationMessage vm = msgs.iterator().next();
        assertEquals("format", vm.getCode());
    }

    @Test
    void validate_RecursiveAnchor_SelfReferentialSchemaValidates() {
        String schemaStr = """
            {
              "$id": "http://example.com/rec.json",
              "$recursiveAnchor": true,
              "type": "object",
              "properties": {
                "node": { "$recursiveRef": "#" }
              },
              "required": []
            }""";
        JsonSchema s = createSchema(schemaStr);
        JsonNode data = readJson("{\"node\":{\"node\":{\"value\":5}}}");
        Set<ValidationMessage> msgs = s.validate(data);
        assertTrue(msgs.isEmpty(), "Recursive schema should validate without stack overflow");
    }

    @Test
    void validate_InternalRef_ResolvesDefinitionCorrectly() {
        String schemaStr = """
            {
              "definitions": {
                "code": {
                  "type": "string",
                  "pattern": "^[a-z]{3}[0-9]{3}$"
                }
              },
              "$ref": "#/definitions/code"
            }""";
        JsonSchema s = createSchema(schemaStr);
        JsonNode data = readJson("\"abc123\"");
        Set<ValidationMessage> msgs = s.validate(data);
        assertTrue(msgs.isEmpty(), "Internal $ref should resolve and validate the pattern");
    }

    @Test
    void validate_RelativeRefWithId_ResolvesBaseUriCorrectly() {
        // main schema
        String mainSchema = """
            {
              "$id": "http://example.com/schemas/main.json",
              "type": "object",
              "properties": {
                "age": { "$ref": "./sub.json" }
              }
            }""";
        // sub schema (normally would be loaded from classpath; we simulate by registering it manually)
        String subSchema = """
            {
              "$id": "http://example.com/schemas/sub.json",
              "type": "integer"
            }""";
        // Register the sub‑schema in the factory's URI cache via a mocked static call.
        // The library loads external schemas via the classpath or URL; for the test we
        // mock the static method JsonSchemaFactory.getSchema(String) to return our sub‑schema.
        try (MockedStatic<JsonSchemaFactory> mockedFactory = Mockito.mockStatic(JsonSchemaFactory.class)) {
            JsonSchemaFactory realFactory = JsonSchemaFactory.getInstance(VersionFlag.V7);
            JsonSchema sub = realFactory.getSchema(subSchema);
            mockedFactory.when(() -> JsonSchemaFactory.getInstance(VersionFlag.V7)).thenReturn(realFactory);
            mockedFactory.when(() -> realFactory.getSchema("http://example.com/schemas/sub.json")).thenReturn(sub);
            JsonSchema s = realFactory.getSchema(mainSchema);
            JsonNode data = readJson("{\"age\":42}");
            Set<ValidationMessage> msgs = s.validate(data);
            assertTrue(msgs.isEmpty(), "Relative $ref with $id should resolve and validate the integer");
        }
    }

    @Test
    void validate_InvalidRef_UnresolvedReferenceThrowsException() {
        String schemaStr = """
            {
              "$ref": "#/nonexistent"
            }""";
        JsonSchema s = createSchema(schemaStr);
        JsonNode data = readJson("{\"name\":\"John\"}");
        // The library throws a JsonSchemaException (runtime) when a $ref cannot be resolved.
        assertThrows(JsonSchemaException.class, () -> s.validate(data));
    }

    @Test
    void validate_UnsupportedKeyword_ThrowsIllegalArgumentException() {
        String schemaStr = """
            {
              "unknownKeyword": true
            }""";
        // Construction itself should fail.
        assertThrows(IllegalArgumentException.class, () -> createSchema(schemaStr));
    }

    @Test
    void validate_InvalidFormatValue_ThrowsIllegalArgumentException() {
        String schemaStr = """
            {
              "type": "string",
              "format": "unknown-format"
            }""";
        // The validator throws when encountering an unknown format during validation.
        JsonSchema s = createSchema(schemaStr);
        JsonNode data = readJson("\"someValue\"");
        assertThrows(IllegalArgumentException.class, () -> s.validate(data));
    }

    @Test
    void validate_CircularRefWithoutRecursiveAnchor_LeadsToStackOverflowError() {
        // schema A references B, schema B references A – both without $recursiveAnchor.
        String schemaA = """
            {
              "$id": "http://example.com/a.json",
              "$ref": "b.json"
            }""";
        String schemaB = """
            {
              "$id": "http://example.com/b.json",
              "$ref": "a.json"
            }""";
        // Mock static loading of the two schemas.
        try (MockedStatic<JsonSchemaFactory> mockedFactory = Mockito.mockStatic(JsonSchemaFactory.class)) {
            JsonSchemaFactory realFactory = JsonSchemaFactory.getInstance(VersionFlag.V7);
            JsonSchema a = realFactory.getSchema(schemaA);
            JsonSchema b = realFactory.getSchema(schemaB);
            mockedFactory.when(() -> JsonSchemaFactory.getInstance(VersionFlag.V7)).thenReturn(realFactory);
            mockedFactory.when(() -> realFactory.getSchema("http://example.com/a.json")).thenReturn(a);
            mockedFactory.when(() -> realFactory.getSchema("http://example.com/b.json")).thenReturn(b);
            // start validation with schema A
            JsonSchema s = a;
            JsonNode data = readJson("\"any\"");
            // Depending on implementation the library may detect the cycle and throw IllegalStateException,
            // otherwise a StackOverflowError will surface.
            assertThrows(RuntimeException.class, () -> s.validate(data));
        }
    }

    @Test
    void validate_MalformedId_UrlDecodingErrorWrappedInRuntimeException() {
        // The $id contains characters that are not valid UTF‑8 when decoded with the default charset.
        String schemaStr = """
            {
              "$id": "http://example.com/ma%zzl%id",
              "type": "string"
            }""";
        JsonSchema s = createSchema(schemaStr);
        JsonNode data = readJson("\"test\"");
        // The library wraps UnsupportedEncodingException in a RuntimeException.
        assertThrows(RuntimeException.class, () -> s.validate(data));
    }
}
