"""Smoke-check the prescribed Mockito/Jupiter runtime independently of generated tests."""
from start import EXP,RESULTS,write_json
from run_commit import run_file

source=EXP/'work/runner-smoke/MockitoCheck.java'
source.parent.mkdir(parents=True,exist_ok=True)
source.write_text('''import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
@ExtendWith(MockitoExtension.class)
public class MockitoCheck {
  @Mock List<String> values;
  @Test void stubbing() { when(values.size()).thenReturn(2); assertEquals(2, values.size()); }
}
''')
filename=str(source.relative_to(EXP))
row=dict(file=filename,**{'class':'MockitoCheck'},method='stubbing',id='runner-smoke|MockitoCheck|stubbing')
result=run_file(0,'ktester',filename,[row],EXP/'work/runner-smoke/execution',retry=False)
write_json(RESULTS/'runner-mockito-check.json',result)
assert result['methods'][0]['state']=='pass',result
print('Mockito Jupiter extension, Byte Buddy attachment, stubbing, and JUnit execution passed.')
