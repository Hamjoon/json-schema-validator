{
    "groups": [
        {
            "group": "Cache Miss with Caching Enabled",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "http://example.com/new-schema.json"
                        },
                        {
                            "parameter": "config",
                            "value": "configWithCacheEnabled"
                        }
                    ],
                    "expected": "JsonSchema instance returned; schema is stored in uriSchemaCache because enableUriSchemaCache=true and cache was previously empty for this URI",
                    "description": "When caching is turned on but the requested URI is not yet cached, the method must fetch, parse and then put the newly created JsonSchema into the cache."
                }
            ]
        },
        {
            "group": "Cache Hit after Miss with Caching Enabled",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "http://example.com/new-schema.json"
                        },
                        {
                            "parameter": "config",
                            "value": "secondConfig"
                        }
                    ],
                    "expected": "Previously cached JsonSchema instance returned; its ValidationContext config replaced with secondConfig",
                    "description": "After the first call cached the schema (see previous test), a second call with a different config must hit the cache, return the same JsonSchema object and update its ValidationContext with the new config."
                }
            ]
        },
        {
            "group": "URI Translation Alters URI",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "http://example.com/base/schema.json"
                        },
                        {
                            "parameter": "config",
                            "value": "configWithRelativeTranslator"
                        }
                    ],
                    "expected": "JsonSchema instance returned; the URI passed to uriFactory.create is the result of config.getUriTranslator().with(factoryTranslator).translate(originalUri) (e.g., http://example.com/base/translated/schema.json)",
                    "description": "When a config provides its own URITranslator that rewrites the incoming URI (e.g., adds a path segment), the method must use the combined translator, and the mapped URI used for fetching and caching must reflect the translation."
                }
            ]
        },
        {
            "group": "Config with Null URI Translator",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "http://example.com/null-translator-schema.json"
                        },
                        {
                            "parameter": "config",
                            "value": "configWithNullTranslator"
                        }
                    ],
                    "expected": "JsonSchema instance returned; factory's default URI translator is used because config.getUriTranslator() is null; no NullPointerException is thrown",
                    "description": "The method calls config.getUriTranslator().with(factoryTranslator). If the config's translator is null, the implementation should fall back to the factory translator without throwing NPE. The test verifies graceful handling."
                }
            ]
        },
        {
            "group": "Parsing Error due to Wrong Content Type (YAML in JSON file)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "http://example.com/wrong-type.json"
                        },
                        {
                            "parameter": "config",
                            "value": null
                        }
                    ],
                    "expected": "JsonSchemaException thrown (cause JsonProcessingException) because jsonMapper fails to parse YAML content from a .json file",
                    "description": "The URI ends with .json, so isYaml() returns false and jsonMapper is used. The underlying stream actually contains YAML, causing a parsing failure that must be wrapped in JsonSchemaException."
                }
            ]
        },
        {
            "group": "Null schemaUri handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": null
                        },
                        {
                            "parameter": "config",
                            "value": null
                        }
                    ],
                    "expected": "JsonSchemaException thrown (cause IllegalArgumentException or NullPointerException) because uriFactory.create cannot handle a null URI",
                    "description": "Passing a null schemaUri should result in an exception wrapped as JsonSchemaException; the test confirms that the method does not silently succeed."
                }
            ]
        },
        {
            "group": "Null InputStream from uriFetcher",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "http://example.com/null-stream-schema.json"
                        },
                        {
                            "parameter": "config",
                            "value": null
                        }
                    ],
                    "expected": "JsonSchemaException thrown (cause NullPointerException) when attempting to read from a null InputStream",
                    "description": "If the URISchemeFetcher returns null instead of a valid InputStream, the subsequent readTree call will throw NPE; the method must wrap it in JsonSchemaException."
                }
            ]
        }
    ]
}