public class Main {

  public static void main(String[] args) {

    if (args.length != 1) {
      usage();
      System.exit(1);
    }

    final String fileName = args[0];
    final ConcurrentExecutorV2 concurrentExecutor = new ConcurrentExecutorV2();
    concurrentExecutor.run(fileName);

  }

  private static void usage() {
    System.err.println("Usage: crawler input_file_name");
  }
}
