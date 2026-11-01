{
  "group": "checkDiscriminatorMatch – branch coverage",
  "cases": [
    {
      "input": [
        {
          "parameter": "currentDiscriminatorContext",
          "value": "mockDiscriminatorContext (markIgnore() records a flag)"
        },
        {
          "parameter": "discriminator",
          "value": "null (not used when property value is null)"
        },
        {
          "parameter": "discriminatorPropertyValue",
          "value": "null"
        },
        {
          "parameter": "jsonSchema",
          "value": "mockJsonSchema (unused in this branch)"
        }
      ],
      "expected": "currentDiscriminatorContext.markIgnore() is invoked exactly once; method returns early; no calls to checkForExplicitDiscriminatorMappingMatch or checkForImplicitDiscriminatorMappingMatch.",
      "description": "When the discriminator property value is null the validator must ignore the discriminator and exit immediately."
    },
    {
      "input": [
        {
          "parameter": "currentDiscriminatorContext",
          "value": "mockDiscriminatorContext (no match flag set)"
        },
        {
          "parameter": "discriminator",
          "value": "ObjectNode without a \"mapping\" field"
        },
        {
          "parameter": "discriminatorPropertyValue",
          "value": "\"cat\""
        },
        {
          "parameter": "jsonSchema",
          "value": "mockJsonSchema"
        }
      ],
      "expected": "checkForImplicitDiscriminatorMappingMatch(currentDiscriminatorContext, \"cat\", mockJsonSchema) is invoked once; no explicit‑mapping logic is executed.",
      "description": "When a mapping node is absent the validator must fall back to the implicit‑discriminator logic."
    },
    {
      "input": [
        {
          "parameter": "currentDiscriminatorContext",
          "value": "mockDiscriminatorContext where isDiscriminatorMatchFound() will return true after explicit check"
        },
        {
          "parameter": "discriminator",
          "value": "ObjectNode with a non‑null \"mapping\" field (e.g., {\"mapping\": {\"cat\": \"#/components/schemas/Cat\"}})"
        },
        {
          "parameter": "discriminatorPropertyValue",
          "value": "\"cat\""
        },
        {
          "parameter": "jsonSchema",
          "value": "mockJsonSchema"
        }
      ],
      "expected": [
        "checkForExplicitDiscriminatorMappingMatch(...) is invoked once",
        "currentDiscriminatorContext.isDiscriminatorMatchFound() returns true",
        "checkForImplicitDiscriminatorMappingMatch(...) is NOT invoked"
      ],
      "description": "When an explicit mapping exists and the explicit match is found, the validator must not attempt the implicit fallback."
    },
    {
      "input": [
        {
          "parameter": "currentDiscriminatorContext",
          "value": "mockDiscriminatorContext where isDiscriminatorMatchFound() returns false after explicit check"
        },
        {
          "parameter": "discriminator",
          "value": "ObjectNode with a non‑null \"mapping\" field (e.g., {\"mapping\": {\"dog\": \"#/components/schemas/Dog\"}})"
        },
        {
          "parameter": "discriminatorPropertyValue",
          "value": "\"cat\""
        },
        {
          "parameter": "jsonSchema",
          "value": "mockJsonSchema"
        }
      ],
      "expected": [
        "checkForExplicitDiscriminatorMappingMatch(...) is invoked once",
        "currentDiscriminatorContext.isDiscriminatorMatchFound() returns false",
        "noExplicitDiscriminatorKeyOverride(mappingNode, mockJsonSchema) returns true",
        "checkForImplicitDiscriminatorMappingMatch(...) is invoked once"
      ],
      "description": "When an explicit mapping exists but does not match and the parent schema does not override the explicit key, the validator must fall back to the implicit mapping logic."
    },
    {
      "input": [
        {
          "parameter": "currentDiscriminatorContext",
          "value": "mockDiscriminatorContext where isDiscriminatorMatchFound() returns false after explicit check"
        },
        {
          "parameter": "discriminator",
          "value": "ObjectNode with a non‑null \"mapping\" field (e.g., {\"mapping\": {\"dog\": \"#/components/schemas/Dog\"}})"
        },
        {
          "parameter": "discriminatorPropertyValue",
          "value": "\"cat\""
        },
        {
          "parameter": "jsonSchema",
          "value": "mockJsonSchema where noExplicitDiscriminatorKeyOverride(mappingNode, mockJsonSchema) returns false"
        }
      ],
      "expected": [
        "checkForExplicitDiscriminatorMappingMatch(...) is invoked once",
        "currentDiscriminatorContext.isDiscriminatorMatchFound() returns false",
        "noExplicitDiscriminatorKeyOverride(mappingNode, mockJsonSchema) returns false",
        "checkForImplicitDiscriminatorMappingMatch(...) is NOT invoked"
      ],
      "description": "When an explicit mapping exists but does not match and the parent schema explicitly overrides the key, the validator must stop without trying the implicit mapping."
    }
  ]
}