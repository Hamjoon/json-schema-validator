{
  "testCases": [
    {
      "group": "SchemaLoader throws IOException",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation whose schemaLoader.getSchema(...) throws IOException"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonSchemaException thrown with message containing \"Failed to load json schema from\" and cause being the original IOException",
          "description": "When the underlying SchemaLoader cannot obtain a Schema object (throws IOException), getMappedSchema should catch the exception, log it and re‑throw a JsonSchemaException preserving the original cause."
        }
      ]
    },
    {
      "group": "Invalid YAML content causing parse exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation pointing to a .yaml file; schemaLoader returns a non‑null InputStream whose content is not valid YAML"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonSchemaException thrown with cause being the IOException (or JsonProcessingException) produced by readTree",
          "description": "If readTree fails while parsing malformed YAML, the IOException is wrapped in a JsonSchemaException, verifying error handling for invalid YAML documents."
        }
      ]
    },
    {
      "group": "Uppercase file extension handling (YAML)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation whose URI ends with .YAML (uppercase) and getFragment() returns null"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Non‑null JsonSchema instance returned; the stream is read with InputFormat.YAML",
          "description": "The isYaml method should treat case‑insensitive extensions as YAML. The method must read the document as YAML and create the schema with suppressSubSchemaRetrieval = true."
        }
      ]
    },
    {
      "group": "Uppercase file extension handling (JSON)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation whose URI ends with .JSON (uppercase) and getFragment() returns null"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Non‑null JsonSchema instance returned; the stream is read with InputFormat.JSON",
          "description": "The isYaml method must not mistakenly treat .JSON as YAML; the document should be parsed as JSON and the schema created correctly."
        }
      ]
    },
    {
      "group": "Fragment points to non‑existent sub‑schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation pointing to a valid .json file with fragment '#/nonexistent/path'"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonSchemaException thrown with message indicating that the fragment could not be resolved",
          "description": "When the fragment does not match any sub‑schema, document.getRefSchema(fragment) should fail, resulting in a JsonSchemaException. This verifies proper error reporting for unresolved $ref fragments."
        }
      ]
    },
    {
      "group": "Fragment points to root schema ('#/')",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation pointing to a valid .json file with fragment '#/' (root of the document)"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonSchema instance identical to the document schema (non‑null)",
          "description": "A fragment that points to the root should cause getRefSchema to return the same schema object that was created for the whole document."
        }
      ]
    },
    {
      "group": "Null config parameter",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation pointing to a valid .json file with no fragment"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "NullPointerException thrown (or JsonSchemaException if the implementation guards against null)",
          "description": "Passing a null SchemaValidatorsConfig should cause the method to fail early (most likely NPE when config.getPathType() is called). This tests defensive handling of null configuration."
        }
      ]
    },
    {
      "group": ".yml extension recognized as YAML",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation whose URI ends with .yml and getFragment() returns null"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Non‑null JsonSchema instance returned; the stream is read with InputFormat.YAML",
          "description": "The isYaml method should treat both .yaml and .yml extensions as YAML files."
        }
      ]
    },
    {
      "group": "SchemaLocation with query string but no fragment",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation whose URI is 'http://example.com/schema.json?version=1' and getFragment() returns null"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Non‑null JsonSchema instance returned; query part is ignored for loading, fragment handling follows the no‑fragment path",
          "description": "Ensures that a URI containing a query component does not interfere with fragment detection and schema loading."
        }
      ]
    },
    {
      "group": "Fragment is just '#', nameCount == 0",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation whose fragment string is exactly '#', resulting in a Path object with nameCount == 0"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonSchema instance created via the \"no fragment\" path (suppressSubSchemaRetrieval = true)",
          "description": "A fragment consisting solely of '#' should be treated as an empty fragment; the method must follow the same logic as when fragment is null."
        }
      ]
    }
  ]
}