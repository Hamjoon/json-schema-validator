{
  "testCases": [
    {
      "group": "Remote HTTP URI – successful load",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://localhost:8080/remote-schema.json\")"
            }
          ],
          "expected": "JsonSchema instance is returned and its root node matches the JSON content served by the HTTP endpoint",
          "description": "A well‑formed HTTP URI that points to an accessible JSON schema served by a mock HTTP server. The method should perform an HTTP GET, read the response, and return a valid JsonSchema."
        }
      ]
    },
    {
      "group": "Remote HTTP URI – 404 Not Found",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://localhost:8080/missing-schema.json\")"
            }
          ],
          "expected": "JsonSchemaException (or IOException) indicating that the remote resource could not be retrieved (HTTP 404)",
          "description": "The HTTP endpoint returns a 404 status. The factory should surface the I/O problem rather than returning a null schema."
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
              "value": "new URI(\"classpath:/schemas/simple-schema.json#section\")"
            }
          ],
          "expected": "JsonSchema instance is returned and its root node matches the JSON content of simple-schema.json (fragment is ignored)",
          "description": "A classpath URI that includes a fragment. The fragment should not affect schema loading; the factory should strip it and load the underlying resource."
        }
      ]
    },
    {
      "group": "URI with query parameters",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"classpath:/schemas/simple-schema.json?version=1\")"
            }
          ],
          "expected": "JsonSchema instance is returned and its root node matches the JSON content of simple-schema.json (query is ignored)",
          "description": "A classpath URI that contains a query string. The loader should ignore the query part and successfully load the schema."
        }
      ]
    },
    {
      "group": "Empty schema file",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"classpath:/schemas/empty-schema.json\")"
            }
          ],
          "expected": "JsonSchemaException (or IOException) indicating that the schema content is empty or cannot be parsed",
          "description": "The referenced file exists but contains no characters. The factory should attempt to parse it and fail with a clear exception."
        }
      ]
    },
    {
      "group": "Invalid JSON content",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"classpath:/schemas/invalid-json-schema.json\")"
            }
          ],
          "expected": "JsonSchemaException (or IOException) indicating JSON parsing error",
          "description": "The file exists but contains malformed JSON (e.g., missing commas). The method should throw an exception that reflects the parsing problem."
        }
      ]
    },
    {
      "group": "Caching – repeated calls with same URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"classpath:/schemas/simple-schema.json\")"
            }
          ],
          "expected": "The second invocation of getSchema(URI) returns the same JsonSchema instance (reference equality) as the first call",
          "description": "JsonSchemaFactory caches loaded schemas in a ConcurrentMap. Verify that calling getSchema with an identical URI reuses the cached JsonSchema rather than creating a new object."
        }
      ]
    },
    {
      "group": "Relative URI handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"schemas/simple-schema.json\")"
            }
          ],
          "expected": "IllegalArgumentException with message containing \"Unsupported schema URI scheme\" or \"Relative URIs are not supported\"",
          "description": "A URI without a scheme (relative path) is supplied. The factory expects absolute URIs and should reject the input with an appropriate exception."
        }
      ]
    },
    {
      "group": "Jar scheme URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"jar:file:/path/to/app.jar!/schemas/simple-schema.json\")"
            }
          ],
          "expected": "JsonSchema instance is returned and its root node matches the JSON content of simple-schema.json inside the JAR",
          "description": "A JAR URL points to a schema packaged within a JAR file. The loader should be able to resolve the resource and return a valid JsonSchema."
        }
      ]
    },
    {
      "group": "Schema with $ref to external resource",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"classpath:/schemas/with-ref-schema.json\")"
            }
          ],
          "expected": "JsonSchema instance is returned; validation of a sample JSON document that follows the $ref resolves the external schema correctly",
          "description": "The primary schema contains a $ref to another schema located via a classpath URI. Verify that getSchema loads both schemas and that the resulting JsonSchema can resolve the reference during validation."
        }
      ]
    }
  ]
}