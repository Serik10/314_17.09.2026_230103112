
import java.util.Random;

public class practice_task3 {

    static final long TOTAL_POINTS = 100_000_000L;


    static long runBenchmark(int numberOfThreads)
            throws InterruptedException {

        Thread[] threads =
                new Thread[numberOfThreads];

        // Each thread will have its own counter
        long[] localHits =
                new long[numberOfThreads];

        long pointsPerThread =
                TOTAL_POINTS / numberOfThreads;


        long startTime = System.nanoTime();


        // Create threads
        for (int i = 0; i < numberOfThreads; i++) {

            final int threadNumber = i;

            threads[i] = new Thread(() -> {

                Random random = new Random();

                // Local counter
                long hits = 0;


                for (long j = 0;
                     j < pointsPerThread;
                     j++) {

                    double x = random.nextDouble();
                    double y = random.nextDouble();


                    if (x * x + y * y <= 1.0) {

                        // Only this thread changes its counter
                        hits++;
                    }
                }


                // Save the result
                localHits[threadNumber] = hits;
            });

            threads[i].start();
        }


        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }


        // -------------------------------
        // REDUCTION
        // -------------------------------

        long totalHits = 0;

        for (int i = 0; i < numberOfThreads; i++) {

            totalHits += localHits[i];
        }


        long endTime = System.nanoTime();

        long runtime =
                (endTime - startTime) / 1_000_000;


        double pi =
                4.0 * totalHits / TOTAL_POINTS;


        System.out.printf(
                "%d threads | Runtime: %d ms | Pi: %.6f%n",
                numberOfThreads,
                runtime,
                pi
        );


        return runtime;
    }


    public static void main(String[] args)
            throws InterruptedException {


        System.out.println(
                "=== Part 3: OpenMP-Style Reduction ==="
        );

        System.out.println();


        // Required thread counts
        int[] threadCounts =
                {1, 2, 4, 8, 16, 32};


        long[] runtimes =
                new long[threadCounts.length];


        // Run benchmarks
        for (int i = 0; i < threadCounts.length; i++) {

            runtimes[i] =
                    runBenchmark(threadCounts[i]);
        }


        // -------------------------------
        // Calculate speedup and efficiency
        // -------------------------------

        long baseline = runtimes[0];


        System.out.println();
        System.out.println(
                "Threads | Runtime | Speedup | Efficiency"
        );


        for (int i = 0; i < threadCounts.length; i++) {

            int threads = threadCounts[i];

            double speedup =
                    (double) baseline / runtimes[i];

            double efficiency =
                    speedup / threads * 100;


            System.out.printf(
                    "%7d | %7d ms | %.2fx | %.2f%%%n",
                    threads,
                    runtimes[i],
                    speedup,
                    efficiency
            );
        }
    }
}
