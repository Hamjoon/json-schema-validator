{
    "groups": [
        {
            "group": "Unsupported scheme – FTP",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"ftp://example.com/schema.json\")"
                        }
                    ],
                    "expected": "IllegalArgumentException with message containing \"Unsupported schema URI scheme\"",
                    "description": "The URI uses the FTP scheme which the factory does not support. The method should detect the unsupported scheme and throw an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Unsupported scheme – Data URI",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"data:application/json,{\\\"type\\\":\\\"object\\\"}\")"
                        }
                    ],
                    "expected": "IllegalArgumentException with message containing \"Unsupported schema URI scheme\"",
                    "description": "A data URI embeds the schema directly. The factory does not handle data URIs and must reject it with an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Directory URI – loading failure",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"file:/tmp/\" )"
                        }
                    ],
                    "expected": "JsonSchemaException (or IOException) indicating that the target is not a readable schema file",
                    "description": "The URI points to a directory rather than a regular file. When the loader attempts to read it, it should fail and propagate an appropriate exception."
                }
            ]
        },
        {
            "group": "Unreachable HTTP host – connection error",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"http://nonexistent-host-12345.local/schema.json\")"
                        }
                    ],
                    "expected": "JsonSchemaException (or IOException) caused by a connection failure (e.g., UnknownHostException)",
                    "description": "The HTTP URI refers to a host that cannot be resolved or contacted. The factory should surface the I/O problem rather than returning a schema."
                }
            ]
        },
        {
            "group": "Circular $ref – recursion detection",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"classpath:/schemas/circular-ref-schema.json\")"
                        }
                    ],
                    "expected": "JsonSchemaException (or IllegalStateException) indicating a circular reference or recursion limit exceeded",
                    "description": "The referenced schema contains a $ref that ultimately points back to itself, forming a cycle. The factory must detect the circular reference and throw a clear exception."
                }
            ]
        },
        {
            "group": "Unsupported draft version – unknown $schema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"classpath:/schemas/unknown-draft-schema.json\")"
                        }
                    ],
                    "expected": "IllegalArgumentException with message containing \"Unsupported schema version\"",
                    "description": "The schema declares a $schema IRI that does not correspond to any known draft (e.g., \"http://json-schema.org/draft-99/schema#\"). The factory should reject it with an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Empty URI string – invalid argument",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"\")"
                        }
                    ],
                    "expected": "IllegalArgumentException with message containing \"Schema location must not be empty\"",
                    "description": "An empty URI string is syntactically valid but provides no location information. The factory should treat this as an illegal argument and throw an IllegalArgumentException."
                }
            ]
        }
    ]
}