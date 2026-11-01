```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OneOfValidator#walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal behaviour when {@code shouldValidateSchema} is {@code true} or {@code false}.</li>
 *   <li>Edge cases such as empty or null internal schema list.</li>
 *   <li>Exception handling for null arguments and for unexpected null returns.</li>
 *   <li>Propagation of runtime exceptions from child schemas.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class OneOfValidator_walk_Test {

    /*** Mocks used for every test ***/
    @Mock ExecutionContext executionContext;
    @Mock JsonNode node;
    @Mock JsonNode rootNode;
    @Mock JsonNodePath instanceLocation;
    @Mock ValidationContext validationContext;
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;

    /*** Child schemas that will be injected into the validator ***/
    @Mock JsonSchema childSchema1;
    @Mock JsonSchema childSchema2;
    @Mock JsonSchema childSchema3; // used for the three‑schema scenario

    /*** The validator under test ***/
    OneOfValidator validator;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed
    }

    @BeforeEach
    void setupBeforeEach() {
        // -----------------------------------------------------------------
        // Build a dummy "oneOf" array node – its content is irrelevant because
        // ValidationContext#newSchema is stubbed to return our mocks.
        // -----------------------------------------------------------------
        JsonNode schemaNode = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                .getObjectMapper()
                .createArrayNode()
                .addObject()   // first subschema
                .addObject();  // second subschema

        // -----------------------------------------------------------------
        // Stub ValidationContext so that each call to newSchema returns a mocked
        // JsonSchema. The order matches the iteration order inside the
        // OneOfValidator constructor.
        // -----------------------------------------------------------------
        when(validationContext.newSchema(any(SchemaLocation.class),
                any(JsonNodePath.class),
                any(JsonNode.class),
                any(JsonSchema.class)))
                .thenReturn(childSchema1)   // index 0
                .thenReturn(childSchema2);  // index 1

        // -----------------------------------------------------------------
        // Create the validator instance. The constructor will fill the private
        // `schemas` list with the two mocked child schemas.
        // -----------------------------------------------------------------
        validator = new OneOfValidator(schemaLocation,
                evaluationPath,
                schemaNode,
                null,
                validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /* --------------------------------------------------------------------- *
     *  Helper to set private fields via reflection.
     * --------------------------------------------------------------------- */
    private static void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* --------------------------------------------------------------------- *
     *  Normal behaviour – shouldValidateSchema == true
     * --------------------------------------------------------------------- */
    @Nested
    class ValidateWhenShouldValidateTrue {

        static Stream<Arguments> provideValidateScenarios() {
            ValidationMessage msgA = mock(ValidationMessage.class);
            ValidationMessage msgB = mock(ValidationMessage.class);
            Set<ValidationMessage> twoMessages = new LinkedHashSet<>(Arrays.asList(msgA, msgB));
            Set<ValidationMessage> empty = Collections.emptySet();

            return Stream.of(
                    Arguments.of(twoMessages, "Set containing two ValidationMessage objects"),
                    Arguments.of(empty, "Empty Set")
            );
        }

        @ParameterizedTest(name = "{index}: {1}")
        @MethodSource("provideValidateScenarios")
        void walkShouldDelegateToValidateAndReturnMessages(Set<ValidationMessage> stubbedMessages,
                                                          String description) {
            // Spy the validator so we can stub the protected validate method.
            OneOfValidator spy = spy(validator);

            // Stub the overload that receives the boolean flag.
            doReturn(stubbedMessages).when(spy)
                    .validate(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(true));

            // Invoke walk with shouldValidateSchema = true.
            Set<ValidationMessage> result = spy.walk(executionContext, node, rootNode, instanceLocation, true);

            // The result must be exactly the stubbed set (including emptiness).
            assertSame(stubbedMessages, result,
                    "walk should return the exact set produced by validate(..., true)");
        }
    }

    /* --------------------------------------------------------------------- *
     *  Normal behaviour – shouldValidateSchema == false
     * --------------------------------------------------------------------- */
    @Nested
    class WalkWhenShouldValidateFalse {

        @Test
        void walkWithEmptySchemasReturnsEmptySet() throws Exception {
            // Replace the internal schemas list with an empty list.
            setPrivateField(validator, "schemas", Collections.emptyList());

            Set<ValidationMessage> result = validator.walk(executionContext, node, rootNode,
                    instanceLocation, false);

            assertTrue(result.isEmpty(), "Result should be empty when there are no child schemas");
            // No child walk should have been invoked.
            verifyNoInteractions(childSchema1, childSchema2);
        }

        @Test
        void walkWithThreeSchemasInvokesEachChildAndReturnsEmptySet() throws Exception {
            // Build a list containing three mocked child schemas.
            List<JsonSchema> threeSchemas = Arrays.asList(childSchema1, childSchema2, childSchema3);
            setPrivateField(validator, "schemas", threeSchemas);

            Set<ValidationMessage> result = validator.walk(executionContext, node, rootNode,
                    instanceLocation, false);

            assertTrue(result.isEmpty(), "Result must be empty – OneOfValidator never aggregates child messages");

            // Verify each child received exactly one walk call with the same arguments.
            for (JsonSchema child : threeSchemas) {
                verify(child, times(1)).walk(executionContext, node, rootNode, instanceLocation, false);
            }
        }

        @Test
        void walkIgnoresMessagesReturnedByChildSchemas() throws Exception {
            // Stub child schemas to return non‑empty sets (the return value is ignored).
            Set<ValidationMessage> childMessages = new HashSet<>(Collections.singleton(mock(ValidationMessage.class)));
            when(childSchema1.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(childMessages);
            when(childSchema2.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(childMessages);

            Set<ValidationMessage> result = validator.walk(executionContext, node, rootNode,
                    instanceLocation, false);

            assertTrue(result.isEmpty(), "OneOfValidator.walk must return an empty set even if children produce messages");
            verify(childSchema1, times(1)).walk(executionContext, node, rootNode, instanceLocation, false);
            verify(childSchema2, times(1)).walk(executionContext, node, rootNode, instanceLocation, false);
        }
    }

    /* --------------------------------------------------------------------- *
     *  Exception scenarios – shouldValidateSchema == true
     * --------------------------------------------------------------------- */
    @Nested
    class WalkExceptionWhenValidateTrue {

        @Test
        void throwsNPEWhenExecutionContextIsNull() {
            OneOfValidator spy = spy(validator);
            // Force the protected validate method to be called with a null context.
            doThrow(NullPointerException.class).when(spy)
                    .validate(isNull(), any(), any(), any(), eq(true));

            assertThrows(NullPointerException.class,
                    () -> spy.walk(null, node, rootNode, instanceLocation, true),
                    "Null ExecutionContext should cause NPE");
        }

        @Test
        void throwsNPEWhenNodeIsNull() {
            OneOfValidator spy = spy(validator);
            doThrow(NullPointerException.class).when(spy)
                    .validate(eq(executionContext), isNull(), any(), any(), eq(true));

            assertThrows(NullPointerException.class,
                    () -> spy.walk(executionContext, null, rootNode, instanceLocation, true),
                    "Null node should cause NPE");
        }

        @Test
        void throwsNPEWhenRootNodeIsNull() {
            OneOfValidator spy = spy(validator);
            doThrow(NullPointerException.class).when(spy)
                    .validate(eq(executionContext), any(), isNull(), any(), eq(true));

            assertThrows(NullPointerException.class,
                    () -> spy.walk(executionContext, node, null, instanceLocation, true),
                    "Null rootNode should cause NPE");
        }

        @Test
        void throwsNPEWhenInstanceLocationIsNull() {
            OneOfValidator spy = spy(validator);
            doThrow(NullPointerException.class).when(spy)
                    .validate(eq(executionContext), any(), any(), isNull(), eq(true));

            assertThrows(NullPointerException.class,
                    () -> spy.walk(executionContext, node, rootNode, null, true),
                    "Null instanceLocation should cause NPE");
        }

        @Test
        void throwsNPEWhenValidateReturnsNull() {
            OneOfValidator spy = spy(validator);
            // Stub validate to return null – addAll(null) will trigger NPE.
            doReturn(null).when(spy)
                    .validate(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(true));

            assertThrows(NullPointerException.class,
                    () -> spy.walk(executionContext, node, rootNode, instanceLocation, true),
                    "validate returning null should cause NPE");
        }
    }

    /* --------------------------------------------------------------------- *
     *  Exception scenarios – shouldValidateSchema == false
     * --------------------------------------------------------------------- */
    @Nested
    class WalkExceptionWhenValidateFalse {

        @Test
        void throwsNPEWhenSchemasListIsNull() throws Exception {
            setPrivateField(validator, "schemas", null); // simulate uninitialized field

            assertThrows(NullPointerException.class,
                    () -> validator.walk(executionContext, node, rootNode, instanceLocation, false),
                    "Iterating over a null schemas list must throw NPE");
        }

        @Test
        void throwsNPEWhenSchemasContainNullElement() throws Exception {
            List<JsonSchema> listWithNull = new ArrayList<>();
            listWithNull.add(childSchema1);
            listWithNull.add(null); // the problematic entry
            setPrivateField(validator, "schemas", listWithNull);

            assertThrows(NullPointerException.class,
                    () -> validator.walk(executionContext, node, rootNode, instanceLocation, false),
                    "Calling walk on a null child schema should throw NPE");
        }

        @Test
        void propagatesRuntimeExceptionFromChildWalk() throws Exception {
            RuntimeException runtime = new RuntimeException("child failure");
            doThrow(runtime).when(childSchema1).walk(any(), any(), any(), any(), anyBoolean());

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> validator.walk(executionContext, node, rootNode, instanceLocation, false),
                    "Exception from child schema must propagate");
            assertSame(runtime, thrown, "Propagated exception should be the original one");
        }
    }
}
```