Invalid control character at: line 78 column 36 (char 2978)
{
  "group": "registerAndMergeDiscriminator - branch coverage",
  "cases": [
    {
      "input": [
        {
          "parameter": "currentDiscriminatorContext",
          "value": "MockDiscriminatorContext { getDiscriminatorForPath returns null }"
        },
        {
          "parameter": "discriminator",
          "value": "ObjectNode {}"
        },
        {
          "parameter": "schema",
          "value": "JsonSchema { schemaNode without \"discriminator\" field }"
        },
        {
          "parameter": "instanceLocation",
          "value": "\"/root\""
        }
      ],
      "expected": "No exception. The method should simply call currentDiscriminatorContext.registerDiscriminator with the provided schemaLocation and discriminator. No modifications to the discriminator node.",
      "description": "When the schema does not contain a \"discriminator\" property, the method should skip all inner logic and just register the discriminator."
    },
    {
      "input": [
        {
          "parameter": "currentDiscriminatorContext",
          "value": "MockDiscriminatorContext { getDiscriminatorForPath returns null }"
        },
        {
          "parameter": "discriminator",
          "value": "ObjectNode {}"
        },
        {
          "parameter": "schema",
          "value": "JsonSchema { schemaNode.discriminator present but context returns null for that path }"
        },
        {
          "parameter": "instanceLocation",
          "value": "\"/root\""
        }
      ],
      "expected": "No exception. The method should behave the same as the previous case – only registerDiscriminator is invoked. No mapping manipulation occurs.",
      "description": "Even if the schema has a discriminator, if the current context does not have a discriminator for the schema location, the method must not process the mapping."
    },
    {
      "input": [
        {
          "parameter": "currentDiscriminatorContext",
          "value": "MockDiscriminatorContext { getDiscriminatorForPath returns non‑null ObjectNode }"
        },
        {
          "parameter": "discriminator",
          "value": "ObjectNode {}"
        },
        {
          "parameter": "schema",
          "value": "JsonSchema { schemaNode.discriminator { \"propertyName\": \"type\" } }"
        },
        {
          "parameter": "instanceLocation",
          "value": "\"/root\""
        }
      ],
      "expected": "JsonSchemaException with message containing \"attempts redefining the discriminator property\"",
      "description": "If the discriminator defined on the schema contains a \"propertyName\" field, the method must reject it by throwing an exception."
    },
    {
      "input": [
        {
          "parameter": "currentDiscriminatorContext",
          "value": "MockDiscriminatorContext { getDiscriminatorForPath returns non‑null ObjectNode }"
        },
        {
          "parameter": "discriminator",
          "value": "ObjectNode {}   // no \"mapping\" field"
        },
        {
          "parameter": "schema",
          "value": "JsonSchema { schemaNode.discriminator { \"mapping\": { \"A\": \"#/components/schemas/A\" } } }"
        },
        {
          "parameter": "instanceLocation",
          "value": "\"/root\""
        }
      ],
      "expected": "No exception. After execution, discriminator node should contain a \"mapping\" field equal to the schema's discriminator object (i.e., the whole discriminatorOnSchema node).",
      "description": "When the root discriminator has a mapping but the current discriminator does not, the method should copy the whole discriminator object onto the root discriminator."
    },
    {
      "input": [
        {
          "parameter": "currentDiscriminatorContext",
          "value": "MockDiscriminatorContext { getDiscriminatorForPath returns non‑null ObjectNode }"
        },
        {
          "parameter": "discriminator",
          "value": "ObjectNode { \"mapping\": { \"A\": \"#/components/schemas/A\" } }"
        },
        {
          "parameter": "schema",
          "value": "JsonSchema { schemaNode.discriminator { \"mapping\": { \"A\": \"#/components/schemas/A\", \"B\": \"#/components/schemas/B\" } } }"
        },
        {
          "parameter": "instanceLocation",
          "value": "\"/root\""
        }
      ],
      "expected": "No exception. The method should add the new key \"B\" with its value to the existing mapping in the discriminator node. Existing key \"A\" stays unchanged.",
      "description": "Both context and schema have a mapping. The method must merge them, adding only the entries that are not already present."
    },
    {
      "input": [
        {
          "parameter": "currentDiscriminatorContext",
          "value": "MockDiscriminatorContext { getDiscriminatorForPath returns non‑null ObjectNode }"
        },
        {
          "parameter": "discriminator",
          "value": "ObjectNode { \"mapping\": { \"A\": \"#/components/schemas/A\" } }"
        },
        {
          "parameter": "schema",
          "value": "JsonSchema { schemaNode.discriminator { \"mapping\": { \"A\": \"#/components/schemas/OtherA\" } } }"
        },
        {
          "parameter": "instanceLocation",
          "value": "\"/root\""
        }
      ],
      "expected": "JsonSchemaException with message containing \"discriminator mapping redefinition from A\" and the two conflicting values.",
      "description": "When a mapping key already exists in the context but points to a different value than the one defined in the schema, the method must throw an exception."
    },
    {
      "input": [
        {
          "parameter": "currentDiscriminatorContext",
          "value": "MockDiscriminatorContext { getDiscriminatorForPath returns non‑null ObjectNode }"
        },
        {
          "parameter": "discriminator",
          "value": "ObjectNode { \"mapping\": { \"A\": \"#/components/schemas/A\" } }"
        },
        {
          "parameter": "schema",
          "value": "JsonSchema { schemaNode.discriminator { \"mapping\": { \"A\": \"#/components/schemas/A\" } } }"
        },
        {
          "parameter": "instanceLocation",
          "value": "\"/root\""
        }
      ],
      "expected": "No exception. No new entries are added because the mapping already exists with the same value. The discriminator node remains unchanged.",
      "description": "When the schema mapping duplicates an existing mapping with identical value, the method should silently ignore it (no exception, no modification)."
    },
    {
      "input": [
        {
          "parameter": "currentDiscriminatorContext",
          "value": "MockDiscriminatorContext { getDiscriminatorForPath returns non‑null ObjectNode }"
        },
        {
          "parameter": "discriminator",
          "value": "ObjectNode { \"mapping\": { \"A\": \"#/components/schemas/A\" } }"
        },
        {
          "parameter": "schema",
          "value": "JsonSchema { schemaNode.discriminator { \"mapping\": null } }"
        },
        {
          "parameter": "instanceLocation",
          "value": "\"/root\""
        }
      ],
      "expected": "No exception. Since mappingOnCurrentSchemaDiscriminator is null, the method does nothing besides registering the discriminator.",
      "description": "Edge case where the schema's discriminator exists but has no mapping; the method should not attempt any merge."
    }
  ]
}