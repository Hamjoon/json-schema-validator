{
  "testCases": [
    {
      "group": "FailFast flag propagation – error message includes failFast true",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with failFast=true, discriminator disabled, reportChildErrors=true, canShortCircuit=false"
            },
            {
              "parameter": "node",
              "value": "invalid JsonNode that fails both schemas (e.g., {\"a\":\"string\"})"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "walk",
              "value": "false"
            }
          ],
          "expected": "Set containing a ValidationMessage with messageKey \"oneOf\" whose failFast flag is true, plus the child ValidationMessages from both schemas.",
          "description": "When ExecutionContext.failFast is true the generated OneOf ValidationMessage must carry failFast=true. The test verifies that the flag is propagated correctly."
        }
      ]
    },
    {
      "group": "FailFast flag propagation – error message includes failFast false",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with failFast=false, discriminator disabled, reportChildErrors=true, canShortCircuit=false"
            },
            {
              "parameter": "node",
              "value": "invalid JsonNode that fails both schemas"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "walk",
              "value": "false"
            }
          ],
          "expected": "Set containing a ValidationMessage with messageKey \"oneOf\" whose failFast flag is false, plus the child ValidationMessages from both schemas.",
          "description": "When ExecutionContext.failFast is false the OneOf ValidationMessage must have failFast=false. This mirrors the previous test but with the flag cleared."
        }
      ]
    },
    {
      "group": "Discriminator enabled but no discriminator validator present – behaves like normal OneOf",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with failFast=false, discriminator enabled, canShortCircuit=false, reportChildErrors=false, DiscriminatorContext active"
            },
            {
              "parameter": "node",
              "value": "JsonNode {\"type\":\"A\"}"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "walk",
              "value": "false"
            }
          ],
          "expected": "Set containing one ValidationMessage with messageKey \"oneOf\" and argument \"0\" (no schemas match). No discriminator‑specific message is produced because no discriminator validator exists in the parent schema.",
          "description": "Even though the configuration enables discriminators, if the parent schema does not contain a discriminator validator the validator should fall back to the normal OneOf logic."
        }
      ]
    },
    {
      "group": "Discriminator enabled – match found but another schema also validates (indexes reported)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with failFast=false, discriminator enabled, canShortCircuit=false, reportChildErrors=false, DiscriminatorContext active"
            },
            {
              "parameter": "node",
              "value": "JsonNode {\"type\":\"A\", \"extra\":1} (matches discriminator and also satisfies second schema)"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "walk",
              "value": "false"
            }
          ],
          "expected": "Set containing one ValidationMessage with messageKey \"oneOf.indexes\" and arguments \"2, 0, 1\" (both schemas are valid). No discriminator‑specific error is emitted because a match was found.",
          "description": "When a discriminator matches a schema but other schemas also validate, the validator must still report the generic OneOf error with all matching indexes. The discriminator does not suppress the generic error when more than one schema is valid."
        }
      ]
    },
    {
      "group": "Walk flag true with discriminator enabled – matching discriminator leads to success",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with failFast=false, discriminator enabled, canShortCircuit=true, reportChildErrors=false, DiscriminatorContext active"
            },
            {
              "parameter": "node",
              "value": "JsonNode {\"type\":\"A\"}"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "walk",
              "value": "true"
            }
          ],
          "expected": "empty Set – the schema selected by the discriminator validates successfully via walk(), so numberOfValidSchema == 1 and no errors are returned.",
          "description": "Ensures that when walk=true the validator calls schema.walk() even when discriminators are active, and a matching discriminator still results in a successful OneOf validation."
        }
      ]
    },
    {
      "group": "ReportChildErrors false – child errors are not included in result",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with failFast=false, discriminator disabled, reportChildErrors=false, canShortCircuit=false"
            },
            {
              "parameter": "node",
              "value": "invalid JsonNode that fails both schemas"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "walk",
              "value": "false"
            }
          ],
          "expected": "Set containing only one ValidationMessage with messageKey \"oneOf\" (argument \"0\"). No child ValidationMessages from the individual schemas are present.",
          "description": "When reportChildErrors() returns false the validator must not union the individual schema error sets into the final result, even though all schemas failed."
        }
      ]
    },
    {
      "group": "Single valid schema with child errors present – child errors ignored",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with failFast=false, discriminator disabled, reportChildErrors=true, canShortCircuit=false"
            },
            {
              "parameter": "node",
              "value": "JsonNode that validates against the second schema but fails the first (e.g., {\"b\":2})"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "walk",
              "value": "false"
            }
          ],
          "expected": "empty Set – because exactly one schema validates, the OneOf validator returns success and discards the child errors collected from the failing schema.",
          "description": "Even though reportChildErrors is true and a failing schema produced errors, when exactly one schema is valid the validator must return an empty set, ignoring the previously collected child errors."
        }
      ]
    },
    {
      "group": "Short‑circuit true – first two schemas valid, third not evaluated",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with failFast=false, discriminator disabled, reportChildErrors=false, canShortCircuit=true"
            },
            {
              "parameter": "node",
              "value": "JsonNode that matches schema0 and schema1 but not schema2 (e.g., {\"a\":1, \"b\":2})"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "walk",
              "value": "false"
            }
          ],
          "expected": "Set containing one ValidationMessage with messageKey \"oneOf.indexes\" and arguments \"2, 0, 1\". The third schema is never evaluated because short‑circuit stops after the second valid schema.",
          "description": "Verifies that canShortCircuit() stops the loop as soon as more than one valid schema is found, even when additional schemas exist."
        }
      ]
    }
  ]
}