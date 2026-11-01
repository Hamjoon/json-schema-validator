{
    "testCases": [
        {
            "group": "loadMetaSchema – empty IRI string",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "iri",
                            "value": "\"\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "Returns the JsonMetaSchema instance supplied by the selected factory (custom or default) for the empty IRI; no exception is thrown because the method does not validate the IRI format.",
                    "description": "Ensures that an empty string is treated as a legitimate IRI argument and is simply forwarded to the underlying MetaSchemaFactory, which may return a default meta‑schema or null depending on its implementation."
                }
            ]
        },
        {
            "group": "loadMetaSchema – IRI containing only whitespace",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "iri",
                            "value": "\"   \""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "Returns whatever JsonMetaSchema instance the underlying factory provides for the whitespace IRI; the method does not trim or validate the string.",
                    "description": "Verifies that whitespace is not stripped and is passed unchanged to the factory, confirming that loadMetaSchema does not perform any pre‑processing of the IRI argument."
                }
            ]
        },
        {
            "group": "loadMetaSchema – custom factory returns null",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "iri",
                            "value": "\"http://example.com/null-meta\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "Returns null (the exact value returned by the injected JsonMetaSchemaFactory mock).",
                    "description": "Checks that loadMetaSchema forwards the result of the custom factory without additional null‑checking; when the mock is programmed to return null, the method should also return null."
                }
            ]
        },
        {
            "group": "loadMetaSchema – custom factory throws RuntimeException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "iri",
                            "value": "\"http://example.com/bad-meta\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "Throws the RuntimeException propagated from the injected JsonMetaSchemaFactory mock.",
                    "description": "Validates that loadMetaSchema does not catch exceptions from the underlying factory; an exception thrown by the mock (e.g., IllegalArgumentException) should bubble up to the caller."
                }
            ]
        },
        {
            "group": "loadMetaSchema – non‑null config with custom settings",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "iri",
                            "value": "\"http://example.com/custom-config-meta\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig(){ { setTypeLoose(true); } }"
                        }
                    ],
                    "expected": "Returns the JsonMetaSchema instance from the selected factory; the custom config object is passed unchanged, allowing the factory to honour its settings.",
                    "description": "Ensures that a populated SchemaValidatorsConfig (e.g., with typeLoose = true) is correctly forwarded to the MetaSchemaFactory and does not cause any side‑effects within loadMetaSchema itself."
                }
            ]
        }
    ]
}