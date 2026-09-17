import java.util.Random;

public class practice_task1 {

    static final long TOTAL_POINTS = 50_000_000L;

    // Shared variable
    static long totalHits = 0;

    public static void main(String[] args) throws InterruptedException {

        int numberOfThreads = 4;

        Thread[] threads = new Thread[numberOfThreads];

        long pointsPerThread = TOTAL_POINTS / numberOfThreads;

        long startTime = System.nanoTime();

        // Create 4 threads
        for (int i = 0; i < numberOfThreads; i++) {

            threads[i] = new Thread(() -> {

                Random random = new Random();

                for (long j = 0; j < pointsPerThread; j++) {

                    double x = random.nextDouble();
                    double y = random.nextDouble();

                    // Check if point is inside quarter circle
                    if (x * x + y * y <= 1.0) {

                        // DATA RACE
                        totalHits++;
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

        double pi = 4.0 * totalHits / TOTAL_POINTS;

        double runtime =
                (endTime - startTime) / 1_000_000_000.0;

        System.out.println("Total hits: " + totalHits);
        System.out.println("Pi: " + pi);
        System.out.println("Runtime: " + runtime + " seconds");
    }
}

