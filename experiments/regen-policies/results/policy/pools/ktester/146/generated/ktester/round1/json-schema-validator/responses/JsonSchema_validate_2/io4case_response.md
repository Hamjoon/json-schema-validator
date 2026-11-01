{
  "tests": [
    {
      "group": "validate(JsonNode) - boolean schema handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"any\": \"value\" }"
            },
            {
              "parameter": "schema",
              "value": "false"
            }
          ],
          "expected": "non‑empty Set<ValidationMessage> containing a message that the instance does not match a false schema",
          "description": "When the schema is the boolean literal `false`, every instance must produce a validation error regardless of its content."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "null"
            },
            {
              "parameter": "schema",
              "value": "true"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "When the schema is the boolean literal `true`, any instance (including JSON null) should be considered valid and return an empty set."
        }
      ]
    },
    {
      "group": "validate(JsonNode) - enum and const constraints",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "\"RED\""
            },
            {
              "parameter": "schema",
              "value": "{ \"enum\": [\"RED\", \"GREEN\", \"BLUE\"] }"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "A value that matches one of the enumerated options should pass validation."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "\"YELLOW\""
            },
            {
              "parameter": "schema",
              "value": "{ \"enum\": [\"RED\", \"GREEN\", \"BLUE\"] }"
            }
          ],
          "expected": "non‑empty Set<ValidationMessage> containing an enum‑mismatch message",
          "description": "A value not present in the enum list must generate a validation error."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "42"
            },
            {
              "parameter": "schema",
              "value": "{ \"const\": 42 }"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "The instance exactly equals the const value, so validation succeeds."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "43"
            },
            {
              "parameter": "schema",
              "value": "{ \"const\": 42 }"
            }
          ],
          "expected": "non‑empty Set<ValidationMessage> containing a const‑mismatch message",
          "description": "A value differing from the const definition must be reported as invalid."
        }
      ]
    },
    {
      "group": "validate(JsonNode) - numeric limits and multipleOf",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "10"
            },
            {
              "parameter": "schema",
              "value": "{ \"type\": \"integer\", \"minimum\": 5, \"maximum\": 15 }"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "An integer within the inclusive minimum and maximum bounds validates successfully."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "4"
            },
            {
              "parameter": "schema",
              "value": "{ \"type\": \"integer\", \"minimum\": 5 }"
            }
          ],
          "expected": "non‑empty Set<ValidationMessage> containing a minimum‑violation message",
          "description": "A value lower than the defined minimum must produce a validation error."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "20.5"
            },
            {
              "parameter": "schema",
              "value": "{ \"type\": \"number\", \"maximum\": 20 }"
            }
          ],
          "expected": "non‑empty Set<ValidationMessage> containing a maximum‑violation message",
          "description": "A number greater than the maximum limit should be flagged."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "9"
            },
            {
              "parameter": "schema",
              "value": "{ \"type\": \"integer\", \"multipleOf\": 3 }"
            }
          ],
          "expected": "non‑empty Set<ValidationMessage> containing a multipleOf‑violation message",
          "description": "An integer not divisible by the specified multipleOf must generate a validation message."
        }
      ]
    },
    {
      "group": "validate(JsonNode) - array constraints",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "[1,2,3]"
            },
            {
              "parameter": "schema",
              "value": "{ \"type\": \"array\", \"minItems\": 2, \"maxItems\": 5 }"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "An array whose length satisfies both minItems and maxItems passes validation."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "[1]"
            },
            {
              "parameter": "schema",
              "value": "{ \"type\": \"array\", \"minItems\": 2 }"
            }
          ],
          "expected": "non‑empty Set<ValidationMessage> containing a minItems‑violation message",
          "description": "An array shorter than minItems must be reported as invalid."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "[1,2,3,4,5,6]"
            },
            {
              "parameter": "schema",
              "value": "{ \"type\": \"array\", \"maxItems\": 5 }"
            }
          ],
          "expected": "non‑empty Set<ValidationMessage> containing a maxItems‑violation message",
          "description": "An array longer than maxItems should trigger a validation error."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "[\"a\",\"b\",\"a\"]"
            },
            {
              "parameter": "schema",
              "value": "{ \"type\": \"array\", \"uniqueItems\": true }"
            }
          ],
          "expected": "non‑empty Set<ValidationMessage> containing a uniqueItems‑violation message",
          "description": "Duplicate items in an array when uniqueItems is true must be flagged."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "[1, \"two\", 3]"
            },
            {
              "parameter": "schema",
              "value": "{ \"type\": \"array\", \"items\": { \"type\": \"integer\" } }"
            }
          ],
          "expected": "non‑empty Set<ValidationMessage> containing an item‑type‑mismatch message for index 1",
          "description": "When a single schema is provided for items, each element must conform; a non‑integer element should generate a message indicating the offending index."
        }
      ]
    },
    {
      "group": "validate(JsonNode) - combinators (oneOf, anyOf, allOf)",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "\"cat\""
            },
            {
              "parameter": "schema",
              "value": "{ \"oneOf\": [ { \"type\": \"string\", \"enum\": [\"dog\", \"cat\"] }, { \"type\": \"integer\" } ] }"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "The instance matches exactly one subschema (the string enum), satisfying oneOf."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "42"
            },
            {
              "parameter": "schema",
              "value": "{ \"oneOf\": [ { \"type\": \"string\" }, { \"type\": \"integer\" } ] }"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "The integer matches exactly one of the subschemas, so oneOf validation succeeds."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "\"dog\""
            },
            {
              "parameter": "schema",
              "value": "{ \"oneOf\": [ { \"type\": \"string\", \"enum\": [\"dog\", \"cat\"] }, { \"type\": \"string\", \"pattern\": \"^d.*\" } ] }"
            }
          ],
          "expected": "non‑empty Set<ValidationMessage> containing a oneOf‑violation message",
          "description": "The instance matches both subschemas, violating the exactly‑one requirement of oneOf."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "\"blue\""
            },
            {
              "parameter": "schema",
              "value": "{ \"anyOf\": [ { \"type\": \"string\", \"enum\": [\"red\", \"green\"] }, { \"type\": \"string\", \"pattern\": \"^b.*\" } ] }"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "The instance satisfies at least one subschema (the pattern), so anyOf validation passes."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "5"
            },
            {
              "parameter": "schema",
              "value": "{ \"allOf\": [ { \"type\": \"integer\", \"minimum\": 0 }, { \"type\": \"integer\", \"maximum\": 10 } ] }"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "The instance meets all subschemas in an allOf definition, thus validation succeeds."
        }
      ]
    },
    {
      "group": "validate(JsonNode) - conditional if/then/else",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"A\", \"value\": 5 }"
            },
            {
              "parameter": "schema",
              "value": "{ \"if\": { \"properties\": { \"type\": { \"const\": \"A\" } } }, \"then\": { \"properties\": { \"value\": { \"type\": \"integer\", \"minimum\": 0 } }, \"required\": [\"value\"] }, \"else\": { \"properties\": { \"value\": { \"type\": \"string\" } } } }"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "When the `if` condition matches, the `then` schema is applied; the instance satisfies the `then` constraints."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"B\", \"value\": 5 }"
            },
            {
              "parameter": "schema",
              "value": "{ \"if\": { \"properties\": { \"type\": { \"const\": \"A\" } } }, \"then\": { \"properties\": { \"value\": { \"type\": \"integer\" } }, \"required\": [\"value\"] }, \"else\": { \"properties\": { \"value\": { \"type\": \"string\" } }, \"required\": [\"value\"] } }"
            }
          ],
          "expected": "non‑empty Set<ValidationMessage> containing a type‑mismatch for 'value' (expected string)",
          "description": "The `if` condition does not match, so the `else` schema is applied; the instance violates the `else` type requirement."
        }
      ]
    },
    {
      "group": "validate(JsonNode) - additionalProperties false",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"known\": 1 }"
            },
            {
              "parameter": "schema",
              "value": "{ \"type\": \"object\", \"properties\": { \"known\": { \"type\": \"integer\" } }, \"additionalProperties\": false }"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "An object containing only defined properties passes when additionalProperties is false."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"known\": 1, \"extra\": \"no\" }"
            },
            {
              "parameter": "schema",
              "value": "{ \"type\": \"object\", \"properties\": { \"known\": { \"type\": \"integer\" } }, \"additionalProperties\": false }"
            }
          ],
          "expected": "non‑empty Set<ValidationMessage> containing an additionalProperties violation for 'extra'",
          "description": "An object with an undefined property must be reported when additionalProperties is false."
        }
      ]
    },
    {
      "group": "validate(JsonNode) - $ref with URL‑encoded fragment",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"$ref\": \"#/definitions%2Fcolor\" }"
            },
            {
              "parameter": "schema",
              "value": "{ \"definitions\": { \"color\": { \"type\": \"string\", \"enum\": [\"red\",\"green\",\"blue\"] } } }"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "The $ref contains a URL‑encoded fragment (`%2F` for `/`). The validator must decode the fragment and resolve the definition correctly."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"$ref\": \"#/definitions%2Funknown\" }"
            },
            {
              "parameter": "schema",
              "value": "{ \"definitions\": { \"color\": { \"type\": \"string\" } } }"
            }
          ],
          "expected": "ValidationMessage indicating unresolved $ref error",
          "description": "A URL‑encoded $ref that points to a non‑existent definition should result in an unresolved reference validation message."
        }
      ]
    },
    {
      "group": "validate(JsonNode) - nullable type handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "null"
            },
            {
              "parameter": "schema",
              "value": "{ \"type\": [\"string\", \"null\"] }"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "When the schema type includes `null`, a JSON null instance must be accepted."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "\"text\""
            },
            {
              "parameter": "schema",
              "value": "{ \"type\": [\"string\", \"null\"] }"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "A non‑null value that matches one of the allowed types (string) also passes."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "123"
            },
            {
              "parameter": "schema",
              "value": "{ \"type\": [\"string\", \"null\"] }"
            }
          ],
          "expected": "non‑empty Set<ValidationMessage> containing a type‑mismatch message",
          "description": "A value of a type not listed (number) must be reported as invalid."
        }
      ]
    },
    {
      "group": "validate(JsonNode) - empty schema (no constraints)",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "\"anything\""
            },
            {
              "parameter": "schema",
              "value": "{}"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "An empty schema imposes no restrictions; any instance, including a string, should be considered valid."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "123"
            },
            {
              "parameter": "schema",
              "value": "{}"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "Even a numeric instance passes when the schema is empty."
        }
      ]
    },
    {
      "group": "validate(JsonNode) - not keyword",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "\"allowed\""
            },
            {
              "parameter": "schema",
              "value": "{ \"not\": { \"type\": \"integer\" } }"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "The instance does not match the schema inside `not`, so validation succeeds."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "42"
            },
            {
              "parameter": "schema",
              "value": "{ \"not\": { \"type\": \"integer\" } }"
            }
          ],
          "expected": "non‑empty Set<ValidationMessage> containing a not‑violation message",
          "description": "An instance that matches the subschema inside `not` must be reported as invalid."
        }
      ]
    }
  ]
}