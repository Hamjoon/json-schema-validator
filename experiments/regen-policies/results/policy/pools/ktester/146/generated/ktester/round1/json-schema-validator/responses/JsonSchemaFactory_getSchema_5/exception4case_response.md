{
    "groups": [
        {
            "group": "JsonSchemaFactory.getSchema(String) - parsing error cases",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "{ \"type\": \"object\", \"properties\": { \"id\": { \"type\": \"integer\" }"
                        }
                    ],
                    "expected": "JsonProcessingException (or IOException) because the JSON is malformed and cannot be parsed",
                    "description": "Provide a schema string with missing closing braces. The parser should fail during tree construction, exposing the branch that validates raw JSON syntax."
                },
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "ftp://example.com/schema.json"
                        }
                    ],
                    "expected": "IllegalArgumentException because the URI scheme 'ftp' is not supported by the factory",
                    "description": "Pass a URI with an unsupported scheme. The method should reject it early rather than attempting to load the resource."
                },
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "http://example.com/nonexistent-schema.json"
                        }
                    ],
                    "expected": "IOException (or JsonSchemaException) indicating that the HTTP resource could not be retrieved (e.g., 404 Not Found)",
                    "description": "Use a well‑formed HTTP URI that points to a non‑existent remote schema. The loader should attempt the request and surface the I/O failure."
                },
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "{ \"$ref\": \"#/definitions/a\", \"definitions\": { \"a\": { \"$ref\": \"#/definitions/b\" }, \"b\": { \"$ref\": \"#/definitions/a\" } } }"
                        }
                    ],
                    "expected": "JsonSchemaException (or IllegalStateException) because a circular $ref is detected during schema resolution",
                    "description": "Supply a schema containing mutually recursive internal references. The factory must detect the cycle and abort with a clear exception."
                },
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "{ \"$ref\": \"http://example.com/missing.json#/def\" }"
                        }
                    ],
                    "expected": "JsonSchemaCreationException (or ValidationException) because the external $ref cannot be resolved",
                    "description": "Provide a schema that references an external definition that does not exist. Resolution should fail and propagate a creation‑time exception."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(String) - resource handling error cases",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "file:/tmp/"
                        }
                    ],
                    "expected": "IOException (or IllegalArgumentException) because the path points to a directory, not a readable file",
                    "description": "Pass a file‑URI that resolves to a directory. The loader should reject it as an invalid schema source."
                },
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "file:/tmp/plain.txt"
                        }
                    ],
                    "expected": "JsonProcessingException (or IOException) because the file content is not valid JSON/YAML for a schema",
                    "description": "Reference a file that exists but contains plain text rather than JSON/YAML. Parsing should fail, exposing the content‑validation branch."
                }
            ]
        }
    ]
}