Invalid control character at: line 70 column 268 (char 3529)
{
    "tests": [
        {
            "group": "Invalid $id URI",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "new ByteArrayInputStream('{\"$id\":\"ht!tp://[invalid]\",\"type\":\"object\"}'.getBytes(StandardCharsets.UTF_8))"
                        }
                    ],
                    "expected": "IllegalArgumentException with message containing \"$id\"",
                    "description": "The schema declares an $id that is not a valid URI. The factory should detect the malformed identifier and reject the schema with an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Unresolvable $ref",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "new ByteArrayInputStream('{\"$ref\":\"#/definitions/missing\",\"definitions\":{}}'.getBytes(StandardCharsets.UTF_8))"
                        }
                    ],
                    "expected": "JsonSchemaException with message containing \"Unresolved reference\"",
                    "description": "The schema contains a $ref that points to a definition that does not exist. During schema loading the factory should fail to resolve the reference and throw a JsonSchemaException."
                }
            ]
        },
        {
            "group": "Circular $ref detection",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "new ByteArrayInputStream('{\"definitions\":{\"node\":{\"$ref\":\"#/definitions/node\"}},\"$ref\":\"#/definitions/node\"}'.getBytes(StandardCharsets.UTF_8))"
                        }
                    ],
                    "expected": "JsonSchemaException with message containing \"Circular reference\"",
                    "description": "The schema defines a $ref that eventually resolves to itself, creating a circular reference. The factory must detect this cycle and abort with a JsonSchemaException."
                }
            ]
        },
        {
            "group": "Closed InputStream",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "(() -> { ByteArrayInputStream s = new ByteArrayInputStream(\"{}\".getBytes(StandardCharsets.UTF_8)); s.close(); return s; }).get()"
                        }
                    ],
                    "expected": "IOException with message containing \"Stream closed\"",
                    "description": "Passing a stream that has already been closed should cause an IOException when the factory attempts to read from it."
                }
            ]
        },
        {
            "group": "Partial read IOException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "new InputStream() {\n    private final byte[] data = \"{\\\"type\\\":\\\"object\\\"}\".getBytes(StandardCharsets.UTF_8);\n    private int pos = 0;\n    @Override\n    public int read() throws IOException {\n        if (pos < 5) { // return first few bytes successfully\n            return data[pos++] & 0xFF;\n        }\n        throw new IOException(\"simulated read error after partial data\");\n    }\n}"
                        }
                    ],
                    "expected": "IOException with message \"simulated read error after partial data\"",
                    "description": "The InputStream supplies a few bytes before failing. The factory should propagate the IOException that occurs during reading."
                }
            ]
        }
    ]
}