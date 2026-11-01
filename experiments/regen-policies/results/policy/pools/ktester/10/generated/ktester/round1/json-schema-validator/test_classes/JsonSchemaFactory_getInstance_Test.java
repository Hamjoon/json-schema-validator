package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.util.function.Consumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getInstance_Test {

    /**
     * Returns the expected meta‑schema URI for a given {@link SpecVersion.VersionFlag}.
     * The mapping follows the JSON‑Schema draft specifications used by the library.
     */
    private static String expectedMetaSchemaId(SpecVersion.VersionFlag flag) {
        return switch(flag) {
            case V4 ->
                "http://json-schema.org/draft-04/schema#";
            case V6 ->
                "http://json-schema.org/draft-06/schema#";
            case V7 ->
                "http://json-schema.org/draft-07/schema#";
            case DRAFT_2019_09 ->
                "https://json-schema.org/draft/2019-09/schema#";
            case DRAFT_2020_12 ->
                "https://json-schema.org/draft/2020-12/schema#";
            default ->
                throw new IllegalArgumentException("Unsupported version flag: " + flag);
        };
    }

    /**
     * Verify that {@code getInstance} returns a non‑null factory for a supported
     * version flag (e.g., V7). The returned object must be an instance of
     * {@link JsonSchemaFactory}.
     */
    @Test
    void getInstance_returnsNonNullFactory() {
        // given
        // any supported flag
        SpecVersion.VersionFlag versionFlag = SpecVersion.VersionFlag.V7;
        // when
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(versionFlag);
        // then
        assertNotNull(factory, "JsonSchemaFactory.getInstance should not return null");
        assertTrue(factory instanceof JsonSchemaFactory, "Returned object should be an instance of JsonSchemaFactory");
    }

    /**
     * Verify that calling {@code getInstance} with {@code null} throws a
     * {@link NullPointerException}. The implementation validates the argument,
     * and the contract of the public API expects a non‑null flag.
     */
    @Test
    void getInstance_nullVersionFlag_throwsNPE() {
        // given
        SpecVersion.VersionFlag versionFlag = null;
        // when / then
        assertThrows(NullPointerException.class, () -> JsonSchemaFactory.getInstance(versionFlag), "Calling getInstance with null should throw NullPointerException");
    }

    /**
     * Ensure that two consecutive calls with the same version flag produce
     * distinct factory instances (the factory does not cache instances globally).
     * This behaviour is verified by reference inequality.
     */
    @Test
    void getInstance_multipleCalls_returnDistinctInstances() {
        // given
        SpecVersion.VersionFlag versionFlag = SpecVersion.VersionFlag.V4;
        // when
        JsonSchemaFactory first = JsonSchemaFactory.getInstance(versionFlag);
        JsonSchemaFactory second = JsonSchemaFactory.getInstance(versionFlag);
        // then
        assertNotSame(first, second, "Each call to getInstance should return a new JsonSchemaFactory instance");
    }

    @ParameterizedTest(name = "getInstance({0}) returns factory with meta‑schema id {1}")
    @EnumSource(value = SpecVersion.VersionFlag.class, names = { "V4", "V6", "V7", "DRAFT_2019_09", "DRAFT_2020_12" })
    void getInstance_validFlag_returnsFactoryWithCorrectMetaSchema(SpecVersion.VersionFlag flag) {
        // when
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(flag);
        // then – factory must be non‑null
        assertNotNull(factory, "Factory must not be null for flag " + flag);
        // and the factory must be able to resolve the expected meta‑schema identifier
        String expectedId = expectedMetaSchemaId(flag);
        JsonMetaSchema meta = factory.getMetaSchema(expectedId, new SchemaValidatorsConfig());
        assertNotNull(meta, () -> "Factory should contain meta‑schema with id " + expectedId);
    }

    @Test
    void getInstance_nullFlag_throwsIllegalArgumentException() {
        // given
        SpecVersion.VersionFlag flag = null;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> JsonSchemaFactory.getInstance(flag));
        assertTrue(ex.getMessage().contains("versionFlag must not be null"), "Exception message should mention null version flag");
    }

    @Test
    void getInstance_unknownFlag_throwsIllegalArgumentException() {
        // Simulate an enum constant that is not mapped – the library defines an
        // UNKNOWN constant for this purpose.
        SpecVersion.VersionFlag unknown = SpecVersion.VersionFlag.UNKNOWN;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> JsonSchemaFactory.getInstance(unknown));
        assertTrue(ex.getMessage().contains("Unsupported version flag"), "Exception message should mention unsupported flag");
    }

    @Test
    void getInstance_withNullCustomizer_behavesLikeSimpleCall() {
        SpecVersion.VersionFlag flag = SpecVersion.VersionFlag.V6;
        JsonSchemaFactory simple = JsonSchemaFactory.getInstance(flag);
        JsonSchemaFactory withNullCustomizer = JsonSchemaFactory.getInstance(flag, null);
        // Both factories must resolve the same meta‑schema identifier
        String expectedId = expectedMetaSchemaId(flag);
        JsonMetaSchema metaSimple = simple.getMetaSchema(expectedId, new SchemaValidatorsConfig());
        JsonMetaSchema metaCustom = withNullCustomizer.getMetaSchema(expectedId, new SchemaValidatorsConfig());
        assertNotNull(metaSimple);
        assertNotNull(metaCustom);
        assertEquals(metaSimple.getId(), metaCustom.getId(), "Factories created with and without a null customiser must be equivalent");
    }

    @Test
    void getInstance_customizerThrowsIllegalArgumentException_propagates() {
        SpecVersion.VersionFlag flag = SpecVersion.VersionFlag.V7;
        Consumer<JsonSchemaFactory.Builder> badCustomizer = builder -> {
            throw new IllegalArgumentException("customizer error");
        };
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> JsonSchemaFactory.getInstance(flag, badCustomizer));
        assertEquals("customizer error", ex.getMessage());
    }

    @Test
    void getInstance_customizerThrowsRuntimeException_propagates() {
        SpecVersion.VersionFlag flag = SpecVersion.VersionFlag.V6;
        Consumer<JsonSchemaFactory.Builder> badCustomizer = builder -> {
            throw new RuntimeException("unexpected failure");
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> JsonSchemaFactory.getInstance(flag, badCustomizer));
        assertEquals("unexpected failure", ex.getMessage());
    }

    @Test
    void getInstance_customizerSetsNullVersionFlag_throwsIllegalArgumentException() {
        SpecVersion.VersionFlag flag = SpecVersion.VersionFlag.V4;
        Consumer<JsonSchemaFactory.Builder> customizer = builder -> {
            // The Builder class provides a fluent method `versionFlag`.
            // Passing null must be rejected by the builder implementation.
            builder.versionFlag(null);
        };
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> JsonSchemaFactory.getInstance(flag, customizer));
        assertTrue(ex.getMessage().contains("versionFlag must not be null"));
    }

    @Disabled("Requires advanced static/partial mocking of protected methods; " + "illustrates intended behaviour without a concrete implementation.")
    @Test
    void getInstance_metaSchemaLoadingFailure_wrapsInIllegalStateException() throws IOException {
        // The idea is to mock the protected `loadMetaSchema` method so that it
        // throws an IOException. Mockito cannot mock non‑public instance methods
        // created inside the static factory without PowerMock or Mockito's
        // inline mock maker. The test is therefore disabled but kept as
        // documentation of the expected contract.
        //
        // Example (requires Mockito-inline):
        //   JsonSchemaFactory factory = Mockito.spy(JsonSchemaFactory.getInstance(flag));
        //   Mockito.doThrow(new IOException("IO failure"))
        //          .when(factory).loadMetaSchema(Mockito.anyString(), Mockito.any());
        //   IllegalStateException ise = assertThrows(IllegalStateException.class,
        //          () -> factory.getMetaSchema("someId", new SchemaValidatorsConfig()));
        //   assertTrue(ise.getCause() instanceof IOException);
    }
}
