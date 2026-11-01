{
  "group": "checkDiscriminatorMatch – additional edge cases",
  "cases": [
    {
      "input": [
        {
          "parameter": "currentDiscriminatorContext",
          "value": "mockDiscriminatorContext (no match flag set, isDiscriminatorMatchFound() returns false after explicit check)"
        },
        {
          "parameter": "discriminator",
          "value": "ObjectNode with a \"mapping\" field whose value is null (i.e., {\"mapping\": null})"
        },
        {
          "parameter": "discriminatorPropertyValue",
          "value": "\"cat\""
        },
        {
          "parameter": "jsonSchema",
          "value": "mockJsonSchema (noExplicitDiscriminatorKeyOverride will not be consulted because mapping is null)"
        }
      ],
      "expected": [
        "checkForExplicitDiscriminatorMappingMatch(...) is NOT invoked because discriminatorMapping is null",
        "checkForImplicitDiscriminatorMappingMatch(currentDiscriminatorContext, \"cat\", mockJsonSchema) is invoked exactly once"
      ],
      "description": "When the discriminator object contains a \"mapping\" field whose value is JSON null, the method should treat it as absent and fall back to the implicit‑discriminator logic."
    },
    {
      "input": [
        {
          "parameter": "currentDiscriminatorContext",
          "value": "mockDiscriminatorContext (no match flag set, isDiscriminatorMatchFound() returns false after explicit check)"
        },
        {
          "parameter": "discriminator",
          "value": "ObjectNode with a non‑object \"mapping\" field (e.g., {\"mapping\": [\"cat\", \"dog\"]})"
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
        "checkForExplicitDiscriminatorMappingMatch(...) is invoked once (receives a non‑object mapping node)",
        "currentDiscriminatorContext.isDiscriminatorMatchFound() returns false",
        "noExplicitDiscriminatorKeyOverride(mappingNode, mockJsonSchema) returns false",
        "checkForImplicitDiscriminatorMappingMatch(...) is NOT invoked"
      ],
      "description": "If the \"mapping\" field exists but is not an object, the explicit‑mapping check runs, fails to find a match, and because the parent schema explicitly overrides the discriminator key, the implicit fallback must not be attempted."
    },
    {
      "input": [
        {
          "parameter": "currentDiscriminatorContext",
          "value": "mockDiscriminatorContext (no match flag set, isDiscriminatorMatchFound() returns false after explicit check)"
        },
        {
          "parameter": "discriminator",
          "value": "ObjectNode with an empty object as \"mapping\" (e.g., {\"mapping\": {}})"
        },
        {
          "parameter": "discriminatorPropertyValue",
          "value": "\"cat\""
        },
        {
          "parameter": "jsonSchema",
          "value": "mockJsonSchema where noExplicitDiscriminatorKeyOverride(mappingNode, mockJsonSchema) returns true"
        }
      ],
      "expected": [
        "checkForExplicitDiscriminatorMappingMatch(...) is invoked once (receives an empty mapping object)",
        "currentDiscriminatorContext.isDiscriminatorMatchFound() returns false",
        "noExplicitDiscriminatorKeyOverride(mappingNode, mockJsonSchema) returns true",
        "checkForImplicitDiscriminatorMappingMatch(currentDiscriminatorContext, \"cat\", mockJsonSchema) is invoked once"
      ],
      "description": "An empty explicit mapping provides no match; because the parent schema does not override the explicit key, the validator must fall back to the implicit‑discriminator logic."
    },
    {
      "input": [
        {
          "parameter": "currentDiscriminatorContext",
          "value": "mockDiscriminatorContext (no match flag set, isDiscriminatorMatchFound() returns false after explicit check)"
        },
        {
          "parameter": "discriminator",
          "value": "ObjectNode with a \"mapping\" field that contains an empty string key (e.g., {\"mapping\": {\"\": \"#/components/schemas/Empty\"}})"
        },
        {
          "parameter": "discriminatorPropertyValue",
          "value": "\"\""
        },
        {
          "parameter": "jsonSchema",
          "value": "mockJsonSchema where noExplicitDiscriminatorKeyOverride(mappingNode, mockJsonSchema) returns true"
        }
      ],
      "expected": [
        "checkForExplicitDiscriminatorMappingMatch(...) is invoked once and finds a match for the empty string key",
        "currentDiscriminatorContext.isDiscriminatorMatchFound() returns true",
        "checkForImplicitDiscriminatorMappingMatch(...) is NOT invoked"
      ],
      "description": "The discriminator property value can be an empty string; if the explicit mapping contains an empty‑string key, the explicit‑mapping match should succeed and the implicit fallback must be skipped."
    },
    {
      "input": [
        {
          "parameter": "currentDiscriminatorContext",
          "value": "mockDiscriminatorContext (any implementation, not used because method throws early)"
        },
        {
          "parameter": "discriminator",
          "value": "null"
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
      "expected": "NullPointerException is thrown when attempting to call discriminator.get(\"mapping\") on a null discriminator",
      "description": "If the discriminator argument itself is null (which should not happen in normal usage), the method will attempt to dereference it and therefore throws a NullPointerException."
    }
  ]
}