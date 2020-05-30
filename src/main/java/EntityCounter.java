import com.google.common.collect.Maps;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class EntityCounter {

  Map<String, AtomicInteger> entityCounterMap = Maps.newHashMap();

  public void increase(final String entity) {
    synchronized (this) {
      if (entityCounterMap.containsKey(entity)) {
        entityCounterMap.get(entity).incrementAndGet();
      } else {
        entityCounterMap.put(entity, new AtomicInteger(1));
      }
    }
  }

  public boolean compareAndIncrease(final String entity, final Integer compareTo) {
    synchronized (this) {
      if (compareTo == null || get(entity) >= compareTo) {
        return false;
      }
      increase(entity);
      return true;
    }
  }

  public void decrease(final String entity) {
    synchronized (this) {
      if (entityCounterMap.containsKey(entity)) {
        if (entityCounterMap.get(entity).get() > 0) {
          entityCounterMap.get(entity).decrementAndGet();
        }
      }
    }
  }

  protected int get(final String entity) {
    if (entity == null || !entityCounterMap.containsKey(entity)) {
      return 0;
    } else {
      return entityCounterMap.get(entity).get();
    }
  }

  protected void clear() {
    entityCounterMap.clear();
  }
}
