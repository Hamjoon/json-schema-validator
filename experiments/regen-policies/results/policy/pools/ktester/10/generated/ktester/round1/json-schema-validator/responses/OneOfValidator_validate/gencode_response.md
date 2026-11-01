```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OneOfValidator#validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)}.
 *
 * The test suite covers the exhaustive list of scenarios described in the specification JSON.
 * <p>
 *   • Walk enabled / disabled<br>
 *   • Fail‑fast flag<br>
 *   • Different numbers of matching subschemas<br>
 *   • State flags (matchedNode, validationEnabled)<br>
 *   • Edge cases such as null arguments and null returns from child schemas<br>
 * </p>
 *
 * The validator internally keeps a private {@code List<JsonSchema> schemas} field.
 * Tests manipulate this list via reflection so that each scenario can control exactly
 * which child schemas participate.
 */
@ExtendWith(MockitoExtension.class)
class OneOfValidatorValidateTest {

    // ----- shared collaborators -------------------------------------------------
    ExecutionContext executionContext;
    CollectorContext collectorContext;
    ExecutionConfig executionConfig;
    ValidatorState validatorState;
    ValidationContext validationContext;
    SchemaLocation schemaLocation;
    JsonNodePath instanceLocation;
    JsonSchema parentSchema;
    JsonNode schemaNode;

    // child schemas used in many tests
    JsonSchema childSchema0;
    JsonSchema childSchema1;

    OneOfValidator validator;

    // ----- common setup ---------------------------------------------------------
    @BeforeEach
    void setUp() {
        // ----- execution context & related collaborators -----
        executionContext = mock(ExecutionContext.class);
        collectorContext = mock(CollectorContext.class);
        executionConfig = mock(ExecutionConfig.class);
        validatorState = mock(ValidatorState.class);

        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(executionContext.getExecutionConfig()).thenReturn(executionConfig);
        when(executionContext.getValidatorState()).thenReturn(validatorState);

        // default state behaviour (can be overridden per test)
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);
        when(executionConfig.getLocale()).thenReturn(Locale.ENGLISH);

        // ----- dynamic scopes (enter/exit) -----
        when(collectorContext.enterDynamicScope()).thenAnswer(i -> mock(Scope.class));
        when(collectorContext.exitDynamicScope()).thenAnswer(i -> mock(Scope.class));
        when(collectorContext.getEvaluatedItems()).thenReturn(new HashSet<>());
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<>());

        // ----- validation context & schema construction -----
        validationContext = mock(ValidationContext.class);
        schemaLocation = mock(SchemaLocation.class);
        instanceLocation = mock(JsonNodePath.class);
        parentSchema = mock(JsonSchema.class);
        schemaNode = mock(JsonNode.class);

        // By default the schema node contains two child schemas.
        when(schemaNode.size()).thenReturn(2);
        JsonNode childNode0 = mock(JsonNode.class);
        JsonNode childNode1 = mock(JsonNode.class);
        when(schemaNode.get(0)).thenReturn(childNode0);
        when(schemaNode.get(1)).thenReturn(childNode1);

        childSchema0 = mock(JsonSchema.class);
        childSchema1 = mock(JsonSchema.class);
        when(validationContext.newSchema(any(), any(), eq(childNode0), any())).thenReturn(childSchema0);
        when(validationContext.newSchema(any(), any(), eq(childNode1), any())).thenReturn(childSchema1);

        // ----- create validator under test -----
        validator = new OneOfValidator(schemaLocation, instanceLocation, schemaNode, parentSchema, validationContext);
    }

    // -------------------------------------------------------------------------
    // Helper utilities
    // -------------------------------------------------------------------------

    /** Replace the private {@code schemas} list with the provided one. */
    private void setValidatorSchemas(List<JsonSchema> schemas) throws Exception {
        Field f = OneOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        f.set(validator, schemas);
    }

    /** Set the private {@code failFast} flag (inherited from BaseJsonValidator). */
    private void setFailFast(boolean failFast) throws Exception {
        Field f = BaseJsonValidator.class.getDeclaredField("failFast");
        f.setAccessible(true);
        f.set(validator, failFast);
    }

    /** Create a fresh ExecutionContext mock with the given state flags. */
    private ExecutionContext mockExecutionContext(boolean walkEnabled,
                                                  boolean failFast,
                                                  boolean validationEnabled,
                                                  List<JsonSchema> schemas) throws Exception {
        ExecutionContext ctx = mock(ExecutionContext.class);
        CollectorContext cc = mock(CollectorContext.class);
        ExecutionConfig ec = mock(ExecutionConfig.class);
        ValidatorState vs = mock(ValidatorState.class);

        when(ctx.getCollectorContext()).thenReturn(cc);
        when(ctx.getExecutionConfig()).thenReturn(ec);
        when(ctx.getValidatorState()).thenReturn(vs);
        when(vs.isWalkEnabled()).thenReturn(walkEnabled);
        when(vs.isValidationEnabled()).thenReturn(validationEnabled);
        when(vs.hasMatchedNode()).thenReturn(true); // default – can be overridden later
        when(ec.getLocale()).thenReturn(Locale.ENGLISH);
        when(cc.enterDynamicScope()).thenAnswer(i -> mock(Scope.class));
        when(cc.exitDynamicScope()).thenAnswer(i -> mock(Scope.class));
        when(cc.getEvaluatedItems()).thenReturn(new HashSet<>());
        when(cc.getEvaluatedProperties()).thenReturn(new HashSet<>());

        // inject the supplied schemas list into a new validator instance
        OneOfValidator v = new OneOfValidator(schemaLocation, instanceLocation, schemaNode, parentSchema, validationContext);
        setValidatorSchemas(schemas);
        setFailFast(failFast);
        // expose the validator via a wrapper object for the test
        // (the test methods will use the class‑level `validator` field, so we replace it)
        validator = v;
        return ctx;
    }

    // -------------------------------------------------------------------------
    // Existing basic tests (kept for completeness)
    // -------------------------------------------------------------------------

    @Test
    void validate_whenExactlyOneSchemaMatches_returnsNoErrors() {
        ValidationMessage childError = mock(ValidationMessage.class);
        when(childSchema0.validate(any(), any(), any(), any())).thenReturn(Set.of(childError));
        when(childSchema1.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.validate(
                executionContext,
                mock(JsonNode.class),
                mock(JsonNode.class),
                mock(JsonNodePath.class));

        assertTrue(result.isEmpty(), "Expected no validation errors when exactly one schema matches");
    }

    @Test
    void validate_whenNoSchemaMatches_returnsOneOfErrorAndChildErrors() {
        ValidationMessage error0 = mock(ValidationMessage.class);
        ValidationMessage error1 = mock(ValidationMessage.class);
        when(childSchema0.validate(any(), any(), any(), any())).thenReturn(Set.of(error0));
        when(childSchema1.validate(any(), any(), any(), any())).thenReturn(Set.of(error1));

        Set<ValidationMessage> result = validator.validate(
                executionContext,
                mock(JsonNode.class),
                mock(JsonNode.class),
                mock(JsonNodePath.class));

        assertEquals(3, result.size(), "Expected OneOf error plus two child errors");
        assertTrue(result.contains(error0));
        assertTrue(result.contains(error1));
    }

    @Test
    void validate_whenMultipleSchemasMatch_returnsOneOfErrorOnly() {
        when(childSchema0.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        when(childSchema1.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.validate(
                executionContext,
                mock(JsonNode.class),
                mock(JsonNode.class),
                mock(JsonNodePath.class));

        assertEquals(1, result.size(), "Expected a single OneOf error when more than one schema matches");
    }

    // -------------------------------------------------------------------------
    // Parameterized tests for the exhaustive JSON spec
    // -------------------------------------------------------------------------

    /** Data holder for a single scenario. */
    private static class Scenario {
        final String description;
        final boolean walkEnabled;
        final boolean failFast;
        final boolean validationEnabled;
        final List<JsonSchema> schemas; // order matters
        final List<Set<ValidationMessage>> schemaValidateReturns; // per schema
        final List<Set<ValidationMessage>> schemaWalkReturns; // per schema (used when walkEnabled)
        final Boolean hasMatchedNodeAfterValidate; // null = default true
        final Boolean hasMatchedNodeAfterWalk; // null = default true
        final Class<? extends Throwable> expectedException; // null = no exception
        final int expectedErrorCount; // -1 = not asserted (exception case)

        Scenario(String description,
                 boolean walkEnabled,
                 boolean failFast,
                 boolean validationEnabled,
                 List<JsonSchema> schemas,
                 List<Set<ValidationMessage>> schemaValidateReturns,
                 List<Set<ValidationMessage>> schemaWalkReturns,
                 Boolean hasMatchedNodeAfterValidate,
                 Boolean hasMatchedNodeAfterWalk,
                 Class<? extends Throwable> expectedException,
                 int expectedErrorCount) {
            this.description = description;
            this.walkEnabled = walkEnabled;
            this.failFast = failFast;
            this.validationEnabled = validationEnabled;
            this.schemas = schemas;
            this.schemaValidateReturns = schemaValidateReturns;
            this.schemaWalkReturns = schemaWalkReturns;
            this.hasMatchedNodeAfterValidate = hasMatchedNodeAfterValidate;
            this.hasMatchedNodeAfterWalk = hasMatchedNodeAfterWalk;
            this.expectedException = expectedException;
            this.expectedErrorCount = expectedErrorCount;
        }

        @Override public String toString() {
            return description;
        }
    }

    /** Provides all scenarios defined in the specification JSON. */
    private static Stream<Scenario> scenarios() {
        // reusable mocks
        ValidationMessage vm0 = mock(ValidationMessage.class);
        ValidationMessage vm1 = mock(ValidationMessage.class);
        ValidationMessage vm2 = mock(ValidationMessage.class);

        // child schema placeholders – will be replaced by reflection in each test
        JsonSchema validSchema = mock(JsonSchema.class);
        JsonSchema errorSchema = mock(JsonSchema.class);
        JsonSchema resetMatchedNodeSchema = mock(JsonSchema.class);
        JsonSchema nullReturningValidateSchema = mock(JsonSchema.class);
        JsonSchema nullReturningWalkSchema = mock(JsonSchema.class);

        // Helper to create a list with a single element
        List<JsonSchema> list(JsonSchema... schemas) { return Arrays.asList(schemas); }

        return Stream.of(
                // 1. Walk disabled – exactly one matching schema
                new Scenario(
                        "Walk disabled – exactly one matching schema",
                        false, false, true,
                        list(validSchema),
                        List.of(Collections.emptySet()), // validate returns empty -> match
                        null,
                        null, null,
                        null,
                        0),

                // 2. Walk disabled – schema returns errors
                new Scenario(
                        "Walk disabled – schema returns errors",
                        false, false, true,
                        list(errorSchema),
                        List.of(Set.of(vm0)),
                        null,
                        null, null,
                        null,
                        2), // OneOf + child error

                // 3. Walk disabled – schema matches but state.hasMatchedNode is false
                new Scenario(
                        "Walk disabled – schema matches but state.hasMatchedNode is false",
                        false, false, true,
                        list(validSchema),
                        List.of(Collections.emptySet()),
                        null,
                        false, // after validate, hasMatchedNode false
                        null,
                        null,
                        null,
                        1), // only OneOf

                // 4. Walk disabled – more than one matching schema
                new Scenario(
                        "Walk disabled – more than one matching schema",
                        false, false, true,
                        list(validSchema, validSchema),
                        List.of(Collections.emptySet(), Collections.emptySet()),
                        null,
                        null, null,
                        null,
                        1),

                // 5. Fail‑fast mode – invalid number of matching schemas
                new Scenario(
                        "Fail‑fast mode – invalid number of matching schemas",
                        false, true, true,
                        list(errorSchema),
                        List.of(Set.of(vm0)),
                        null,
                        null, null,
                        JsonSchemaException.class,
                        -1),

                // 6. Walk enabled – schema validation performed via walk()
                new Scenario(
                        "Walk enabled – schema validation performed via walk()",
                        true, false, true,
                        list(validSchema),
                        null,
                        List.of(Collections.emptySet()),
                        null,
                        true,
                        null,
                        0),

                // 7. Walk enabled – schema matches but state.hasMatchedNode false after walk
                new Scenario(
                        "Walk enabled – schema matches but state.hasMatchedNode false after walk",
                        true, false, true,
                        list(validSchema),
                        null,
                        List.of(Collections.emptySet()),
                        null,
                        false,
                        null,
                        1),

                // 8. No schemas registered – always OneOf error
                new Scenario(
                        "No schemas registered – always OneOf error",
                        false, false, true,
                        Collections.emptyList(),
                        Collections.emptyList(),
                        null,
                        null, null,
                        null,
                        1),

                // 9. Walk enabled – schema returns errors
                new Scenario(
                        "Walk enabled – schema returns errors",
                        true, false, true,
                        list(errorSchema),
                        null,
                        List.of(Set.of(vm0)),
                        null,
                        null,
                        null,
                        2),

                // 10. Fail‑fast mode – exactly one matching schema
                new Scenario(
                        "Fail‑fast mode – exactly one matching schema",
                        false, true, true,
                        list(validSchema),
                        List.of(Collections.emptySet()),
                        null,
                        null, null,
                        null,
                        0),

                // 11. Walk enabled – validation disabled (state.isValidationEnabled = false)
                new Scenario(
                        "Walk enabled – validation disabled (state.isValidationEnabled = false)",
                        true, false, false,
                        list(validSchema),
                        null,
                        List.of(Collections.emptySet()),
                        null,
                        true,
                        null,
                        0),

                // 12. Multiple schemas – first matches, second returns errors
                new Scenario(
                        "Multiple schemas – first matches, second returns errors",
                        false, false, true,
                        list(validSchema, errorSchema),
                        List.of(Collections.emptySet(), Set.of(vm0)),
                        null,
                        null, null,
                        null,
                        2),

                // 13. State.matchedNode reset between schemas – first matches but resets to false before second
                new Scenario(
                        "State.matchedNode reset between schemas – first matches but resets to false before second",
                        false, false, true,
                        list(validSchema, resetMatchedNodeSchema),
                        List.of(Collections.emptySet(), Collections.emptySet()),
                        null,
                        null, // after first validate matchedNode true, after second we will force false via stub
                        false,
                        null,
                        1),

                // 14. Fail‑fast mode – multiple matching schemas
                new Scenario(
                        "Fail‑fast mode – multiple matching schemas",
                        false, true, true,
                        list(validSchema, validSchema),
                        List.of(Collections.emptySet(), Collections.emptySet()),
                        null,
                        null, null,
                        JsonSchemaException.class,
                        -1),

                // 15. Null executionContext
                new Scenario(
                        "Null executionContext",
                        false, false, true,
                        list(validSchema),
                        List.of(Collections.emptySet()),
                        null,
                        null, null,
                        NullPointerException.class,
                        -1),

                // 16. Null collectorContext inside executionContext
                new Scenario(
                        "Null collectorContext inside executionContext",
                        false, false, true,
                        list(validSchema),
                        List.of(Collections.emptySet()),
                        null,
                        null, null,
                        NullPointerException.class,
                        -1),

                // 17. Null schema element in schemas list
                new Scenario(
                        "Null schema element in schemas list",
                        false, false, true,
                        list(validSchema, null),
                        List.of(Collections.emptySet(), null),
                        null,
                        null, null,
                        NullPointerException.class,
                        -1),

                // 18. Schema.validate returns null
                new Scenario(
                        "Schema.validate returns null",
                        false, false, true,
                        list(nullReturningValidateSchema),
                        List.of(null),
                        null,
                        null, null,
                        NullPointerException.class,
                        -1),

                // 19. Schema.walk returns null
                new Scenario(
                        "Schema.walk returns null",
                        true, false, true,
                        list(nullReturningWalkSchema),
                        null,
                        List.of(null),
                        null,
                        null,
                        NullPointerException.class,
                        -1)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void validate_scenarios(Scenario sc) throws Exception {
        // ---------------------------------------------------------------------
        // 1️⃣ Prepare validator's schema list
        // ---------------------------------------------------------------------
        setValidatorSchemas(sc.schemas);

        // ---------------------------------------------------------------------
        // 2️⃣ Configure failFast flag if needed
        // ---------------------------------------------------------------------
        setFailFast(sc.failFast);

        // ---------------------------------------------------------------------
        // 3️⃣ Configure the validator state mock according to the scenario
        // ---------------------------------------------------------------------
        when(validatorState.isWalkEnabled()).thenReturn(sc.walkEnabled);
        when(validatorState.isValidationEnabled()).thenReturn(sc.validationEnabled);
        // default hasMatchedNode = true; override later if scenario specifies
        if (sc.hasMatchedNodeAfterValidate != null) {
            // first call after resetMatchedNode(true) will be after validation of each schema
            when(validatorState.hasMatchedNode()).thenReturn(sc.hasMatchedNodeAfterValidate);
        }
        if (sc.hasMatchedNodeAfterWalk != null) {
            when(validatorState.hasMatchedNode()).thenReturn(sc.hasMatchedNodeAfterWalk);
        }

        // ---------------------------------------------------------------------
        // 4️⃣ Stub child schema behavior (validate or walk)
        // ---------------------------------------------------------------------
        for (int i = 0; i < sc.schemas.size(); i++) {
            JsonSchema schema = sc.schemas.get(i);
            if (schema == null) {
                // leave as null – will trigger NPE later as expected
                continue;
            }
            if (sc.walkEnabled) {
                Set<ValidationMessage> walkResult = (sc.schemaWalkReturns != null && i < sc.schemaWalkReturns.size())
                        ? sc.schemaWalkReturns.get(i) : Collections.emptySet();
                when(schema.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(walkResult);
            } else {
                Set<ValidationMessage> validateResult = (sc.schemaValidateReturns != null && i < sc.schemaValidateReturns.size())
                        ? sc.schemaValidateReturns.get(i) : Collections.emptySet();
                when(schema.validate(any(), any(), any(), any())).thenReturn(validateResult);
            }
        }

        // ---------------------------------------------------------------------
        // 5️⃣ Special handling for "null executionContext" and "null collectorContext"
        // ---------------------------------------------------------------------
        ExecutionContext ctxToUse = executionContext;
        if (sc.expectedException == NullPointerException.class && sc.description.contains("Null executionContext")) {
            ctxToUse = null;
        } else if (sc.expectedException == NullPointerException.class && sc.description.contains("Null collectorContext")) {
            // make the collectorContext inside executionContext null
            when(executionContext.getCollectorContext()).thenReturn(null);
        }

        // ---------------------------------------------------------------------
        // 6️⃣ Execute & verify
        // ---------------------------------------------------------------------
        if (sc.expectedException != null) {
            assertThrows(sc.expectedException,
                    () -> validator.validate(ctxToUse,
                            mock(JsonNode.class),
                            mock(JsonNode.class),
                            mock(JsonNodePath.class)),
                    "Expected exception for scenario: " + sc.description);
            return;
        }

        Set<ValidationMessage> result = validator.validate(
                ctxToUse,
                mock(JsonNode.class),
                mock(JsonNode.class),
                mock(JsonNodePath.class));

        assertEquals(sc.expectedErrorCount, result.size(),
                "Unexpected number of errors for scenario: " + sc.description);
    }
}
```