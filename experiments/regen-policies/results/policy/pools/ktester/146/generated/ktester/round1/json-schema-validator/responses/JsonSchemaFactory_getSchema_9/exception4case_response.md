{
    "test_cases": [
        {
            "group": "Invalid JSON content at schemaUri – malformed file",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new SchemaLocation(\"classpath:/schemas/malformed-schema.json\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "null"
                        }
                    ],
                    "expected": "IOException",
                    "description": "The schema file exists but contains malformed JSON. The factory should attempt to read it and propagate an IOException (or a subclass) indicating the parsing failure."
                }
            ]
        },
        {
            "group": "Fragment points to non‑existent definition",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new SchemaLocation(\"http://example.com/schema.json#/$defs/missingDefinition\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The URI contains a fragment that refers to a definition that is not present in the loaded schema. The factory must detect the missing fragment and throw an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "jsonNode is a primitive value – should raise IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new SchemaLocation(\"http://example.com/primitive-schema.json\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "JsonNodeFactory.instance.textNode(\"just a string\")"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "A JSON Schema must be a JSON object. Supplying a primitive JsonNode (e.g., a text node) should cause the factory to reject the input with an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "SchemaLocation points to unreadable file – IOException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new SchemaLocation(\"file:/nonexistent/path/schema.json\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "null"
                        }
                    ],
                    "expected": "IOException",
                    "description": "The provided file URI does not exist or cannot be opened. Attempting to read the schema should result in an IOException."
                }
            ]
        },
        {
            "group": "Unsupported scheme ftp – IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new SchemaLocation(\"ftp://example.com/schema.json\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "JsonNodeFactory.instance.objectNode().put(\"type\", \"string\")"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The factory only supports http, https, file and classpath schemes. Supplying an ftp URI should be rejected with an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "jsonNode is a numeric primitive – should raise IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new SchemaLocation(\"http://example.com/numeric-schema.json\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "JsonNodeFactory.instance.numberNode(42)"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "A numeric JsonNode is not a valid schema object. The factory must throw an IllegalArgumentException when such a node is supplied."
                }
            ]
        }
    ]
}