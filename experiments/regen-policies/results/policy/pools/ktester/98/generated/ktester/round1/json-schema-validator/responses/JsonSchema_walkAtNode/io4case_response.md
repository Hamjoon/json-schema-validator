{
  "tests": [
    {
      "group": "Successful validation – $ref to external schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext where getSchemaNode for URI \"http://example.com/address\" returns schema { \"type\": \"object\", \"properties\": { \"street\": { \"type\": \"string\" } }, \"required\": [\"street\"] }"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing { \"address\": { \"street\": \"Main St\" } }"
            },
            {
              "parameter": "rootNode",
              "value": "same JsonNode as above (root)"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == true and no ValidationMessage objects (the $ref is resolved and the instance satisfies the referenced schema)",
          "description": "The schema contains a $ref to an external schema (address). The ExecutionContext is mocked to provide that schema. walkAtNode should resolve the reference, apply the referenced validators, and return a successful result."
        }
      ]
    },
    {
      "group": "Validation failure – $ref to external schema with missing required property",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext where getSchemaNode for URI \"http://example.com/address\" returns schema { \"type\": \"object\", \"properties\": { \"street\": { \"type\": \"string\" } }, \"required\": [\"street\"] }"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing { \"address\": { \"city\": \"Metropolis\" } }"
            },
            {
              "parameter": "rootNode",
              "value": "same JsonNode as above (root)"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == false and ValidationMessage contains a message about missing required property \"street\" in the referenced schema",
          "description": "The $ref resolves to a schema that requires \"street\". The instance lacks this property, so walkAtNode must report the missing‑property error coming from the referenced schema."
        }
      ]
    },
    {
      "group": "Successful validation – enum constraint",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext with default settings (schema contains enum [\"red\",\"green\",\"blue\"])"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing \"green\""
            },
            {
              "parameter": "rootNode",
              "value": "same JsonNode as above (root)"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == true and no ValidationMessage objects",
          "description": "The schema restricts the value to an enum. The provided value matches one of the allowed enum entries, so validation succeeds."
        }
      ]
    },
    {
      "group": "Validation failure – enum constraint mismatch",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext with default settings (schema contains enum [\"red\",\"green\",\"blue\"])"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing \"yellow\""
            },
            {
              "parameter": "rootNode",
              "value": "same JsonNode as above (root)"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == false and ValidationMessage contains a message about value not being one of the allowed enum values",
          "description": "The instance value is not present in the enum list, so walkAtNode should report an enum violation."
        }
      ]
    },
    {
      "group": "Successful validation – oneOf with matching subschema",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext with default settings (schema contains oneOf: [{\"type\":\"string\"},{\"type\":\"integer\"}])"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing 42"
            },
            {
              "parameter": "rootNode",
              "value": "same JsonNode as above (root)"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == true and no ValidationMessage objects (the integer matches the second subschema)",
          "description": "The schema uses oneOf with a string and an integer option. The instance is an integer, so validation should succeed via the matching subschema."
        }
      ]
    },
    {
      "group": "Validation failure – oneOf with no matching subschema",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext with default settings (schema contains oneOf: [{\"type\":\"string\"},{\"type\":\"integer\"}])"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing true"
            },
            {
              "parameter": "rootNode",
              "value": "same JsonNode as above (root)"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == false and ValidationMessage contains a message about failing oneOf validation",
          "description": "The instance is a boolean, which does not satisfy any of the subschemas in the oneOf clause, so walkAtNode must report a oneOf failure."
        }
      ]
    },
    {
      "group": "Successful validation – array items validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext with default settings (schema: {\"type\":\"array\",\"items\":{\"type\":\"integer\"}})"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing [1, 2, 3]"
            },
            {
              "parameter": "rootNode",
              "value": "same JsonNode as above (root)"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == true and no ValidationMessage objects",
          "description": "The schema requires an array of integers. All elements are integers, so validation succeeds."
        }
      ]
    },
    {
      "group": "Validation failure – array items type mismatch",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext with default settings (schema: {\"type\":\"array\",\"items\":{\"type\":\"integer\"}})"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing [1, \"two\", 3]"
            },
            {
              "parameter": "rootNode",
              "value": "same JsonNode as above (root)"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == false and ValidationMessage contains a message about element at index 1 not being an integer",
          "description": "One array element violates the items schema (string instead of integer). walkAtNode should report the specific location and type error."
        }
      ]
    },
    {
      "group": "Instance location propagation – nested property validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext with default settings (schema: {\"type\":\"object\",\"properties\":{\"person\":{\"type\":\"object\",\"required\":[\"age\"],\"properties\":{\"age\":{\"type\":\"integer\"}}}}})"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing { \"person\": { \"age\": \"twenty\" } }"
            },
            {
              "parameter": "rootNode",
              "value": "same JsonNode as above (root)"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == false and ValidationMessage contains instance location \"/person/age\"",
          "description": "The validation error occurs deep inside the instance. walkAtNode must correctly build the JsonNodePath so the message points to \"/person/age\"."
        }
      ]
    },
    {
      "group": "Root node differs from node – sub‑node validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext with default settings (schema: {\"type\":\"string\",\"minLength\":5})"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing \"short\" (length 5) – this is the sub‑node to validate"
            },
            {
              "parameter": "rootNode",
              "value": "JsonNode representing { \"title\": \"short\", \"description\": \"A longer description\" }"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.of(\"/title\")"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == true and no ValidationMessage objects (validation succeeds on the sub‑node while rootNode is larger)",
          "description": "walkAtNode is invoked on a sub‑node (the value of \"/title\"). The method must use the provided rootNode for $ref resolution but validate only the supplied node, returning a successful result."
        }
      ]
    },
    {
      "group": "Configuration – failFast stops after first error",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext with SchemaValidatorsConfig.failFast = true (schema has two failing constraints: required \"name\" and type integer for \"age\")"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing { \"age\": \"not-an-int\" }"
            },
            {
              "parameter": "rootNode",
              "value": "same JsonNode as above (root)"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == false and exactly one ValidationMessage (the first detected error, e.g., missing required property \"name\")",
          "description": "When failFast is enabled, walkAtNode should abort after the first validation error, not collecting subsequent errors."
        }
      ]
    }
  ]
}