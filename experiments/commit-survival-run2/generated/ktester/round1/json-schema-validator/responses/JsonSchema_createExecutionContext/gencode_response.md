```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchema#createExecutionContext()}.
 *
 * <p>The tests cover all logical branches of the method:
 * <ul>
 *   <li>custom {@link ExecutionContext} supplied via {@link SchemaValidatorsConfig#getExecutionContextSupplier()}</li>
 *   <li>default creation with different combinations of the two analysis‑disable flags</li>
 *   <li>edge‑cases such as a supplier returning {@code null}, a supplier throwing, and missing configuration objects</li>
 *   <li>behaviour on repeated invocations</li>
 * </ul>
 *
 * <p>Reflection is used only to inspect the private {@code CollectorContext} held inside
 * {@link ExecutionContext} because there are no public accessors.</p>
 */
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
        // Remaining constructor arguments are irrelevant for createExecutionContext()
        return ctor.newInstance(vc, null, null, null, null, false);
    }

    /*** Helper – extracts a private field value via reflection ***/
    private static <T> T getPrivateField(Object target, String fieldName, Class<T> type) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return type.cast(f.get(target));
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ValidationContext#getConfig() must return the mocked config
        when(validationContext.getConfig()).thenReturn(config);
        // Build the JsonSchema instance that will be exercised
        jsonSchema = createJsonSchema(validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Mockito cleans up automatically
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
        boolean itemsDisabled = getPrivateField(collector, "unevaluatedItemsAnalysisDisabled", boolean.class);
        boolean propsDisabled = getPrivateField(collector, "unevaluatedPropertiesAnalysisDisabled", boolean.class);
        assertTrue(itemsDisabled, "unevaluatedItemsAnalysisDisabled flag must be propagated");
        assertFalse(propsDisabled, "unevaluatedPropertiesAnalysisDisabled flag must be propagated");
    }

    /*** Parameterized test for all flag combinations when the supplier is null ***/
    @ParameterizedTest(name = "flags – items:{0} props:{1}")
    @CsvSource({
            "false, false",
            "true, true",
            "false, true",
            "true, false"
    })
    void createExecutionContext_defaultFlags(boolean itemsDisabled, boolean propsDisabled) throws Exception {
        // Arrange
        when(config.getExecutionContextSupplier()).thenReturn(null);
        when(config.isUnevaluatedItemsAnalysisDisabled()).thenReturn(itemsDisabled);
        when(config.isUnevaluatedPropertiesAnalysisDisabled()).thenReturn(propsDisabled);

        // Act
        ExecutionContext ctx = jsonSchema.createExecutionContext();

        // Assert
        assertNotNull(ctx, "ExecutionContext must not be null");
        CollectorContext collector = getPrivateField(ctx, "collectorContext", CollectorContext.class);
        assertNotNull(collector, "CollectorContext must be present");
        boolean actualItems = getPrivateField(collector, "unevaluatedItemsAnalysisDisabled", boolean.class);
        boolean actualProps = getPrivateField(collector, "unevaluatedPropertiesAnalysisDisabled", boolean.class);
        assertEquals(itemsDisabled, actualItems, "Items flag should match config");
        assertEquals(propsDisabled, actualProps, "Properties flag should match config");
    }

    /*** Scenario – supplier returns null ***/
    @Test
    void createExecutionContext_supplierReturnsNull() {
        // Arrange
        @SuppressWarnings("unchecked")
        Supplier<ExecutionContext> supplier = mock(Supplier.class);
        when(supplier.get()).thenReturn(null);
        when(config.getExecutionContextSupplier()).thenReturn(supplier);

        // Act
        ExecutionContext ctx = jsonSchema.createExecutionContext();

        // Assert
        assertNull(ctx, "When the supplier returns null the method should return null");
        verify(supplier, times(1)).get();
    }

    /*** Scenario – supplier throws a RuntimeException ***/
    @Test
    void createExecutionContext_supplierThrowsException() {
        // Arrange
        @SuppressWarnings("unchecked")
        Supplier<ExecutionContext> supplier = mock(Supplier.class);
        RuntimeException ex = new RuntimeException("supplier failure");
        when(supplier.get()).thenThrow(ex);
        when(config.getExecutionContextSupplier()).thenReturn(supplier);

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> jsonSchema.createExecutionContext(),
                "The RuntimeException from the supplier should propagate");
        assertSame(ex, thrown, "The thrown exception must be the same instance");
    }

    /*** Scenario – multiple invocations produce distinct instances ***/
    @Test
    void createExecutionContext_multipleInvocationsProduceDistinctInstances() throws Exception {
        // Arrange
        when(config.getExecutionContextSupplier()).thenReturn(null);
        when(config.isUnevaluatedItemsAnalysisDisabled()).thenReturn(false);
        when(config.isUnevaluatedPropertiesAnalysisDisabled()).thenReturn(false);

        // Act
        ExecutionContext first = jsonSchema.createExecutionContext();
        ExecutionContext second = jsonSchema.createExecutionContext();

        // Assert
        assertNotSame(first, second, "Each call should return a new ExecutionContext instance");
        CollectorContext firstCollector = getPrivateField(first, "collectorContext", CollectorContext.class);
        CollectorContext secondCollector = getPrivateField(second, "collectorContext", CollectorContext.class);
        assertNotNull(firstCollector, "First ExecutionContext must contain a CollectorContext");
        assertNotNull(secondCollector, "Second ExecutionContext must contain a CollectorContext");
        // Ensure they are distinct objects as well
        assertNotSame(firstCollector, secondCollector, "Each ExecutionContext should have its own CollectorContext");
    }

    /*** Scenario – ValidationContext returns null config (NPE expected) ***/
    @Test
    void createExecutionContext_validationContextReturnsNullConfig() {
        // Arrange
        when(validationContext.getConfig()).thenReturn(null);

        // Act & Assert
        NullPointerException npe = assertThrows(NullPointerException.class,
                () -> jsonSchema.createExecutionContext(),
                "A null config should cause a NullPointerException");
        assertNotNull(npe.getMessage()); // message is not important, just ensure NPE occurs
    }

    /*** Scenario – JsonSchema constructed with a null ValidationContext (NPE expected) ***/
    @Test
    void createExecutionContext_nullValidationContext() throws Exception {
        // Create a JsonSchema where the internal validationContext field is null
        JsonSchema schemaWithNullVc = createJsonSchema(null);

        // Act & Assert
        NullPointerException npe = assertThrows(NullPointerException.class,
                () -> schemaWithNullVc.createExecutionContext(),
                "When validationContext is null, a NullPointerException should be thrown");
        assertNotNull(npe.getMessage());
    }
}
```