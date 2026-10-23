Invalid control character at: line 25 column 99 (char 1080)
{
    "testCases": [
        {
            "group": "InvalidYaml",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "new ByteArrayInputStream(\"type: object\\nproperties: [\".getBytes(StandardCharsets.UTF_8))"
                        }
                    ],
                    "expected": "JsonSchemaException wrapping a JsonProcessingException (YAML parsing error)",
                    "description": "The InputStream contains malformed YAML (truncated sequence). The factory should attempt to parse with YAMLMapper, encounter a parsing error, and wrap it in a JsonSchemaException."
                }
            ]
        },
        {
            "group": "InvalidUtf8Encoding",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "new ByteArrayInputStream(new byte[]{(byte)0xC3, (byte)0x28}) // invalid UTF‑8 sequence"
                        }
                    ],
                    "expected": "JsonSchemaException caused by an IOException or JsonProcessingException due to malformed UTF‑8",
                    "description": "The stream supplies bytes that do not form a valid UTF‑8 sequence. When the factory reads the stream and attempts to decode it as UTF‑8 JSON/YAML, a decoding error occurs, which should be wrapped in a JsonSchemaException."
                }
            ]
        },
        {
            "group": "UnknownMetaSchema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "new ByteArrayInputStream(\"{ \\\"$schema\\\": \\\"http://example.com/unknown-schema#\\\", \\\"type\\\": \\\"object\\\" }\".getBytes(StandardCharsets.UTF_8))"
                        }
                    ],
                    "expected": "JsonSchemaException (or IllegalArgumentException) indicating that the meta‑schema URI is not recognized",
                    "description": "A well‑formed JSON schema references a $schema URI that the factory does not know. The factory should validate the meta‑schema reference and throw an exception when it cannot resolve it."
                }
            ]
        },
        {
            "group": "IOExceptionOnClose",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "new InputStream() {\n    private final byte[] data = \"{ \\\"type\\\": \\\"object\\\" }\".getBytes(StandardCharsets.UTF_8);\n    private int pos = 0;\n    @Override public int read() throws IOException {\n        return (pos < data.length) ? (data[pos++] & 0xFF) : -1;\n    }\n    @Override public void close() throws IOException {\n        throw new IOException(\"forced close error\");\n    }\n}"
                        }
                    ],
                    "expected": "JsonSchema instance (non‑null); no exception propagated from close()",
                    "description": "The InputStream provides valid JSON but its close() method throws an IOException. The factory must still return a JsonSchema and must not let the close‑time IOException escape (it may be logged or suppressed)."
                }
            ]
        },
        {
            "group": "PartialReadIOException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "new InputStream() {\n    private final byte[] data = \"{ \\\"type\\\": \\\"object\\\" }\".getBytes(StandardCharsets.UTF_8);\n    private int pos = 0;\n    @Override public int read() throws IOException {\n        if (pos < 5) { // return first few bytes then fail\n            return data[pos++] & 0xFF;\n        }\n        throw new IOException(\"forced read error after partial data\");\n    }\n    @Override public void close() throws IOException { /* normal close */ }\n}"
                        }
                    ],
                    "expected": "JsonSchemaException wrapping the IOException thrown during read",
                    "description": "The stream returns a few bytes of a valid JSON schema and then throws an IOException. The factory should treat this as a read failure, close the stream, and propagate the error wrapped in a JsonSchemaException."
                }
            ]
        }
    ]
}