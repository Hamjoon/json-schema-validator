{
  "testCases": [
    {
      "group": "Custom OutputFormat – exception propagation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "objectNode().put(\"name\", \"Bob\")"
            },
            {
              "parameter": "format",
              "value": "new OutputFormat<String>() { public String apply(Set<ValidationMessage> messages) { throw new IllegalStateException(\"format failure\"); } }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "IllegalStateException with message \"format failure\"",
          "description": "When the supplied OutputFormat throws an exception while formatting the validation messages, validate should propagate that exception unchanged."
        }
      ]
    },
    {
      "group": "Custom OutputFormat – collection result",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "objectNode().put(\"age\", \"twenty\")"
            },
            {
              "parameter": "format",
              "value": "new OutputFormat<List<ValidationMessage>>() { public List<ValidationMessage> apply(Set<ValidationMessage> messages) { return new ArrayList<>(messages); } }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "List size > 0 (contains at least one ValidationMessage)",
          "description": "A custom OutputFormat that returns a List<ValidationMessage> should receive the set of validation errors; for an invalid document the list must contain one or more messages."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "objectNode().put(\"name\", \"Alice\")"
            },
            {
              "parameter": "format",
              "value": "new OutputFormat<List<ValidationMessage>>() { public List<ValidationMessage> apply(Set<ValidationMessage> messages) { return new ArrayList<>(messages); } }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "Empty List (size == 0)",
          "description": "When the JSON document satisfies the schema, the custom OutputFormat should receive an empty set and therefore return an empty list."
        }
      ]
    },
    {
      "group": "Invalid rootNode type handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.arrayNode().add(1).add(2)"
            },
            {
              "parameter": "format",
              "value": "new SimpleStringOutputFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "String containing a ValidationMessage indicating type mismatch (expected object, got array)",
          "description": "The schema expects an object; providing a JSON array should cause a type‑validation error that is reflected in the formatted output."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.booleanNode(true)"
            },
            {
              "parameter": "format",
              "value": "new SimpleStringOutputFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "String containing a ValidationMessage indicating type mismatch (expected object, got boolean)",
          "description": "Supplying a primitive boolean where an object is required must produce a validation error."
        }
      ]
    },
    {
      "group": "JSON null literal handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.nullNode()"
            },
            {
              "parameter": "format",
              "value": "new SimpleStringOutputFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "String containing a ValidationMessage about type mismatch (null not allowed)",
          "description": "A JSON literal null should be validated against the schema; because the schema does not allow null, a validation error is expected."
        }
      ]
    },
    {
      "group": "ExecutionCustomizer – attribute set but not used",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "objectNode().put(\"name\", \"Charlie\")"
            },
            {
              "parameter": "format",
              "value": "new SimpleStringOutputFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setAttribute(\"unusedFlag\", \"value\")"
            }
          ],
          "expected": "String \"[]\" (empty validation set)",
          "description": "Setting an attribute that the validator does not consult must not affect the validation outcome; a valid document should still produce an empty result."
        }
      ]
    }
  ]
}