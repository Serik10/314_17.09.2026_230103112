# Advanced Parallel Programming & Architecture
## Laboratory Examination — Empirical Microarchitectural Bottlenecks

This README contains the answers and empirical results for Tasks 1–4.

> **Important:** The worksheet requires machine-specific empirical measurements. Values below are based on the benchmark results actually obtained during execution. Missing machine-specific measurements are marked as `REQUIRED`.

---

# Task 1 — Amdahl's Law & Physical Silicon Saturation

## Measured Results

| Workers | Time (s) | Observed Speedup |
|---:|---:|---:|
| 1 | 4.7554 | 1.11x |
| 2 | 3.4114 | 1.55x |
| 4 | 2.2211 | 2.38x |
| 8 | 2.2233 | 2.38x |
| 12 | 2.3439 | 2.26x |
| 16 | 2.6144 | 2.02x |

Single-process baseline printed by the benchmark:

```text
T(1) = 5.2878 s
```

OS reported logical cores:

```text
8
```

## Q1.1 — Identify the inflection point p*

**Answer:**

The inflection point is approximately **p\* = 8 workers**. Speedup improves up to 4 workers, but increasing from 4 to 8 workers produces essentially no additional speedup (2.38x to 2.38x). After 8 workers, performance degrades: speedup falls to 2.26x at 12 workers and 2.02x at 16 workers.

This indicates that the workload reaches its useful hardware parallelism limit around 8 logical workers. Additional workers introduce process-management overhead, scheduling overhead, and resource contention.

### Hardware topology

The benchmark reports 8 logical cores, but this output alone does not show how many are physical cores or SMT threads.

Run:

```cmd
wmic cpu get name,NumberOfCores,NumberOfLogicalProcessors
```

Then add the exact physical-core and logical-thread counts here.

**Hardware topology: REQUIRED**

## Q1.2 — Calculate the parallel fraction P

Using:

```text
T(1) = 5.2878 s
T(4) = 2.2211 s
```

First calculate the measured speedup:

\[
S(4) = \frac{T(1)}{T(4)}
\]

\[
S(4) = \frac{5.2878}{2.2211} \approx 2.3807
\]

Amdahl's Law:

\[
S(4)=\frac{1}{(1-P)+P/4}
\]

Take the reciprocal:

\[
\frac{1}{S(4)}=1-P+\frac{P}{4}
\]

\[
\frac{1}{S(4)}=1-\frac{3P}{4}
\]

Therefore:

\[
P=\frac{4}{3}\left(1-\frac{1}{S(4)}\right)
\]

\[
P \approx 0.773277
\]

### Answer

\[
\boxed{P \approx 0.7733 = 77.33\%}
\]

Therefore, the estimated serial fraction is:

\[
1-P \approx 0.2267 = 22.67\%
\]

## Q1.3 — Calculate the asymptotic maximum speedup

Amdahl's asymptotic limit is:

\[
S_{max}=\frac{1}{1-P}
\]

Using \(P=0.773277\):

\[
S_{max}=\frac{1}{1-0.773277}
\]

\[
\boxed{S_{max}\approx4.41x}
\]

### Explanation

Even with a very large number of cores, this workload cannot achieve unlimited speedup. Approximately 22.67% of the workload remains serial according to the measured Amdahl estimate.

Therefore, a 128-core server would not make this particular workload 128 times faster. Once the parallel portion is sufficiently accelerated, the serial portion becomes the main limitation.

---

# Task 2 — Cache Coherence & False Sharing

## Measured Result — Trial 1

```text
Adjacent Indices (False Sharing): 1.8018s
Padded Indices (Cache-Aligned): 1.7760s
Slowdown Factor: 1.01x
```

Exact ratio:

\[
\frac{1.8018}{1.7760}\approx1.0145x
\]

Rounded:

\[
\boxed{1.01x}
\]

The worksheet requires **3 separate runs** and the **median**.

| Layout | Trial 1 | Trial 2 | Trial 3 | Median |
|---|---:|---:|---:|---:|
| Adjacent / False Sharing | 1.8018 s | REQUIRED | REQUIRED | REQUIRED |
| Padded / Cache-Aligned | 1.7760 s | REQUIRED | REQUIRED | REQUIRED |

## Q2.1 — Calculate the exact memory footprint and explain stride 16

**Answer:**

In a 64-bit CPython process, a list element stores an 8-byte object reference.

\[
\boxed{8\text{ bytes}}
\]

The padded worker uses a stride of 16 elements:

\[
16\times8=128\text{ bytes}
\]

The worksheet uses a 64-byte cache line:

