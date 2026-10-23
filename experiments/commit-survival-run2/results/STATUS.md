# Run 2 status

STOP: scope contains fewer than three classes. Step 1 populations and dataset preparation are complete; Steps 2–7 were not run.

The run 2 order is JsonSchema (c=26, m=23), SchemaValidatorsConfig (c=17, m=57), BaseJsonValidator (c=17, m=15), then the remaining ranked classes. The first two reach m=80, satisfying the >=50 target and exactly filling the cap. Adding the third would give m=95, exceeding the cap.

Run 2 explicitly inherits run 1 retries and stop conditions. Run 1 states:

> If Step 1.2 (build at t) fails twice, or Step 2 selects fewer than 3 classes, or both LLM groups end with zero methods: stop after writing the handover and `results/STATUS.md`.

The “fewer than 3 classes” scope condition applies to run 2 Step 1 (renumbered from run 1 Step 2). The run 2 exception for cumulative m < 50 does not apply because m=80.

Dev: 185 stable methods. KTester population: 80 unique final IDs, one raw ID collision corrected. KTester and Ouédraogo retained denominators: unavailable (generation not run). Model calls: 0. Phase A/B: not run. No author code changed; no push.
