{
    "groups": [
        {
            "group": "jsonNodeReader is null – delegate to ObjectMapper",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "new ByteArrayInputStream('{\"key\":\"value\"}'.getBytes(StandardCharsets.UTF_8))"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        }
                    ],
                    "expected": "JsonNode representing an object with a single field \"key\" equal to \"value\"",
                    "description": "When jsonNodeReader is null the method should obtain a JSON ObjectMapper via getObjectMapper(JSON) and parse the stream successfully."
                },
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "new ByteArrayInputStream('key: value'.getBytes(StandardCharsets.UTF_8))"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.YAML"
                        }
                    ],
                    "expected": "JsonNode representing an object with a single field \"key\" equal to \"value\"",
                    "description": "When jsonNodeReader is null the method should obtain a YAML ObjectMapper via getObjectMapper(YAML) and parse the YAML stream successfully."
                },
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "new ByteArrayInputStream(''.getBytes(StandardCharsets.UTF_8))"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        }
                    ],
                    "expected": "IOException thrown (or JsonProcessingException wrapped as IOException) because the input stream is empty/invalid JSON",
                    "description": "Verify that parsing an empty or invalid JSON stream results in an IOException propagating from ObjectMapper.readTree."
                },
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "null"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        }
                    ],
                    "expected": "NullPointerException thrown by ObjectMapper.readTree",
                    "description": "When the InputStream argument is null the underlying ObjectMapper should throw NPE, which must propagate."
                }
            ]
        },
        {
            "group": "jsonNodeReader is provided – delegate to JsonNodeReader",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "new ByteArrayInputStream('{\"a\":1}'.getBytes(StandardCharsets.UTF_8))"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        }
                    ],
                    "expected": "JsonNode returned by the mocked JsonNodeReader containing field \"a\" with numeric value 1",
                    "description": "When jsonNodeReader is non‑null the method must call jsonNodeReader.readTree(content, inputFormat) and return its result."
                },
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "new ByteArrayInputStream('a: 1'.getBytes(StandardCharsets.UTF_8))"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.YAML"
                        }
                    ],
                    "expected": "JsonNode returned by the mocked JsonNodeReader representing the YAML content",
                    "description": "Same as above but with YAML format – ensures the format argument is passed through unchanged."
                },
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "new ByteArrayInputStream('invalid'.getBytes(StandardCharsets.UTF_8))"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        }
                    ],
                    "expected": "IOException thrown by the mocked JsonNodeReader",
                    "description": "If the JsonNodeReader encounters malformed content it should throw IOException, which must propagate unchanged."
                },
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "null"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        }
                    ],
                    "expected": "IOException or NullPointerException thrown by the mocked JsonNodeReader",
                    "description": "When the InputStream is null the JsonNodeReader implementation is expected to fail; the factory method must not swallow the exception."
                }
            ]
        }
    ]
}