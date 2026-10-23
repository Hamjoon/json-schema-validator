
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_2_Test {
    private JsonSchemaFactory factory;

    class ClosableFailingInputStream extends InputStream {
        private boolean closed = false;

        @Override
        public int read() throws IOException {
            throw new IOException("forced error");
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }

        public boolean isClosed() {
            return closed;
        }
    }

    /**
     * Simple InputStream that always throws an IOException on read.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Obtain a ready‑to‑use factory for Draft‑07 (the most common version).
        // The static factory method hides all the constructor complexity.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        assertNotNull(factory, "Factory should be created");
    }

    @Test
    void getSchema_fromInputStream_returnsJsonSchema() throws Exception {
        // GIVEN a simple JSON schema as an InputStream
        String schemaJson = "{ \"type\": \"string\" }";
        InputStream schemaStream = new ByteArrayInputStream(schemaJson.getBytes(StandardCharsets.UTF_8));
        // WHEN the factory creates a JsonSchema from the stream
        JsonSchema schema = factory.getSchema(schemaStream);
        // THEN a non‑null JsonSchema instance is returned
        assertNotNull(schema, "The returned JsonSchema should not be null");
        // The schema should be able to validate a simple string value
        ValidationResult result = schema.validate("hello");
        assertTrue(result.isValid(), "A valid string should pass validation");
    }

    @Test
    void getSchema_fromNullInputStream_throwsException() {
        // GIVEN a null InputStream
        InputStream nullStream = null;
        // WHEN/THEN an IllegalArgumentException (or NullPointerException) is thrown
        // The exact exception type depends on the implementation; we accept either.
        assertThrows(Exception.class, () -> factory.getSchema(nullStream));
    }

    @Test
    @DisplayName("Empty InputStream results in an exception (schema content is empty)")
    void getSchema_emptyInputStream_throwsException() {
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        Exception ex = assertThrows(Exception.class, () -> factory.getSchema(empty));
        // The concrete type may be JsonSchemaException or IllegalArgumentException.
        // We only verify that an exception is thrown.
    }

    @Test
    @DisplayName("Malformed JSON causes a parsing exception wrapped in JsonSchemaException")
    void getSchema_invalidJson_throwsParsingException() {
        String malformed = "{ invalid json }";
        InputStream stream = new ByteArrayInputStream(malformed.getBytes(StandardCharsets.UTF_8));
        Exception ex = assertThrows(Exception.class, () -> factory.getSchema(stream));
        // The implementation typically wraps JsonProcessingException.
        assertNotNull(ex.getMessage());
    }

    @Test
    @DisplayName("Well‑formed JSON schema without a custom config returns a JsonSchema")
    void getSchema_validJsonWithoutConfig_returnsSchema() {
        String json = "{ \"$schema\": \"http://json-schema.org/draft-07/schema#\", \"type\": \"object\" }";
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        JsonSchema schema = factory.getSchema(stream);
        assertNotNull(schema, "Schema should be created with default configuration");
    }

    @Test
    @DisplayName("Valid JSON with a custom SchemaValidatorsConfig (failFast) is honoured")
    void getSchema_validJsonWithCustomConfig_respectsConfig() {
        String json = "{ \"type\": \"string\" }";
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        SchemaValidatorsConfig config = new SchemaValidatorsConfig().setFailFast(true);
        JsonSchema schema = factory.getSchema(stream, config);
        assertNotNull(schema, "Schema should be created with custom configuration");
        // Verify that failFast is active – we feed an input that triggers two errors.
        // Using a JSON object where two required properties are missing. The validator
        // should report only the first error because failFast=true.
        String instance = "{ }";
        ValidationResult result = schema.validate(instance);
        assertNotNull(result);
    }

    @Test
    @DisplayName("YAML schema is parsed correctly")
    void getSchema_validYaml_returnsSchema() {
        String yaml = "type: object\nproperties:\n  name:\n    type: string";
        InputStream stream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        JsonSchema schema = factory.getSchema(stream);
        assertNotNull(schema, "YAML schema should be parsed into a JsonSchema");
    }

    @Test
    @DisplayName("IOException while reading the stream is wrapped in a JsonSchemaException")
    void getSchema_ioExceptionDuringRead_throwsWrappedException() {
        InputStream faulty = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("forced I/O error");
            }

            @Override
            public void close() throws IOException {
                // nothing to close
            }
        };
        Exception ex = assertThrows(Exception.class, () -> factory.getSchema(faulty));
        assertTrue(ex.getMessage().contains("forced I/O error"));
    }

    @Test
    @DisplayName("The provided InputStream is closed after successful parsing")
    void getSchema_streamClosedAfterParsing() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream("{ \"type\": \"null\" }".getBytes(StandardCharsets.UTF_8));
        JsonSchema schema = factory.getSchema(stream);
        assertNotNull(schema);
        // After the factory finishes reading, the stream should be closed.
        // ByteArrayInputStream returns -1 after being closed.
        assertEquals(-1, stream.read(), "Stream should be closed (read() returns -1)");
    }

    @Test
    @DisplayName("UTF‑8 BOM is ignored and schema is parsed")
    void getSchema_jsonWithUtf8Bom_isParsed() {
        byte[] bom = new byte[]{ ((byte) (0xef)), ((byte) (0xbb)), ((byte) (0xbf)) };
        String json = "{ \"type\": \"object\" }";
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        byte[] combined = new byte[bom.length + jsonBytes.length];
        System.arraycopy(bom, 0, combined, 0, bom.length);
        System.arraycopy(jsonBytes, 0, combined, bom.length, jsonBytes.length);
        InputStream stream = new ByteArrayInputStream(combined);
        JsonSchema schema = factory.getSchema(stream);
        assertNotNull(schema, "Schema should be parsed even with a leading UTF‑8 BOM");
    }

    @Test
    @DisplayName("Leading and trailing whitespace does not affect parsing")
    void getSchema_jsonWithWhitespace_isParsed() {
        String json = "   \n\t{ \"type\": \"string\" }   \n\t";
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        JsonSchema schema = factory.getSchema(stream);
        assertNotNull(schema);
    }

    @Test
    @DisplayName("Unicode characters in schema are handled correctly")
    void getSchema_jsonWithUnicode_isParsed() {
        String json = "{ \"title\": \"Привет мир\", \"type\": \"object\" }";
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        JsonSchema schema = factory.getSchema(stream);
        assertNotNull(schema);
    }

    @Test
    @DisplayName("JSON containing comments throws an exception (comments not allowed by default)")
    void getSchema_jsonWithComments_throwsException() {
        String json = "{ /* comment */ \"type\": \"string\" }";
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        Exception ex = assertThrows(Exception.class, () -> factory.getSchema(stream));
        // The underlying cause should be a JsonProcessingException.
        assertNotNull(ex.getMessage());
    }

    @Test
    @DisplayName("Whitespace‑only InputStream results in an empty‑content exception")
    void getSchema_whitespaceOnly_throwsException() {
        String ws = "   \n\t   ";
        InputStream stream = new ByteArrayInputStream(ws.getBytes(StandardCharsets.UTF_8));
        Exception ex = assertThrows(Exception.class, () -> factory.getSchema(stream));
        assertNotNull(ex.getMessage());
    }

    @Test
    @DisplayName("When parsing fails due to IOException, the stream is still closed")
    void getSchema_streamClosedOnParsingFailure() {
        // Custom InputStream that throws on read and records close()
        class ClosableFailingInputStream extends InputStream {
            private boolean closed = false;

            @Override
            public int read() throws IOException {
                throw new IOException("forced error");
            }

            @Override
            public void close() throws IOException {
                closed = true;
                super.close();
            }

            public boolean isClosed() {
                return closed;
            }
        }
        ClosableFailingInputStream stream = new ClosableFailingInputStream();
        Exception ex = assertThrows(Exception.class, () -> factory.getSchema(stream));
        assertTrue(ex.getMessage().contains("forced error"));
        assertTrue(stream.isClosed(), "Stream must be closed even when parsing fails");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:1: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
 ^
  symbol: class ExtendWith
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:3: error: cannot find symbol
    private JsonSchemaFactory factory;
            ^
  symbol:   class JsonSchemaFactory
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:131: error: cannot find symbol
    void getSchema_streamClosedAfterParsing() throws IOException {
                                                     ^
  symbol:   class IOException
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:5: error: cannot find symbol
    class ClosableFailingInputStream extends InputStream {
                                             ^
  symbol:   class InputStream
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:9: error: cannot find symbol
        public int read() throws IOException {
                                 ^
  symbol:   class IOException
  location: class JsonSchemaFactory_getSchema_2_Test.ClosableFailingInputStream
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:14: error: cannot find symbol
        public void close() throws IOException {
                                   ^
  symbol:   class IOException
  location: class JsonSchemaFactory_getSchema_2_Test.ClosableFailingInputStream
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:1: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:27: error: cannot find symbol
    @BeforeEach
     ^
  symbol:   class BeforeEach
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:35: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:49: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:58: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:59: error: cannot find symbol
    @DisplayName("Empty InputStream results in an exception (schema content is empty)")
     ^
  symbol:   class DisplayName
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:67: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:68: error: cannot find symbol
    @DisplayName("Malformed JSON causes a parsing exception wrapped in JsonSchemaException")
     ^
  symbol:   class DisplayName
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:77: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:78: error: cannot find symbol
    @DisplayName("Well‑formed JSON schema without a custom config returns a JsonSchema")
     ^
  symbol:   class DisplayName
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:86: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:87: error: cannot find symbol
    @DisplayName("Valid JSON with a custom SchemaValidatorsConfig (failFast) is honoured")
     ^
  symbol:   class DisplayName
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:102: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:103: error: cannot find symbol
    @DisplayName("YAML schema is parsed correctly")
     ^
  symbol:   class DisplayName
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:111: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:112: error: cannot find symbol
    @DisplayName("IOException while reading the stream is wrapped in a JsonSchemaException")
     ^
  symbol:   class DisplayName
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:129: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:130: error: cannot find symbol
    @DisplayName("The provided InputStream is closed after successful parsing")
     ^
  symbol:   class DisplayName
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:140: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:141: error: cannot find symbol
    @DisplayName("UTF‑8 BOM is ignored and schema is parsed")
     ^
  symbol:   class DisplayName
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:154: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:155: error: cannot find symbol
    @DisplayName("Leading and trailing whitespace does not affect parsing")
     ^
  symbol:   class DisplayName
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:163: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:164: error: cannot find symbol
    @DisplayName("Unicode characters in schema are handled correctly")
     ^
  symbol:   class DisplayName
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:172: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:173: error: cannot find symbol
    @DisplayName("JSON containing comments throws an exception (comments not allowed by default)")
     ^
  symbol:   class DisplayName
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:182: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:183: error: cannot find symbol
    @DisplayName("Whitespace‑only InputStream results in an empty‑content exception")
     ^
  symbol:   class DisplayName
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:191: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:192: error: cannot find symbol
    @DisplayName("When parsing fails due to IOException, the stream is still closed")
     ^
  symbol:   class DisplayName
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:8: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:10: error: cannot find symbol
            throw new IOException("forced error");
                      ^
  symbol:   class IOException
  location: class JsonSchemaFactory_getSchema_2_Test.ClosableFailingInputStream
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:13: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:16: error: cannot find symbol
            super.close();
                 ^
  symbol: method close()
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:31: error: cannot find symbol
        factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
                                                ^
  symbol:   variable VersionFlag
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:31: error: cannot find symbol
        factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
                  ^
  symbol:   variable JsonSchemaFactory
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:39: error: cannot find symbol
        InputStream schemaStream = new ByteArrayInputStream(schemaJson.getBytes(StandardCharsets.UTF_8));
        ^
  symbol:   class InputStream
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:39: error: cannot find symbol
        InputStream schemaStream = new ByteArrayInputStream(schemaJson.getBytes(StandardCharsets.UTF_8));
                                       ^
  symbol:   class ByteArrayInputStream
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:39: error: cannot find symbol
        InputStream schemaStream = new ByteArrayInputStream(schemaJson.getBytes(StandardCharsets.UTF_8));
                                                                                ^
  symbol:   variable StandardCharsets
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:41: error: cannot find symbol
        JsonSchema schema = factory.getSchema(schemaStream);
        ^
  symbol:   class JsonSchema
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:45: error: cannot find symbol
        ValidationResult result = schema.validate("hello");
        ^
  symbol:   class ValidationResult
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:52: error: cannot find symbol
        InputStream nullStream = null;
        ^
  symbol:   class InputStream
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:55: error: cannot find symbol
        assertThrows(Exception.class, () -> factory.getSchema(nullStream));
        ^
  symbol:   method assertThrows(Class<Exception>,()->factor[...]ream))
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:61: error: cannot find symbol
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        ^
  symbol:   class InputStream
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:61: error: cannot find symbol
        InputStream empty = new ByteArrayInputStream(new byte[0]);
                                ^
  symbol:   class ByteArrayInputStream
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:62: error: cannot find symbol
        Exception ex = assertThrows(Exception.class, () -> factory.getSchema(empty));
                       ^
  symbol:   method assertThrows(Class<Exception>,()->factor[...]mpty))
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:71: error: cannot find symbol
        InputStream stream = new ByteArrayInputStream(malformed.getBytes(StandardCharsets.UTF_8));
        ^
  symbol:   class InputStream
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:71: error: cannot find symbol
        InputStream stream = new ByteArrayInputStream(malformed.getBytes(StandardCharsets.UTF_8));
                                 ^
  symbol:   class ByteArrayInputStream
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:71: error: cannot find symbol
        InputStream stream = new ByteArrayInputStream(malformed.getBytes(StandardCharsets.UTF_8));
                                                                         ^
  symbol:   variable StandardCharsets
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:72: error: cannot find symbol
        Exception ex = assertThrows(Exception.class, () -> factory.getSchema(stream));
                       ^
  symbol:   method assertThrows(Class<Exception>,()->factor[...]ream))
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:74: error: cannot find symbol
        assertNotNull(ex.getMessage());
        ^
  symbol:   method assertNotNull(String)
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:81: error: cannot find symbol
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        ^
  symbol:   class InputStream
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:81: error: cannot find symbol
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
                                 ^
  symbol:   class ByteArrayInputStream
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:81: error: cannot find symbol
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
                                                                    ^
  symbol:   variable StandardCharsets
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:82: error: cannot find symbol
        JsonSchema schema = factory.getSchema(stream);
        ^
  symbol:   class JsonSchema
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:90: error: cannot find symbol
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        ^
  symbol:   class InputStream
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:90: error: cannot find symbol
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
                                 ^
  symbol:   class ByteArrayInputStream
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:90: error: cannot find symbol
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
                                                                    ^
  symbol:   variable StandardCharsets
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:91: error: cannot find symbol
        SchemaValidatorsConfig config = new SchemaValidatorsConfig().setFailFast(true);
        ^
  symbol:   class SchemaValidatorsConfig
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:91: error: cannot find symbol
        SchemaValidatorsConfig config = new SchemaValidatorsConfig().setFailFast(true);
                                            ^
  symbol:   class SchemaValidatorsConfig
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:92: error: cannot find symbol
        JsonSchema schema = factory.getSchema(stream, config);
        ^
  symbol:   class JsonSchema
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:98: error: cannot find symbol
        ValidationResult result = schema.validate(instance);
        ^
  symbol:   class ValidationResult
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:106: error: cannot find symbol
        InputStream stream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        ^
  symbol:   class InputStream
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:106: error: cannot find symbol
        InputStream stream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
                                 ^
  symbol:   class ByteArrayInputStream
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:106: error: cannot find symbol
        InputStream stream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
                                                                    ^
  symbol:   variable StandardCharsets
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:107: error: cannot find symbol
        JsonSchema schema = factory.getSchema(stream);
        ^
  symbol:   class JsonSchema
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:114: error: cannot find symbol
        InputStream faulty = new InputStream() {
        ^
  symbol:   class InputStream
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:114: error: cannot find symbol
        InputStream faulty = new InputStream() {
                                 ^
  symbol:   class InputStream
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:116: error: cannot find symbol
            public int read() throws IOException {
                                     ^
  symbol: class IOException
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:121: error: cannot find symbol
            public void close() throws IOException {
                                       ^
  symbol: class IOException
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:115: error: method does not override or implement a method from a supertype
            @Override
            ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:117: error: cannot find symbol
                throw new IOException("forced I/O error");
                          ^
  symbol: class IOException
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:120: error: method does not override or implement a method from a supertype
            @Override
            ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:125: error: cannot find symbol
        Exception ex = assertThrows(Exception.class, () -> factory.getSchema(faulty));
                       ^
  symbol:   method assertThrows(Class<Exception>,()->factor[...]ulty))
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:126: error: cannot find symbol
        assertTrue(ex.getMessage().contains("forced I/O error"));
        ^
  symbol:   method assertTrue(boolean)
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:132: error: cannot find symbol
        ByteArrayInputStream stream = new ByteArrayInputStream("{ \"type\": \"null\" }".getBytes(StandardCharsets.UTF_8));
        ^
  symbol:   class ByteArrayInputStream
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:132: error: cannot find symbol
        ByteArrayInputStream stream = new ByteArrayInputStream("{ \"type\": \"null\" }".getBytes(StandardCharsets.UTF_8));
                                          ^
  symbol:   class ByteArrayInputStream
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:132: error: cannot find symbol
        ByteArrayInputStream stream = new ByteArrayInputStream("{ \"type\": \"null\" }".getBytes(StandardCharsets.UTF_8));
                                                                                                 ^
  symbol:   variable StandardCharsets
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:133: error: cannot find symbol
        JsonSchema schema = factory.getSchema(stream);
        ^
  symbol:   class JsonSchema
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:145: error: cannot find symbol
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
                                         ^
  symbol:   variable StandardCharsets
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:149: error: cannot find symbol
        InputStream stream = new ByteArrayInputStream(combined);
        ^
  symbol:   class InputStream
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:149: error: cannot find symbol
        InputStream stream = new ByteArrayInputStream(combined);
                                 ^
  symbol:   class ByteArrayInputStream
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:150: error: cannot find symbol
        JsonSchema schema = factory.getSchema(stream);
        ^
  symbol:   class JsonSchema
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:158: error: cannot find symbol
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        ^
  symbol:   class InputStream
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:158: error: cannot find symbol
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
                                 ^
  symbol:   class ByteArrayInputStream
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:158: error: cannot find symbol
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
                                                                    ^
  symbol:   variable StandardCharsets
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:159: error: cannot find symbol
        JsonSchema schema = factory.getSchema(stream);
        ^
  symbol:   class JsonSchema
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:167: error: cannot find symbol
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        ^
  symbol:   class InputStream
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:167: error: cannot find symbol
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
                                 ^
  symbol:   class ByteArrayInputStream
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:167: error: cannot find symbol
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
                                                                    ^
  symbol:   variable StandardCharsets
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:168: error: cannot find symbol
        JsonSchema schema = factory.getSchema(stream);
        ^
  symbol:   class JsonSchema
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:176: error: cannot find symbol
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        ^
  symbol:   class InputStream
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:176: error: cannot find symbol
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
                                 ^
  symbol:   class ByteArrayInputStream
  location: class JsonSchemaFactory_getSchema_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_2_Test.java:176: error: cannot find symbol
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
                                                                    ^
  symbol:   variable StandardCharsets
  location: class JsonSchemaFactory_getSchema_2_Test
100 errors
only showing the first 100 errors, of 117 total; use -Xmaxerrs if you would like to see more

```

And you can use the following related information to assist you in your analysis and correction:
```
{'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchemaFactory`:\n\trelated functions:\n\t\tmethod `public JsonSchema getSchema(InputStream)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(InputStream)`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(InputStream)`, api document: \n     * Returns the singleton instance of this validator.\n     *\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


