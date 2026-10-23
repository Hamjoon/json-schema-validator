{
  "testCases": [
    {
      "group": "Null Config – default URI translator",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "http://example.com/schema.json"
            },
            {
              "parameter": "config",
              "value": null
            }
          ],
          "expected": "JsonSchema instance returned; default URI translator used; cache miss path exercised; JSON parser invoked",
          "description": "When config is null the factory must fall back to its own URI translator. The schema is a regular JSON file, cache is disabled, and id does not match the source URI, so the else‑branch of idMatchesSourceUri is taken."
        }
      ]
    },
    {
      "group": "Custom Config – custom URI translator",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "http://example.com/custom-schema.json"
            },
            {
              "parameter": "config",
              "value": "customConfigWithTranslator"
            }
          ],
          "expected": "JsonSchema instance returned; config.getUriTranslator() combined with factory translator; cache miss path exercised; JSON parser invoked",
          "description": "When a non‑null config is supplied the method must combine the config‑provided URI translator with the factory‑provided one."
        }
      ]
    },
    {
      "group": "Invalid URI – IllegalArgumentException from URISchemeFactory",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "invalid:\\\\uri"
            },
            {
              "parameter": "config",
              "value": null
            }
          ],
          "expected": "JsonSchemaException thrown (cause IllegalArgumentException)",
          "description": "The uriFactory.create call throws IllegalArgumentException for a malformed URI; the method must log the error and wrap it in JsonSchemaException."
        }
      ]
    },
    {
      "group": "Cache Hit – enableUriSchemaCache = true",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "http://example.com/cached-schema.json"
            },
            {
              "parameter": "config",
              "value": "anyConfig"
            }
          ],
          "expected": "Previously cached JsonSchema instance returned; its ValidationContext config replaced with supplied config",
          "description": "When caching is enabled and the mapped URI is already present in uriSchemaCache, the method must return the cached JsonSchema and update its ValidationContext with the new config."
        }
      ]
    },
    {
      "group": "Cache Miss – YAML schema detection",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "http://example.com/schema.yaml"
            },
            {
              "parameter": "config",
              "value": null
            }
          ],
          "expected": "JsonSchema instance returned; yamlMapper.readTree used; idMatchesSourceUri evaluated (any result) but not relevant for branch coverage",
          "description": "When the URI ends with a .yaml/.yml extension, isYaml() returns true, causing the method to parse the InputStream with yamlMapper instead of jsonMapper."
        }
      ]
    },
    {
      "group": "Cache Miss – JSON schema with id matching source URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "http://example.com/matching-id-schema.json"
            },
            {
              "parameter": "config",
              "value": null
            }
          ],
          "expected": "JsonSchema instance returned; idMatchesSourceUri true; ValidationContext created with jsonMetaSchema and config; doCreate called with suppressSubSchemaRetrieval = true",
          "description": "When the schema’s $id (or id) exactly matches the source URI, the method follows the true‑branch of idMatchesSourceUri, creating a ValidationContext that includes the meta‑schema and passes ‘true’ to suppress sub‑schema retrieval."
        }
      ]
    },
    {
      "group": "Cache Miss – JSON schema with id NOT matching source URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "http://example.com/nonmatching-id-schema.json"
            },
            {
              "parameter": "config",
              "value": null
            }
          ],
          "expected": "JsonSchema instance returned; idMatchesSourceUri false; ValidationContext created via createValidationContext(schemaNode); doCreate called with suppressSubSchemaRetrieval = false",
          "description": "When the schema’s $id does not match the source URI, the else‑branch is taken, a ValidationContext is built from the schema node, config is set afterwards, and doCreate is invoked with ‘false’."
        }
      ]
    },
    {
      "group": "IOException while reading schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "http://example.com/unreachable-schema.json"
            },
            {
              "parameter": "config",
              "value": null
            }
          ],
          "expected": "JsonSchemaException thrown (cause IOException)",
          "description": "If uriFetcher.fetch throws or the InputStream cannot be read (e.g., network error), the method must catch IOException, log it and re‑throw as JsonSchemaException."
        }
      ]
    },
    {
      "group": "InputStream closed in finally block",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "http://example.com/valid-schema.json"
            },
            {
              "parameter": "config",
              "value": null
            }
          ],
          "expected": "InputStream.close() invoked exactly once after successful schema load",
          "description": "Regardless of success or failure, the finally block must close the InputStream if it was opened."
        }
      ]
    }
  ]
}