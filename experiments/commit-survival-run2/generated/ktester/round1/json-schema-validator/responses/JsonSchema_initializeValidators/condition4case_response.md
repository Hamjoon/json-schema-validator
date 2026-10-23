{
  "testCases": [
    {
      "group": "initializeValidators - first call with non‑empty validators map",
      "cases": [
        {
          "input": [],
          "expected": "validatorsLoaded flag becomes true; preloadJsonSchema() is invoked exactly once on each JsonValidator returned by getValidators() (e.g., two mock validators).",
          "description": "When the schema has not yet loaded its validators (validatorsLoaded == false) and getValidators() returns a map containing validators, initializeValidators() should set validatorsLoaded to true and call preloadJsonSchema() on every validator."
        }
      ]
    },
    {
      "group": "initializeValidators - first call with empty validators map",
      "cases": [
        {
          "input": [],
          "expected": "validatorsLoaded flag becomes true; no call to preloadJsonSchema() because the validators map is empty.",
          "description": "Ensures that the method correctly handles an empty validators collection: it still flips the loaded flag but does not attempt to iterate over any validators."
        }
      ]
    },
    {
      "group": "initializeValidators - subsequent call when already loaded",
      "cases": [
        {
          "input": [],
          "expected": "validatorsLoaded flag remains true; preloadJsonSchema() is NOT invoked on any validator (previously loaded validators are not re‑preloaded).",
          "description": "When validatorsLoaded is already true, the method must exit early without touching the validators map, guaranteeing idempotent behaviour."
        }
      ]
    }
  ]
}