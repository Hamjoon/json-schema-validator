{
    "test_cases": [
        {
            "group": "jsonNodeReader provided – null inputFormat handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "new ByteArrayInputStream('{\"sample\":123}'.getBytes(StandardCharsets.UTF_8))"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException (or IllegalArgumentException) thrown by the mocked JsonNodeReader because the inputFormat argument is null",
                    "description": "When jsonNodeReader is non‑null the factory method delegates directly to jsonNodeReader.readTree. Supplying a null InputFormat should cause the JsonNodeReader implementation to reject the argument and throw an NPE/IAE, which must propagate unchanged."
                }
            ]
        },
        {
            "group": "jsonNodeReader provided – input stream read error propagation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "new InputStream() {\n    @Override public int read() throws IOException { throw new IOException(\"forced read error\"); }\n}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        }
                    ],
                    "expected": "IOException thrown by the mocked JsonNodeReader due to underlying stream read failure",
                    "description": "If the supplied InputStream throws an IOException while the JsonNodeReader is reading, the JsonSchemaFactory.readTree method must let that IOException propagate (it does not catch it internally)."
                }
            ]
        }
    ]
}