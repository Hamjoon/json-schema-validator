{
  "testCases": [
    {
      "group": "URI with YAML extension – classpath resource",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "URI.create(\"classpath:/schemas/simple-schema.yaml\")"
            }
          ],
          "expected": "JsonSchema instance parsed from the YAML file (no exception)",
          "description": "Pass a valid classpath URI that points to a well‑formed YAML schema. The factory should detect the .yaml extension, use the YAML mapper, and return a non‑null JsonSchema."
        }
      ]
    },
    {
      "group": "URI with encoded characters",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "URI.create(\"classpath:/schemas/simple%20schema.json\")"
            }
          ],
          "expected": "JsonSchema instance representing the parsed schema (no exception)",
          "description": "Supply a classpath URI that contains percent‑encoded characters (space). The method must correctly decode the path and load the schema."
        }
      ]
    },
    {
      "group": "URI with fragment identifier",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "URI.create(\"classpath:/schemas/simple-schema.json#section\")"
            }
          ],
          "expected": "JsonSchema instance identical to the one loaded from the same URI without fragment (no exception)",
          "description": "A fragment part after ‘#’ should be ignored when locating the schema. The returned JsonSchema must be the same as when the fragment is omitted."
        }
      ]
    },
    {
      "group": "Repeated calls – caching behavior",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "URI.create(\"classpath:/schemas/simple-schema.json\")"
            }
          ],
          "expected": "Two successive calls to getSchema(uri) return the same JsonSchema instance (reference equality)",
          "description": "Invoke getSchema(uri) twice in the same factory instance and verify that the second call returns the cached JsonSchema rather than creating a new object."
        }
      ]
    },
    {
      "group": "Unsupported scheme – custom protocol",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "URI.create(\"custom://schemas/simple-schema.json\")"
            }
          ],
          "expected": "RuntimeException (or SchemaLoadingException) indicating that the scheme is not supported",
          "description": "Provide a URI with a scheme that the default SchemaLoader does not understand. The method should propagate a clear exception rather than silently succeed."
        }
      ]
    },
    {
      "group": "URI pointing to a directory",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "URI.create(\"classpath:/schemas/\")"
            }
          ],
          "expected": "SchemaLoadingException (or RuntimeException) because the location resolves to a directory, not a file",
          "description": "Pass a URI that resolves to a directory. The loader should reject it and throw an appropriate exception."
        }
      ]
    },
    {
      "group": "Upper‑case scheme handling – FILE URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "URI.create(\"FILE:/tmp/test-schema.json\")"
            }
          ],
          "expected": "JsonSchema instance parsed from the file (no exception)",
          "description": "Test that the factory treats scheme case‑insensitively; a FILE scheme in upper case must be handled like a normal file URI."
        }
      ]
    }
  ]
}