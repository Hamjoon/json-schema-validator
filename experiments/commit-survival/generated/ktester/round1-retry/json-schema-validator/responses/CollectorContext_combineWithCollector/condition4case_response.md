{
  "group": "Collector exists and is instance of Collector",
  "cases": [
    {
      "input": [
        {
          "parameter": "name",
          "value": "myCollector"
        },
        {
          "parameter": "data",
          "value": "payload123"
        }
      ],
      "expected": "collector.combine called once with argument \"payload123\"",
      "description": "The map contains a Collector instance under the given name. The method should retrieve the Collector and invoke its combine method with the supplied data."
    },
    {
      "input": [
        {
          "parameter": "name",
          "value": "nullDataCollector"
        },
        {
          "parameter": "data",
          "value": null
        }
      ],
      "expected": "collector.combine called once with argument null",
      "description": "A Collector is stored for the name but the data argument is null. The method must still call combine(null) on the Collector."
    }
  ]
}