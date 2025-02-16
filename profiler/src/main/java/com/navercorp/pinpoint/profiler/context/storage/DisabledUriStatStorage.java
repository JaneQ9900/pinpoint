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

import com.navercorp.pinpoint.profiler.monitor.metric.uri.AgentUriStatData;

/**
 * @author Taejin Koo
 */
public class DisabledUriStatStorage implements UriStatStorage {

    @Override
    public void store(String uri, boolean status, long startTime, long endTime) {
        // Do nothing
    }

    @Override
    public AgentUriStatData poll() {
        return null;
    }

    @Override
    public void close() {
    }

}
