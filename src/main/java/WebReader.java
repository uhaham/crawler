import com.github.rholder.retry.*;
import org.jsoup.nodes.Document;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.google.common.net.HttpHeaders.USER_AGENT;

public class WebReader {

  private static final WaitStrategy UPDATE_WAIT_STRATEGY =
      WaitStrategies.fixedWait(Config.RETRIES_SLEEP_TIME_BETWEEN_UPDATE_SECONDS, TimeUnit.SECONDS);
  private static final StopStrategy UPDATE_STOP_STRATEGY =
      StopStrategies.stopAfterAttempt(Config.RETRIES_MAX_ALLOWED);

  private Document doc;

  public Document readWithRetry(final String url) throws ExecutionException, RetryException {
    doc = null;
    Callable<Boolean> read = () -> {
      doc = SSLHelper.getConnection(url).userAgent(USER_AGENT).get();
      return true;
    };

    final Retryer<Boolean> retryer =
        RetryerBuilder.<Boolean>newBuilder().retryIfExceptionOfType(Exception.class)
            .retryIfResult(success -> !success) // retry on failure
            .withStopStrategy(UPDATE_STOP_STRATEGY).withWaitStrategy(UPDATE_WAIT_STRATEGY).build();

    retryer.call(read);
    return doc;
  }


}
