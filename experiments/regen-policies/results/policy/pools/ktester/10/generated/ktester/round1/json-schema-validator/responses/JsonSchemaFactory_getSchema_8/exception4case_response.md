{
    "groups": [
        {
            "group": "MissingNode as Schema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "missingNode (Jackson MissingNode instance)"
                        },
                        {
                            "parameter": "config",
                            "value": "defaultConfig"
                        }
                    ],
                    "expected": "NullPointerException thrown because the factory attempts to treat a MissingNode as a concrete schema node",
                    "description": "When the supplied JsonNode is a MissingNode (represents a non‑existent value), newJsonSchema cannot determine a schema location and throws NPE."
                }
            ]
        },
        {
            "group": "Unsupported $schema Identifier",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "validSchemaNode { \"$schema\": \"http://example.com/unknown/schema#\", \"type\": \"object\" }"
                        },
                        {
                            "parameter": "config",
                            "value": "defaultConfig"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown because the $schema URI does not map to a known meta‑schema",
                    "description": "The factory should reject schemas that declare an unknown meta‑schema identifier, raising an IllegalArgumentException during meta‑schema loading."
                }
            ]
        },
        {
            "group": "Malformed $schema URI",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "validSchemaNode { \"$schema\": \"ht!tp://[malformed]\", \"type\": \"array\" }"
                        },
                        {
                            "parameter": "config",
                            "value": "defaultConfig"
                        }
                    ],
                    "expected": "IllegalArgumentException (or URISyntaxException wrapped in IllegalArgumentException) thrown because the $schema value is not a valid URI",
                    "description": "A malformed URI in the $schema keyword should cause the factory to fail while trying to resolve the meta‑schema."
                }
            ]
        },
        {
            "group": "SchemaValidatorsConfig with Null Internal Map",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "validSchemaNode { \"type\": \"string\" }"
                        },
                        {
                            "parameter": "config",
                            "value": "configWithNullMap (SchemaValidatorsConfig whose internal map field is explicitly set to null)"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown because the factory expects a non‑null configuration map when propagating flags",
                    "description": "If a custom SchemaValidatorsConfig instance has its internal map cleared (null), the factory should detect the invalid state and raise an exception rather than proceeding with a null map."
                }
            ]
        },
        {
            "group": "Null JsonNode with Non‑Null Config (explicit check)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": null
                        },
                        {
                            "parameter": "config",
                            "value": "customConfig { \"typeLoose\": true }"
                        }
                    ],
                    "expected": "NullPointerException thrown because jsonNode is null irrespective of a provided configuration",
                    "description": "Even when a non‑null configuration is supplied, a null JsonNode must cause an NPE; this case verifies that the config does not mask the null‑node check."
                }
            ]
        }
    ]
}