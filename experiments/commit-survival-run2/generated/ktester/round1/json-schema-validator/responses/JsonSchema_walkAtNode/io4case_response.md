{
  "groups": [
    {
      "group": "$ref resolution and URI handling",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mock(ExecutionContext.class, with -> when(with.getSchema()).thenReturn(schemaWithRef(\"#/definitions/positiveInteger\")))" },
            { "parameter": "node", "value": "JsonNodeFactory.instance.numberNode(-5)" },
            { "parameter": "rootNode", "value": "JsonNodeFactory.instance.numberNode(-5)" },
            { "parameter": "at", "value": "\"/value\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "ValidationResult.isValid()==false and contains a ValidationMessage about the value being less than the minimum defined in the referenced schema",
          "description": "The schema contains a $ref to a definition that enforces a minimum of 0. The referenced schema should be resolved and applied, causing a validation error for a negative number."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mock(ExecutionContext.class, with -> when(with.getSchema()).thenReturn(schemaWithRef(\"http://example.com/remote-schema.json\")))" },
            { "parameter": "node", "value": "JsonNodeFactory.instance.textNode(\"abc\")" },
            { "parameter": "rootNode", "value": "JsonNodeFactory.instance.textNode(\"abc\")" },
            { "parameter": "at", "value": "\"/field\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "ValidationResult.isValid()==false and contains a ValidationMessage wrapping URISyntaxException (or IOException) because the remote $ref cannot be fetched",
          "description": "A $ref points to an external URI that cannot be resolved. The walk should catch the resulting exception and return an invalid ValidationResult rather than propagating the exception."
        }
      ]
    },
    {
      "group": "Array constraint handling",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mock(ExecutionContext.class, with -> when(with.getSchema()).thenReturn(schemaWithArrayConstraints(2, 3, true)))" },
            { "parameter": "node", "value": "JsonNodeFactory.instance.arrayNode().add(1)" },
            { "parameter": "rootNode", "value": "JsonNodeFactory.instance.arrayNode().add(1)" },
            { "parameter": "at", "value": "\"/items\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "ValidationResult.isValid()==false and contains messages about minItems (2) and uniqueItems violation",
          "description": "The schema requires an array with at least 2 items and all items must be unique. The instance contains a single element, triggering both minItems and uniqueItems errors."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mock(ExecutionContext.class, with -> when(with.getSchema()).thenReturn(schemaWithArrayConstraints(null, 2, false)))" },
            { "parameter": "node", "value": "JsonNodeFactory.instance.arrayNode().add(1).add(2).add(3)" },
            { "parameter": "rootNode", "value": "JsonNodeFactory.instance.arrayNode().add(1).add(2).add(3)" },
            { "parameter": "at", "value": "\"/list\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "ValidationResult.isValid()==false and contains a message about maxItems (2) exceeded",
          "description": "The schema limits the array to a maximum of 2 items. The instance provides three items, so a maxItems validation error is expected."
        }
      ]
    },
    {
      "group": "Enum and const keyword handling",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mock(ExecutionContext.class, with -> when(with.getSchema()).thenReturn(schemaWithEnum(new String[]{\"red\", \"green\", \"blue\"})))" },
            { "parameter": "node", "value": "JsonNodeFactory.instance.textNode(\"yellow\")" },
            { "parameter": "rootNode", "value": "JsonNodeFactory.instance.textNode(\"yellow\")" },
            { "parameter": "at", "value": "\"/color\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "ValidationResult.isValid()==false and contains a ValidationMessage indicating the value is not one of the allowed enum values",
          "description": "The schema defines an enum of allowed colors. The instance supplies a color not in the enum, so validation must fail."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mock(ExecutionContext.class, with -> when(with.getSchema()).thenReturn(schemaWithConst(JsonNodeFactory.instance.numberNode(42)))" },
            { "parameter": "node", "value": "JsonNodeFactory.instance.numberNode(42)" },
            { "parameter": "rootNode", "value": "JsonNodeFactory.instance.numberNode(42)" },
            { "parameter": "at", "value": "\"/answer\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "ValidationResult.isValid()==true (const matches)",
          "description": "The schema uses the const keyword to require the exact value 42. The instance matches, so validation succeeds."
        }
      ]
    },
    {
      "group": "Numeric constraints (minimum, maximum, multipleOf)",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mock(ExecutionContext.class, with -> when(with.getSchema()).thenReturn(schemaWithNumericConstraints(0, 100, 5)))" },
            { "parameter": "node", "value": "JsonNodeFactory.instance.numberNode(103)" },
            { "parameter": "rootNode", "value": "JsonNodeFactory.instance.numberNode(103)" },
            { "parameter": "at", "value": "\"/score\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "ValidationResult.isValid()==false and contains messages for exceeding maximum (100) and not being a multiple of 5",
          "description": "The schema defines minimum=0, maximum=100, multipleOf=5. The instance value 103 violates both maximum and multipleOf constraints."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mock(ExecutionContext.class, with -> when(with.getSchema()).thenReturn(schemaWithNumericConstraints(-10, null, 0.1)))" },
            { "parameter": "node", "value": "JsonNodeFactory.instance.numberNode(-5.05)" },
            { "parameter": "rootNode", "value": "JsonNodeFactory.instance.numberNode(-5.05)" },
            { "parameter": "at", "value": "\"/delta\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "ValidationResult.isValid()==false and contains a message about the value not being a multiple of 0.1",
          "description": "Only the multipleOf constraint is defined (0.1). The value -5.05 is not a multiple of 0.1 due to floating‑point rounding, causing a validation error."
        }
      ]
    },
    {
      "group": "String format and pattern handling",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mock(ExecutionContext.class, with -> when(with.getSchema()).thenReturn(schemaWithFormat(\"email\")))" },
            { "parameter": "node", "value": "JsonNodeFactory.instance.textNode(\"not-an-email\")" },
            { "parameter": "rootNode", "value": "JsonNodeFactory.instance.textNode(\"not-an-email\")" },
            { "parameter": "at", "value": "\"/contact\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "ValidationResult.isValid()==false and contains a format validation message for email",
          "description": "The schema requires a string with email format. The provided value does not match the email regex, so validation fails."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mock(ExecutionContext.class, with -> when(with.getSchema()).thenReturn(schemaWithPattern(\"^[A-Z]{3}[0-9]{4}$\")))" },
            { "parameter": "node", "value": "JsonNodeFactory.instance.textNode(\"abc1234\")" },
            { "parameter": "rootNode", "value": "JsonNodeFactory.instance.textNode(\"abc1234\")" },
            { "parameter": "at", "value": "\"/code\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "ValidationResult.isValid()==false and contains a pattern‑mismatch ValidationMessage",
          "description": "The schema defines a regular expression pattern that the instance string must satisfy. The lower‑case string does not match the upper‑case pattern, leading to an error."
        }
      ]
    },
    {
      "group": "Composite keyword handling (allOf, anyOf, oneOf, not)",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mock(ExecutionContext.class, with -> when(with.getSchema()).thenReturn(schemaWithAllOf(new String[]{\"type:string\", \"minLength:5\"}))" },
            { "parameter": "node", "value": "JsonNodeFactory.instance.textNode(\"abc\")" },
            { "parameter": "rootNode", "value": "JsonNodeFactory.instance.textNode(\"abc\")" },
            { "parameter": "at", "value": "\"/title\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "ValidationResult.isValid()==false and contains messages for both type mismatch (if applicable) and minLength violation",
          "description": "The schema uses allOf to combine a string type and a minLength of 5. The instance is a short string, causing both constituent validators to fail."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mock(ExecutionContext.class, with -> when(with.getSchema()).thenReturn(schemaWithAnyOf(new String[]{\"enum:[\\\"A\\\",\\\"B\\\"]\", \"enum:[\\\"C\\\",\\\"D\\\"]\"}))" },
            { "parameter": "node", "value": "JsonNodeFactory.instance.textNode(\"E\")" },
            { "parameter": "rootNode", "value": "JsonNodeFactory.instance.textNode(\"E\")" },
            { "parameter": "at", "value": "\"/choice\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "ValidationResult.isValid()==false and contains a message that none of the anyOf subschemas matched",
          "description": "The anyOf combines two enum subschemas. The instance value is not present in either enum, so validation fails."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mock(ExecutionContext.class, with -> when(with.getSchema()).thenReturn(schemaWithOneOf(new String[]{\"type:string\", \"type:integer\"}))" },
            { "parameter": "node", "value": "JsonNodeFactory.instance.numberNode(10)" },
            { "parameter": "rootNode", "value": "JsonNodeFactory.instance.numberNode(10)" },
            { "parameter": "at", "value": "\"/id\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "ValidationResult.isValid()==true (exactly one subschema matches)",
          "description": "OneOf combines a string type and an integer type. The instance is an integer, satisfying exactly one subschema, so validation succeeds."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mock(ExecutionContext.class, with -> when(with.getSchema()).thenReturn(schemaWithNot(schemaWithType(\"null\"))))" },
            { "parameter": "node", "value": "JsonNodeFactory.instance.nullNode()" },
            { "parameter": "rootNode", "value": "JsonNodeFactory.instance.nullNode()" },
            { "parameter": "at", "value": "\"/maybeNull\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "ValidationResult.isValid()==false and contains a not‑validation message because the instance matches the negated schema",
          "description": "The schema uses not to forbid null values. The instance is null, so the not‑validator should trigger an error."
        }
      ]
    },
    {
      "group": "ExecutionContext null handling",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "null" },
            { "parameter": "node", "value": "JsonNodeFactory.instance.objectNode()" },
            { "parameter": "rootNode", "value": "JsonNodeFactory.instance.objectNode()" },
            { "parameter": "at", "value": "\"/\""},
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "ValidationResult.isValid()==false and contains a ValidationMessage indicating that the ExecutionContext is missing",
          "description": "When the ExecutionContext is null the method should gracefully handle the situation and report an appropriate validation error instead of throwing a NullPointerException."
        }
      ]
    },
    {
      "group": "RootNode null handling with non‑null instance",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mock(ExecutionContext.class)" },
            { "parameter": "node", "value": "JsonNodeFactory.instance.textNode(\"test\")" },
            { "parameter": "rootNode", "value": "null" },
            { "parameter": "at", "value": "\"/field\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "ValidationResult.isValid()==false and contains a ValidationMessage stating that the root node is null",
          "description": "The walk method expects a non‑null root node for relative $ref resolution. Supplying null should result in a clear validation error."
        }
      ]
    },
    {
      "group": "Path string edge cases",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mock(ExecutionContext.class, with -> when(with.getSchema()).thenReturn(schemaWithRequired(\"id\")))" },
            { "parameter": "node", "value": "JsonNodeFactory.instance.objectNode()" },
            { "parameter": "rootNode", "value": "JsonNodeFactory.instance.objectNode()" },
            { "parameter": "at", "value": "\"\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "ValidationResult.isValid()==false and contains a required‑property message for \"id\"",
          "description": "An empty string for the 'at' parameter represents the document root. The required validator should still be applied correctly."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mock(ExecutionContext.class, with -> when(with.getSchema()).thenReturn(schemaWithRequired(\"id\")))" },
            { "parameter": "node", "value": "JsonNodeFactory.instance.objectNode().put(\"id\", 1)" },
            { "parameter": "rootNode", "value": "JsonNodeFactory.instance.objectNode().put(\"id\", 1)" },
            { "parameter": "at", "value": "\"/\""},
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "ValidationResult.isValid()==true (required property present at root)",
          "description": "Using '/' as the path should also be interpreted as the document root and validation should succeed when required properties are present."
        }
      ]
    },
    {
      "group": "KeywordWalkListenerRunner null fallback",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mock(ExecutionContext.class, with -> when(with.getKeywordWalkListenerRunner()).thenReturn(null))" },
            { "parameter": "node", "value": "JsonNodeFactory.instance.objectNode()" },
            { "parameter": "rootNode", "value": "JsonNodeFactory.instance.objectNode()" },
            { "parameter": "at", "value": "\"/\""},
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "ValidationResult.isValid()==true and no NullPointerException is thrown",
          "description": "If the ExecutionContext does not provide a KeywordWalkListenerRunner, walkAtNode should continue validation without invoking listener callbacks."
        }
      ]
    },
    {
      "group": "Boolean schema handling",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mock(ExecutionContext.class, with -> when(with.getSchema()).thenReturn(booleanSchema(true)))" },
            { "parameter": "node", "value": "JsonNodeFactory.instance.objectNode()" },
            { "parameter": "rootNode", "value": "JsonNodeFactory.instance.objectNode()" },
            { "parameter": "at", "value": "\"/any\""},
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "ValidationResult.isValid()==true (boolean schema true always passes)",
          "description": "A schema that is the literal boolean true should accept any instance without errors."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mock(ExecutionContext.class, with -> when(with.getSchema()).thenReturn(booleanSchema(false)))" },
            { "parameter": "node", "value": "JsonNodeFactory.instance.objectNode()" },
            { "parameter": "rootNode", "value": "JsonNodeFactory.instance.objectNode()" },
            { "parameter": "at", "value": "\"/any\""},
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "ValidationResult.isValid()==false and contains a ValidationMessage indicating that the schema is false (no instance can satisfy it)",
          "description": "A schema that is the literal boolean false should reject any instance, producing a validation error."
        }
      ]
    }
  ]
}