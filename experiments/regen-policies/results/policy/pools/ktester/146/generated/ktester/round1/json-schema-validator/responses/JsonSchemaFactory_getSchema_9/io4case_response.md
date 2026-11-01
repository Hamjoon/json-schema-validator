{
  "newTestCases": [
    {
      "group": "Valid inputs – schemaUri with fragment and null jsonNode",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/schema.json#/$defs/address\")"
            },
            {
              "parameter": "jsonNode",
              "value": "null"
            }
          ],
          "expected": "returns a non‑null JsonSchema instance whose root node corresponds to the fragment definition",
          "description": "When the supplied SchemaLocation contains a fragment that points to a definition inside the schema and jsonNode is null, the factory must load the schema, resolve the fragment and return a usable JsonSchema object."
        }
      ]
    },
    {
      "group": "Invalid schemaUri – unsupported scheme (mailto)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"mailto:user@example.com\")"
            },
            {
              "parameter": "jsonNode",
              "value": "JsonNodeFactory.instance.objectNode().put(\"type\", \"string\")"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "The factory only supports http, https, file and classpath schemes. Supplying a mailto URI should cause an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Non‑existent classpath resource – should raise IllegalArgumentException",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/missing-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "When jsonNode is null the factory tries to read the schema from the given classpath location. If the resource does not exist, an IllegalArgumentException (or a subclass) must be thrown."
        }
      ]
    },
    {
      "group": "jsonNode is an array node – should raise IllegalArgumentException",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/array-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "JsonNodeFactory.instance.arrayNode().add(\"string\")"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "A JSON Schema must be a JSON object. Supplying an array node as the schema definition is illegal and the factory should reject it with an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "schemaUri with encoded spaces – should succeed",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/my%20schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "JsonNodeFactory.instance.objectNode().put(\"type\", \"number\")"
            }
          ],
          "expected": "returns a non‑null JsonSchema instance",
          "description": "URIs containing percent‑encoded spaces are valid. The method must correctly handle them and produce a JsonSchema."
        }
      ]
    },
    {
      "group": "schemaUri with query parameters – should succeed",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/schema.json?version=1\")"
            },
            {
              "parameter": "jsonNode",
              "value": "JsonNodeFactory.instance.objectNode().put(\"type\", \"boolean\")"
            }
          ],
          "expected": "returns a non‑null JsonSchema instance",
          "description": "Query strings are allowed in a SchemaLocation. The factory should ignore the query part for loading the schema and still return a valid JsonSchema."
        }
      ]
    },
    {
      "group": "jsonNode with $ref to external schema – should resolve external reference",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/root-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "JsonNodeFactory.instance.objectNode().put(\"$ref\", \"classpath:/schemas/child-schema.json\")"
            }
          ],
          "expected": "returns a non‑null JsonSchema instance whose validation includes the referenced external schema",
          "description": "When the supplied JsonNode contains a $ref that points to another schema (classpath URI), the factory must load the referenced schema and incorporate it, resulting in a fully resolved JsonSchema."
        }
      ]
    },
    {
      "group": "schemaUri with trailing slash – should succeed",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/schemas/\")"
            },
            {
              "parameter": "jsonNode",
              "value": "JsonNodeFactory.instance.objectNode().put(\"type\", \"null\")"
            }
          ],
          "expected": "returns a non‑null JsonSchema instance",
          "description": "A URI ending with a slash is still a valid location. The method must not treat it as an error and should create a JsonSchema."
        }
      ]
    }
  ]
}