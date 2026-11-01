{
  "group": "JsonSchema.isRecursiveAnchor() basic behavior",
  "cases": [
    {
      "input": [],
      "expected": false,
      "description": "A freshly constructed JsonSchema should have its private boolean `recursiveAnchor` defaulted to false, so `isRecursiveAnchor()` must return false."
    },
    {
      "input": [
        {
          "parameter": "setRecursiveAnchor",
          "value": true
        }
      ],
      "expected": true,
      "description": "Using reflection to set the private field `recursiveAnchor` to true should cause `isRecursiveAnchor()` to return true, confirming the getter reflects the field value."
    },
    {
      "input": [
        {
          "parameter": "setRecursiveAnchor",
          "value": false
        }
      ],
      "expected": false,
      "description": "After the field has been toggled back to false via reflection, `isRecursiveAnchor()` must again return false, proving the getter does not cache a previous value."
    }
  ]
}