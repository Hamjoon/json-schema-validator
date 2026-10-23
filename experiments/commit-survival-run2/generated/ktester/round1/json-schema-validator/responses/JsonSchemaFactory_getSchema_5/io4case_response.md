{
  "testCases": [
    {
      "group": "Relative URI (no scheme) – should be rejected",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "java.net.URI.create(\"schema.json\")"
            }
          ],
          "expected": "Throws IllegalArgumentException with message containing \"schemaUri must be absolute\"",
          "description": "A URI without a scheme or authority is supplied. The factory is expected to validate that the URI is absolute and reject relative URIs."
        }
      ]
    },
    {
      "group": "Upper‑case scheme handling (case‑insensitivity)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "java.net.URI.create(\"HTTP://raw.githubusercontent.com/networknt/json-schema-validator/master/src/test/resources/schema/draftv4/schema.json\")"
            }
          ],
          "expected": "Returns a non‑null JsonSchema instance representing the parsed remote schema",
          "description": "The scheme part of the URI is upper‑case. The implementation should treat it as case‑insensitive and handle it like a normal http URI."
        }
      ]
    },
    {
      "group": "Percent‑encoded characters in path",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "java.net.URI.create(\"classpath:/schemas/%65xample-schema.json\")"
            }
          ],
          "expected": "Returns a non‑null JsonSchema instance identical to loading \"classpath:/schemas/example-schema.json\"",
          "description": "The path contains percent‑encoded characters (\"%65\" = \"e\"). The factory must decode the path before locating the resource."
        }
      ]
    },
    {
      "group": "Caching – same URI returns same JsonSchema instance",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "java.net.URI.create(\"classpath:/schemas/example-schema.json\")"
            }
          ],
          "expected": "Second call returns the exact same JsonSchema object (reference equality) as the first call",
          "description": "When the same absolute URI is requested repeatedly, the factory should reuse the previously built schema rather than parsing it again."
        }
      ]
    },
    {
      "group": "YAML schema loading via classpath",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "java.net.URI.create(\"classpath:/schemas/example-schema.yaml\")"
            }
          ],
          "expected": "Returns a non‑null JsonSchema instance representing the parsed YAML schema",
          "description": "The factory supports YAML (via YAMLMapper). Providing a .yaml resource on the classpath should be parsed correctly."
        }
      ]
    },
    {
      "group": "URI with empty fragment identifier",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "java.net.URI.create(\"classpath:/schemas/example-schema.json#\")"
            }
          ],
          "expected": "Returns the same JsonSchema as loading the URI without the fragment",
          "description": "An empty fragment should be ignored; the factory should load the whole document."
        }
      ]
    },
    {
      "group": "File‑scheme URI pointing to a directory",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "java.net.URI.create(\"file:/tmp/\")"
            }
          ],
          "expected": "Throws JsonSchemaException (or IOException) indicating that the target is not a regular file",
          "description": "A URI that resolves to a directory rather than a file should be rejected because a schema must be a readable file."
        }
      ]
    },
    {
      "group": "URI with both query and fragment – fragment takes precedence after query stripping",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "java.net.URI.create(\"classpath:/schemas/complex-schema.json?ver=2#definitions/address\")"
            }
          ],
          "expected": "Returns a JsonSchema representing the subschema at \"#/definitions/address\" (query is ignored)",
          "description": "The implementation must strip the query part, then resolve the fragment to a subschema."
        }
      ]
    },
    {
      "group": "Trailing slash in file URI – should be treated as invalid resource",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "java.net.URI.create(\"file:/tmp/example-schema.json/\")"
            }
          ],
          "expected": "Throws JsonSchemaException (or IOException) because the path does not point to a readable file",
          "description": "A trailing slash after a file name makes the path refer to a non‑existent directory; the factory must fail gracefully."
        }
      ]
    },
    {
      "group": "Fragment pointing to root ('#/') – should return the whole schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "java.net.URI.create(\"classpath:/schemas/example-schema.json#/ \")"
            }
          ],
          "expected": "Returns a JsonSchema identical to loading the base URI without fragment",
          "description": "A fragment that resolves to the root of the document should result in the full schema being returned."
        }
      ]
    }
  ]
}