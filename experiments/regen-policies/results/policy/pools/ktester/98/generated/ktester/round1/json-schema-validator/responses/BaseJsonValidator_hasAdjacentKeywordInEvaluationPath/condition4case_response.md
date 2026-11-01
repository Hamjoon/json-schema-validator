{
  "testCases": [
    {
      "group": "No evaluation parent schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "keyword",
              "value": "type"
            }
          ],
          "expected": false,
          "description": "When getEvaluationParentSchema() returns null the method should skip the while‑loop and return false because no adjacent validator can be found."
        }
      ]
    },
    {
      "group": "Parent schema without validators",
      "cases": [
        {
          "input": [
            {
              "parameter": "keyword",
              "value": "minimum"
            }
          ],
          "expected": false,
          "description": "A non‑null parent schema exists but its getValidators() collection is empty. The loop iterates once, finds no matching validator, moves to the next parent (null) and finally returns false."
        }
      ]
    },
    {
      "group": "Direct match in first parent schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "keyword",
              "value": "maxLength"
            }
          ],
          "expected": true,
          "description": "The immediate evaluation parent schema contains a validator whose getKeyword() equals the supplied keyword. The method should set hasValidator to true, break out of the for‑loop and the while‑loop and return true."
        }
      ]
    },
    {
      "group": "Match in ancestor schema (nested hierarchy)",
      "cases": [
        {
          "input": [
            {
              "parameter": "keyword",
              "value": "pattern"
            }
          ],
          "expected": true,
          "description": "The first (closest) parent schema does not contain a matching validator, but its own evaluation parent (grand‑parent) does. The method must continue the while‑loop, locate the match in the ancestor and return true."
        }
      ]
    },
    {
      "group": "Multiple validators, early break",
      "cases": [
        {
          "input": [
            {
              "parameter": "keyword",
              "value": "exclusiveMaximum"
            }
          ],
          "expected": true,
          "description": "The parent schema holds several validators; the matching one appears before others. The method should stop iterating the validator list as soon as the match is found (break), and not evaluate remaining validators."
        }
      ]
    },
    {
      "group": "Validator with null keyword",
      "cases": [
        {
          "input": [
            {
              "parameter": "keyword",
              "value": "required"
            }
          ],
          "expected": false,
          "description": "A validator in the parent schema returns null from getKeyword(). The equals check (keyword.equals(null)) returns false, so the method must continue searching and finally return false."
        }
      ]
    },
    {
      "group": "Null input keyword",
      "cases": [
        {
          "input": [
            {
              "parameter": "keyword",
              "value": null
            }
          ],
          "expected": "NullPointerException",
          "description": "Passing a null keyword leads to a NullPointerException when the code executes keyword.equals(...). The test verifies that the method does not swallow the exception."
        }
      ]
    }
  ]
}