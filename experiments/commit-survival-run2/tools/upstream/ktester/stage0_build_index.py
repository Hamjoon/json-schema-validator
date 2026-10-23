#!/usr/bin/env python3
"""Explicit scratch-only driver for the bundled extractor and index jars."""
import argparse,json,pathlib,sys,time,types,traceback
import jpype
ROOT=pathlib.Path(__file__).resolve().parents[1]
sys.dont_write_bytecode=True  # Import pipeline modules without writing under code/.
def start_jvm():
 jpype.startJVM(jpype.getDefaultJVMPath(),'-Xmx4g',classpath=[str(ROOT/'code/Java/project-info-process.jar'),str(ROOT/'code/Java/project-index-builder.jar')])
def normalize(v):
 if isinstance(v,str):return v.replace('\r\n','\n')
 if isinstance(v,list):return [normalize(x) for x in v]
 if isinstance(v,dict):return {k:normalize(x) for k,x in sorted(v.items())}
 return v
def compare(a,b,path=''):
 if type(a)!=type(b):return [path+': type differs']
 if isinstance(a,dict):
  dif=[path+'/'+k+': missing/extra' for k in a.keys()^b.keys()]
  for k in a.keys()&b.keys():dif+=compare(a[k],b[k],path+'/'+k)
  return dif
 if isinstance(a,list):
  dif=[] if len(a)==len(b) else [path+': lengths '+str(len(a))+'/'+str(len(b))]
  for i,(x,y) in enumerate(zip(a,b)):dif+=compare(x,y,path+'/'+str(i))
  return dif
 return [] if a==b else [path]
def lucene(path):
 Paths=jpype.JClass('java.nio.file.Paths');FS=jpype.JClass('org.apache.lucene.store.FSDirectory');DR=jpype.JClass('org.apache.lucene.index.DirectoryReader')
 directory=FS.open(Paths.get(str(path)));reader=DR.open(directory);stored=reader.storedFields();ids=[]
 for i in range(reader.maxDoc()):
  d=stored.document(i);ids.append(str(d.get('class_fqn'))+'#'+str(d.get('signature')))
 result={'document_count':reader.numDocs(),'identifiers':sorted(set(ids))};reader.close();directory.close();return result
if __name__=='__main__':
 ap=argparse.ArgumentParser();ap.add_argument('--dataset',default=str(ROOT/'scratch/extract-check'));ap.add_argument('--output',default=str(ROOT/'scratch/extract-check/project_index'));args=ap.parse_args();dataset=pathlib.Path(args.dataset);out=pathlib.Path(args.output)
 if ROOT/'scratch' not in out.resolve().parents:raise ValueError('output must be scratch-only')
 for sub in ['json','codegraph','lucene']:(out/sub).mkdir(parents=True,exist_ok=True)
 start=time.monotonic();start_jvm()
 # Source inspection shows PreProcessor enumerates project directories and appends json/codegraph itself.
 jpype.JClass('PreProcessor').main([str(dataset/'projects'),str(out)])
 sys.path.insert(0,str(ROOT/'code'));import procedure.preprocess_project as PP
 fs=types.SimpleNamespace(DATASET_PATH=str(dataset),CODE_INFO_PATH=str(out));PP.build_calling_graph(fs)
 errors=[]
 try:PP.extract_invoke_patterns(fs)
 except Exception:
  errors.append(traceback.format_exc());print(errors[-1])
 jpype.JClass('IndexBuilder').main(['group',str(out/'json'),str(out/'lucene')])
 result={'seconds':round(time.monotonic()-start,3),'errors':errors,'json':{}}
 authors=ROOT/'data/project_index'
 for f in [authors/'json/commons-cli.json',*sorted((authors/'codegraph').glob('commons-cli*.json'))]:
  rel=f.relative_to(authors);new=out/rel
  if not new.exists():result['json'][str(rel)]={'error':'missing generated file'};continue
  a=json.loads(f.read_text());b=json.loads(new.read_text());diff=compare(normalize(a),normalize(b));result['json'][str(rel)]={'identical_normalized':not diff,'differing_paths_count':len(diff),'differing_paths':diff,'author_top_level_entries':len(a),'generated_top_level_entries':len(b)}
 a=lucene(authors/'lucene/commons-cli');b=lucene(out/'lucene/commons-cli');result['lucene']={'authors':a,'generated':b,'identifiers_equal':a['identifiers']==b['identifiers']}
 (ROOT/'results/stage0-survey/extractor-check.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps({'seconds':result['seconds'],'json':{k:{a:b for a,b in v.items() if a!='differing_paths'} for k,v in result['json'].items()},'lucene_counts':[a['document_count'],b['document_count']],'identifiers_equal':result['lucene']['identifiers_equal']},indent=2));jpype.shutdownJVM()
