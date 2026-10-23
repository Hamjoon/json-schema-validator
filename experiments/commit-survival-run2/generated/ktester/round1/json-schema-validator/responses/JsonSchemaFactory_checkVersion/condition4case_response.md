{
  "group": "checkVersion",
  "cases": [
    {
      "input": [
        {
          "parameter": "versionFlag",
          "value": null
        }
      ],
      "expected": "null",
      "description": "When the versionFlag argument is null the method should return null (first guard clause)."
    },
    {
      "input": [
        {
          "parameter": "versionFlag",
          "value": "V202012"
        }
      ],
      "expected": "Version202012",
      "description": "Enum constant V202012 should map to a new instance of Version202012."
    },
    {
      "input": [
        {
          "parameter": "versionFlag",
          "value": "V201909"
        }
      ],
      "expected": "Version201909",
      "description": "Enum constant V201909 should map to a new instance of Version201909."
    },
    {
      "input": [
        {
          "parameter": "versionFlag",
          "value": "V7"
        }
      ],
      "expected": "Version7",
      "description": "Enum constant V7 should map to a new instance of Version7."
    },
    {
      "input": [
        {
          "parameter": "versionFlag",
          "value": "V6"
        }
      ],
      "expected": "Version6",
      "description": "Enum constant V6 should map to a new instance of Version6."
    },
    {
      "input": [
        {
          "parameter": "versionFlag",
          "value": "V4"
        }
      ],
      "expected": "Version4",
      "description": "Enum constant V4 should map to a new instance of Version4."
    },
    {
      "input": [
        {
          "parameter": "versionFlag",
          "value": "UNSUPPORTED"
        }
      ],
      "expected": "IllegalArgumentException",
      "description": "If a future/unsupported enum constant is passed (not covered by the switch cases), the method must throw IllegalArgumentException (default branch). In the test a mock enum value is created via reflection to simulate this scenario."
    }
  ]
}