{
  "testCases": [
    {
      "group": "Null handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "null"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "When both parameters are null the method should throw NPE because the root JSON node is required."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "null"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {}"
            }
          ],
          "expected": "NullPointerException",
          "description": "When the JSON node is null but a customizer is supplied the method must still reject the call with NPE."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "A null customizer is allowed; the method should behave as if no customizer was provided and return an empty validation result for a valid empty object."
        }
      ]
    },
    {
      "group": "ExecutionCustomizer effects",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"a\":1}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> consumer.setAttribute(\"skip\", true)"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "When the customizer sets a flag that the validator checks (e.g., \"skip\"), the validator should bypass further validation and return an empty set."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"a\":1}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> { throw new IllegalStateException(\"customizer failure\"); }"
            }
          ],
          "expected": "IllegalStateException",
          "description": "If the customizer itself throws an exception, the validate method must propagate that exception."
        }
      ]
    },
    {
      "group": "Successful validation (no errors)",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"name\":\"John\",\"age\":30}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {}"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "A JSON document that satisfies the schema (e.g., required fields, correct types) should produce an empty validation message set."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ArrayNode representing []"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {}"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "An empty array that conforms to an \"array\" type schema should also return an empty set."
        }
      ]
    },
    {
      "group": "Validation failure (error messages)",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"age\":\"thirty\"}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {}"
            }
          ],
          "expected": "Set containing ValidationMessage with type 'type' and path '/age'",
          "description": "When a property has an invalid type (string instead of integer) the validator must return a ValidationMessage describing the type error."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {}"
            }
          ],
          "expected": "Set containing ValidationMessage with type 'required' and path ''",
          "description": "When required properties are missing the validator must return a required‑field ValidationMessage."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"extra\":true}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {}"
            }
          ],
          "expected": "Set containing ValidationMessage with type 'additionalProperties' and path ''",
          "description": "When additionalProperties is false and an unexpected property is present, a corresponding ValidationMessage must be produced."
        }
      ]
    },
    {
      "group": "Recursive anchor handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"self\":{}}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {}"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "If the schema contains a recursive anchor (isRecursiveAnchor() == true) and the instance resolves correctly, validation should succeed without infinite recursion."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"self\":{ \"self\": {\"self\":{} } } }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {}"
            }
          ],
          "expected": "Set containing ValidationMessage with type 'maxDepth' (or similar) if implementation limits recursion",
          "description": "When the recursive anchor leads to a depth that exceeds the validator’s recursion limit, an appropriate ValidationMessage must be returned."
        }
      ]
    },
    {
      "group": "Schema resource root flag",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"id\":\"http://example.com/schema\"}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {}"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "When the schema is marked as a resource root (isSchemaResourceRoot() == true) the validator should treat $id resolution accordingly and still validate successfully."
        }
      ]
    }
  ]
}