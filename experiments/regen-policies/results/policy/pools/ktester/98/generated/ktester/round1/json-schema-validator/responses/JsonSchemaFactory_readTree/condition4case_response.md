{
    "testCases": [
        {
            "group": "jsonNodeReader is null – ObjectMapper used",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "{\"name\":\"John\",\"age\":30}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "JSON"
                        }
                    ],
                    "expected": "JsonNode with fields name='John' and age=30",
                    "description": "When jsonNodeReader is null the method should delegate to the JSON ObjectMapper and correctly parse a valid JSON string."
                },
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "name: John\nage: 30"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "YAML"
                        }
                    ],
                    "expected": "JsonNode with fields name='John' and age=30",
                    "description": "When jsonNodeReader is null the method should delegate to the YAML ObjectMapper and correctly parse a valid YAML string."
                },
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "{invalidJson: true"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "JSON"
                        }
                    ],
                    "expected": "IOException",
                    "description": "If the JSON content is malformed, the underlying ObjectMapper throws IOException which must be propagated by readTree."
                }
            ]
        },
        {
            "group": "jsonNodeReader is non‑null – custom reader used",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "{\"custom\":\"value\"}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "JSON"
                        }
                    ],
                    "expected": "JsonNode returned by custom JsonNodeReader",
                    "description": "When jsonNodeReader is set, readTree must invoke jsonNodeReader.readTree with the same arguments and return its result."
                },
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "anyContent"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "YAML"
                        }
                    ],
                    "expected": "IOException",
                    "description": "If the custom JsonNodeReader throws IOException, readTree must propagate the exception."
                }
            ]
        }
    ]
}