{
    "testCases": [
        {
            "group": "InputStream null handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "SchemaLocation pointing to a valid URI but schemaLoader returns a Schema whose getInputStream() returns null"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchemaException thrown with message containing \"Failed to load json schema from\"",
                    "description": "When the schema loader returns a null InputStream the method should throw an IOException which is caught and re‑thrown as a JsonSchemaException."
                }
            ]
        },
        {
            "group": "IOException while reading stream",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "SchemaLocation for a JSON schema where schemaLoader returns a non‑null InputStream but readTree throws IOException"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchemaException thrown with cause being the original IOException",
                    "description": "If reading the InputStream fails, the catch block should wrap the IOException in a JsonSchemaException and preserve the cause."
                }
            ]
        },
        {
            "group": "YAML schema without fragment",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "SchemaLocation whose URI ends with .yaml and getFragment() returns null"
                        },
                        {
                            "parameter": "config",
                            "value": "SchemaValidatorsConfig with default path type"
                        }
                    ],
                    "expected": "JsonSchema instance returned (non‑null) created via doCreate with suppressSubSchemaRetrieval = true",
                    "description": "When the URI indicates a YAML document and there is no fragment, the method must read the stream as YAML, obtain the meta‑schema, create a ValidationContext and call doCreate with suppressSubSchemaRetrieval = true."
                }
            ]
        },
        {
            "group": "JSON schema without fragment",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "SchemaLocation whose URI ends with .json and getFragment() returns null"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchema instance returned (non‑null) created via doCreate with suppressSubSchemaRetrieval = true",
                    "description": "When the document is JSON and has no fragment, the method must read the stream as JSON and follow the same path as the YAML case, but using InputFormat.JSON."
                }
            ]
        },
        {
            "group": "YAML schema with fragment",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "SchemaLocation pointing to a .yaml file with a non‑empty fragment (e.g. #/properties/name)"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchema instance representing the sub‑schema referenced by the fragment (non‑null)",
                    "description": "When a fragment is present, the method must read the whole document, create a ValidationContext for the document, build the document schema via doCreate (suppressSubSchemaRetrieval = false) and finally return document.getRefSchema(fragment)."
                }
            ]
        },
        {
            "group": "JSON schema with fragment",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "SchemaLocation pointing to a .json file with a fragment (e.g. #/definitions/address)"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchema instance representing the referenced sub‑schema (non‑null)",
                    "description": "Same as the previous case but for a JSON document; verifies that the JSON branch and fragment handling work together."
                }
            ]
        },
        {
            "group": "Fragment present but empty (nameCount == 0)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "SchemaLocation whose fragment object exists but getNameCount() returns 0"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchema instance created via the \"no fragment\" path (suppressSubSchemaRetrieval = true)",
                    "description": "Even though a fragment object is present, a name count of 0 should be treated as no fragment, exercising the condition `(schemaUri.getFragment() == null) || (schemaUri.getFragment().getNameCount() == 0)`."
                }
            ]
        }
    ]
}