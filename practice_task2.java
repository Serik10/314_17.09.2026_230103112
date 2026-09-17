
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class practice_task2 {

    static final long TOTAL_POINTS = 50_000_000L;

    // Thread-safe shared counter
    static AtomicLong totalHits = new AtomicLong(0);


    // -------------------------------
    // Single-threaded version
    // -------------------------------

    static void singleThread() {

        Random random = new Random();

        long hits = 0;

        long startTime = System.nanoTime();

        for (long i = 0; i < TOTAL_POINTS; i++) {

            double x = random.nextDouble();
            double y = random.nextDouble();

            if (x * x + y * y <= 1.0) {
                hits++;
            }
        }

        long endTime = System.nanoTime();

        double pi = 4.0 * hits / TOTAL_POINTS;

        double runtime =
                (endTime - startTime) / 1_000_000_000.0;

        System.out.println("Single-threaded:");
        System.out.println("Pi: " + pi);
        System.out.println("Runtime: " + runtime + " seconds");
    }


    // -------------------------------
    // 4-thread AtomicLong version
    // -------------------------------

    static void fourThreads() throws InterruptedException {

        totalHits.set(0);

        int numberOfThreads = 4;

        Thread[] threads = new Thread[numberOfThreads];

        long pointsPerThread =
                TOTAL_POINTS / numberOfThreads;

        long startTime = System.nanoTime();


        for (int i = 0; i < numberOfThreads; i++) {

            threads[i] = new Thread(() -> {

                Random random = new Random();

                for (long j = 0; j < pointsPerThread; j++) {

                    double x = random.nextDouble();
                    double y = random.nextDouble();

                    if (x * x + y * y <= 1.0) {

                        // Thread-safe increment
                        totalHits.incrementAndGet();
                    }
                }
            });

            threads[i].start();
        }


        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }


        long endTime = System.nanoTime();

        double pi =
                4.0 * totalHits.get() / TOTAL_POINTS;

        double runtime =
                (endTime - startTime) / 1_000_000_000.0;


        System.out.println();
        System.out.println("4 threads with AtomicLong:");
        System.out.println("Pi: " + pi);
        System.out.println("Runtime: " + runtime + " seconds");
    }


    public static void main(String[] args)
            throws InterruptedException {

        singleThread();

        fourThreads();
    }
}
