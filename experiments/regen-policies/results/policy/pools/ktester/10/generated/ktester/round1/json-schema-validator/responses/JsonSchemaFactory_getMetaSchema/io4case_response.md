{
  "newTestCases": [
    {
      "group": "Config parameter variations",
      "cases": [
        {
          "input": [
            { "parameter": "id", "value": "\"http://json-schema.org/draft-07/schema#\"" },
            { "parameter": "config", "value": "null" }
          ],
          "expected": "A JsonMetaSchema instance equal to JsonSchemaVersion.DRAFT_07.getInstance(). The method must handle a null SchemaValidatorsConfig without throwing, and must not invoke loadMetaSchema.",
          "description": "Null config with a well‑known dialect should still resolve the dialect via SpecVersionDetector and return the corresponding meta‑schema, proving that the config argument is optional for the true branch."
        },
        {
          "input": [
            { "parameter": "id", "value": "\"urn:custom:my-meta-schema\"" },
            { "parameter": "config", "value": "null" }
          ],
          "expected": "The JsonMetaSchema returned by loadMetaSchema(\"urn:custom:my-meta-schema\", null). The fallback branch must be taken and the null config must be forwarded to loadMetaSchema.",
          "description": "Null config with an unknown id forces the fallback; verifies that the method forwards a null config unchanged to loadMetaSchema and does not throw NPE."
        }
      ]
    },
    {
      "group": "Id string variations (whitespace & case)",
      "cases": [
        {
          "input": [
            { "parameter": "id", "value": "\"  http://json-schema.org/draft-07/schema#  \"" },
            { "parameter": "config", "value": "new SchemaValidatorsConfig()" }
          ],
          "expected": "The JsonMetaSchema returned by loadMetaSchema(\"  http://json-schema.org/draft-07/schema#  \", config). Because the id contains surrounding whitespace, SpecVersionDetector does not recognise it, so the fallback branch is executed.",
          "description": "Tests that the detection logic is strict about whitespace; the method should treat the id as unknown and delegate to loadMetaSchema."
        },
        {
          "input": [
            { "parameter": "id", "value": "\"HTTP://JSON-SCHEMA.ORG/DRAFT-07/SCHEMA#\"" },
            { "parameter": "config", "value": "new SchemaValidatorsConfig()" }
          ],
          "expected": "The JsonMetaSchema returned by loadMetaSchema(\"HTTP://JSON-SCHEMA.ORG/DRAFT-07/SCHEMA#\", config). Upper‑case scheme/host makes the id unrecognised, causing fallback.",
          "description": "Ensures that id matching is case‑sensitive; a well‑known dialect written in upper case should not be detected and must trigger the custom‑schema path."
        }
      ]
    },
    {
      "group": "Malformed or non‑URI ids",
      "cases": [
        {
          "input": [
            { "parameter": "id", "value": "\"not a uri\"" },
            { "parameter": "config", "value": "new SchemaValidatorsConfig()" }
          ],
          "expected": "The JsonMetaSchema returned by loadMetaSchema(\"not a uri\", config). SpecVersionDetector cannot parse the string, so Optional.empty leads to fallback.",
          "description": "Validates that arbitrary non‑URI strings are safely handed over to loadMetaSchema without causing an exception."
        },
        {
          "input": [
            { "parameter": "id", "value": "\"http://[invalid-ip]/schema#\"" },
            { "parameter": "config", "value": "new SchemaValidatorsConfig()" }
          ],
          "expected": "The JsonMetaSchema returned by loadMetaSchema(\"http://[invalid-ip]/schema#\", config). Even though the URI is syntactically invalid, the method should not attempt to resolve it as a known dialect and must fall back.",
          "description": "Checks robustness when the id is a malformed URI; the method should treat it as unknown and delegate to loadMetaSchema."
        }
      ]
    },
    {
      "group": "Repeated calls with same unknown id (caching behavior)",
      "cases": [
        {
          "input": [
            { "parameter": "id", "value": "\"urn:custom:cached-schema\"" },
            { "parameter": "config", "value": "new SchemaValidatorsConfig()" }
          ],
          "expected": "First call: loadMetaSchema is invoked and its result cached. Second call: loadMetaSchema is NOT invoked again; the cached JsonMetaSchema is returned.",
          "description": "Ensures that for an unknown id the factory caches the loaded meta‑schema, preventing duplicate loading on subsequent calls."
        }
      ]
    }
  ]
}