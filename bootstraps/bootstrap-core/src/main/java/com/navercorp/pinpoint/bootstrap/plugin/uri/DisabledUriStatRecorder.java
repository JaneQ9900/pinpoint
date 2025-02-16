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

package com.navercorp.pinpoint.bootstrap.plugin.uri;

/**
 * @author Taejin Koo
 */
public class DisabledUriStatRecorder implements UriStatRecorder {

    private static final UriStatRecorder INSTANCE = new DisabledUriStatRecorder();

    public static <T> UriStatRecorder<T> create() {
        return (UriStatRecorder<T>) INSTANCE;
    }

    @Override
    public void record(Object request, String rawUri, boolean status, long startTime, long endTime) {
        // do nothing
    }

}
