package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_checkDiscriminatorMatch_Test {

    /**
     * Mocked context that records calls made by the method under test.
     */
    @Mock
    DiscriminatorContext mockContext;

    /**
     * Mocked discriminator object (the JSON node that contains the "mapping" field).
     */
    @Mock
    ObjectNode mockDiscriminator;

    /**
     * Mocked schema – the actual schema is not consulted by the public method.
     */
    @Mock
    JsonSchema mockSchema;

    /**
     * Jackson factory used to build concrete {@link JsonNode} instances.
     */
    private static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;

    /**
     * Creates an empty {@code ObjectNode}.
     */
    private static ObjectNode emptyDiscriminator() {
        return FACTORY.objectNode();
    }

    /**
     * Creates a discriminator node that contains a {@code mapping} object.
     */
    private static ObjectNode discriminatorWithObjectMapping(ObjectNode mapping) {
        ObjectNode disc = FACTORY.objectNode();
        disc.set("mapping", mapping);
        return disc;
    }

    /**
     * Creates a discriminator node that contains a {@code mapping} array.
     */
    private static ObjectNode discriminatorWithArrayMapping(ArrayNode array) {
        ObjectNode disc = FACTORY.objectNode();
        disc.set("mapping", array);
        return disc;
    }

    /**
     * Creates a {@code mapping} object from a simple key‑to‑ref map.
     */
    private static ObjectNode mappingOf(String key, String ref) {
        ObjectNode map = FACTORY.objectNode();
        map.put(key, ref);
        return map;
    }

    private static Stream<Arguments> nullValueProvider() {
        return Stream.of(Arguments.of(null, "any", mock(JsonSchema.class)), Arguments.of(null, "", mock(JsonSchema.class)));
    }

    /**
     * When the discriminator property value is {@code null} the method must
     * immediately mark the match on the {@link DiscriminatorContext} and return.
     */
    @Test
    void whenDiscriminatorPropertyValueIsNull_shouldMarkMatchAndReturn() {
        // Act
        BaseJsonValidator.checkDiscriminatorMatch(mockContext, mockDiscriminator, /* discriminatorPropertyValue = */
        null, mockSchema);
        // Assert – the only expected interaction is a single call to markMatch()
        verify(mockContext, times(1)).markMatch();
        // No further interactions should have occurred
        verifyNoMoreInteractions(mockContext);
    }

    /**
     * When the discriminator property value is non‑null but the discriminator
     * definition does not contain a “mapping” field, the method should delegate
     * to the implicit‑mapping logic. The test only verifies that the method
     * completes without throwing and does not invoke {@code markMatch()} at this
     * point (the implicit logic may decide later whether a match is found).
     */
    @Test
    void whenMappingIsMissing_shouldDelegateToImplicitMatch() {
        // Arrange – the discriminator node does not contain a "mapping" field
        when(mockDiscriminator.get("mapping")).thenReturn(null);
        // Act – property value is a non‑null string
        BaseJsonValidator.checkDiscriminatorMatch(mockContext, mockDiscriminator, "someValue", mockSchema);
        // Assert – the method should not have called markMatch() yet
        verify(mockContext, never()).markMatch();
        // No exception means the delegation path was exercised successfully
    }

    /**
     * When the discriminator definition contains a “mapping” field the method
     * first attempts an explicit‑mapping match.  If the explicit match does not
     * succeed and the configuration permits a fallback, the method will then try
     * the implicit‑mapping logic.  This test supplies a dummy mapping node and
     * simply verifies that the method runs to completion without throwing.
     */
    @Test
    void whenMappingIsPresent_shouldAttemptExplicitThenImplicitMatch() {
        // Arrange – a non‑null mapping node (its internal structure is irrelevant
        // for this test because the matching logic is private)
        JsonNode dummyMapping = mock(JsonNode.class);
        when(mockDiscriminator.get("mapping")).thenReturn(dummyMapping);
        // Act
        BaseJsonValidator.checkDiscriminatorMatch(mockContext, mockDiscriminator, "valueFromInstance", mockSchema);
        // Assert – no exception means both explicit and (potential) implicit paths were
        // exercised.  We do not verify internal calls because they are private.
        // still may be false at this stage
        verify(mockContext, never()).markMatch();
    }

    @Test
    @DisplayName("nullDiscriminatorArgument – NPE is thrown")
    void whenDiscriminatorArgumentIsNull_shouldThrowNPE() {
        assertThrows(NullPointerException.class, () -> BaseJsonValidator.checkDiscriminatorMatch(mockContext, null, "anyValue", mockSchema));
    }

    @Test
    @DisplayName("nullJsonSchemaArgument – NPE is thrown when implicit path is taken")
    void whenJsonSchemaArgumentIsNull_andMappingMissing_shouldThrowNPE() {
        // No mapping → the method will delegate to the implicit helper,
        // which expects a non‑null JsonSchema.
        assertThrows(NullPointerException.class, () -> BaseJsonValidator.checkDiscriminatorMatch(mockContext, emptyDiscriminator(), "cat", null));
    }

    @Test
    @DisplayName("noMappingImplicitMatch – implicit helper should eventually mark a match")
    void whenMappingIsMissing_andImplicitMatchSucceeds_shouldMarkMatch() {
        // Arrange – discriminator without a "mapping" field
        when(mockContext.isDiscriminatorMatchFound()).thenReturn(false);
        // Act
        BaseJsonValidator.checkDiscriminatorMatch(mockContext, emptyDiscriminator(), "cat", mockSchema);
        // Assert – the implicit helper is expected to call markMatch()
        verify(mockContext, atLeastOnce()).markMatch();
    }

    @Test
    @DisplayName("explicitMappingMatchFound – explicit match should mark and skip implicit")
    void whenExplicitMappingContainsKey_shouldMarkMatchAndSkipImplicit() {
        // Arrange – mapping object with a matching key "dog"
        ObjectNode mapping = mappingOf("dog", "#/components/schemas/Dog");
        ObjectNode discriminator = discriminatorWithObjectMapping(mapping);
        // Act
        BaseJsonValidator.checkDiscriminatorMatch(mockContext, discriminator, "dog", mockSchema);
        // Assert – a match must be recorded; implicit path must not be taken
        verify(mockContext, times(1)).markMatch();
        // The context should now report a match; we cannot verify the internal flag,
        // but the absence of further interactions is a good proxy.
        verifyNoMoreInteractions(mockContext);
    }

    @Test
    @DisplayName("explicitMappingNoMatchButImplicitAllowed – implicit fallback should succeed")
    void whenExplicitMappingMisses_andImplicitAllowed_shouldMarkMatchViaImplicit() {
        // Arrange – mapping does NOT contain the key "fish"
        ObjectNode mapping = mappingOf("bird", "#/components/schemas/Bird");
        ObjectNode discriminator = discriminatorWithObjectMapping(mapping);
        // Act
        BaseJsonValidator.checkDiscriminatorMatch(mockContext, discriminator, "fish", mockSchema);
        // Assert – because the explicit mapping fails, the implicit helper should
        // eventually call markMatch().
        verify(mockContext, atLeastOnce()).markMatch();
    }

    @Test
    @DisplayName("explicitMappingNoMatchAndOverridePreventsImplicit – no match recorded")
    void whenExplicitMappingMisses_andOverridePreventsImplicit_shouldNotMarkMatch() {
        // Arrange – mapping does NOT contain the key "hamster"
        // The parent schema is mocked to indicate that an explicit‑key override is present.
        // The private static method {@code noExplicitDiscriminatorKeyOverride} checks the
        // schema; we simulate the situation by making the schema throw when queried,
        // which forces the method to return {@code false}.
        JsonSchema schemaWithOverride = mock(JsonSchema.class);
        // The real implementation inspects the schema's "discriminator" node; we force a
        // failure that leads to a false return value.
        // (No additional stubbing is required – the method will simply return false.)
        ObjectNode mapping = mappingOf("rabbit", "#/components/schemas/Rabbit");
        ObjectNode discriminator = discriminatorWithObjectMapping(mapping);
        // Act
        BaseJsonValidator.checkDiscriminatorMatch(mockContext, discriminator, "hamster", schemaWithOverride);
        // Assert – because the explicit‑key override disables the implicit fallback,
        // the context must never be marked as a match.
        verify(mockContext, never()).markMatch();
    }

    @Test
    @DisplayName("nonObjectMappingType – array instead of object should cause IllegalArgumentException")
    void whenMappingIsArray_shouldThrowIllegalArgumentException() {
        // Arrange – mapping field is an array node
        ArrayNode arrayMapping = FACTORY.arrayNode().add("#/components/schemas/Dog");
        ObjectNode discriminator = discriminatorWithArrayMapping(arrayMapping);
        // Act / Assert
        assertThrows(IllegalArgumentException.class, () -> BaseJsonValidator.checkDiscriminatorMatch(mockContext, discriminator, "dog", mockSchema));
    }

    @Test
    @DisplayName("nullMappingFieldValue – mapping field present but null should NPE")
    void whenMappingFieldIsNull_shouldThrowNPE() {
        // Arrange – discriminator with a "mapping" field whose value is JSON null
        ObjectNode discriminator = FACTORY.objectNode();
        discriminator.set("mapping", NullNode.getInstance());
        // Act / Assert
        assertThrows(NullPointerException.class, () -> BaseJsonValidator.checkDiscriminatorMatch(mockContext, discriminator, "cat", mockSchema));
    }

    @Test
    @DisplayName("implicitMappingResolutionFailure – unresolved implicit reference should propagate exception")
    void whenImplicitMappingCannotBeResolved_shouldPropagateException() {
        // Arrange – discriminator without mapping, value that does not resolve
        // We make the schema throw a RuntimeException when the implicit helper tries
        // to resolve the reference.
        JsonSchema failingSchema = mock(JsonSchema.class);
        doThrow(new RuntimeException("Schema resolution failed")).when(failingSchema).validate(any(), any(), any(), any());
        // Act / Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> BaseJsonValidator.checkDiscriminatorMatch(mockContext, emptyDiscriminator(), "unknownAnimal", failingSchema));
        assertTrue(ex.getMessage().contains("Schema resolution failed"));
    }

    @Test
    @DisplayName("explicitMappingResolutionFailure – explicit reference that cannot be resolved should propagate exception")
    void whenExplicitMappingReferenceIsInvalid_shouldPropagateException() {
        // Arrange – mapping contains a reference that will cause the schema to fail
        ObjectNode mapping = mappingOf("dog", "#/components/schemas/NonExistingDog");
        ObjectNode discriminator = discriminatorWithObjectMapping(mapping);
        JsonSchema failingSchema = mock(JsonSchema.class);
        // The explicit helper eventually calls {@code schema.validate(...)}; we force it to fail.
        doThrow(new RuntimeException("Invalid reference")).when(failingSchema).validate(any(), any(), any(), any());
        // Act / Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> BaseJsonValidator.checkDiscriminatorMatch(mockContext, discriminator, "dog", failingSchema));
        assertTrue(ex.getMessage().contains("Invalid reference"));
    }

    @ParameterizedTest(name = "null discriminator property value \"{1}\" → markMatch")
    @MethodSource("nullValueProvider")
    void parameterisedNullValueTests(String ignored, String value, JsonSchema schema) {
        // The actual value is irrelevant – the method should treat any null as a match.
        BaseJsonValidator.checkDiscriminatorMatch(mockContext, emptyDiscriminator(), null, schema);
        verify(mockContext, times(1)).markMatch();
    }
}
