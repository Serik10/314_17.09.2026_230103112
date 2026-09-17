# Java Multithreading Benchmark

This project demonstrates three approaches to multithreading in Java:

1. Part 1 — Data Race
2. Part 2 — Synchronization with `AtomicLong`
3. Part 3 — OpenMP-Style Reduction

---

## Part 1 — The Phantom Bug

The program generates 50,000,000 random `(x, y)` points inside a `1 x 1` square and uses 4 Java threads.

All threads update the same shared variable:

```java
static long totalHits = 0;
```

The counter is updated using:

```java
totalHits++;
```

Pi is approximated using:

```text
Pi ≈ 4 × hits / total
```

### Results

| Run | Total Hits | Pi | Runtime |
|---:|---:|---:|---:|
| 1 | 19,892,648 | 1.59141184 | 1.4877 s |
| 2 | 19,597,764 | 1.56782112 | 1.5528 s |
| 3 | 19,597,764 | 1.56782112 | 1.5528 s |
| 4 | 19,615,778 | 1.56926224 | 1.4934 s |
| 5 | 19,415,173 | 1.55321384 | 1.5206 s |

### Explanation

The calculated value of Pi is incorrect because all 4 threads modify the same shared variable at the same time.

The operation `totalHits++` is not atomic. Multiple threads can read and modify `totalHits` simultaneously, causing some updates to be lost.

This is called a **data race**.

---

## Part 2 — The Synchronization Trap

The race condition from Part 1 was fixed using `AtomicLong`.

The counter is updated with:

```java
totalHits.incrementAndGet();
```

`AtomicLong` provides a thread-safe way to update the shared counter.

### Results

| Version | Pi | Runtime |
|---|---:|---:|
| Single-threaded | 3.14148376 | 2.3111 s |
| 4 threads + AtomicLong | 3.14149648 | 1.2287 s |

### Speedup

```text
Speedup = 2.3111425 / 1.2286983 ≈ 1.88x
```

Both versions produced an accurate value of Pi, approximately 3.1415.

In this benchmark, the 4-thread AtomicLong version was faster than the single-threaded version. However, the shared AtomicLong introduces synchronization overhead because multiple threads access the same counter.

---

## Part 3 — OpenMP-Style Reduction

In Part 3, each thread uses its own local counter instead of updating a shared counter.

Each thread counts its own hits independently. After all threads finish, the local results are combined into one total.

This approach is called **reduction**.

The benchmark uses 100,000,000 iterations with:

- 1 thread
- 2 threads
- 4 threads
- 8 threads
- 16 threads
- 32 threads

### Results

| Threads | Runtime (ms) | Speedup | Efficiency |
|---:|---:|---:|---:|
| 1 | 4746 | 1.00x | 100.00% |
| 2 | 2415 | 1.97x | 98.26% |
| 4 | 1283 | 3.70x | 92.48% |
| 8 | 686 | 6.92x | 86.48% |
| 16 | 739 | 6.42x | 40.14% |
| 32 | 723 | 6.56x | 20.51% |

### Pi Results

| Threads | Pi |
|---:|---:|
| 1 | 3.141705 |
| 2 | 3.141507 |
| 4 | 3.141652 |
| 8 | 3.141425 |
| 16 | 3.141528 |
| 32 | 3.141742 |

All Pi values are close to the expected value of approximately 3.14159.

### Speedup Formula

```text
Speedup = T1 / Tn
```

Where:

- `T1` = runtime with 1 thread
- `Tn` = runtime with n threads

### Efficiency Formula

```text
Efficiency = (Speedup / Number of Threads) × 100%
```

---

# Questions

## 1. Why didn't the 8-core CPU run twice as fast as 8 threads?

The benchmark produced:

```text
8 threads  → 686 ms
16 threads → 739 ms
32 threads → 723 ms
```

The runtime actually increased when moving from 8 to 16 threads.

An 8-core CPU has a limited number of physical cores. With 8 threads, the available cores can be used effectively. With 16 or 32 threads, multiple threads have to share the available CPU resources.

Thread scheduling, context switching, and other overhead prevent performance from scaling linearly.

Therefore, using more threads does not necessarily make the program faster.

## 2. Why was the synchronized version in Part 2 slower than running on one single core?

In this experiment, the 4-thread AtomicLong version was actually faster than the single-threaded version.

```text
Single-threaded:        2.3111 s
4 threads + AtomicLong: 1.2287 s
```

The AtomicLong version was faster because the work was divided between 4 threads.

However, using a shared AtomicLong introduces synchronization overhead because multiple threads access the same counter. If this overhead becomes large enough, it can reduce or eliminate the performance benefit of multiple threads.

---

# Conclusion

The benchmark demonstrates three different approaches to parallel programming:

- A shared non-synchronized counter can cause a **data race** and produce incorrect results.
- `AtomicLong` fixes the race condition and produces an accurate result, but introduces synchronization overhead.
- Using **local counters and reduction** avoids frequent shared-state updates and provides better scalability.

In this experiment, performance improved as the number of threads increased up to 8 threads. Increasing the number of threads to 16 and 32 did not improve the runtime further.
