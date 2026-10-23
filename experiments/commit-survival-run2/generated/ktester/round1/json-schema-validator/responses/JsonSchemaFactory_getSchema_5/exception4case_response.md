{
    "testCases": [
        {
            "group": "Invalid schema content – JSON parsing error",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "java.net.URI.create(\"classpath:/schemas/invalid-json-schema.json\")"
                        }
                    ],
                    "expected": "Throws JsonSchemaException (or JsonProcessingException) with a message indicating a JSON parsing error.",
                    "description": "The URI points to a classpath resource that exists but contains malformed JSON. The factory should attempt to parse the content and fail with a parsing‑related exception."
                }
            ]
        },
        {
            "group": "Invalid schema content – YAML parsing error",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "java.net.URI.create(\"classpath:/schemas/invalid-yaml-schema.yaml\")"
                        }
                    ],
                    "expected": "Throws JsonSchemaException (or JsonProcessingException) with a message indicating a YAML parsing error.",
                    "description": "The URI points to a YAML file on the classpath that is syntactically invalid. The factory uses YAMLMapper to read the file and must surface the parsing failure."
                }
            ]
        },
        {
            "group": "Fragment points to non‑existent subschema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "java.net.URI.create(\"classpath:/schemas/example-schema.json#/definitions/doesNotExist\")"
                        }
                    ],
                    "expected": "Throws JsonSchemaException with a message indicating that the fragment '#/definitions/doesNotExist' could not be resolved.",
                    "description": "A valid base schema is loaded, but the fragment refers to a JSON Pointer that does not exist in the document. The factory should detect the missing subschema and raise an exception."
                }
            ]
        },
        {
            "group": "HTTP URI returns 404 Not Found",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "java.net.URI.create(\"https://raw.githubusercontent.com/networknt/json-schema-validator/master/src/test/resources/schema/nonexistent-schema.json\")"
                        }
                    ],
                    "expected": "Throws JsonSchemaException (or IOException) indicating an HTTP 404 response – resource not found.",
                    "description": "The method attempts to fetch a remote schema that does not exist. The HTTP client should surface the 404 status as an exception."
                }
            ]
        },
        {
            "group": "File URI with unreadable file (permission denied)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "java.net.URI.create(\"file:/tmp/unreadable-schema.json\")"
                        }
                    ],
                    "expected": "Throws IOException (or JsonSchemaException) indicating that the file cannot be read due to insufficient permissions.",
                    "description": "The URI points to a regular file that exists but the process lacks read permission. The factory should attempt to open the file and fail with an I/O‑related exception."
                }
            ]
        },
        {
            "group": "Empty path with scheme (invalid URI for schema loading)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "java.net.URI.create(\"file:\")"
                        }
                    ],
                    "expected": "Throws IllegalArgumentException with a message containing \"schemaUri must have a non‑empty path\".",
                    "description": "A URI that has a valid scheme but no path component is supplied. The factory should validate the URI and reject it as unusable for schema loading."
                }
            ]
        },
        {
            "group": "Unsupported URN scheme",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "java.net.URI.create(\"urn:example:schema\")"
                        }
                    ],
                    "expected": "Throws JsonSchemaException (or RuntimeException) indicating that the scheme 'urn' is not supported.",
                    "description": "Although the URI is absolute, the scheme 'urn' is not handled by the factory's URITranslator. The method must detect this and raise an appropriate exception."
                }
            ]
        }
    ]
}