package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.util.EnumSet;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchemaLoader_Test {

    /**
     * Factory created with the default builder (no customisation).
     */
    private JsonSchemaFactory defaultFactory;

    /**
     * Factory created with a custom {@link SchemaLoaders.Builder}.
     */
    private JsonSchemaFactory customFactory;

    @BeforeEach
    void setupBeforeEach() {
        // Default factory – uses the standard meta‑schema for the 2020‑12 version.
        defaultFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        // Custom factory – we inject a dummy schema loader map to prove that a
        // builder can be used without breaking the getSchemaLoader contract.
        customFactory = JsonSchemaFactory.builder().schemaLoaders(schemaLoaders -> schemaLoaders.schemas(Collections.singletonMap("https://example.com/dummy-schema", // a minimal valid JSON schema (as a String)
        "{ \"type\": \"object\" }"))).build();
    }

    @Test
    void getSchemaLoader_defaultFactory_notNullAndSingleton() {
        SchemaLoader loader1 = defaultFactory.getSchemaLoader();
        assertNotNull(loader1, "SchemaLoader should not be null for the default factory");
        assertTrue(loader1 instanceof DefaultSchemaLoader, "Default factory must return an instance of DefaultSchemaLoader");
        SchemaLoader loader2 = defaultFactory.getSchemaLoader();
        assertSame(loader1, loader2, "Subsequent calls must return the same SchemaLoader instance");
    }

    @Test
    void getSchemaLoader_customFactory_notNullAndSingleton() {
        SchemaLoader loader1 = customFactory.getSchemaLoader();
        assertNotNull(loader1, "SchemaLoader should not be null for a factory built with custom SchemaLoaders");
        SchemaLoader loader2 = customFactory.getSchemaLoader();
        assertSame(loader1, loader2, "Subsequent calls must return the same SchemaLoader instance even when customised");
    }

    @Test
    void getSchemaLoader_builderWithCustomLoader_returnsExactInstance() {
        // Arrange – create a Mockito stub for SchemaLoader
        SchemaLoader customLoader = mock(SchemaLoader.class);
        JsonSchemaFactory factory = JsonSchemaFactory.builder().schemaLoader(// assume builder has this method
        customLoader).build();
        // Act
        SchemaLoader returned = factory.getSchemaLoader();
        // Assert
        assertSame(customLoader, returned, "Factory built with a custom SchemaLoader must return that exact instance");
    }

    @Test
    void getSchemaLoader_builderWithCustomLoader_isSingleton() {
        SchemaLoader customLoader = mock(SchemaLoader.class);
        JsonSchemaFactory factory = JsonSchemaFactory.builder().schemaLoader(customLoader).build();
        SchemaLoader first = factory.getSchemaLoader();
        SchemaLoader second = factory.getSchemaLoader();
        SchemaLoader third = factory.getSchemaLoader();
        assertSame(first, second, "Repeated calls must return the same loader (first vs second)");
        assertSame(first, third, "Repeated calls must return the same loader (first vs third)");
    }

    @Test
    void getSchemaLoader_blueprintFactory_reusesLoader() {
        // Blueprint factory with a custom loader
        SchemaLoader blueprintLoader = mock(SchemaLoader.class);
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder().schemaLoader(blueprintLoader).build();
        // New factory created from the blueprint
        JsonSchemaFactory derived = JsonSchemaFactory.builder(blueprint).build();
        assertSame(blueprintLoader, derived.getSchemaLoader(), "Derived factory must reuse the SchemaLoader from its blueprint");
    }

    @Test
    void getSchemaLoader_blueprintFactory_loaderImmutableAfterBlueprintChange() {
        // Initial custom loader
        SchemaLoader initialLoader = mock(SchemaLoader.class);
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder().schemaLoader(initialLoader).build();
        // Create derived factory before mutating the blueprint reference
        JsonSchemaFactory derived = JsonSchemaFactory.builder(blueprint).build();
        // Mutate the blueprint's internal reference (if it were mutable). Since the
        // actual implementation stores the loader in a final field, we simulate the
        // scenario by rebuilding the blueprint with a different loader.
        SchemaLoader newLoader = mock(SchemaLoader.class);
        JsonSchemaFactory mutatedBlueprint = JsonSchemaFactory.builder().schemaLoader(newLoader).build();
        // The derived factory must still hold the original loader.
        assertSame(initialLoader, derived.getSchemaLoader(), "Derived factory must keep the original loader even if the blueprint is rebuilt with a different loader");
    }

    @Test
    void getSchemaLoader_getInstanceWithCustomizer_returnsCustomLoader() {
        SchemaLoader customLoader = mock(SchemaLoader.class);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909, builder -> builder.schemaLoader(customLoader));
        assertSame(customLoader, factory.getSchemaLoader(), "Factory obtained via getInstance(customizer) must return the injected custom loader");
    }

    @Test
    void getSchemaLoader_getInstanceWithSameCustomizer_returnsSameLoaderInstance() {
        SchemaLoader customLoader = mock(SchemaLoader.class);
        JsonSchemaFactory factory1 = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909, builder -> builder.schemaLoader(customLoader));
        JsonSchemaFactory factory2 = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909, builder -> builder.schemaLoader(customLoader));
        assertSame(customLoader, factory1.getSchemaLoader(), "First factory must return the custom loader");
        assertSame(customLoader, factory2.getSchemaLoader(), "Second factory must return the same custom loader");
    }

    @ParameterizedTest(name = "VersionFlag {0} yields a non‑null DefaultSchemaLoader")
    @EnumSource(SpecVersion.VersionFlag.class)
    void getSchemaLoader_eachVersionFlag_nonNullDefaultLoader(SpecVersion.VersionFlag flag) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(flag);
        SchemaLoader loader = factory.getSchemaLoader();
        assertNotNull(loader, "SchemaLoader must not be null for flag " + flag);
        assertTrue(loader instanceof DefaultSchemaLoader, "Factory for flag " + flag + " should return a DefaultSchemaLoader");
    }

    @Test
    void getSchemaLoader_differentVersionFlags_produceDistinctLoaders() {
        JsonSchemaFactory factoryV4 = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        JsonSchemaFactory factoryV7 = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        SchemaLoader loaderV4 = factoryV4.getSchemaLoader();
        SchemaLoader loaderV7 = factoryV7.getSchemaLoader();
        assertNotSame(loaderV4, loaderV7, "Loaders for different VersionFlag values must be distinct instances");
    }
}
