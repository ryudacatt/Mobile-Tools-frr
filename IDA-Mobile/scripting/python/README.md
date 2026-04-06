# scripting/python

Runtime Python modules executed inside the Android app through Chaquopy.

Current modules:

- `analysis_tools.py`
  - ranks extracted DEX strings with entropy/pattern heuristics
  - returns JSON consumed by Kotlin repositories

Execution path:

- Kotlin calls Python via `Python.getInstance().getModule(...)`
- No network service is required
- Scripts run locally inside app process
