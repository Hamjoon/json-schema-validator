package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_newJsonSchema_Test {

    private static JsonSchemaFactory factory;

    private static ObjectMapper objectMapper;

    // real factory used as a base
    private static JsonSchemaFactory realFactory;

    @Mock
    SchemaLocation // generic mock for schema location
    mockSchemaLocation;

    @Mock
    JsonNode // generic mock for schema node
    mockSchemaNode;

    @BeforeAll
    static void setupBeforeAll() {
        // Obtain a concrete factory instance – the static builder gives us a fully‑initialised object.
        realFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        objectMapper = new ObjectMapper();
    }

    /**
     * Helper that uses reflection to invoke the protected {@code newJsonSchema}
     * method on the supplied factory instance.
     */
    private JsonSchema invokeNewJsonSchema(JsonSchemaFactory factory, SchemaLocation schemaUri, JsonNode schemaNode, SchemaValidatorsConfig config) throws Exception {
        Method m = JsonSchemaFactory.class.getDeclaredMethod("newJsonSchema", SchemaLocation.class, JsonNode.class, SchemaValidatorsConfig.class);
        m.setAccessible(true);
        return (JsonSchema) m.invoke(factory, schemaUri, schemaNode, config);
    }

    /**
     * Verifies that the protected {@code newJsonSchema} method can create a {@link JsonSchema}
     * instance without throwing and that the returned object is not {@code null}.
     *
     * The method is accessed via reflection because it is {@code protected}.
     */
    @Test
    void testNewJsonSchemaCreatesSchema() throws Exception {
        // --- given ------------------------------------------------------------
        // a simple JSON schema node
        String schemaJson = "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }";
        JsonNode schemaNode = objectMapper.readTree(schemaJson);
        // a schema location (the static factory method `of` is part of the library)
        SchemaLocation schemaLocation = SchemaLocation.of("http://example.com/test-schema.json");
        // default validators configuration
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // --- when -------------------------------------------------------------
        // use reflection to call the protected method
        Method newJsonSchemaMethod = JsonSchemaFactory.class.getDeclaredMethod("newJsonSchema", SchemaLocation.class, JsonNode.class, SchemaValidatorsConfig.class);
        newJsonSchemaMethod.setAccessible(true);
        JsonSchema result = (JsonSchema) newJsonSchemaMethod.invoke(factory, schemaLocation, schemaNode, config);
        // --- then -------------------------------------------------------------
        assertNotNull(result, "newJsonSchema should return a non‑null JsonSchema instance");
    }

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
        when(spyFactory.createValidationContext(any(JsonNode.class), configCaptor.capture())).thenCallRealMethod();
        // stub doCreate to return a non‑null schema
        doReturn(jsonSchemaMock).when(spyFactory).doCreate(any(), any(), any(), any(), any(), anyBoolean());
        // WHEN
        JsonSchema result = invokeNewJsonSchema(spyFactory, mockSchemaLocation, mockSchemaNode, null);
        // THEN
        assertNotNull(result, "Result should be a JsonSchema");
        // verify that a default config was created and passed downstream
        assertSame(defaultConfig, configCaptor.getValue(), "The default SchemaValidatorsConfig should be supplied to createValidationContext");
        verify(spyFactory, times(1)).createSchemaValidatorsConfig();
        verify(jsonSchemaMock, times(1)).initializeValidators();
    }

    @Test
    void testCustomConfigIsPropagatedUnchanged() throws Exception {
        // GIVEN
        JsonSchemaFactory spyFactory = Mockito.spy(realFactory);
        JsonSchema jsonSchemaMock = mock(JsonSchema.class);
        SchemaValidatorsConfig customConfig = new SchemaValidatorsConfig();
        // capture the config passed to createValidationContext
        ArgumentCaptor<SchemaValidatorsConfig> configCaptor = ArgumentCaptor.forClass(SchemaValidatorsConfig.class);
        when(spyFactory.createValidationContext(any(JsonNode.class), configCaptor.capture())).thenCallRealMethod();
        // stub doCreate
        doReturn(jsonSchemaMock).when(spyFactory).doCreate(any(), any(), any(), any(), any(), anyBoolean());
        // WHEN
        JsonSchema result = invokeNewJsonSchema(spyFactory, mockSchemaLocation, mockSchemaNode, customConfig);
        // THEN
        assertNotNull(result, "Result should be a JsonSchema");
        assertSame(customConfig, configCaptor.getValue(), "The supplied custom config must be passed unchanged to createValidationContext");
        verify(jsonSchemaMock, times(1)).initializeValidators();
    }

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
        assertTrue(ex.getCause() instanceof NullPointerException, "Root cause must be NullPointerException");
    }

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
        assertTrue(ex.getCause() instanceof NullPointerException, "Root cause must be NullPointerException");
    }
}
