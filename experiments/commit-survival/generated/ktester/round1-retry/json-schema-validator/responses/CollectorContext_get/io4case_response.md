{
    "group": "CollectorContext#get - additional cases",
    "cases": [
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "null"
                }
            ],
            "expected": "null",
            "description": "Calling get(null) when both maps are empty should return null (Map#get accepts null keys and returns null)."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "\"\""
                }
            ],
            "expected": "\"emptyValue\"",
            "description": "Key is empty string; collectorMap contains a plain (non‑Collector) value and collectorLoadMap also contains a value. Since the value in collectorMap is not a Collector, the method must return the plain value, ignoring the load map."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "\"collectorNullLoad\""
                }
            ],
            "expected": "collectorInstance",
            "description": "collectorMap holds a Collector instance, collectorLoadMap holds an explicit null for the same key. The load‑map check fails (null), so the Collector from collectorMap is returned."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "\"nullCollector\""
                }
            ],
            "expected": "null",
            "description": "collectorMap contains a null entry for the key, while collectorLoadMap contains a non‑null loaded value. Because the first lookup yields null (object is not a Collector), the method returns the null from collectorMap, ignoring the load map."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "\"bothNull\""
                }
            ],
            "expected": "null",
            "description": "Both collectorMap and collectorLoadMap contain explicit null values for the key. The method returns null."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "\"plainWithLoad\""
                }
            ],
            "expected": "\"plain\"",
            "description": "collectorMap holds a plain object (String) and collectorLoadMap holds a different value. Since the object is not a Collector, the method must return the plain value from collectorMap, ignoring the load map."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "\"collectorWithIntegerLoad\""
                }
            ],
            "expected": "42",
            "description": "collectorMap holds a Collector instance, collectorLoadMap holds an Integer (non‑null). The method should return the Integer loaded value, demonstrating that the loaded value can be of any type."
        }
    ]
}