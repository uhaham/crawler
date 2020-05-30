public class Config {

  // worker settings
  public static final int NUMBER_OF_CONCURRENT_WORKERS = 10;
  public static final int WORKER_MAX_CONCURRENT_PAGES_PER_DOMAIN = 2;
  public static final long WAIT_TIME_READING_FROM_QUEUE_MILLIS = 1000L;

  // retries settings
  public static final int RETRIES_MAX_ALLOWED = 3;
  public static final long RETRIES_SLEEP_TIME_BETWEEN_UPDATE_SECONDS = 5;

}