\[
128>64
\]

Therefore, two corresponding padded accesses are separated by 128 bytes, which is greater than one 64-byte cache line. They therefore cannot occupy the same 64-byte cache line.

### Final answer

**Pointer/reference size = 8 bytes.**  
**Stride-16 separation = 128 bytes.**  
Since 128 > 64, the accesses are separated by at least two cache-line widths.

## Q2.2 — Trace the MESI state transitions

**Answer:**

When both cores have a copy of the same cache line, the line may initially be in the **Shared (S)** state.

When Core 0 writes index 0, it needs exclusive ownership:

```text
Core 0: S -> M
Core 1: S -> I
```

Core 1's copy is invalidated.

When Core 1 writes index 1, it must obtain ownership:

```text
Core 1: I -> M
Core 0: M -> I
```

If Core 0 writes again:

```text
Core 0: I -> M
Core 1: M -> I
```

The cache line therefore repeatedly moves between the cores. This creates coherence traffic and stalls even though the threads modify different logical variables. This is **false sharing**.

## Q2.3 — Two industry-standard mechanisms

**Answer:**

1. **C++11 `alignas(64)`** — aligns frequently modified data to a 64-byte boundary.
2. **Java `@Contended`** — allows the JVM to separate fields that could otherwise suffer from false sharing.

Another valid technique is explicit padding, for example padding fields in a Go struct.

---

# Task 3 — Synchronization Tax & Lockless Redesign

## Measured Results

```text
Unsafe: Value = 2,000,000 / 2,000,000 | Time: 0.2189s
Locked: Value = 2,000,000 / 2,000,000 | Time: 0.7676s
Contention Cost Multiplier: 3.51x
```

## Q3.1 — Analyze the Unsafe Accumulator

**Answer:**

The observed Unsafe Accumulator value was:

\[
\boxed{2,000,000}
\]

The expected value was also:

\[
2,000,000
\]

Therefore, the observed corruption in this particular run was:

\[
\boxed{0}
\]

So this run did **not** reproduce the race-condition corruption.

Conceptually, `self.val += 1` is a read-modify-write sequence:

```text
LOAD -> ADD -> STORE
```

For example, two threads may both load 100, both calculate 101, and both store 101. One update is then lost, producing 101 instead of 102. This is called a **lost-update race condition**.

### Final empirical result

```text
Unsafe value = 2,000,000
Data corruption defect count = 0
```

## Q3.2 — Lockless Thread-Local Accumulator

The required approach is to give every thread its own private accumulator and perform one reduction after all threads finish.

### Implementation

```python
import threading
import time

TOTAL_OPS = 2_000_000
NUM_THREADS = 4


def bench_lockless():
    results = [0] * NUM_THREADS

    def work(thread_id):
        ops_per_thread = TOTAL_OPS // NUM_THREADS
        local_sum = 0

        for _ in range(ops_per_thread):
            local_sum += 1

        results[thread_id] = local_sum

    threads = []

    start = time.perf_counter()

    for i in range(NUM_THREADS):
        t = threading.Thread(target=work, args=(i,))
        threads.append(t)
        t.start()

    for t in threads:
        t.join()

    total = sum(results)
    elapsed = time.perf_counter() - start

    return total, elapsed


if __name__ == "__main__":
    value, elapsed = bench_lockless()

    print(f"Lockless: Value = {value:,} / {TOTAL_OPS:,}")
    print(f"Time: {elapsed:.4f}s")
```

Correctness:

\[
4\times500,000=2,000,000
\]

The required speedup is:

\[
\text{Speedup}=\frac{T_{locked}}{T_{lockless}}
\]

With the measured locked time:

\[
\text{Speedup}=\frac{0.7676}{T_{lockless}}
\]

To achieve the required 2.0x speedup:

\[
T_{lockless}\le0.3838s
\]

### Actual lockless benchmark

**REQUIRED — run the implementation and add the measured time here.**

```text
Lockless value: REQUIRED
Lockless time: REQUIRED
Lockless speedup over LockedCounter: REQUIRED
```

## Q3.3 — Why eliminate shared mutable state?

**Answer:**

Eliminating shared mutable state is fundamentally superior because threads no longer compete for a common synchronization point. Each thread performs its work on private state, avoiding lock acquisition, lock contention, and repeated synchronization.

After all threads finish, their independent results are combined in a final reduction step.

Optimizing a lock can reduce its overhead, but the lock remains a shared bottleneck. Thread-local partitioning removes that bottleneck from the main computation.

---

# Task 4 — Memory Wall vs. Compute Saturation

## Measured Results

