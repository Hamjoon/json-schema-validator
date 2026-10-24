package renderer.fixture;
public abstract class Generic<T extends Number> {
  protected Generic(String... names) {}
  public abstract void accept(T... values);
  public void arrays(String[] values) {}
  public static final class Plain {}
  public enum Mode { FIRST, SECOND }
  public interface Contract { void run(String... values); class Nested {} }
}
