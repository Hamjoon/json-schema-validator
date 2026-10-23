{
  "groups": [
    {
      "group": "Invalid path string handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock(ExecutionContext.class, with -> when(with.getSchema()).thenReturn(simpleSchema()))"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "at",
              "value": "\"invalidPathWithoutSlash\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.isValid()==false and contains a ValidationMessage indicating an illegal JSON‑pointer path (IllegalArgumentException)",
          "description": "The 'at' argument does not start with '/' and therefore cannot be parsed as a JSON Pointer; walkAtNode should catch the IllegalArgumentException and return a failed ValidationResult."
        }
      ]
    },
    {
      "group": "Malformed $ref URI handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock(ExecutionContext.class, with -> when(with.getSchema()).thenReturn(schemaWithRef(\"http://[invalid-uri]\")))"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.textNode(\"test\")"
            },
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.textNode(\"test\")"
            },
            {
              "parameter": "at",
              "value": "\"/field\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.isValid()==false and contains a ValidationMessage wrapping URISyntaxException",
          "description": "A $ref contains a syntactically invalid URI; the resolver throws URISyntaxException which must be caught and reported as a validation error."
        }
      ]
    },
    {
      "group": "KeywordWalkListenerRunner exception handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock(ExecutionContext.class, with -> {\n    when(with.getSchema()).thenReturn(simpleSchema());\n    when(with.getKeywordWalkListenerRunner()).thenReturn(new WalkListenerRunner() {\n        @Override public void beforeWalk(JsonSchema schema, JsonNode node, JsonNode rootNode, String at) {\n            throw new RuntimeException(\"listener failure\");\n        }\n        @Override public void afterWalk(JsonSchema schema, JsonNode node, JsonNode rootNode, String at, ValidationResult result) {}\n    });\n})"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "at",
              "value": "\"/\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.isValid()==false and contains a ValidationMessage wrapping the RuntimeException thrown by the listener",
          "description": "If a KeywordWalkListenerRunner throws an unchecked exception during beforeWalk, walkAtNode must catch it and convert it into a ValidationResult rather than letting the exception propagate."
        }
      ]
    },
    {
      "group": "ExecutionContext returns null schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock(ExecutionContext.class, with -> when(with.getSchema()).thenReturn(null))"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "at",
              "value": "\"/\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.isValid()==false and contains a ValidationMessage indicating that the schema is missing (NullPointerException is handled internally)",
          "description": "When ExecutionContext is present but its getSchema() returns null, walkAtNode should not throw NPE; it must report a clear validation error."
        }
      ]
    },
    {
      "group": "Unknown keyword handling (JsonSchemaException)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock(ExecutionContext.class, with -> when(with.getSchema()).thenReturn(schemaWithUnknownKeyword()))"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "at",
              "value": "\"/\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.isValid()==false and contains a ValidationMessage wrapping JsonSchemaException (or IllegalArgumentException) caused by the unknown keyword",
          "description": "If the schema contains a keyword that the validator does not recognise, JsonSchema throws a JsonSchemaException; walkAtNode must catch it and return an invalid ValidationResult."
        }
      ]
    }
  ]
}