### Compute-Bound Suite

| Workers | Time (s) | Scaling ratio \(T_w/T_1\) |
|---:|---:|---:|
| 1 | 1.2715 | 1.0000x |
| 2 | 1.4900 | 1.1718x |
| 4 | 1.9408 | 1.5264x |

### Memory-Bound Suite

| Workers | Time (s) | Degradation ratio \(T_w/T_1\) |
|---:|---:|---:|
| 1 | 0.7314 | 1.0000x |
| 2 | 0.8570 | 1.1717x |
| 4 | 1.4091 | 1.9266x |

## Q4.1 — Analyze the scaling divergence

**Answer:**

The compute-bound workload mainly consumes CPU execution resources, while the memory-bound workload depends heavily on the shared DRAM memory subsystem.

For the compute workload:

\[
\frac{1.4900}{1.2715}\approx1.1718x
\]

at 2 workers, and:

\[
\frac{1.9408}{1.2715}\approx1.5264x
\]

at 4 workers.

For the memory workload:

\[
\frac{0.8570}{0.7314}\approx1.1717x
\]

at 2 workers, and:

\[
\frac{1.4091}{0.7314}\approx1.9266x
\]

at 4 workers.

In the actual measurement, **both workloads became slower when additional worker processes were added**. The memory-bound workload degraded especially strongly, which is consistent with contention for the shared memory subsystem and DRAM bandwidth.

The compute benchmark also failed to scale on this host, showing that additional Python processes were not beneficial for this particular workload and hardware/runtime combination.

## Q4.2 — Host memory specifications and theoretical peak bandwidth

The exact machine-specific memory information was not provided in the benchmark output.

**REQUIRED:**

```text
Memory type: REQUIRED
Memory speed: REQUIRED
Channel configuration: REQUIRED
Theoretical peak bandwidth: REQUIRED
```

Windows command from the worksheet:

```cmd
wmic memorychip get speed,capacity,devicelocator
```

Useful CPU topology command:

```cmd
wmic cpu get name,NumberOfCores,NumberOfLogicalProcessors
```

For standard 64-bit memory channels:

\[
\text{Bandwidth per channel}\approx\text{MT/s}\times8\text{ bytes}
\]

Example only:

```text
DDR4-3200 single-channel = 25.6 GB/s
DDR4-3200 dual-channel   = 51.2 GB/s
```

Do not use these example values unless they match the actual system.

## Q4.3 — CUDA or CPU OpenMP?

**Answer:**

If the workload is already saturated by CPU DRAM bandwidth, adding more CPU parallelism with OpenMP is unlikely to provide a large additional speedup because the same shared memory subsystem remains the bottleneck.

A CUDA implementation may provide more benefit if the GPU has substantially greater memory bandwidth and the data can remain on the GPU long enough to justify host-device transfer costs.

Therefore, the architectural choice should target the actual bottleneck: **memory bandwidth**, rather than simply increasing CPU thread count.

---

# Final Results Summary

## Task 1

```text
Inflection point:      p* ≈ 8 workers
Parallel fraction P:   0.7733 = 77.33%
Serial fraction:       22.67%
Amdahl S_max:          ≈ 4.41x
Logical cores:         8
```

## Task 2

```text
Adjacent:              1.8018 s  (Trial 1)
Padded:                1.7760 s  (Trial 1)
Slowdown:              1.01x     (Trial 1)
Reference size:        8 bytes
Stride-16 spacing:     128 bytes
Cache line:            64 bytes
```

Trials 2–3 and median are still required by the worksheet.

## Task 3

```text
Unsafe value:          2,000,000
Unsafe time:            0.2189 s
Locked value:          2,000,000
Locked time:            0.7676 s
Contention multiplier:  3.51x
Observed corruption:   0
```

Lockless timing and speedup are still required.

## Task 4

```text
Compute, 1 worker:     1.2715 s
Compute, 2 workers:    1.4900 s
Compute, 4 workers:    1.9408 s

Memory, 1 worker:      0.7314 s
Memory, 2 workers:     0.8570 s
Memory, 4 workers:     1.4091 s
```

Memory hardware specifications and theoretical peak bandwidth are still required.

---

# Commands for Remaining Measurements

### CPU topology

```cmd
wmic cpu get name,NumberOfCores,NumberOfLogicalProcessors
```

### Memory specification

```cmd
wmic memorychip get speed,capacity,devicelocator
```

### Task 2

Run the Task 2 benchmark **two more times** to complete Trial 2 and Trial 3.

### Task 3

Run the lockless implementation above and record:

```text
Lockless: Value = ...
Time: ...
```

