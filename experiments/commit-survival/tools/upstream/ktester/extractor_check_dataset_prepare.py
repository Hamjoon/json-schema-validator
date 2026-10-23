#!/usr/bin/env python3
"""Run unmodified DatasetPrepare for all declared non-private Options methods."""
import argparse
import collections
import json
from pathlib import Path
import sys
import time

sys.dont_write_bytecode = True
import jpype
from stage0_build_index import start_jvm

ROOT = Path(__file__).resolve().parents[1]
DS = ROOT / 'scratch/extractor-check/cli-fresh'
OUT = ROOT / 'results/extractor-check'
CLASS = 'org.apache.commons.cli.Options'


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--attempt', type=int, required=True)
    args = ap.parse_args()
    start = time.monotonic()
    info = json.loads((DS / 'project_index/json/commons-cli.json').read_text())['source'][CLASS]
    methods = sorted(CLASS + '.' + name + m['signature'].split(name, 1)[1]
                     for name, overloads in info['methods'].items() for m in overloads
                     if m['access_type'] != 'PRIVATE')
    meta = [{'project_name': 'commons-cli', 'put_path': '/root/experiments/puts/commons-cli',
             'method_name_to_idx': {m: i for i, m in enumerate(methods)}}]
    (DS / 'dataset_meta.json').write_text(json.dumps(meta, indent=2) + '\n')
    (OUT / 'fresh-dataset-meta.json').write_text(json.dumps(meta, indent=2) + '\n')
    link = DS / 'commons-cli'
    if not link.exists():
        link.symlink_to('projects/commons-cli', target_is_directory=True)
    start_jvm()
    jpype.JClass('DatasetPrepare').main([str(DS)])
    jpype.shutdownJVM()
    raw = (DS / 'dataset_info.json').read_bytes()
    (OUT / f'fresh-dataset-info-attempt-{args.attempt}.json').write_bytes(raw)
    generated = json.loads(raw)['commons-cli']['focal-methods']
    actual = [m['class'] + '.' + m['method-name'] for m in generated]
    by_id = collections.defaultdict(list)
    for m in generated:
        by_id[m['id']].append(m['method-name'])
    result = {'attempt': args.attempt, 'class': CLASS, 'requested_method_count': len(methods),
              'generated_method_count': len(generated), 'unique_id_count': len(by_id),
              'id_collisions': {k: v for k, v in by_id.items() if len(v) > 1},
              'missing_methods': sorted(set(methods) - set(actual)),
              'unexpected_methods': sorted(set(actual) - set(methods)),
              'empty_fields': [{'method': m['method-name'], 'field': field} for m in generated
                               for field in ['id', 'focal-method', 'class-code'] if not m.get(field, '').strip()],
              'seconds': round(time.monotonic() - start, 3)}
    result['extraction_succeeded'] = (len(generated) == len(methods) and not any(
        result[k] for k in ['missing_methods', 'unexpected_methods', 'empty_fields']))
    # Preserve the released tool's IDs. Collisions are reported, never silently repaired.
    (OUT / f'fresh-dataset-check-attempt-{args.attempt}.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result, indent=2))
    if not result['extraction_succeeded']:
        raise SystemExit('DatasetPrepare extraction failed validation')


if __name__ == '__main__':
    main()
