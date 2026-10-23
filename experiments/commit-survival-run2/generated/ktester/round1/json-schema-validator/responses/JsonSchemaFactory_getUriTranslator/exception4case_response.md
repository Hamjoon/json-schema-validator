{
    "groups": [
        {
            "group": "InvalidUriInMap",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "uriMap",
                            "value": {
                                "ht!tp://bad uri": "classpath:/valid-schema.json"
                            }
                        }
                    ],
                    "expected": "IllegalArgumentException (or URISyntaxException) thrown when getUriTranslator() is invoked because the map contains a key that is not a valid URI",
                    "description": "Create a JsonSchemaFactory with a uriMap whose key is syntactically invalid as a URI. Calling getUriTranslator() should fail during the creation of the map‑based translator, verifying that malformed source URIs are rejected."
                },
                {
                    "input": [
                        {
                            "parameter": "uriMap",
                            "value": {
                                "http://example.com/schema": "bad://:@@"
                            }
                        }
                    ],
                    "expected": "IllegalArgumentException (or URISyntaxException) thrown when getUriTranslator() is invoked because the map contains a value that is not a valid URI",
                    "description": "Create a JsonSchemaFactory with a uriMap whose target value cannot be parsed as a URI. The translator construction should detect the invalid target and throw an exception."
                }
            ]
        },
        {
            "group": "EmptyStringKeyOrValue",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "uriMap",
                            "value": {
                                "": "classpath:/empty-key.json"
                            }
                        }
                    ],
                    "expected": "IllegalArgumentException thrown when getUriTranslator() is invoked because the map contains an empty string as a key",
                    "description": "Inject a map with an empty string key. The URITranslator.map implementation should reject empty keys and raise an exception during translator creation."
                },
                {
                    "input": [
                        {
                            "parameter": "uriMap",
                            "value": {
                                "http://example.com/schema": ""
                            }
                        }
                    ],
                    "expected": "IllegalArgumentException thrown when getUriTranslator() is invoked because the map contains an empty string as a value",
                    "description": "Inject a map where the target URI is an empty string. The translator should treat this as invalid and throw an exception at construction time."
                }
            ]
        }
    ]
}