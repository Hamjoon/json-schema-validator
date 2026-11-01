{
  "test_cases": [
    {
      "group": "Null executionContext – NPE",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "node",
              "value": "valid JsonNode (e.g., {\"a\":1})"
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
          "expected": "NullPointerException",
          "description": "The method dereferences executionContext (e.g., executionContext.isFailFast()) before any null‑check, so passing a null ExecutionContext must raise NPE."
        }
      ]
    },
    {
      "group": "Null node – NPE",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (failFast=false, discriminator disabled)"
            },
            {
              "parameter": "node",
              "value": "null"
            },
            {
              "parameter": "rootNode",
              "value": "null"
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
          "expected": "NullPointerException",
          "description": "The code accesses node (e.g., node.get(...)) without a null guard; a null node should cause NPE."
        }
      ]
    },
    {
      "group": "Null parent schema – NPE",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (discriminator enabled, failFast=false)"
            },
            {
              "parameter": "node",
              "value": "valid JsonNode (e.g., {\"type\":\"A\"})"
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
          "expected": "NullPointerException",
          "description": "When the parent schema is null, the call this.getParentSchema().getValidators() triggers NPE. Mock the OneOfValidator subclass to return null from getParentSchema()."
        }
      ]
    },
    {
      "group": "Parent schema without validators – NPE",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (discriminator enabled, failFast=false)"
            },
            {
              "parameter": "node",
              "value": "valid JsonNode (e.g., {\"type\":\"A\"})"
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
          "expected": "NullPointerException",
          "description": "If the parent schema exists but its getValidators() returns null, the subsequent stream operation causes NPE. Mock parent schema to return null validators."
        }
      ]
    },
    {
      "group": "Discriminator validator of wrong type – ClassCastException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (discriminator enabled, failFast=false)"
            },
            {
              "parameter": "node",
              "value": "valid JsonNode (e.g., {\"type\":\"A\"})"
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
          "expected": "ClassCastException",
          "description": "The code assumes the first validator with keyword \"discriminator\" is a DiscriminatorValidator and casts it directly. Provide a parent schema whose validator list contains a mock validator with keyword \"discriminator\" but of a different class; the cast should fail."
        }
      ]
    },
    {
      "group": "Discriminator schema node not ObjectNode – ClassCastException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (discriminator enabled, failFast=false)"
            },
            {
              "parameter": "node",
              "value": "valid JsonNode (e.g., {\"type\":\"A\"})"
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
          "expected": "ClassCastException",
          "description": "When discriminator is present the code casts discriminator.getSchemaNode() to ObjectNode. Mock a DiscriminatorValidator whose schemaNode is a JsonNode of type ArrayNode (or any non‑ObjectNode). The cast should throw ClassCastException."
        }
      ]
    }
  ]
}