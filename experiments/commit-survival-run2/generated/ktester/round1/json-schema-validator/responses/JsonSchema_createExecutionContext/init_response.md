```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_createExecutionContext_Test {

    /*** Mocks used for every test ***/
    @Mock
    private ValidationContext validationContext;

    @Mock
    private SchemaValidatorsConfig config;

    /*** Instance under test ***/
    private JsonSchema jsonSchema;

    /*** Helper – creates a JsonSchema instance via the private constructor ***/
    private JsonSchema createJsonSchema(ValidationContext vc) throws Exception {
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(
                ValidationContext.class,
                String.class,
                URI.class,
                JsonNode.class,
                JsonSchema.class,
                boolean.class);
        ctor.setAccessible(true);
        // all other arguments can be null / false – they are not used by createExecutionContext()
        return ctor.newInstance(vc, null, null, null, null, false);
    }

    /*** Helper – extracts a private field value via reflection ***/
    private static <T> T getPrivateField(Object target, String fieldName, Class<T> type) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return type.cast(f.get(target));
    }

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ValidationContext#getConfig() must return our mocked config
        when(validationContext.getConfig()).thenReturn(config);
        // Build the JsonSchema instance that will be exercised
        jsonSchema = createJsonSchema(validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for the current tests
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required
    }

    /*** Scenario 1 – a supplier is supplied in the config ***/
    @Test
    void createExecutionContext_usesSuppliedExecutionContext() {
        // Arrange
        @SuppressWarnings("unchecked")
        Supplier<ExecutionContext> supplier = mock(Supplier.class);
        ExecutionContext suppliedCtx = mock(ExecutionContext.class);
        when(supplier.get()).thenReturn(suppliedCtx);
        when(config.getExecutionContextSupplier()).thenReturn(supplier);

        // Act
        ExecutionContext ctx = jsonSchema.createExecutionContext();

        // Assert
        assertSame(suppliedCtx, ctx, "The ExecutionContext returned should be the one supplied by the config");
        verify(supplier, times(1)).get();
    }

    /*** Scenario 2 – no supplier, default CollectorContext is created ***/
    @Test
    void createExecutionContext_createsDefaultExecutionContextWhenNoSupplier() throws Exception {
        // Arrange
        when(config.getExecutionContextSupplier()).thenReturn(null);
        when(config.isUnevaluatedItemsAnalysisDisabled()).thenReturn(true);
        when(config.isUnevaluatedPropertiesAnalysisDisabled()).thenReturn(false);

        // Act
        ExecutionContext ctx = jsonSchema.createExecutionContext();

        // Assert – a non‑null ExecutionContext must be produced
        assertNotNull(ctx, "ExecutionContext must not be null when no supplier is configured");

        // Verify that the internal CollectorContext reflects the config flags
        CollectorContext collector = getPrivateField(ctx, "collectorContext", CollectorContext.class);
        assertNotNull(collector, "CollectorContext should be created inside ExecutionContext");
        // The CollectorContext constructor stores the two booleans; we can read them via reflection as well
        boolean itemsDisabled = getPrivateField(collector, "unevaluatedItemsAnalysisDisabled", boolean.class);
        boolean propsDisabled = getPrivateField(collector, "unevaluatedPropertiesAnalysisDisabled", boolean.class);
        assertTrue(itemsDisabled, "unevaluatedItemsAnalysisDisabled flag must be propagated");
        assertFalse(propsDisabled, "unevaluatedPropertiesAnalysisDisabled flag must be propagated");
    }
}
```