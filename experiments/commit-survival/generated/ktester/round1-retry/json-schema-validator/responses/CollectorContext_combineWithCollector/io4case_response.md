{
    "test_cases": [
        {
            "group": "Name not present in collectorMap",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "unknownCollector"
                        },
                        {
                            "parameter": "data",
                            "value": "anyPayload"
                        }
                    ],
                    "expected": "no interaction with any Collector; method returns normally",
                    "description": "The supplied name does not exist in the internal map, so combineWithCollector should simply do nothing and not throw an exception."
                }
            ]
        },
        {
            "group": "Name present but value is not a Collector",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "plainObject"
                        },
                        {
                            "parameter": "data",
                            "value": "someData"
                        }
                    ],
                    "expected": "no interaction with Collector; method returns normally",
                    "description": "The map contains an entry for the given name, but the stored value is a plain Object (not a Collector). The method must ignore it and not attempt to call combine."
                }
            ]
        },
        {
            "group": "Null name with matching Collector",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": null
                        },
                        {
                            "parameter": "data",
                            "value": "nullKeyPayload"
                        }
                    ],
                    "expected": "collector.combine called once with argument \"nullKeyPayload\"",
                    "description": "A Collector is stored under a null key. Passing null as the name should retrieve that Collector and invoke combine with the provided data."
                }
            ]
        },
        {
            "group": "Null name with no matching Collector",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": null
                        },
                        {
                            "parameter": "data",
                            "value": "anyData"
                        }
                    ],
                    "expected": "no interaction with any Collector; method returns normally",
                    "description": "The map does not contain a null key entry. Supplying null as the name should result in a no‑op."
                }
            ]
        },
        {
            "group": "Collector throws exception during combine",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "exceptionCollector"
                        },
                        {
                            "parameter": "data",
                            "value": "trigger"
                        }
                    ],
                    "expected": "RuntimeException propagated from collector.combine",
                    "description": "The stored Collector's combine method throws a RuntimeException. combineWithCollector must not catch it, so the exception should propagate to the caller."
                }
            ]
        },
        {
            "group": "Different data types passed to Collector",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "typeCollector"
                        },
                        {
                            "parameter": "data",
                            "value": 42
                        }
                    ],
                    "expected": "collector.combine called once with argument 42",
                    "description": "A Collector is present; an Integer payload is supplied. The method should forward the Integer unchanged to combine."
                },
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "typeCollector"
                        },
                        {
                            "parameter": "data",
                            "value": [1, 2, 3]
                        }
                    ],
                    "expected": "collector.combine called once with argument [1, 2, 3]",
                    "description": "A Collector is present; a List payload is supplied. The method should forward the List unchanged to combine."
                },
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "typeCollector"
                        },
                        {
                            "parameter": "data",
                            "value": { "key": "value" }
                        }
                    ],
                    "expected": "collector.combine called once with argument {\"key\":\"value\"}",
                    "description": "A Collector is present; a Map payload is supplied. The method should forward the Map unchanged to combine."
                }
            ]
        }
    ]
}