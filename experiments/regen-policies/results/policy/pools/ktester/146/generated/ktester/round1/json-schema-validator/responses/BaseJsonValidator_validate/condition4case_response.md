{
  "testCases": [
    {
      "group": "Normal validation – happy path",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(collectAnnotations=true, suppressSubSchemaRetrieval=false)"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode(valid=true, type=ObjectNode)"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "When a valid JSON node is supplied and the validator is configured to collect annotations, the `validate` method should return an empty set indicating no validation errors."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(collectAnnotations=true, suppressSubSchemaRetrieval=false)"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode(valid=false, type=ObjectNode, errors=[\"field missing\"])"
            }
          ],
          "expected": "Set containing a ValidationMessage with message \"field missing\"",
          "description": "When the JSON node violates the schema (e.g., a required field is missing), the validator should return a set with the corresponding ValidationMessage."
        }
      ]
    },
    {
      "group": "Null or missing parameters",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode(valid=true)"
            }
          ],
          "expected": "IllegalArgumentException with message \"ExecutionContext must not be null\"",
          "description": "The method should guard against a null ExecutionContext and throw an IllegalArgumentException."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException with message \"JsonNode must not be null\"",
          "description": "The method should guard against a null JsonNode and throw an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Root vs. non‑root evaluation path",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode()"
            }
          ],
          "expected": "validation performed with atRoot() == true",
          "description": "When `atRoot()` returns true the validator should treat the supplied node as the root of the evaluation path."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode()"
            }
          ],
          "expected": "validation performed with atRoot() == false",
          "description": "When `atRoot()` returns false the validator should treat the supplied node as a child node; the internal call to the overloaded `validate` receives `node` as both the instance node and the parent node."
        }
      ]
    },
    {
      "group": "Suppress sub‑schema retrieval flag",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(suppressSubSchemaRetrieval=true)"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode()"
            }
          ],
          "expected": "Set without invoking fetchSubSchemaNode (no sub‑schema validation)",
          "description": "When the validator is constructed with `suppressSubSchemaRetrieval` set to true, the `validate` method must skip any sub‑schema fetching and only apply its own keyword validation."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(suppressSubSchemaRetrieval=false)"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode()"
            }
          ],
          "expected": "Set may contain messages produced by fetchSubSchemaNode",
          "description": "When `suppressSubSchemaRetrieval` is false the validator is allowed to retrieve and evaluate sub‑schemas; the test verifies that the sub‑schema path is exercised."
        }
      ]
    },
    {
      "group": "Annotation collection branches",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(collectAnnotations=true, keyword=\"type\")"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode()"
            }
          ],
          "expected": "annotation for keyword \"type\" is present in ExecutionContext after validation",
          "description": "When `collectAnnotations` returns true for a specific keyword, the validator must store a JsonNodeAnnotation for that keyword."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(collectAnnotations=false, keyword=\"type\")"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode()"
            }
          ],
          "expected": "no annotation added for keyword \"type\"",
          "description": "When `collectAnnotations` returns false, the validator must skip annotation creation."
        }
      ]
    },
    {
      "group": "Discriminator handling (static helper branches)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode(withDiscriminator=true, discriminatorProperty=\"type\", discriminatorValue=\"A\")"
            }
          ],
          "expected": "registerAndMergeDiscriminator invoked, no ValidationMessage for discriminator mismatch",
          "description": "When a node contains a discriminator object that matches the expected value, the static helper `registerAndMergeDiscriminator` should be called and validation should pass."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode(withDiscriminator=true, discriminatorProperty=\"type\", discriminatorValue=\"B\")"
            }
          ],
          "expected": "Set containing ValidationMessage indicating discriminator mismatch",
          "description": "When the discriminator value does not match any defined mapping, `checkDiscriminatorMatch` should add a ValidationMessage describing the mismatch."
        }
      ]
    }
  ]
}