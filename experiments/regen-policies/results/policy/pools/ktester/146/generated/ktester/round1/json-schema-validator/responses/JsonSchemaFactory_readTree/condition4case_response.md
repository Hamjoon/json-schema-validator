{
    "testCases": [
        {
            "group": "jsonNodeReader is null – delegate to ObjectMapper",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "{\"name\":\"John\"}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        }
                    ],
                    "expected": "JsonNode representing the parsed JSON object (field \"name\" with value \"John\")",
                    "description": "When jsonNodeReader is null the method should obtain the JSON ObjectMapper via getObjectMapper(JSON) and return the JsonNode produced by ObjectMapper.readTree. The content is a valid JSON string."
                },
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "name: John\nage: 30"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.YAML"
                        }
                    ],
                    "expected": "JsonNode representing the parsed YAML (fields \"name\" and \"age\" with values \"John\" and 30)",
                    "description": "When jsonNodeReader is null and the requested format is YAML the method should delegate to the YAML ObjectMapper and return the parsed JsonNode."
                },
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "{invalid json}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        }
                    ],
                    "expected": "IOException thrown by ObjectMapper.readTree",
                    "description": "If the underlying ObjectMapper cannot parse the supplied content (invalid JSON), the IOException should propagate out of readTree."
                }
            ]
        },
        {
            "group": "jsonNodeReader is set – use JsonNodeReader implementation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "{\"id\":123}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        }
                    ],
                    "expected": "Mocked JsonNode with field \"id\" = 123",
                    "description": "When jsonNodeReader is non‑null the method must call jsonNodeReader.readTree(content, JSON) and return whatever JsonNode the reader provides. The mock is configured to return a JsonNode containing the field \"id\"."
                },
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "id: 123\nactive: true"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.YAML"
                        }
                    ],
                    "expected": "Mocked JsonNode with fields \"id\" = 123 and \"active\" = true",
                    "description": "Same as above but for YAML format. The JsonNodeReader mock should be invoked with InputFormat.YAML and return the prepared JsonNode."
                },
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "any content"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        }
                    ],
                    "expected": "IOException thrown by JsonNodeReader.readTree",
                    "description": "If the injected JsonNodeReader throws an IOException (e.g., due to I/O error), the exception must propagate unchanged from readTree."
                }
            ]
        }
    ]
}