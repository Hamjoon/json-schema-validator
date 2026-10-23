{
    "testCases": [
        {
            "group": "JsonWithUTF8BOM",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "new ByteArrayInputStream((new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF}) + \"{ \\\"type\\\": \\\"object\\\" }\".getBytes(StandardCharsets.UTF_8))"
                        }
                    ],
                    "expected": "JsonSchema instance (non‑null) – the UTF‑8 BOM is ignored and the schema is parsed successfully",
                    "description": "InputStream begins with a UTF‑8 Byte Order Mark before a minimal valid JSON schema. The factory should strip the BOM and return a proper JsonSchema object."
                }
            ]
        },
        {
            "group": "JsonWithLeadingAndTrailingWhitespace",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "new ByteArrayInputStream(\"   \\n\\t{ \\\"type\\\": \\\"string\\\" }   \\n\\t\".getBytes(StandardCharsets.UTF_8))"
                        }
                    ],
                    "expected": "JsonSchema instance (non‑null) – whitespace surrounding the JSON does not affect parsing",
                    "description": "The stream contains only whitespace before and after a valid JSON schema. The parser must ignore surrounding whitespace and produce a JsonSchema."
                }
            ]
        },
        {
            "group": "JsonWithUnicodeCharacters",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "new ByteArrayInputStream(\"{ \\\"title\\\": \\\"Привет мир\\\", \\\"type\\\": \\\"object\\\" }\".getBytes(StandardCharsets.UTF_8))"
                        }
                    ],
                    "expected": "JsonSchema instance (non‑null) – Unicode characters are correctly decoded and stored",
                    "description": "A schema containing non‑ASCII characters (Cyrillic) is provided. The factory must read the stream using UTF‑8 and create a JsonSchema without errors."
                }
            ]
        },
        {
            "group": "JsonWithComments",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "new ByteArrayInputStream(\"{ /* comment */ \\\"type\\\": \\\"string\\\" }\".getBytes(StandardCharsets.UTF_8))"
                        }
                    ],
                    "expected": "JsonSchemaException (caused by JsonProcessingException) – comments are not allowed by default Jackson configuration",
                    "description": "The input JSON contains a block comment. Since the default ObjectMapper used by JsonSchemaFactory does not enable ALLOW_COMMENTS, parsing should fail and the method should wrap the JsonProcessingException."
                }
            ]
        },
        {
            "group": "WhitespaceOnly",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "new ByteArrayInputStream(\"   \\n\\t   \".getBytes(StandardCharsets.UTF_8))"
                        }
                    ],
                    "expected": "JsonSchemaException (or IllegalArgumentException) indicating that the schema content is empty or whitespace only",
                    "description": "An InputStream that contains only whitespace characters should be treated as having no usable schema content and result in a clear exception."
                }
            ]
        },
        {
            "group": "StreamClosedOnParsingFailure",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "new InputStream() {\n    private boolean closed = false;\n    @Override public int read() throws IOException { throw new IOException(\"forced error\"); }\n    @Override public void close() throws IOException { closed = true; }\n    public boolean isClosed() { return closed; }\n}"
                        }
                    ],
                    "expected": "JsonSchemaException wrapping IOException and the stream is closed (isClosed() == true)",
                    "description": "When parsing fails due to an IOException, the factory must still close the provided InputStream. The custom stream tracks closure to verify this behavior."
                }
            ]
        }
    ]
}