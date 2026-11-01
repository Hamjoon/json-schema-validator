{
  "test_cases": [
    {
      "group": "Invalid $ref URI handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaLocation",
              "value": "new SchemaLocation(\"classpath:/schemas/invalidRef.json\")"
            },
            {
              "parameter": "evaluationPath",
              "value": "new JsonNodePath(\"$\")"
            },
            {
              "parameter": "schemaNode",
              "value": "objectNode({\"$ref\":\"ht!tp://bad-uri\"})"
            },
            {
              "parameter": "parentSchema",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "A $ref value that is not a valid URI should cause the factory to reject the schema with IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Invalid $id URI handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaLocation",
              "value": "new SchemaLocation(\"classpath:/schemas/invalidId.json\")"
            },
            {
              "parameter": "evaluationPath",
              "value": "new JsonNodePath(\"$\")"
            },
            {
              "parameter": "schemaNode",
              "value": "objectNode({\"$id\":\"ht!tp://bad-id\"})"
            },
            {
              "parameter": "parentSchema",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "A $id keyword containing a malformed URI must trigger IllegalArgumentException during schema creation."
        }
      ]
    },
    {
      "group": "Invalid type keyword value",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaLocation",
              "value": "new SchemaLocation(\"classpath:/schemas/invalidType.json\")"
            },
            {
              "parameter": "evaluationPath",
              "value": "new JsonNodePath(\"$\")"
            },
            {
              "parameter": "schemaNode",
              "value": "objectNode({\"type\":123})"
            },
            {
              "parameter": "parentSchema",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "The \"type\" keyword must be a string or an array; providing a numeric value should cause IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Top‑level non‑object/non‑boolean schema node (numeric)",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaLocation",
              "value": "new SchemaLocation(\"classpath:/schemas/numericNode.json\")"
            },
            {
              "parameter": "evaluationPath",
              "value": "new JsonNodePath(\"$\")"
            },
            {
              "parameter": "schemaNode",
              "value": "numberNode(42)"
            },
            {
              "parameter": "parentSchema",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "A top‑level numeric JsonNode is not a valid JSON Schema; the factory should reject it with IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Top‑level non‑object/non‑boolean schema node (string)",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaLocation",
              "value": "new SchemaLocation(\"classpath:/schemas/stringNode.json\")"
            },
            {
              "parameter": "evaluationPath",
              "value": "new JsonNodePath(\"$\")"
            },
            {
              "parameter": "schemaNode",
              "value": "textNode(\"just a string\")"
            },
            {
              "parameter": "parentSchema",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "A top‑level text JsonNode is not a valid JSON Schema; the method should throw IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Unknown $schema meta‑schema identifier",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaLocation",
              "value": "new SchemaLocation(\"classpath:/schemas/unknownMetaSchema.json\")"
            },
            {
              "parameter": "evaluationPath",
              "value": "new JsonNodePath(\"$\")"
            },
            {
              "parameter": "schemaNode",
              "value": "objectNode({\"$schema\":\"http://example.com/unknown-meta-schema\"})"
            },
            {
              "parameter": "parentSchema",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "If the $schema keyword references a meta‑schema that the factory does not recognise, it should raise IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Unsupported scheme in SchemaLocation",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaLocation",
              "value": "new SchemaLocation(\"ftp://example.com/schema.json\")"
            },
            {
              "parameter": "evaluationPath",
              "value": "new JsonNodePath(\"$\")"
            },
            {
              "parameter": "schemaNode",
              "value": "objectNode({\"type\":\"string\"})"
            },
            {
              "parameter": "parentSchema",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "SchemaLocation using an unsupported URI scheme (e.g., ftp) should be rejected with IllegalArgumentException."
        }
      ]
    }
  ]
}