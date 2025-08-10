package igentuman.lazycrafter.util;

import igentuman.lazycrafter.LazyCrafter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple performance monitoring utility for tracking recipe processing performance
 */
public class PerformanceMonitor {
    private static final PerformanceMonitor INSTANCE = new PerformanceMonitor();
    
    private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> timings = new ConcurrentHashMap<>();
    
    public static PerformanceMonitor getInstance() {
        return INSTANCE;
    }
    
    private PerformanceMonitor() {}
    
    /**
     * Increment a counter
     */
    public void incrementCounter(String name) {
        counters.computeIfAbsent(name, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * Add timing measurement
     */
    public void addTiming(String name, long timeMs) {
        timings.computeIfAbsent(name, k -> new AtomicLong(0)).addAndGet(timeMs);
    }
    
    /**
     * Start timing an operation
     */
    public TimingContext startTiming(String name) {
        return new TimingContext(name);
    }
    
    /**
     * Get counter value
     */
    public long getCounter(String name) {
        AtomicLong counter = counters.get(name);
        return counter != null ? counter.get() : 0;
    }
    
    /**
     * Get total timing for an operation
     */
    public long getTiming(String name) {
        AtomicLong timing = timings.get(name);
        return timing != null ? timing.get() : 0;
    }
    
    /**
     * Log performance statistics
     */
    public void logStats() {
        LazyCrafter.logger.info("=== Performance Statistics ===");
        
        counters.forEach((name, value) -> {
            LazyCrafter.logger.info("Counter {}: {}", name, value.get());
        });
        
        timings.forEach((name, value) -> {
            long totalMs = value.get();
            long count = getCounter(name + "_count");
            if (count > 0) {
                LazyCrafter.logger.info("Timing {}: {}ms total, {}ms avg ({} operations)", 
                    name, totalMs, totalMs / count, count);
            } else {
                LazyCrafter.logger.info("Timing {}: {}ms total", name, totalMs);
            }
        });
    }
    
    /**
     * Reset all statistics
     */
    public void reset() {
        counters.clear();
        timings.clear();
    }
    
    /**
     * Context for timing operations
     */
    public class TimingContext implements AutoCloseable {
        private final String name;
        private final long startTime;
        
        public TimingContext(String name) {
            this.name = name;
            this.startTime = System.currentTimeMillis();
        }
        
        @Override
        public void close() {
            long duration = System.currentTimeMillis() - startTime;
            addTiming(name, duration);
            incrementCounter(name + "_count");
        }
    }
}