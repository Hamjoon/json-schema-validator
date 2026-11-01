{
    "testCases": [
        {
            "group": "Invalid UTF-8 byte sequence",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "ByteArrayInputStream containing bytes 0xC3 0x28 (invalid UTF‑8) followed by a minimal JSON schema: {\"type\":\"object\"}"
                        }
                    ],
                    "expected": "IOException (or JsonProcessingException) indicating malformed UTF‑8 input",
                    "description": "Ensures that the factory fails fast when the InputStream contains an illegal UTF‑8 byte sequence, propagating the parsing exception rather than returning a schema."
                }
            ]
        },
        {
            "group": "Unrecognizable content format",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "ByteArrayInputStream containing plain‑text that is neither valid JSON nor valid YAML (e.g., \"Just some random text\")"
                        }
                    ],
                    "expected": "JsonSchemaException (or IOException) stating that the content format could not be detected as JSON or YAML",
                    "description": "Triggers the branch where format detection fails; the method should throw an exception instead of attempting to parse an unknown format."
                }
            ]
        },
        {
            "group": "Malformed $id URI",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "ByteArrayInputStream containing a JSON schema with an invalid $id value (e.g., {\"$id\":\"ht!tp://[invalid]\",\"type\":\"object\"})"
                        }
                    ],
                    "expected": "IllegalArgumentException (or JsonSchemaException) indicating that the $id URI is malformed",
                    "description": "Verifies that the factory validates the $id field and throws an exception when the URI cannot be parsed."
                }
            ]
        },
        {
            "group": "Closed InputStream before reading",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "InputStream mock that is already closed and throws IOException on any read operation"
                        }
                    ],
                    "expected": "IOException propagated from the read operation",
                    "description": "Checks that the method does not swallow an IOException caused by attempting to read from a closed stream."
                }
            ]
        },
        {
            "group": "Circular $ref leading to recursion overflow",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "ByteArrayInputStream containing a JSON schema with circular $ref (e.g., {\"$defs\":{\"a\":{\"$ref\":\"#/$defs/b\"},\"b\":{\"$ref\":\"#/$defs/a\"}},\"type\":\"object\",\"properties\":{\"c\":{\"$ref\":\"#/$defs/a\"}}})"
                        }
                    ],
                    "expected": "JsonSchemaException (or StackOverflowError) indicating that a circular reference was detected",
                    "description": "Ensures that the factory detects circular $ref loops during schema loading and raises an appropriate exception instead of entering infinite recursion."
                }
            ]
        }
    ]
}