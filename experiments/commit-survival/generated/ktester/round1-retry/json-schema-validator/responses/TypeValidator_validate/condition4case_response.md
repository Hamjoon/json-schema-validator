{
  "testCases": [
    {
      "group": "Union type handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (collectorContext with empty evaluatedItems/evaluatedProperties)"
            },
            {
              "parameter": "node",
              "value": "any JsonNode (e.g., JsonNodeFactory.instance.textNode(\"any\"))"
            },
            {
              "parameter": "rootNode",
              "value": "any JsonNode (e.g., JsonNodeFactory.instance.objectNode())"
            },
            {
              "parameter": "at",
              "value": "\"/some/path\""
            }
          ],
          "expected": "Set returned by UnionTypeValidator.validate – verify that UnionTypeValidator.validate is invoked exactly once with the same parameters and its result is returned unchanged.",
          "description": "When the schema type is UNION the validator should delegate to UnionTypeValidator and return whatever it produces, without performing any further checks."
        }
      ]
    },
    {
      "group": "Non‑union – node type does NOT match schema type",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (collectorContext not used in this branch)"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing a number (e.g., JsonNodeFactory.instance.numberNode(42))"
            },
            {
              "parameter": "rootNode",
              "value": "JsonNode representing the whole document (any, e.g., objectNode)"
            },
            {
              "parameter": "at",
              "value": "\"/age\""
            }
          ],
          "expected": "A singleton Set containing a ValidationMessage whose message includes: at=\"/age\", nodeType=\"INTEGER\" (or whatever TypeFactory returns for a number), schemaType equals the configured schemaType (e.g., \"STRING\").",
          "description": "When the node’s actual type differs from the schema’s declared type, validate() must return a single ValidationMessage describing the mismatch."
        }
      ]
    },
    {
      "group": "Non‑union – node type matches, schemaPath ends with \"/type\", rootNode is an ARRAY",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext with CollectorContext that exposes getEvaluatedItems() set"
            },
            {
              "parameter": "node",
              "value": "JsonNode that equals the schema type (e.g., a string node when schemaType=STRING)"
            },
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.arrayNode()"
            },
            {
              "parameter": "at",
              "value": "\"/0\""
            }
          ],
          "expected": "Empty Set (no validation errors) and the CollectorContext’s evaluatedItems set contains the string \"/0\".",
          "description": "When the schemaPath ends with \"/type\" and the root document is an array, the validator must record the location in evaluatedItems while returning no errors."
        }
      ]
    },
    {
      "group": "Non‑union – node type matches, schemaPath ends with \"/type\", rootNode is an OBJECT",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext with CollectorContext that exposes getEvaluatedProperties() set"
            },
            {
              "parameter": "node",
              "value": "JsonNode that equals the schema type (e.g., a string node when schemaType=STRING)"
            },
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "at",
              "value": "\"/name\""
            }
          ],
          "expected": "Empty Set and the CollectorContext’s evaluatedProperties set contains the string \"/name\".",
          "description": "When the schemaPath ends with \"/type\" and the root document is an object, the validator must record the location in evaluatedProperties while returning no errors."
        }
      ]
    },
    {
      "group": "Non‑union – node type matches, schemaPath does NOT end with \"/type\"",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (collectorContext not used)"
            },
            {
              "parameter": "node",
              "value": "JsonNode that equals the schema type (e.g., a boolean node when schemaType=BOOLEAN)"
            },
            {
              "parameter": "rootNode",
              "value": "any JsonNode (object or array – does not matter because schemaPath check fails)"
            },
            {
              "parameter": "at",
              "value": "\"/flag\""
            }
          ],
          "expected": "Empty Set with no modifications to evaluatedItems/evaluatedProperties.",
          "description": "When the node matches the schema type but the schemaPath does not end with \"/type\", the validator should simply return an empty error set and perform no side‑effects."
        }
      ]
    }
  ]
}