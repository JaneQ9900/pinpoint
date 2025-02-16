/*
 * Copyright 2020 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.profiler.context.storage;

import com.navercorp.pinpoint.common.profiler.clock.Clock;
import com.navercorp.pinpoint.common.profiler.logging.ThrottledLogger;
import com.navercorp.pinpoint.common.util.Assert;
import com.navercorp.pinpoint.common.util.CollectionUtils;
import com.navercorp.pinpoint.profiler.monitor.metric.uri.AgentUriStatData;
import com.navercorp.pinpoint.profiler.monitor.metric.uri.UriStatInfo;
import com.navercorp.pinpoint.profiler.sender.AsyncQueueingExecutor;
import com.navercorp.pinpoint.profiler.sender.AsyncQueueingExecutorListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Taejin Koo
 */
public class AsyncQueueingUriStatStorage extends AsyncQueueingExecutor<UriStatInfo> implements UriStatStorage {

    private static final Logger LOGGER = LogManager.getLogger(AsyncQueueingUriStatStorage.class);
    private static final ThrottledLogger TLogger = ThrottledLogger.getLogger(LOGGER, 100);

    private final ExecutorListener executorListener;

    public AsyncQueueingUriStatStorage(int queueSize, int uriStatDataLimitSize, String executorName) {
        this(queueSize, executorName, new ExecutorListener(uriStatDataLimitSize));
    }

    public AsyncQueueingUriStatStorage(int queueSize, int uriStatDataLimitSize, String executorName, int collectInterval) {
        this(queueSize, executorName, new ExecutorListener(uriStatDataLimitSize, collectInterval));
    }

    private AsyncQueueingUriStatStorage(int queueSize, String executorName, ExecutorListener executorListener) {
        super(queueSize, executorName, executorListener);
        this.executorListener = executorListener;
    }

    @Override
    public void store(String uri, boolean status, long startTime, long endTime) {
        Objects.requireNonNull(uri, "uri");
        UriStatInfo uriStatInfo = new UriStatInfo(uri, status, startTime, endTime);
        execute(uriStatInfo);
    }

    @Override
    public AgentUriStatData poll() {
        return executorListener.pollCompletedData();
    }

    @Override
    public void close() {
        stop();
    }

    @Override
    protected void pollTimeout(long timeout) {
        executorListener.executePollTimeout();
    }

    static class ExecutorListener implements AsyncQueueingExecutorListener<UriStatInfo> {

        private static final int DEFAULT_COLLECT_INTERVAL = 30000; // 30s

        private static final int SNAPSHOT_LIMIT = 4;

        private final Clock clock;
        private final Queue<AgentUriStatData> snapshotQueue;

        private final Snapshot<AgentUriStatData> snapshotManager;


        public ExecutorListener(int uriStatDataLimitSize) {
            this(uriStatDataLimitSize, DEFAULT_COLLECT_INTERVAL);
        }

        public ExecutorListener(int uriStatDataLimitSize, int collectInterval) {
            Assert.isTrue(uriStatDataLimitSize > 0, "uriStatDataLimitSize must be ' > 0'");

            Assert.isTrue(collectInterval > 0, "collectInterval must be ' > 0'");
            this.clock = Clock.tick(collectInterval);

            this.snapshotQueue = new ConcurrentLinkedQueue<>();
            this.snapshotManager = new Snapshot<>(value -> new AgentUriStatData(value, uriStatDataLimitSize), AgentUriStatData::getBaseTimestamp);
        }

        @Override
        public void execute(Collection<UriStatInfo> messageList) {
            final long currentBaseTimestamp = clock.millis();
            checkAndFlushOldData(currentBaseTimestamp);

            AgentUriStatData agentUriStatData = snapshotManager.getCurrent(currentBaseTimestamp);

            Object[] dataList = messageList.toArray();
            for (int i = 0; i < CollectionUtils.nullSafeSize(messageList); i++) {
                addUriData(agentUriStatData, (UriStatInfo) dataList[i]);
            }
        }

        @Override
        public void execute(UriStatInfo message) {
            long currentBaseTimestamp = clock.millis();
            checkAndFlushOldData(currentBaseTimestamp);

            AgentUriStatData agentUriStatData = snapshotManager.getCurrent(currentBaseTimestamp);
            addUriData(agentUriStatData, message);
        }

        private void addUriData(AgentUriStatData agentUriStatData, UriStatInfo uriStatInfo) {
            if (!agentUriStatData.add(uriStatInfo)) {
                TLogger.info("Too many URI pattern. sample-message:{}, capacity:{}, counter:{} ", uriStatInfo, agentUriStatData.getCapacity(), TLogger.getCounter());
            }
        }

        public void executePollTimeout() {
            long currentBaseTimestamp = clock.millis();
            boolean flush = checkAndFlushOldData(currentBaseTimestamp);
            if (flush) {
                LOGGER.debug("checkAndFlushOldData {}", flush);
            }
        }


        private boolean checkAndFlushOldData(long currentBaseTimestamp) {
            final AgentUriStatData snapshot = snapshotManager.takeSnapshot(currentBaseTimestamp);
            if (snapshot != null) {
                addCompletedData(snapshot);
                return true;
            }
            return false;
        }


        private void addCompletedData(AgentUriStatData agentUriStatData) {
            // Thread safety : single consumer
            final int size = snapshotQueue.size();
            if (size > SNAPSHOT_LIMIT) {
                // Prevent OOM. Discard old history
                drainN(size - SNAPSHOT_LIMIT);
            }
            snapshotQueue.offer(agentUriStatData);
        }

        private void drainN(int drainSize) {
            for (int i = 0; i < drainSize; i++) {
                snapshotQueue.poll();
            }
        }

        private AgentUriStatData pollCompletedData() {
            return snapshotQueue.poll();
        }

    }

}