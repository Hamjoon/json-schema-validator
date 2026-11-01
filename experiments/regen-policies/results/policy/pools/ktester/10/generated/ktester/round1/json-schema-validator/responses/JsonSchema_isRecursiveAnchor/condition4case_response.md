{
  "testCases": [
    {
      "group": "Default recursiveAnchor value",
      "cases": [
        {
          "input": [],
          "expected": false,
          "description": "A newly instantiated JsonSchema should have recursiveAnchor set to its default value false. The isRecursiveAnchor() getter must return false."
        }
      ]
    },
    {
      "group": "Modified recursiveAnchor value via reflection",
      "cases": [
        {
          "input": [
            {
              "parameter": "setRecursiveAnchorToTrue",
              "value": true
            }
          ],
          "expected": true,
          "description": "After forcibly setting the private field `recursiveAnchor` to true (e.g., using reflection), isRecursiveAnchor() must return true, confirming that the getter reflects the current field state."
        },
        {
          "input": [
            {
              "parameter": "setRecursiveAnchorToFalse",
              "value": false
            }
          ],
          "expected": false,
          "description": "After resetting the private field `recursiveAnchor` back to false, isRecursiveAnchor() must again return false, ensuring the getter does not cache previous values."
        }
      ]
    },
    {
      "group": "Isolation between instances",
      "cases": [
        {
          "input": [
            {
              "parameter": "instanceARecursiveAnchor",
              "value": true
            },
            {
              "parameter": "instanceBRecursiveAnchor",
              "value": false
            }
          ],
          "expected": {
            "instanceA": true,
            "instanceB": false
          },
          "description": "Create two separate JsonSchema objects. Set `recursiveAnchor` to true on the first instance and leave the second at its default false. Verify that isRecursiveAnchor() returns true for the first and false for the second, confirming that the field is instance‑specific and not static."
        }
      ]
    }
  ]
}