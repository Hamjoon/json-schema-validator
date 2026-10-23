{
  "testCases": [
    {
      "group": "TrailingSlashMapping",
      "cases": [
        {
          "input": [
            {
              "parameter": "uriMap",
              "value": {
                "http://example.com/schema/": "classpath:/schema-slash.json"
              }
            },
            {
              "parameter": "sourceUri",
              "value": "http://example.com/schema"
            }
          ],
          "expected": "Identity translation (output URI equals input URI) because the map key includes a trailing slash that does not match the supplied URI.",
          "description": "Build a JsonSchemaFactory with a uriMap entry whose key ends with a '/'. Verify that a URI without the trailing slash is **not** matched and the translator behaves as an identity translator for that input."
        },
        {
          "input": [
            {
              "parameter": "uriMap",
              "value": {
                "http://example.com/schema/": "classpath:/schema-slash.json"
              }
            },
            {
              "parameter": "sourceUri",
              "value": "http://example.com/schema/"
            }
          ],
          "expected": "classpath:/schema-slash.json",
          "description": "Same factory as above, but the supplied URI includes the trailing slash, so the mapping should be applied and the translator should return the target URI."
        }
      ]
    },
    {
      "group": "CaseSensitivityInMapping",
      "cases": [
        {
          "input": [
            {
              "parameter": "uriMap",
              "value": {
                "HTTP://EXAMPLE.COM/SCHEMA": "classpath:/caps-schema.json"
              }
            },
            {
              "parameter": "sourceUri",
              "value": "http://example.com/schema"
            }
          ],
          "expected": "Identity translation (output URI equals input URI) because map lookup is case‑sensitive.",
          "description": "Create a factory with a mapping that uses upper‑case scheme and host. Pass a lower‑case URI and verify that the translator does **not** perform a case‑insensitive match."
        },
        {
          "input": [
            {
              "parameter": "uriMap",
              "value": {
                "HTTP://EXAMPLE.COM/SCHEMA": "classpath:/caps-schema.json"
              }
            },
            {
              "parameter": "sourceUri",
              "value": "HTTP://EXAMPLE.COM/SCHEMA"
            }
          ],
          "expected": "classpath:/caps-schema.json",
          "description": "When the exact case used in the map key is supplied, the translation succeeds, confirming that the matching is case‑sensitive rather than case‑insensitive."
        }
      ]
    },
    {
      "group": "FragmentAndQueryMapping",
      "cases": [
        {
          "input": [
            {
              "parameter": "uriMap",
              "value": {
                "http://example.com/schema#section": "classpath:/schema-section.json",
                "http://example.com/schema?version=1": "classpath:/schema-v1.json"
              }
            },
            {
              "parameter": "sourceUri",
              "value": "http://example.com/schema#section"
            }
          ],
          "expected": "classpath:/schema-section.json",
          "description": "Mapping that includes a fragment identifier. The translator must match the full URI (including fragment) and return the associated target."
        },
        {
          "input": [
            {
              "parameter": "uriMap",
              "value": {
                "http://example.com/schema#section": "classpath:/schema-section.json",
                "http://example.com/schema?version=1": "classpath:/schema-v1.json"
              }
            },
            {
              "parameter": "sourceUri",
              "value": "http://example.com/schema?version=1"
            }
          ],
          "expected": "classpath:/schema-v1.json",
          "description": "Mapping that includes a query string. The translator must treat the query part as part of the key."
        },
        {
          "input": [
            {
              "parameter": "uriMap",
              "value": {
                "http://example.com/schema#section": "classpath:/schema-section.json",
                "http://example.com/schema?version=1": "classpath:/schema-v1.json"
              }
            },
            {
              "parameter": "sourceUri",
              "value": "http://example.com/schema"
            }
          ],
          "expected": "Identity translation (output URI equals input URI) because the supplied URI lacks both fragment and query and therefore does not match any map entry.",
          "description": "Ensures that a URI that matches the base part of a mapped key but differs in fragment/query does **not** get incorrectly translated."
        }
      ]
    },
    {
      "group": "CompositeTranslatorPrecedence",
      "cases": [
        {
          "input": [
            {
              "parameter": "customComposite",
              "value": "URITranslator that maps http://example.com/override -> classpath:/override-from-composite.json"
            },
            {
              "parameter": "uriMap",
              "value": {
                "http://example.com/override": "classpath:/override-from-map.json"
              }
            },
            {
              "parameter": "sourceUri",
              "value": "http://example.com/override"
            }
          ],
          "expected": "classpath:/override-from-composite.json",
          "description": "Build a factory where the underlying CompositeURITranslator already contains a mapping for the same source URI. After calling getUriTranslator(), the method adds the map‑based translator *after* the existing composite via `with()`. The test verifies that the original composite mapping takes precedence, i.e., the result comes from the pre‑existing translator, not the newly added map entry."
        }
      ]
    },
    {
      "group": "NullKeyOrValueInMap",
      "cases": [
        {
          "input": [
            {
              "parameter": "uriMap",
              "value": {
                "null": "classpath:/null-key.json"
              }
            },
            {
              "parameter": "sourceUri",
              "value": "null"
            }
          ],
          "expected": "NullPointerException thrown when getUriTranslator() is invoked (or when translate is called) because the map contains a null key.",
          "description": "Inject a map with a null key (using reflection or a custom builder). The `URITranslator.map` implementation is expected to reject null keys, resulting in an NPE."
        },
        {
          "input": [
            {
              "parameter": "uriMap",
              "value": {
                "http://example.com/valid": null
              }
            },
            {
              "parameter": "sourceUri",
              "value": "http://example.com/valid"
            }
          ],
          "expected": "NullPointerException thrown when translate() is invoked because the map contains a null target value.",
          "description": "Create a factory with a mapping whose value is null. When the translator attempts to return the mapped value, it should raise an NPE, confirming defensive checks for null values."
        }
      ]
    },
    {
      "group": "TranslatorIsNonNullAndImplementsInterface",
      "cases": [
        {
          "input": [],
          "expected": "Instance of com.networknt.schema.uri.URITranslator (not null)",
          "description": "Regardless of the internal state (default factory, empty map, etc.), calling getUriTranslator() must never return null and must return an object that implements the URITranslator interface."
        }
      ]
    }
  ]
}