{
  "test_cases": [
    {
      "group": "Null discriminator argument",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "MockDiscriminatorContext with entry for path \"/root\" (any discriminator object)"
            },
            {
              "parameter": "discriminator",
              "value": "null"
            },
            {
              "parameter": "schema",
              "value": "JsonSchema with schemaNode {\"discriminator\":{}} and schemaPath \"/root\""
            },
            {
              "parameter": "at",
              "value": "\"/root\""
            }
          ],
          "expected": "NullPointerException (thrown when attempting discriminator.get(\"mapping\"))",
          "description": "Passing a null discriminator should cause a NullPointerException because the method dereferences discriminator without a null‑check."
        }
      ]
    },
    {
      "group": "Null schema argument",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "MockDiscriminatorContext with entry for path \"/root\""
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {} (empty object)"
            },
            {
              "parameter": "schema",
              "value": "null"
            },
            {
              "parameter": "at",
              "value": "\"/root\""
            }
          ],
          "expected": "NullPointerException (thrown when accessing schema.schemaNode)",
          "description": "If the supplied JsonSchema is null, the first line `schema.schemaNode.get(...)` dereferences a null reference."
        }
      ]
    },
    {
      "group": "Null schemaNode inside JsonSchema",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "MockDiscriminatorContext with entry for path \"/root\""
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {} (empty object)"
            },
            {
              "parameter": "schema",
              "value": "JsonSchema with schemaNode null and schemaPath \"/root\""
            },
            {
              "parameter": "at",
              "value": "\"/root\""
            }
          ],
          "expected": "NullPointerException (thrown when calling schema.schemaNode.get(\"discriminator\"))",
          "description": "When the JsonSchema's internal `schemaNode` field is null, the method attempts to call `get` on it, resulting in NPE."
        }
      ]
    },
    {
      "group": "Null schemaPath causing context registration failure",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "MockDiscriminatorContext (registerDiscriminator implementation throws NPE on null path)"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {} (empty object)"
            },
            {
              "parameter": "schema",
              "value": "JsonSchema with schemaNode {} (no \"discriminator\" field) and schemaPath null"
            },
            {
              "parameter": "at",
              "value": "\"/\""
            }
          ],
          "expected": "NullPointerException (thrown inside currentDiscriminatorContext.registerDiscriminator when path is null)",
          "description": "If `schema.schemaPath` is null, the final call to `registerDiscriminator` receives a null key, which typical implementations will reject with NPE."
        }
      ]
    },
    {
      "group": "Schema discriminator mapping is not an ObjectNode (ClassCastException)",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "MockDiscriminatorContext with entry for path \"/root\" (any discriminator object)"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {\"mapping\":{\"A\":\"#/components/schemas/A\"}}"
            },
            {
              "parameter": "schema",
              "value": "JsonSchema with schemaNode {\"discriminator\":{\"mapping\":\"invalidMappingValue\"}} and schemaPath \"/root\""
            },
            {
              "parameter": "at",
              "value": "\"/root\""
            }
          ],
          "expected": "ClassCastException (attempting to cast a non‑ObjectNode mapping value to ObjectNode)",
          "description": "When the nested schema's discriminator contains a `mapping` field that is not an object (e.g., a string), the cast to `ObjectNode` fails."
        }
      ]
    },
    {
      "group": "Root discriminator's mapping field is not an ObjectNode (ClassCastException)",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "MockDiscriminatorContext with entry for path \"/root\" (any discriminator object)"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {\"mapping\":\"invalidRootMapping\"}"
            },
            {
              "parameter": "schema",
              "value": "JsonSchema with schemaNode {\"discriminator\":{\"mapping\":{\"B\":\"#/components/schemas/B\"}}} and schemaPath \"/root\""
            },
            {
              "parameter": "at",
              "value": "\"/root\""
            }
          ],
          "expected": "ClassCastException (attempting to cast the root discriminator's non‑ObjectNode mapping to ObjectNode)",
          "description": "If the provided root discriminator already contains a `mapping` field that is not an object, the cast to `ObjectNode` triggers a ClassCastException before any merge logic."
        }
      ]
    }
  ]
}