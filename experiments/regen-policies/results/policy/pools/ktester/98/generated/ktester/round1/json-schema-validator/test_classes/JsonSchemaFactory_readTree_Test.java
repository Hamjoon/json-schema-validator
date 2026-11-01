package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.io.IOException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_readTree_Test {

    /**
     * Mocked dependencies used across the test methods.
     */
    @Mock
    private JsonNodeReader mockReader;

    @Mock
    private ObjectMapper mockMapper;

    @Mock
    private JsonNode mockNode;

    @Mock
    private InputFormat mockInputFormat;

    /**
     * The factory created without a {@link JsonNodeReader}.
     */
    private JsonSchemaFactory factoryWithoutReader;

    /**
     * The factory created with a {@link JsonNodeReader}.
     */
    private JsonSchemaFactory factoryWithReader;

    @BeforeEach
    void setupBeforeEach() {
        // 1️⃣  Factory without a JsonNodeReader (default configuration)
        // --------------------------------------------------------------------
        factoryWithoutReader = JsonSchemaFactory.getInstance(// any supported version works
        SpecVersion.VersionFlag.V202012);
        // --------------------------------------------------------------------
        // 2️⃣  Factory with a JsonNodeReader supplied via the builder customiser
        // --------------------------------------------------------------------
        factoryWithReader = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder -> builder.jsonNodeReader(mockReader));
        // --------------------------------------------------------------------
        // default factory – uses the built‑in JsonNodeReader (null)
        factoryWithoutReader = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        // factory that contains the mocked JsonNodeReader (via builder customiser)
        factoryWithReader = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder -> builder.jsonNodeReader(mockReader));
    }

    private static Stream<Arguments> validObjectMapperProvider() {
        return Stream.of(// JSON object
        Arguments.of("JSON object", "{\"name\":\"John\",\"age\":30}", InputFormat.JSON, (java.util.function.Consumer<JsonNode>) node -> {
            assertEquals("John", node.get("name").asText());
            assertEquals(30, node.get("age").asInt());
        }), // YAML object
        Arguments.of("YAML object", "name: John\nage: 30", InputFormat.YAML, (java.util.function.Consumer<JsonNode>) node -> {
            assertEquals("John", node.get("name").asText());
            assertEquals(30, node.get("age").asInt());
        }));
    }

    private static Stream<Arguments> customReaderExceptionProvider() {
        return Stream.of(Arguments.of(IOException.class, "IOException"), Arguments.of(IllegalStateException.class, "IllegalStateException"), Arguments.of(NullPointerException.class, "NullPointerException"));
    }

    /**
     * When the factory has **no** {@link JsonNodeReader}, {@code readTree}
     * must delegate to the appropriate {@link ObjectMapper}.
     */
    @Test
    void readTree_delegatesToObjectMapper_whenReaderIsNull() throws IOException {
        // Arrange ------------------------------------------------------------
        // Spy the factory so we can stub {@code getObjectMapper} without
        // touching its private state.
        JsonSchemaFactory spyFactory = Mockito.spy(factoryWithoutReader);
        // Stub the mapper returned for the supplied InputFormat.
        doReturn(mockMapper).when(spyFactory).getObjectMapper(mockInputFormat);
        // Stub the mapper's readTree method.
        when(mockMapper.readTree("some content")).thenReturn(mockNode);
        // Act ---------------------------------------------------------------
        JsonNode result = spyFactory.readTree("some content", mockInputFormat);
        // Assert -------------------------------------------------------------
        assertSame(mockNode, result, "The JsonNode returned by the ObjectMapper should be propagated.");
        verify(mockMapper, times(1)).readTree("some content");
        verifyNoInteractions(mockReader);
        // Ensure the factory never tried to obtain a JsonNodeReader‑based path.
        verify(spyFactory, never()).jsonNodeReader;
    }

    /**
     * When the factory has a {@link JsonNodeReader}, {@code readTree}
     * must delegate to that reader and must **not** call the ObjectMapper.
     */
    @Test
    void readTree_delegatesToJsonNodeReader_whenReaderIsPresent() throws IOException {
        // Arrange ------------------------------------------------------------
        // Spy the factory that already contains the mocked JsonNodeReader.
        JsonSchemaFactory spyFactory = Mockito.spy(factoryWithReader);
        // Stub the JsonNodeReader to return the mock JsonNode.
        when(mockReader.readTree("another content", mockInputFormat)).thenReturn(mockNode);
        // Act ---------------------------------------------------------------
        JsonNode result = spyFactory.readTree("another content", mockInputFormat);
        // Assert -------------------------------------------------------------
        assertSame(mockNode, result, "The JsonNode returned by the JsonNodeReader should be propagated.");
        verify(mockReader, times(1)).readTree("another content", mockInputFormat);
        // The ObjectMapper path must not be touched.
        verify(spyFactory, never()).getObjectMapper(any());
    }

    @ParameterizedTest(name = "[{index}] parse valid {0}")
    @MethodSource("validObjectMapperProvider")
    void readTree_parsesValidContent_usingObjectMapper(String description, String content, InputFormat format, java.util.function.Consumer<JsonNode> assertions) throws IOException {
        // Act – use the real factory (no spying, we want the real mapper)
        JsonNode node = factoryWithoutReader.readTree(content, format);
        // Assert – custom assertion supplied by the provider
        assertions.accept(node);
    }

    @Test
    void readTree_propagatesIOException_whenObjectMapperFailsOnMalformedJson() {
        // missing closing brace
        String malformed = "{invalidJson: true";
        assertThrows(IOException.class, () -> factoryWithoutReader.readTree(malformed, InputFormat.JSON));
    }

    @Test
    void readTree_propagatesIOException_whenObjectMapperFailsOnMalformedYaml() {
        // 'thirty' is not a number but still valid yaml; we force error by using invalid indentation
        String malformed = "name: John\nage: thirty";
        // To guarantee an IOException we use a clearly malformed yaml (e.g., unbalanced brackets)
        String brokenYaml = "name: John\nage: [30";
        assertThrows(IOException.class, () -> factoryWithoutReader.readTree(brokenYaml, InputFormat.YAML));
    }

    @Test
    void readTree_propagatesIOException_whenContentIsEmptyJson() {
        assertThrows(IOException.class, () -> factoryWithoutReader.readTree("", InputFormat.JSON));
    }

    @Test
    void readTree_propagatesIOException_whenContentIsWhitespaceJson() {
        assertThrows(IOException.class, () -> factoryWithoutReader.readTree("   ", InputFormat.JSON));
    }

    @Test
    void readTree_propagatesNullPointerException_whenContentIsNull() {
        assertThrows(NullPointerException.class, () -> factoryWithoutReader.readTree(null, InputFormat.JSON));
    }

    @Test
    void readTree_propagatesNullPointerException_whenInputFormatIsNull() {
        assertThrows(NullPointerException.class, () -> factoryWithoutReader.readTree("{\"valid\":\"json\"}", null));
    }

    @Test
    void readTree_delegatesToCustomReader_andReturnsItsResult() throws IOException {
        JsonSchemaFactory spyFactory = Mockito.spy(factoryWithReader);
        when(mockReader.readTree("custom content", InputFormat.JSON)).thenReturn(mockNode);
        JsonNode result = spyFactory.readTree("custom content", InputFormat.JSON);
        assertSame(mockNode, result);
        verify(mockReader, times(1)).readTree("custom content", InputFormat.JSON);
        verify(spyFactory, never()).getObjectMapper(any());
    }

    @ParameterizedTest(name = "[{index}] custom reader throws {0}")
    @MethodSource("customReaderExceptionProvider")
    void readTree_customReaderExceptionPropagation(Class<? extends Throwable> exceptionClass, String description) throws IOException {
        JsonSchemaFactory spyFactory = Mockito.spy(factoryWithReader);
        when(mockReader.readTree(anyString(), any())).thenThrow(exceptionClass);
        assertThrows(exceptionClass, () -> spyFactory.readTree("any", InputFormat.JSON));
    }

    @Test
    void readTree_customReaderReceivesCorrectInputFormat_JSON() throws IOException {
        JsonSchemaFactory spyFactory = Mockito.spy(factoryWithReader);
        when(mockReader.readTree(anyString(), any())).thenReturn(mockNode);
        spyFactory.readTree("payload", InputFormat.JSON);
        ArgumentCaptor<InputFormat> captor = ArgumentCaptor.forClass(InputFormat.class);
        verify(mockReader).readTree(eq("payload"), captor.capture());
        assertEquals(InputFormat.JSON, captor.getValue());
    }

    @Test
    void readTree_customReaderReceivesCorrectInputFormat_YAML() throws IOException {
        JsonSchemaFactory spyFactory = Mockito.spy(factoryWithReader);
        when(mockReader.readTree(anyString(), any())).thenReturn(mockNode);
        spyFactory.readTree("payload", InputFormat.YAML);
        ArgumentCaptor<InputFormat> captor = ArgumentCaptor.forClass(InputFormat.class);
        verify(mockReader).readTree(eq("payload"), captor.capture());
        assertEquals(InputFormat.YAML, captor.getValue());
    }

    @Test
    void readTree_parsesJsonArrayCorrectly() throws IOException {
        String jsonArray = "[{\"id\":1},{\"id\":2}]";
        JsonNode node = factoryWithoutReader.readTree(jsonArray, InputFormat.JSON);
        assertTrue(node.isArray(), "Result should be an array");
        ArrayNode array = (ArrayNode) node;
        assertEquals(2, array.size());
        assertEquals(1, array.get(0).get("id").asInt());
        assertEquals(2, array.get(1).get("id").asInt());
    }

    @Test
    void readTree_parsesYamlListCorrectly() throws IOException {
        String yamlList = "- apple\n- banana\n- cherry";
        JsonNode node = factoryWithoutReader.readTree(yamlList, InputFormat.YAML);
        assertTrue(node.isArray(), "Result should be an array");
        ArrayNode array = (ArrayNode) node;
        assertEquals(3, array.size());
        assertEquals("apple", array.get(0).asText());
        assertEquals("banana", array.get(1).asText());
        assertEquals("cherry", array.get(2).asText());
    }

    @Test
    void readTree_parsesNestedJsonObjectCorrectly() throws IOException {
        String nested = "{\"person\":{\"name\":\"Alice\",\"address\":{\"city\":\"Wonderland\"}}}";
        JsonNode node = factoryWithoutReader.readTree(nested, InputFormat.JSON);
        JsonNode person = node.get("person");
        assertNotNull(person);
        assertEquals("Alice", person.get("name").asText());
        JsonNode address = person.get("address");
        assertNotNull(address);
        assertEquals("Wonderland", address.get("city").asText());
    }
}
