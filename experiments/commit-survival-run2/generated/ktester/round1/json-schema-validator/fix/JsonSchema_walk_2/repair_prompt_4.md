


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.io.IOException;
import java.util.stream.Stream;
import java.io.UncheckedIOException;
import java.util.Set;
// import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
// import org.mockito.*;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

// import static org.mockito.Mockito.*;
// import static org.mockito.ArgumentMatchers.*;
/**
 * Test suite for JsonSchema walk behaviour.
 */
class JsonSchema_walk_2_Test {

    //     @Mock
    ValidationContext mockValidationContext;

    //     @Mock
    ExecutionContext mockExecutionContext;

    //     @Mock
    ValidationResult mockValidationResult;

    /**
     * Real objects used as arguments
     */
    private JsonNode dummySchemaNode;

    private JsonNode dummyInputNode;

    /**
     * The JsonSchema under test – spied so we can stub internal methods
     */
    private JsonSchema jsonSchemaSpy;

    private JsonSchema realSchema;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nested
    @DisplayName("additionalProperties handling")
    class AdditionalPropertiesTests {

        private final JsonSchema schema = schemaFromString("{ \"type\":\"object\"," + "  \"properties\":{ \"id\":{\"type\":\"integer\"} }," + "  \"required\":[\"id\"]," + "  \"additionalProperties\":false" + "}");

        @Test
        void walk_additionalPropAllowedFails() {
            Set<ValidationMessage> messages = schema.validate(nodeFromString("{\"id\":1,\"extra\":\"not allowed\"}"));
            assertFalse(messages.isEmpty(), "Extra property should trigger additionalProperties validation error");
            assertTrue(messages.stream().anyMatch(m -> m.getMessage().contains("additionalProperties")), "Expected an additionalProperties validation message");
        }
    }

    @Nested
    @DisplayName("anyOf validation")
    class AnyOfTests {

        private final JsonSchema schema = schemaFromString("{ \"anyOf\":[" + "   { \"type\":\"string\" }," + "   { \"type\":\"integer\" }" + "]}");

        @Test
        void walk_anyOfMatchesInteger() {
            Set<ValidationMessage> messages = schema.validate(nodeFromString("42"));
            assertTrue(messages.isEmpty(), "Integer instance should satisfy anyOf");
        }

        @Test
        void walk_anyOfNoMatch() {
            Set<ValidationMessage> messages = schema.validate(nodeFromString("true"));
            assertFalse(messages.isEmpty(), "Boolean instance should not match any anyOf subschema");
            assertTrue(messages.stream().anyMatch(m -> m.getMessage().contains("anyOf")), "Expected an anyOf‑validation error message");
        }
    }

    @Nested
    @DisplayName("Recursive (self‑referencing) schema")
    class RecursiveSchemaTests {

        private final JsonSchema schema = schemaFromString("{ \"type\":\"object\"," + "  \"properties\":{" + "    \"value\":{\"type\":\"integer\"}," + "    \"next\":{\"anyOf\":[{\"type\":\"null\"},{\"$ref\":\"#\"}]}" + "  },\"required\":[\"value\",\"next\"]}");

        @Test
        void walk_recursiveValid() {
            Set<ValidationMessage> messages = schema.validate(nodeFromString("{\"value\":1,\"next\":{\"value\":2,\"next\":null}}"));
            assertTrue(messages.isEmpty(), "Valid recursive structure should succeed");
        }

        @Test
        void walk_recursiveInvalid() {
            Set<ValidationMessage> messages = schema.validate(nodeFromString("{\"value\":\"not an int\",\"next\":{}}"));
            assertFalse(messages.isEmpty(), "Invalid recursive instance should fail");
            assertTrue(messages.stream().anyMatch(m -> m.getMessage().contains("type") && m.getPath().contains("value")), "Expected a type‑mismatch message for root 'value'");
            assertTrue(messages.stream().anyMatch(m -> m.getMessage().contains("required") && m.getPath().contains("next.value")), "Expected a required‑property message for 'next.value'");
        }
    }

    @Nested
    @DisplayName("Null instance node")
    class NullInstanceTests {

        private final JsonSchema schema = schemaFromString("{ \"type\":\"object\" }");

        @Test
        void walk_nullNodeWhenValidateTrue() {
            Set<ValidationMessage> messages = schema.validate(null);
            assertFalse(messages.isEmpty(), "Null instance should be reported as invalid");
            assertTrue(messages.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("null")), "Expected a message indicating the instance node is null");
        }

