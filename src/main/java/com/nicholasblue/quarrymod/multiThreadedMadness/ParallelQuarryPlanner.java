package com.nicholasblue.quarrymod.multiThreadedMadness;

import com.nicholasblue.quarrymod.data.QuarryBlockData;
import com.nicholasblue.quarrymod.data.QuarryRuntimeState;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;


public final class ParallelQuarryPlanner {
    private final ExecutorService pool;
    private final int threadCount;

    public ParallelQuarryPlanner(int threads) {
        this.threadCount = threads;
        this.pool = Executors.newFixedThreadPool(threads);
    }

    /**
     * Plan one tick's worth of work for all quarries, using dynamic load distribution.
     * This method assumes:
     *  - configs and runtimes are frozen for the tick.
     *  - no other thread mutates them during planning.
     *  - all planner threads complete before any world/state mutation occurs.
     */
    public List<QuarryAction> planAll(
            Long2ObjectOpenHashMap<QuarryBlockData> configs,
            Long2ObjectOpenHashMap<QuarryRuntimeState> runtimes, int minY

    ) {
        BlockingQueue<Long> queue = new LinkedBlockingQueue<>();
        queue.addAll(configs.keySet());

        List<Future<List<QuarryAction>>> futures = new ArrayList<>(threadCount);

        for (int t = 0; t < threadCount; t++) {
            futures.add(pool.submit(() -> {
                List<QuarryAction> localActions = new ArrayList<>();
                Long key;
                while ((key = queue.poll()) != null) {
                    QuarryBlockData config = configs.get(key);
                    QuarryRuntimeState runtime = runtimes.get(key);

                    if (config == null || runtime == null || !runtime.isRunning()) continue;

                    QuarryAction action = planQuarryTick(key, config, runtime, minY);
                    if (action != null) localActions.add(action);
                }
                return localActions;
            }));
        }

        List<QuarryAction> allActions = new ArrayList<>();
        for (Future<List<QuarryAction>> future : futures) {
            try {
                allActions.addAll(future.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Planner thread interrupted", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("Planner thread failed", e.getCause());
            }
        }

        return allActions;
    }

    private QuarryAction planQuarryTick(long key, QuarryBlockData config, QuarryRuntimeState runtime, int minY) {
        int progress = runtime.getProgressCounter();
        int dx = progress % config.xSize;
        int dz = progress / config.xSize;

        if (dz >= config.zSize) {
            int nextY = runtime.getCurrentY() - 1;
            boolean stop = nextY < minY;

            //System.out.printf("[Planner] Quarry %d layer complete at Y=%d; nextY=%d; stop=%s\n", key, runtime.getCurrentY(), nextY, stop);

            return new QuarryAction(key, null, false, true, Math.max(nextY, minY), stop);
        }

        BlockPos target = new BlockPos(
                config.origin.getX() + dx,
                runtime.getCurrentY(),
                config.origin.getZ() + dz
        );

        return new QuarryAction(key, target, true, false, runtime.getCurrentY(), false);
    }

    public void shutdown() {
        pool.shutdown();
    }
}

