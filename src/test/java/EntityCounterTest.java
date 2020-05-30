import org.testng.Assert;
import org.testng.annotations.Test;

public class EntityCounterTest {

  private static final String KEY1 = "key1";
  private static final String KEY2 = "key2";

  @Test
  public void testIncrease() {
    final EntityCounter entityCounter = new EntityCounter();
    Assert.assertEquals(0, entityCounter.get(KEY1));
    entityCounter.increase(KEY1);
    Assert.assertEquals(1, entityCounter.get(KEY1));
    entityCounter.increase(KEY2);
    entityCounter.increase(KEY2);
    Assert.assertEquals(1, entityCounter.get(KEY1));
    Assert.assertEquals(2, entityCounter.get(KEY2));
  }

  @Test
  public void testIncreaseNullKey() {
    final EntityCounter entityCounter = new EntityCounter();
    entityCounter.increase(null);
  }

  @Test
  public void testCompareAndIncrease() {
    final EntityCounter entityCounter = new EntityCounter();
    Assert.assertTrue(entityCounter.compareAndIncrease(KEY1, 2));
    Assert.assertEquals(1, entityCounter.get(KEY1));
    Assert.assertTrue(entityCounter.compareAndIncrease(KEY1, 2));
    Assert.assertEquals(2, entityCounter.get(KEY1));
    Assert.assertFalse(entityCounter.compareAndIncrease(KEY1, 2));
    Assert.assertEquals(2, entityCounter.get(KEY1));
  }

  @Test
  public void testCompareAndIncreaseNull() {
    final EntityCounter entityCounter = new EntityCounter();
    Assert.assertFalse(entityCounter.compareAndIncrease(KEY1, null));
    Assert.assertEquals(0, entityCounter.get(KEY1));
  }

  @Test
  public void testDecrease() {
    final EntityCounter entityCounter = new EntityCounter();
    entityCounter.increase(KEY1);
    entityCounter.increase(KEY2);
    entityCounter.increase(KEY2);
    entityCounter.decrease(KEY1);
    Assert.assertEquals(0, entityCounter.get(KEY1));
    Assert.assertEquals(2, entityCounter.get(KEY2));
    entityCounter.decrease(KEY2);
    Assert.assertEquals(0, entityCounter.get(KEY1));
    Assert.assertEquals(1, entityCounter.get(KEY2));
  }

  @Test
  public void testDecreaseNegative() {
    final EntityCounter entityCounter = new EntityCounter();
    entityCounter.increase(KEY1);
    entityCounter.decrease(KEY1);
    entityCounter.decrease(KEY1);
    Assert.assertEquals(0, entityCounter.get(KEY1));
  }

  @Test
  public void testDecreaseNewKey() {
    final EntityCounter entityCounter = new EntityCounter();
    entityCounter.decrease(KEY1);
    Assert.assertEquals(0, entityCounter.get(KEY1));
  }

}