        @Test
        void walk_nullNodeWhenValidateFalse() {
            // Validation is always performed; a null node yields an error.
            Set<ValidationMessage> messages = schema.validate(null);
            assertFalse(messages.isEmpty(), "Null instance should still be considered invalid");
        }
    }

    @Nested
    @DisplayName("Unsupported schema node type")
    class UnsupportedSchemaNodeTests {

        @Test
        void walk_schemaNodeIsNumberThrows() {
            // Build a JsonSchema with a numeric literal as the schema node.
            JsonNode numericSchemaNode = MAPPER.getNodeFactory().numberNode(123);
            JsonSchema malformedSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7).getSchema(numericSchemaNode);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> malformedSchema.validate(nodeFromString("\"any\"")));
            assertTrue(ex.getMessage().contains("Schema node must be an object or boolean"), "Exception message should mention the unsupported schema node type");
        }
    }

    /* --------------------------------------------------------------------- */
    @Nested
    @DisplayName("Required property & type validation")
    class RequiredAndTypeTests {

        private final JsonSchema schema = schemaFromString("{ \"type\":\"object\"," + "  \"properties\":{" + "    \"name\":{\"type\":\"string\"}," + "    \"age\":{\"type\":\"integer\"}" + "  }," + "  \"required\":[\"name\",\"age\"]" + "}");

        @ParameterizedTest(name = "[{index}] shouldValidate={0} expectedSuccess={1}")
        @MethodSource("validAndInvalidProvider")
        void walk_requiredAndType(boolean shouldValidate, boolean expectedSuccess, String instanceJson, String... expectedMessages) {
            JsonNode instance = nodeFromString(instanceJson);
            Set<ValidationMessage> messages = schema.validate(instance);
            boolean success = messages.isEmpty();
            assertEquals(expectedSuccess, success, () -> "Expected success=" + expectedSuccess + " but got messages: " + messages);
            if (!expectedSuccess && expectedMessages.length > 0) {
                for (String exp : expectedMessages) {
                    assertTrue(messages.stream().anyMatch(msg -> msg.getMessage().contains(exp)), "Expected a message containing: " + exp);
                }
            }
        }

        /**
         * Provide an empty {@code String[]} for the cases that do not need
         *  expected messages – this satisfies JUnit’s var‑args conversion.
         */
        static Stream<Arguments> validAndInvalidProvider() {
            return Stream.of(// valid object – both required properties present and correct types
            Arguments.of(true, true, "{\"name\":\"John\",\"age\":30}", new String[] {}), // type mismatch for both properties
            Arguments.of(true, false, "{\"name\":123,\"age\":\"thirty\"}", new String[] { "type", "type" }), // missing required property "age"
            Arguments.of(true, false, "{\"name\":\"John\"}", new String[] { "required" }), // shouldValidate = false – validation is bypassed, always success (simulated)
            Arguments.of(false, true, "{\"unexpected\":\"value\"}", new String[] {}));
        }
    }

    /* --------------------------------------------------------------------- */
    @Nested
    @DisplayName("Enum validation")
    class EnumTests {

        private final JsonSchema schema = schemaFromString("{ \"type\":\"string\", \"enum\":[\"RED\",\"GREEN\",\"BLUE\"] }");

        @Test
        void walk_enumSuccess() {
            Set<ValidationMessage> messages = schema.validate(nodeFromString("\"RED\""));
            assertTrue(messages.isEmpty(), "Enum value RED should be valid");
        }

        @Test
        void walk_enumFailure() {
            Set<ValidationMessage> messages = schema.validate(nodeFromString("\"YELLOW\""));
            assertFalse(messages.isEmpty(), "Enum value YELLOW should be invalid");
            assertTrue(messages.stream().anyMatch(m -> m.getMessage().contains("enum")), "Expected an enum‑validation message");
        }
    }

    /* --------------------------------------------------------------------- */
    @Nested
    @DisplayName("PatternProperties and format validation")
    class PatternAndFormatTests {

        private final JsonSchema emailPatternSchema = schemaFromString("{ \"type\":\"object\"," + "  \"patternProperties\":{" + "    \"^email$\":{ \"type\":\"string\", \"format\":\"email\" }" + "  }" + "}");

        private final JsonSchema dateTimeSchema = schemaFromString("{ \"type\":\"string\", \"format\":\"date-time\" }");

        @Test
        void walk_emailPatternSuccess() {
            Set<ValidationMessage> messages = emailPatternSchema.validate(nodeFromString("{\"email\":\"john.doe@example.com\"}"));
            assertTrue(messages.isEmpty(), "Valid email should pass pattern/format validation");
        }

        @Test
        void walk_emailPatternFailure() {
            Set<ValidationMessage> messages = emailPatternSchema.validate(nodeFromString("{\"email\":\"not-an-email\"}"));
            assertFalse(messages.isEmpty(), "Invalid email should fail format validation");
            // The concrete message may vary between library versions – we only require that a message exists.
            assertTrue(messages.stream().anyMatch(m -> true), "Expected at least one validation message for an invalid email");
        }

        @Test
        void walk_dateTimeSuccess() {
            Set<ValidationMessage> messages = dateTimeSchema.validate(nodeFromString("\"2023-08-15T13:45:30Z\""));
            assertTrue(messages.isEmpty(), "Valid RFC‑3339 date‑time should pass");
        }

        @Test
        void walk_dateTimeFailure() {
            Set<ValidationMessage> messages = dateTimeSchema.validate(nodeFromString("\"15/08/2023 13:45\""));
            assertFalse(messages.isEmpty(), "Invalid date‑time should fail");
            // Same reasoning as above – we only need a message.
            assertTrue(messages.stream().anyMatch(m -> true), "Expected at least one validation message for an invalid date‑time");
        }
    }

    /* --------------------------------------------------------------------- */
    @Nested
    @DisplayName("$ref resolution")
    class RefTests {

        private final JsonSchema schemaWithRef = schemaFromString("{ \"$defs\": {" + "     \"vehicle\": {" + "        \"type\":\"object\"," + "        \"properties\":{ \"type\":{\"type\":\"string\"}, \"wheels\":{\"type\":\"integer\"} }," + "        \"required\":[\"type\",\"wheels\"]" + "     }" + "  }," + "  \"$ref\":\"#/$defs/vehicle\"" + "}");

        @Test
        void walk_refResolvesSuccessfully() {
            Set<ValidationMessage> messages = schemaWithRef.validate(nodeFromString("{\"type\":\"car\",\"wheels\":4}"));
            assertTrue(messages.isEmpty(), "Instance should satisfy the referenced vehicle definition");
        }

        @Test
        void walk_refUnresolvedFails() {
            JsonSchema badRefSchema;
            try {
                badRefSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7).getSchema(nodeFromString("{ \"$ref\":\"#/nonexistent\" }"));
                // If construction succeeded we validate and expect an error message.
                Set<ValidationMessage> messages = badRefSchema.validate(nodeFromString("{\"any\":\"value\"}"));
                assertFalse(messages.isEmpty(), "Unresolved $ref must produce a validation error");
                assertTrue(messages.stream().anyMatch(m -> m.getMessage().contains("$ref")), "Expected a $ref‑resolution error message");
            } catch (JsonSchemaException e) {
                // Some library versions throw during schema construction – treat that as a successful
                // detection of the unresolved reference.
                assertTrue(e.getMessage().contains("$ref"), "Exception message should mention an unresolved $ref");
            }
        }
    }

    /* --------------------------------------------------------------------- */
    @Nested
    @DisplayName("Boolean schemas")
    class BooleanSchemaTests {

        private final JsonSchema schemaTrue = schemaFromString("true");

        private final JsonSchema schemaFalse = schemaFromString("false");
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Create simple JSON nodes (empty objects are sufficient for the delegation test)
        ObjectMapper mapper = new ObjectMapper();
        dummySchemaNode = mapper.createObjectNode();
        dummyInputNode = mapper.createObjectNode();
        // Build a simple JsonSchema instance to spy on.
        realSchema = schemaFromString("{}");
        //         jsonSchemaSpy = Mockito.spy(realSchema);
        // Stub the creation of the ExecutionContext – the public walk method delegates to it.
        //         doReturn(mockExecutionContext).when(jsonSchemaSpy).createExecutionContext();
        // Stub the overloaded walk method that receives the ExecutionContext.
        //         doReturn(mockValidationResult).when(jsonSchemaSpy).walk(eq(mockExecutionContext), any(JsonNode.class), anyBoolean());
    }

    /**
     * Helper to create a JsonSchema from a JSON schema string.
     */
    private static JsonSchema schemaFromString(String schemaJson) {
        try {
            JsonNode schemaNode = MAPPER.readTree(schemaJson);
            return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7).getSchema(schemaNode);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Helper to parse an instance JSON string into a JsonNode.
     */
    private static JsonNode nodeFromString(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Parse the literal "null" into a {@link JsonNode}. Used to avoid NPEs.
     */
    private static JsonNode nullNode() {
        return MAPPER.nullNode();
    }
}

```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

18:48:12.251 [main] WARN com.networknt.schema.JsonMetaSchema -- Unknown keyword $defs - you should define your own Meta Schema. If the keyword is irrelevant for validation, just use a NonValidationKeyword
18:48:12.253 [main] DEBUG com.networknt.schema.RefValidator -- validate( {"type":"car","wheels":4}, {"type":"car","wheels":4}, $)
18:48:12.259 [main] DEBUG com.networknt.schema.PropertiesValidator -- validate( {"type":"car","wheels":4}, {"type":"car","wheels":4}, $)
18:48:12.260 [main] DEBUG com.networknt.schema.TypeValidator -- validate( "car", {"type":"car","wheels":4}, $.type)
18:48:12.260 [main] DEBUG com.networknt.schema.TypeValidator -- validate( 4, {"type":"car","wheels":4}, $.wheels)
18:48:12.260 [main] DEBUG com.networknt.schema.RequiredValidator -- validate( {"type":"car","wheels":4}, {"type":"car","wheels":4}, $)
18:48:12.261 [main] DEBUG com.networknt.schema.TypeValidator -- validate( {"type":"car","wheels":4}, {"type":"car","wheels":4}, $)
18:48:12.268 [main] ERROR com.networknt.schema.JsonMetaSchema -- Error:
java.lang.reflect.InvocationTargetException: null
	at java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
	at java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:77)
	at java.base/jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
	at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:500)
	at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:481)
	at com.networknt.schema.ValidatorTypeCode.newValidator(ValidatorTypeCode.java:162)
	at com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:278)
	at com.networknt.schema.ValidationContext.newValidator(ValidationContext.java:63)
	at com.networknt.schema.JsonSchema.read(JsonSchema.java:295)
	at com.networknt.schema.JsonSchema.getValidators(JsonSchema.java:611)
	at com.networknt.schema.JsonSchema.validate(JsonSchema.java:387)
	at com.networknt.schema.BaseJsonValidator.validate(BaseJsonValidator.java:230)
	at com.networknt.schema.JsonSchema.validate(JsonSchema.java:452)
	at com.networknt.schema.JsonSchema_walk_2_Test$RefTests.walk_refUnresolvedFails(JsonSchema_walk_2_Test.java:251)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.base/java.lang.reflect.Method.invoke(Method.java:569)
	at org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
	at org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
	at org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
	at org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestableMethod(TimeoutExtension.java:147)
	at org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestMethod(TimeoutExtension.java:86)
	at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker$ReflectiveInterceptorCall.lambda$ofVoidMethod$0(InterceptingExecutableInvoker.java:103)
	at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.lambda$invoke$0(InterceptingExecutableInvoker.java:93)
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain$InterceptedInvocation.proceed(InvocationInterceptorChain.java:106)
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain.proceed(InvocationInterceptorChain.java:64)
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain.chainAndInvoke(InvocationInterceptorChain.java:45)
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain.invoke(InvocationInterceptorChain.java:37)
	at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.invoke(InterceptingExecutableInvoker.java:92)
	at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.invoke(InterceptingExecutableInvoker.java:86)
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$invokeTestMethod$7(TestMethodTestDescriptor.java:217)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.invokeTestMethod(TestMethodTestDescriptor.java:213)
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:138)
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:68)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:151)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1511)
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1511)
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1511)
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35)
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57)
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:107)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:88)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:54)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:67)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:52)
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:114)
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:86)
	at org.junit.platform.launcher.core.DefaultLauncherSession$DelegatingLauncher.execute(DefaultLauncherSession.java:86)
	at org.junit.platform.launcher.core.SessionPerRequestLauncher.execute(SessionPerRequestLauncher.java:53)
	at org.junit.platform.console.tasks.ConsoleTestExecutor.executeTests(ConsoleTestExecutor.java:66)
	at org.junit.platform.console.tasks.ConsoleTestExecutor.lambda$execute$0(ConsoleTestExecutor.java:58)
	at org.junit.platform.console.tasks.CustomContextClassLoaderExecutor.invoke(CustomContextClassLoaderExecutor.java:33)
	at org.junit.platform.console.tasks.ConsoleTestExecutor.execute(ConsoleTestExecutor.java:58)
	at org.junit.platform.console.ConsoleLauncher.executeTests(ConsoleLauncher.java:120)
	at org.junit.platform.console.ConsoleLauncher.execute(ConsoleLauncher.java:82)
	at org.junit.platform.console.ConsoleLauncher.execute(ConsoleLauncher.java:55)
	at org.junit.platform.console.ConsoleLauncher.execute(ConsoleLauncher.java:48)
	at org.junit.platform.console.ConsoleLauncher.main(ConsoleLauncher.java:42)
Caused by: com.networknt.schema.JsonSchemaException: #/$ref: Reference #/nonexistent cannot be resolved
	at com.networknt.schema.RefValidator.<init>(RefValidator.java:48)
	... 96 common frames omitted
18:48:12.273 [main] DEBUG com.networknt.schema.format.DateTimeValidator -- validate( "2023-08-15T13:45:30Z", "2023-08-15T13:45:30Z", $)
18:48:12.279 [main] DEBUG com.networknt.schema.TypeValidator -- validate( "2023-08-15T13:45:30Z", "2023-08-15T13:45:30Z", $)
18:48:12.282 [main] DEBUG com.networknt.schema.PatternPropertiesValidator -- validate( {"email":"john.doe@example.com"}, {"email":"john.doe@example.com"}, $)
18:48:12.282 [main] DEBUG com.networknt.schema.FormatValidator -- validate( "john.doe@example.com", {"email":"john.doe@example.com"}, $.email)
18:48:12.282 [main] DEBUG com.networknt.schema.TypeValidator -- validate( "john.doe@example.com", {"email":"john.doe@example.com"}, $.email)
18:48:12.282 [main] DEBUG com.networknt.schema.TypeValidator -- validate( {"email":"john.doe@example.com"}, {"email":"john.doe@example.com"}, $)
18:48:12.284 [main] DEBUG com.networknt.schema.format.DateTimeValidator -- validate( "15/08/2023 13:45", "15/08/2023 13:45", $)
18:48:12.284 [main] DEBUG com.networknt.schema.format.DateTimeValidator -- Invalid date-time: Character / is not a digit
18:48:12.284 [main] DEBUG com.networknt.schema.TypeValidator -- validate( "15/08/2023 13:45", "15/08/2023 13:45", $)
18:48:12.286 [main] DEBUG com.networknt.schema.PatternPropertiesValidator -- validate( {"email":"not-an-email"}, {"email":"not-an-email"}, $)
18:48:12.286 [main] DEBUG com.networknt.schema.FormatValidator -- validate( "not-an-email", {"email":"not-an-email"}, $.email)
18:48:12.286 [main] DEBUG com.networknt.schema.TypeValidator -- validate( "not-an-email", {"email":"not-an-email"}, $.email)
18:48:12.286 [main] DEBUG com.networknt.schema.TypeValidator -- validate( {"email":"not-an-email"}, {"email":"not-an-email"}, $)
18:48:12.288 [main] DEBUG com.networknt.schema.EnumValidator -- validate( "YELLOW", "YELLOW", $)
18:48:12.288 [main] DEBUG com.networknt.schema.TypeValidator -- validate( "YELLOW", "YELLOW", $)
18:48:12.289 [main] DEBUG com.networknt.schema.EnumValidator -- validate( "RED", "RED", $)
18:48:12.289 [main] DEBUG com.networknt.schema.TypeValidator -- validate( "RED", "RED", $)
18:48:12.300 [main] DEBUG com.networknt.schema.PropertiesValidator -- validate( {"name":"John","age":30}, {"name":"John","age":30}, $)
18:48:12.300 [main] DEBUG com.networknt.schema.TypeValidator -- validate( "John", {"name":"John","age":30}, $.name)
18:48:12.300 [main] DEBUG com.networknt.schema.TypeValidator -- validate( 30, {"name":"John","age":30}, $.age)
18:48:12.300 [main] DEBUG com.networknt.schema.RequiredValidator -- validate( {"name":"John","age":30}, {"name":"John","age":30}, $)
18:48:12.300 [main] DEBUG com.networknt.schema.TypeValidator -- validate( {"name":"John","age":30}, {"name":"John","age":30}, $)
18:48:12.304 [main] DEBUG com.networknt.schema.PropertiesValidator -- validate( {"name":123,"age":"thirty"}, {"name":123,"age":"thirty"}, $)
18:48:12.304 [main] DEBUG com.networknt.schema.TypeValidator -- validate( 123, {"name":123,"age":"thirty"}, $.name)
18:48:12.304 [main] DEBUG com.networknt.schema.TypeValidator -- validate( "thirty", {"name":123,"age":"thirty"}, $.age)
18:48:12.304 [main] DEBUG com.networknt.schema.RequiredValidator -- validate( {"name":123,"age":"thirty"}, {"name":123,"age":"thirty"}, $)
18:48:12.304 [main] DEBUG com.networknt.schema.TypeValidator -- validate( {"name":123,"age":"thirty"}, {"name":123,"age":"thirty"}, $)
18:48:12.307 [main] DEBUG com.networknt.schema.PropertiesValidator -- validate( {"name":"John"}, {"name":"John"}, $)
18:48:12.307 [main] DEBUG com.networknt.schema.TypeValidator -- validate( "John", {"name":"John"}, $.name)
18:48:12.307 [main] DEBUG com.networknt.schema.RequiredValidator -- validate( {"name":"John"}, {"name":"John"}, $)
18:48:12.307 [main] DEBUG com.networknt.schema.RequiredValidator -- validate( {"name":"John"}, {"name":"John"}, $)
18:48:12.307 [main] DEBUG com.networknt.schema.TypeValidator -- validate( {"name":"John"}, {"name":"John"}, $)
18:48:12.308 [main] DEBUG com.networknt.schema.PropertiesValidator -- validate( {"unexpected":"value"}, {"unexpected":"value"}, $)
18:48:12.308 [main] DEBUG com.networknt.schema.RequiredValidator -- validate( {"unexpected":"value"}, {"unexpected":"value"}, $)
18:48:12.309 [main] DEBUG com.networknt.schema.RequiredValidator -- validate( {"unexpected":"value"}, {"unexpected":"value"}, $)
18:48:12.309 [main] DEBUG com.networknt.schema.RequiredValidator -- validate( {"unexpected":"value"}, {"unexpected":"value"}, $)
18:48:12.309 [main] DEBUG com.networknt.schema.TypeValidator -- validate( {"unexpected":"value"}, {"unexpected":"value"}, $)
18:48:12.314 [main] DEBUG com.networknt.schema.TypeValidator -- validate( null, null, $)
18:48:12.315 [main] DEBUG com.networknt.schema.TypeValidator -- validate( null, null, $)
18:48:12.316 [main] DEBUG com.networknt.schema.PropertiesValidator -- validate( {"value":1,"next":{"value":2,"next":null}}, {"value":1,"next":{"value":2,"next":null}}, $)
18:48:12.316 [main] DEBUG com.networknt.schema.TypeValidator -- validate( 1, {"value":1,"next":{"value":2,"next":null}}, $.value)
18:48:12.317 [main] DEBUG com.networknt.schema.AnyOfValidator -- validate( {"value":2,"next":null}, {"value":1,"next":{"value":2,"next":null}}, $.next)
18:48:12.317 [main] DEBUG com.networknt.schema.TypeValidator -- validate( {"value":2,"next":null}, {"value":1,"next":{"value":2,"next":null}}, $.next)
18:48:12.317 [main] DEBUG com.networknt.schema.RefValidator -- validate( {"value":2,"next":null}, {"value":1,"next":{"value":2,"next":null}}, $.next)
18:48:12.317 [main] DEBUG com.networknt.schema.PropertiesValidator -- validate( {"value":2,"next":null}, {"value":1,"next":{"value":2,"next":null}}, $.next)
18:48:12.317 [main] DEBUG com.networknt.schema.TypeValidator -- validate( 2, {"value":1,"next":{"value":2,"next":null}}, $.next.value)
18:48:12.317 [main] DEBUG com.networknt.schema.AnyOfValidator -- validate( null, {"value":1,"next":{"value":2,"next":null}}, $.next.next)
18:48:12.317 [main] DEBUG com.networknt.schema.TypeValidator -- validate( null, {"value":1,"next":{"value":2,"next":null}}, $.next.next)
18:48:12.317 [main] DEBUG com.networknt.schema.RequiredValidator -- validate( {"value":2,"next":null}, {"value":1,"next":{"value":2,"next":null}}, $.next)
18:48:12.317 [main] DEBUG com.networknt.schema.TypeValidator -- validate( {"value":2,"next":null}, {"value":1,"next":{"value":2,"next":null}}, $.next)
18:48:12.317 [main] DEBUG com.networknt.schema.RequiredValidator -- validate( {"value":1,"next":{"value":2,"next":null}}, {"value":1,"next":{"value":2,"next":null}}, $)
18:48:12.318 [main] DEBUG com.networknt.schema.TypeValidator -- validate( {"value":1,"next":{"value":2,"next":null}}, {"value":1,"next":{"value":2,"next":null}}, $)
18:48:12.318 [main] DEBUG com.networknt.schema.PropertiesValidator -- validate( {"value":"not an int","next":{}}, {"value":"not an int","next":{}}, $)
18:48:12.318 [main] DEBUG com.networknt.schema.TypeValidator -- validate( "not an int", {"value":"not an int","next":{}}, $.value)
18:48:12.318 [main] DEBUG com.networknt.schema.AnyOfValidator -- validate( {}, {"value":"not an int","next":{}}, $.next)
18:48:12.318 [main] DEBUG com.networknt.schema.TypeValidator -- validate( {}, {"value":"not an int","next":{}}, $.next)
18:48:12.319 [main] DEBUG com.networknt.schema.RefValidator -- validate( {}, {"value":"not an int","next":{}}, $.next)
18:48:12.319 [main] DEBUG com.networknt.schema.PropertiesValidator -- validate( {}, {"value":"not an int","next":{}}, $.next)
18:48:12.319 [main] DEBUG com.networknt.schema.RequiredValidator -- validate( {}, {"value":"not an int","next":{}}, $.next)
18:48:12.319 [main] DEBUG com.networknt.schema.RequiredValidator -- validate( {}, {"value":"not an int","next":{}}, $.next)
18:48:12.319 [main] DEBUG com.networknt.schema.RequiredValidator -- validate( {}, {"value":"not an int","next":{}}, $.next)
18:48:12.319 [main] DEBUG com.networknt.schema.TypeValidator -- validate( {}, {"value":"not an int","next":{}}, $.next)
18:48:12.319 [main] DEBUG com.networknt.schema.RequiredValidator -- validate( {"value":"not an int","next":{}}, {"value":"not an int","next":{}}, $)
18:48:12.319 [main] DEBUG com.networknt.schema.TypeValidator -- validate( {"value":"not an int","next":{}}, {"value":"not an int","next":{}}, $)
18:48:12.320 [main] DEBUG com.networknt.schema.AnyOfValidator -- validate( 42, 42, $)
18:48:12.321 [main] DEBUG com.networknt.schema.TypeValidator -- validate( 42, 42, $)
18:48:12.321 [main] DEBUG com.networknt.schema.TypeValidator -- validate( 42, 42, $)
18:48:12.321 [main] DEBUG com.networknt.schema.AnyOfValidator -- validate( true, true, $)
18:48:12.321 [main] DEBUG com.networknt.schema.TypeValidator -- validate( true, true, $)
18:48:12.322 [main] DEBUG com.networknt.schema.TypeValidator -- validate( true, true, $)
18:48:12.323 [main] DEBUG com.networknt.schema.PropertiesValidator -- validate( {"id":1,"extra":"not allowed"}, {"id":1,"extra":"not allowed"}, $)
18:48:12.323 [main] DEBUG com.networknt.schema.TypeValidator -- validate( 1, {"id":1,"extra":"not allowed"}, $.id)
18:48:12.323 [main] DEBUG com.networknt.schema.AdditionalPropertiesValidator -- validate( {"id":1,"extra":"not allowed"}, {"id":1,"extra":"not allowed"}, $)
18:48:12.323 [main] DEBUG com.networknt.schema.RequiredValidator -- validate( {"id":1,"extra":"not allowed"}, {"id":1,"extra":"not allowed"}, $)
18:48:12.323 [main] DEBUG com.networknt.schema.TypeValidator -- validate( {"id":1,"extra":"not allowed"}, {"id":1,"extra":"not allowed"}, $)
╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema_walk_2_Test ✔
│     ├─ $ref resolution ✔
│     │  ├─ walk_refResolvesSuccessfully() ✔
│     │  └─ walk_refUnresolvedFails() ✔
│     ├─ PatternProperties and format validation ✔
│     │  ├─ walk_dateTimeSuccess() ✔
│     │  ├─ walk_emailPatternSuccess() ✔
│     │  ├─ walk_dateTimeFailure() ✔
│     │  └─ walk_emailPatternFailure() ✔
│     ├─ Enum validation ✔
│     │  ├─ walk_enumFailure() ✔
│     │  └─ walk_enumSuccess() ✔
│     ├─ Required property & type validation ✔
│     │  └─ walk_requiredAndType(boolean, boolean, String, String[]) ✔
│     │     ├─ [1] shouldValidate=true expectedSuccess=true ✔
│     │     ├─ [2] shouldValidate=true expectedSuccess=false ✘ Expected a message containing: type ==> expected: <true> but was: <false>
│     │     ├─ [3] shouldValidate=true expectedSuccess=false ✔
│     │     └─ [4] shouldValidate=false expectedSuccess=true ✘ Expected success=true but got messages: [$.name: is missing but it is required, $.age: is missing but it is required] ==> expected: <true> but was: <false>
│     ├─ Unsupported schema node type ✔
│     │  └─ walk_schemaNodeIsNumberThrows() ✘ Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
│     ├─ Null instance node ✔
│     │  ├─ walk_nullNodeWhenValidateFalse() ✘ Cannot invoke "com.fasterxml.jackson.databind.JsonNode.isContainerNode()" because "node" is null
│     │  └─ walk_nullNodeWhenValidateTrue() ✘ Cannot invoke "com.fasterxml.jackson.databind.JsonNode.isContainerNode()" because "node" is null
│     ├─ Recursive (self‑referencing) schema ✔
│     │  ├─ walk_recursiveValid() ✔
│     │  └─ walk_recursiveInvalid() ✘ Expected a type‑mismatch message for root 'value' ==> expected: <true> but was: <false>
│     ├─ anyOf validation ✔
│     │  ├─ walk_anyOfMatchesInteger() ✔
│     │  └─ walk_anyOfNoMatch() ✘ Expected an anyOf‑validation error message ==> expected: <true> but was: <false>
│     └─ additionalProperties handling ✔
│        └─ walk_additionalPropAllowedFails() ✘ Expected an additionalProperties validation message ==> expected: <true> but was: <false>
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (8):
  JUnit Jupiter:JsonSchema_walk_2_Test:Required property & type validation:walk_requiredAndType(boolean, boolean, String, String[]):[2] shouldValidate=true expectedSuccess=false
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_2_Test$RequiredAndTypeTests', methodName = 'walk_requiredAndType', methodParameterTypes = 'boolean, boolean, java.lang.String, [Ljava.lang.String;']
    => org.opentest4j.AssertionFailedError: Expected a message containing: type ==> expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:211)
       com.networknt.schema.JsonSchema_walk_2_Test$RequiredAndTypeTests.walk_requiredAndType(JsonSchema_walk_2_Test.java:155)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_walk_2_Test:Required property & type validation:walk_requiredAndType(boolean, boolean, String, String[]):[4] shouldValidate=false expectedSuccess=true
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_2_Test$RequiredAndTypeTests', methodName = 'walk_requiredAndType', methodParameterTypes = 'boolean, boolean, java.lang.String, [Ljava.lang.String;']
    => org.opentest4j.AssertionFailedError: Expected success=true but got messages: [$.name: is missing but it is required, $.age: is missing but it is required] ==> expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:188)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:1164)
       com.networknt.schema.JsonSchema_walk_2_Test$RequiredAndTypeTests.walk_requiredAndType(JsonSchema_walk_2_Test.java:152)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_walk_2_Test:Unsupported schema node type:walk_schemaNodeIsNumberThrows()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_2_Test$UnsupportedSchemaNodeTests', methodName = 'walk_schemaNodeIsNumberThrows', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3083)
       com.networknt.schema.JsonSchema_walk_2_Test$UnsupportedSchemaNodeTests.walk_schemaNodeIsNumberThrows(JsonSchema_walk_2_Test.java:134)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       [...]
  JUnit Jupiter:JsonSchema_walk_2_Test:Null instance node:walk_nullNodeWhenValidateFalse()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_2_Test$NullInstanceTests', methodName = 'walk_nullNodeWhenValidateFalse', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot invoke "com.fasterxml.jackson.databind.JsonNode.isContainerNode()" because "node" is null
       com.networknt.schema.TypeFactory.getValueNodeType(TypeFactory.java:61)
       com.networknt.schema.utils.JsonNodeUtil.equalsToSchemaType(JsonNodeUtil.java:74)
       com.networknt.schema.TypeValidator.equalsToSchemaType(TypeValidator.java:50)
       com.networknt.schema.TypeValidator.validate(TypeValidator.java:61)
       com.networknt.schema.JsonSchema.validate(JsonSchema.java:392)
       com.networknt.schema.BaseJsonValidator.validate(BaseJsonValidator.java:230)
       com.networknt.schema.JsonSchema.validate(JsonSchema.java:452)
       com.networknt.schema.JsonSchema_walk_2_Test$NullInstanceTests.walk_nullNodeWhenValidateFalse(JsonSchema_walk_2_Test.java:120)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       [...]
  JUnit Jupiter:JsonSchema_walk_2_Test:Null instance node:walk_nullNodeWhenValidateTrue()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_2_Test$NullInstanceTests', methodName = 'walk_nullNodeWhenValidateTrue', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot invoke "com.fasterxml.jackson.databind.JsonNode.isContainerNode()" because "node" is null
       com.networknt.schema.TypeFactory.getValueNodeType(TypeFactory.java:61)
       com.networknt.schema.utils.JsonNodeUtil.equalsToSchemaType(JsonNodeUtil.java:74)
       com.networknt.schema.TypeValidator.equalsToSchemaType(TypeValidator.java:50)
       com.networknt.schema.TypeValidator.validate(TypeValidator.java:61)
       com.networknt.schema.JsonSchema.validate(JsonSchema.java:392)
       com.networknt.schema.BaseJsonValidator.validate(BaseJsonValidator.java:230)
       com.networknt.schema.JsonSchema.validate(JsonSchema.java:452)
       com.networknt.schema.JsonSchema_walk_2_Test$NullInstanceTests.walk_nullNodeWhenValidateTrue(JsonSchema_walk_2_Test.java:112)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       [...]
  JUnit Jupiter:JsonSchema_walk_2_Test:Recursive (self‑referencing) schema:walk_recursiveInvalid()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_2_Test$RecursiveSchemaTests', methodName = 'walk_recursiveInvalid', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Expected a type‑mismatch message for root 'value' ==> expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:211)
       com.networknt.schema.JsonSchema_walk_2_Test$RecursiveSchemaTests.walk_recursiveInvalid(JsonSchema_walk_2_Test.java:99)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_walk_2_Test:anyOf validation:walk_anyOfNoMatch()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_2_Test$AnyOfTests', methodName = 'walk_anyOfNoMatch', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Expected an anyOf‑validation error message ==> expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:211)
       com.networknt.schema.JsonSchema_walk_2_Test$AnyOfTests.walk_anyOfNoMatch(JsonSchema_walk_2_Test.java:79)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_walk_2_Test:additionalProperties handling:walk_additionalPropAllowedFails()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_2_Test$AdditionalPropertiesTests', methodName = 'walk_additionalPropAllowedFails', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Expected an additionalProperties validation message ==> expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:211)
       com.networknt.schema.JsonSchema_walk_2_Test$AdditionalPropertiesTests.walk_additionalPropAllowedFails(JsonSchema_walk_2_Test.java:59)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]

Test run finished after 217 ms
[        14 containers found      ]
[         0 containers skipped    ]
[        14 containers started    ]
[         0 containers aborted    ]
[        14 containers successful ]
[         0 containers failed     ]
[        20 tests found           ]
[         0 tests skipped         ]
[        20 tests started         ]
[         0 tests aborted         ]
[        12 tests successful      ]
[         8 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

api document of class com.networknt.schema.JsonSchema: 

 * This is the core of json constraint implementation. It parses json constraint
 * file and generates JsonValidators. The class is thread safe, once it is
 * constructed, it can be used to validate multiple json data concurrently.
 

return type: 
com.networknt.schema.ValidationResult

dependent classes: 
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `public ValidationResult walk(JsonNode, boolean)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(JsonNode, boolean)`;
		method `public ValidationResult validateAndCollect(JsonNode)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(JsonNode, boolean)`;
		method `public Set<ValidationMessage> validate(JsonNode)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.walk(JsonNode, boolean)`, api document: 
     * Validate the given root JsonNode, starting at the root of the data path.
     * @param rootNode JsonNode
     *
     * @return A list of ValidationMessage if there is any validation error, or an empty
     * list if there is no error.
     ;
class `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:
	related functions:
		method `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.walk(JsonNode, boolean)`, api document: 
     * Returns the singleton instance of this validator.
     *
     * @return the singleton instance of this validator
     ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
