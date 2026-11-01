```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the protected {@code JsonSchemaFactory.newJsonSchema} method.
 *
 * <p>The method is accessed via reflection because it is {@code protected}.  The
 * factory instance is spied so that we can stub the internal protected methods
 * such as {@code createValidationContext}, {@code doCreate} and
 * {@code createSchemaValidatorsConfig}.  This allows us to focus the tests on the
 * behaviour of the {@code newJsonSchema} method itself (exception handling,
 * default‑config creation, propagation of arguments, etc.) without having to
 * construct real {@link JsonSchema} objects.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_newJsonSchema_Test {

    private static JsonSchemaFactory realFactory;      // real factory used as a base
    private static ObjectMapper objectMapper;

    @Mock
    SchemaLocation mockSchemaLocation;   // generic mock for schema location

    @Mock
    JsonNode mockSchemaNode;              // generic mock for schema node

    @BeforeAll
    static void setupBeforeAll() {
        // Obtain a concrete factory instance – the static builder gives us a fully‑initialised object.
        realFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        objectMapper = new ObjectMapper();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
    }

    /**
     * Helper that uses reflection to invoke the protected {@code newJsonSchema}
     * method on the supplied factory instance.
     */
    private JsonSchema invokeNewJsonSchema(JsonSchemaFactory factory,
                                          SchemaLocation schemaUri,
                                          JsonNode schemaNode,
                                          SchemaValidatorsConfig config) throws Exception {
        Method m = JsonSchemaFactory.class.getDeclaredMethod(
                "newJsonSchema", SchemaLocation.class, JsonNode.class, SchemaValidatorsConfig.class);
        m.setAccessible(true);
        return (JsonSchema) m.invoke(factory, schemaUri, schemaNode, config);
    }

    /* --------------------------------------------------------------------- */
    /*  Group: initializeValidators handling                                 */
    /* --------------------------------------------------------------------- */

    @Test
    void testInitializeValidators_HappyPath() throws Exception {
        // GIVEN
        JsonSchemaFactory spyFactory = Mockito.spy(realFactory);
        JsonSchema jsonSchemaMock = mock(JsonSchema.class);
        // stub the internal calls
        when(spyFactory.createValidationContext(any(JsonNode.class), any())).thenCallRealMethod();
        doReturn(jsonSchemaMock).when(spyFactory).doCreate(any(), any(), any(), any(), any(), anyBoolean());

        // WHEN
        JsonSchema result = invokeNewJsonSchema(spyFactory, mockSchemaLocation, mockSchemaNode, null);

        // THEN
        assertNotNull(result, "Resulting JsonSchema must not be null");
        verify(jsonSchemaMock, times(1)).initializeValidators();
    }

    @Test
    void testInitializeValidators_ExceptionIsSwallowed() throws Exception {
        // GIVEN
        JsonSchemaFactory spyFactory = Mockito.spy(realFactory);
        JsonSchema jsonSchemaMock = mock(JsonSchema.class);
        // make initializeValidators throw a RuntimeException
        doThrow(new RuntimeException("simulated failure")).when(jsonSchemaMock).initializeValidators();

        // stub internal methods
        when(spyFactory.createValidationContext(any(JsonNode.class), any())).thenCallRealMethod();
        doReturn(jsonSchemaMock).when(spyFactory).doCreate(any(), any(), any(), any(), any(), anyBoolean());

        // WHEN
        JsonSchema result = invokeNewJsonSchema(spyFactory, mockSchemaLocation, mockSchemaNode, null);

        // THEN
        assertNotNull(result, "Even when initializeValidators throws, the method should return the JsonSchema");
        verify(jsonSchemaMock, times(1)).initializeValidators();
    }

    /* --------------------------------------------------------------------- */
    /*  Group: default config creation                                        */
    /* --------------------------------------------------------------------- */

    @Test
    void testNullConfigCreatesDefaultConfig() throws Exception {
        // GIVEN
        JsonSchemaFactory spyFactory = Mockito.spy(realFactory);
        JsonSchema jsonSchemaMock = mock(JsonSchema.class);
        SchemaValidatorsConfig defaultConfig = new SchemaValidatorsConfig();

        // when createSchemaValidatorsConfig() is called, return the default instance
        doReturn(defaultConfig).when(spyFactory).createSchemaValidatorsConfig();

        // capture the config passed to createValidationContext
        ArgumentCaptor<SchemaValidatorsConfig> configCaptor = ArgumentCaptor.forClass(SchemaValidatorsConfig.class);
        when(spyFactory.createValidationContext(any(JsonNode.class), configCaptor.capture()))
                .thenCallRealMethod();

        // stub doCreate to return a non‑null schema
        doReturn(jsonSchemaMock).when(spyFactory).doCreate(any(), any(), any(), any(), any(), anyBoolean());

        // WHEN
        JsonSchema result = invokeNewJsonSchema(spyFactory, mockSchemaLocation, mockSchemaNode, null);

        // THEN
        assertNotNull(result, "Result should be a JsonSchema");
        // verify that a default config was created and passed downstream
        assertSame(defaultConfig, configCaptor.getValue(),
                "The default SchemaValidatorsConfig should be supplied to createValidationContext");
        verify(spyFactory, times(1)).createSchemaValidatorsConfig();
        verify(jsonSchemaMock, times(1)).initializeValidators();
    }

    /* --------------------------------------------------------------------- */
    /*  Group: custom config propagation                                       */
    /* --------------------------------------------------------------------- */

    @Test
    void testCustomConfigIsPropagatedUnchanged() throws Exception {
        // GIVEN
        JsonSchemaFactory spyFactory = Mockito.spy(realFactory);
        JsonSchema jsonSchemaMock = mock(JsonSchema.class);
        SchemaValidatorsConfig customConfig = new SchemaValidatorsConfig();

        // capture the config passed to createValidationContext
        ArgumentCaptor<SchemaValidatorsConfig> configCaptor = ArgumentCaptor.forClass(SchemaValidatorsConfig.class);
        when(spyFactory.createValidationContext(any(JsonNode.class), configCaptor.capture()))
                .thenCallRealMethod();

        // stub doCreate
        doReturn(jsonSchemaMock).when(spyFactory).doCreate(any(), any(), any(), any(), any(), anyBoolean());

        // WHEN
        JsonSchema result = invokeNewJsonSchema(spyFactory, mockSchemaLocation, mockSchemaNode, customConfig);

        // THEN
        assertNotNull(result, "Result should be a JsonSchema");
        assertSame(customConfig, configCaptor.getValue(),
                "The supplied custom config must be passed unchanged to createValidationContext");
        verify(jsonSchemaMock, times(1)).initializeValidators();
    }

    /* --------------------------------------------------------------------- */
    /*  Group: null schemaUri handling                                        */
    /* --------------------------------------------------------------------- */

    @Test
    void testNullSchemaUriHandledGracefully() throws Exception {
        // GIVEN
        JsonSchemaFactory spyFactory = Mockito.spy(realFactory);
        JsonSchema jsonSchemaMock = mock(JsonSchema.class);

        // stub createValidationContext (real implementation) – it only needs the node
        when(spyFactory.createValidationContext(any(JsonNode.class), any())).thenCallRealMethod();

        // stub doCreate to return a mock schema
        doReturn(jsonSchemaMock).when(spyFactory).doCreate(any(), any(), any(), any(), any(), anyBoolean());

        // WHEN – schemaUri is null
        JsonSchema result = invokeNewJsonSchema(spyFactory, null, mockSchemaNode, null);

        // THEN
        assertNotNull(result, "Even with a null SchemaLocation the method should return a JsonSchema");
        verify(jsonSchemaMock, times(1)).initializeValidators();
    }

    /* --------------------------------------------------------------------- */
    /*  Group: null schemaNode throws NPE                                      */
    /* --------------------------------------------------------------------- */

    @Test
    void testNullSchemaNodeThrowsNPE() {
        // GIVEN
        JsonSchemaFactory spyFactory = Mockito.spy(realFactory);
        // No stubbing needed – the method should hit a NPE before any internal call

        // WHEN / THEN
        Exception ex = assertThrows(InvocationTargetException.class, () -> {
            invokeNewJsonSchema(spyFactory, mockSchemaLocation, null, null);
        }, "Invoking with a null schemaNode should result in an exception");

        // The underlying cause should be NullPointerException
        assertTrue(ex.getCause() instanceof NullPointerException,
                "Root cause must be NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    /*  Group: doCreate returns null leads to NPE                              */
    /* --------------------------------------------------------------------- */

    @Test
    void testDoCreateReturnsNullCausesNPE() {
        // GIVEN
        JsonSchemaFactory spyFactory = Mockito.spy(realFactory);
        // make doCreate return null
        doReturn(null).when(spyFactory).doCreate(any(), any(), any(), any(), any(), anyBoolean());

        // stub createValidationContext to avoid NPE before doCreate
        when(spyFactory.createValidationContext(any(JsonNode.class), any())).thenCallRealMethod();

        // WHEN / THEN
        Exception ex = assertThrows(InvocationTargetException.class, () -> {
            invokeNewJsonSchema(spyFactory, mockSchemaLocation, mockSchemaNode, null);
        }, "When doCreate returns null, a NullPointerException should be thrown");

        // The root cause must be NPE because the method tries to call initializeValidators on null
        assertTrue(ex.getCause() instanceof NullPointerException,
                "Root cause must be NullPointerException");
    }
}
```