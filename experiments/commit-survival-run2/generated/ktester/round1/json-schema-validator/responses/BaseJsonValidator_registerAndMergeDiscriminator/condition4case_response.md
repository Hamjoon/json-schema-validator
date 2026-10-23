{
  "testCases": [
    {
      "group": "Outer condition false (no discriminator on schema or no registered context)",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "MockDiscriminatorContext with empty internal map"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {} (empty object)"
            },
            {
              "parameter": "schema",
              "value": "JsonSchema with schemaNode {} (no \"discriminator\" field) and schemaPath \"/root\""
            },
            {
              "parameter": "at",
              "value": "\"/root\""
            }
          ],
          "expected": "No exception; the discriminator is registered unchanged (empty mapping).",
          "description": "When the schema does not contain a \"discriminator\" node, the method should skip all merging logic and simply register the provided discriminator."
        }
      ]
    },
    {
      "group": "Property name redefinition (discriminator propertyName present)",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "MockDiscriminatorContext with entry for path \"/root\" (any non‑null discriminator)"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {\"propertyName\":\"type\"}"
            },
            {
              "parameter": "schema",
              "value": "JsonSchema with schemaNode {\"discriminator\":{\"propertyName\":\"type\"}} and schemaPath \"/root\""
            },
            {
              "parameter": "at",
              "value": "\"/root\""
            }
          ],
          "expected": "JsonSchemaException with message containing \"attempts redefining the discriminator property\"",
          "description": "If the schema defines a discriminator that also defines a propertyName, the method must reject it because the root discriminator already defines the property."
        }
      ]
    },
    {
      "group": "Mapping inheritance without existing root mapping (context mapping null, schema mapping present)",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "MockDiscriminatorContext with entry for path \"/root\" (any discriminator object)"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {} (no \"mapping\" field)"
            },
            {
              "parameter": "schema",
              "value": "JsonSchema with schemaNode {\"discriminator\":{\"mapping\":{\"A\":\"#/components/schemas/A\"}}} and schemaPath \"/root\""
            },
            {
              "parameter": "at",
              "value": "\"/root\""
            }
          ],
          "expected": "No exception; discriminator now contains a \"mapping\" field equal to the schema's mapping object.",
          "description": "When the root discriminator has no mapping but a nested schema provides one, the method should copy the nested mapping to the root discriminator."
        }
      ]
    },
    {
      "group": "Mapping merge – add new mapping entry",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "MockDiscriminatorContext with entry for path \"/root\" (discriminator ObjectNode {\"mapping\":{\"A\":\"#/components/schemas/A\"}})"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {\"mapping\":{\"A\":\"#/components/schemas/A\"}}"
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
          "expected": "No exception; after execution the discriminator mapping contains both keys \"A\" and \"B\".",
          "description": "When both root and nested discriminators have a mapping, the method should merge them, adding keys that are not already present."
        }
      ]
    },
    {
      "group": "Mapping merge – same mapping value (no change)",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "MockDiscriminatorContext with entry for path \"/root\" (discriminator ObjectNode {\"mapping\":{\"A\":\"#/components/schemas/A\"}})"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {\"mapping\":{\"A\":\"#/components/schemas/A\"}}"
            },
            {
              "parameter": "schema",
              "value": "JsonSchema with schemaNode {\"discriminator\":{\"mapping\":{\"A\":\"#/components/schemas/A\"}}} and schemaPath \"/root\""
            },
            {
              "parameter": "at",
              "value": "\"/root\""
            }
          ],
          "expected": "No exception; discriminator mapping remains unchanged (key \"A\" still maps to the same value).",
          "description": "If the nested schema defines a mapping entry that already exists in the root with the identical value, the method should silently accept it."
        }
      ]
    },
    {
      "group": "Mapping merge – conflicting mapping (redefinition error)",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "MockDiscriminatorContext with entry for path \"/root\" (discriminator ObjectNode {\"mapping\":{\"A\":\"#/components/schemas/A\"}})"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {\"mapping\":{\"A\":\"#/components/schemas/A\"}}"
            },
            {
              "parameter": "schema",
              "value": "JsonSchema with schemaNode {\"discriminator\":{\"mapping\":{\"A\":\"#/components/schemas/OtherA\"}}} and schemaPath \"/root\""
            },
            {
              "parameter": "at",
              "value": "\"/root\""
            }
          ],
          "expected": "JsonSchemaException with message containing \"discriminator mapping redefinition\" and the conflicting key/value details.",
          "description": "When the nested schema attempts to redefine an existing mapping key with a different value, the method must throw an exception."
        }
      ]
    }
  ]
}