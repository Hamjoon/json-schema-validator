#!/usr/bin/env python3
"""Build and count a fresh index without opening an author index or snapshot."""
import json
from pathlib import Path
import sys
import time
import types

sys.dont_write_bytecode = True
import jpype
from stage0_build_index import start_jvm, lucene

ROOT = Path(__file__).resolve().parents[1]
DS = ROOT / 'scratch/extractor-check/cli-fresh'
OUT = DS / 'project_index'


def main():
    start = time.monotonic()
    for folder in ['json', 'codegraph', 'lucene']:
        (OUT / folder).mkdir(parents=True, exist_ok=True)
    start_jvm()
    jpype.JClass('PreProcessor').main([str(DS / 'projects'), str(OUT)])
    sys.path.insert(0, str(ROOT / 'code'))
    import procedure.preprocess_project as pp
    fs = types.SimpleNamespace(DATASET_PATH=str(DS), CODE_INFO_PATH=str(OUT))
    pp.build_calling_graph(fs)
    pp.extract_invoke_patterns(fs)
    jpype.JClass('IndexBuilder').main(['group', str(OUT / 'json'), str(OUT / 'lucene')])
    info = json.loads((OUT / 'json/commons-cli.json').read_text())
    invoke = json.loads((OUT / 'codegraph/commons-cli_invoke.json').read_text())
    counts = {part: {'classes': len(info[part]),
                     'methods': sum(len(entries) for cls in info[part].values() for entries in cls['methods'].values()),
                     'constructors': sum(len(cls['constructors']) for cls in info[part].values())}
              for part in ['source', 'test']}
    result = {'status': 'completed', 'seconds': round(time.monotonic() - start, 3),
              'unreachable_targets': pp.UNREACHABLE_TARGETS, 'json': counts,
              'invoke_classes': len(invoke), 'invoke_method_keys': sum(map(len, invoke.values())),
              'invoke_examples': sum(len(examples) for cls in invoke.values() for examples in cls.values()),
              'lucene': lucene(OUT / 'lucene/commons-cli')}
    result['json_total_classes'] = sum(c['classes'] for c in counts.values())
    result['json_total_methods'] = sum(c['methods'] for c in counts.values())
    (ROOT / 'results/extractor-check/run4-fresh.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result, indent=2))
    jpype.shutdownJVM()


if __name__ == '__main__':
    main()
