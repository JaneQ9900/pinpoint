/*
 * Copyright 2022 NAVER Corp.
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

package com.navercorp.pinpoint.collector.service;

import com.navercorp.pinpoint.common.server.bo.stat.AgentUriStatBo;

import com.navercorp.pinpoint.common.server.bo.stat.EachUriStatBo;
import com.navercorp.pinpoint.common.server.bo.stat.UriStatHistogram;
import com.navercorp.pinpoint.metric.collector.MetricAppPropertySources;
import com.navercorp.pinpoint.metric.collector.dao.UriStatDao;
import com.navercorp.pinpoint.metric.common.model.UriStat;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Objects;

@Service
@ComponentScan("com.navercorp.pinpoint.metric.collector.dao.pinot")
@ImportResource({"classpath:pinot-collector/applicationContext-collector-pinot.xml", "classpath:pinot-collector/servlet-context-collector-pinot.xml"})
@Import({MetricAppPropertySources.class})
public class PinotAgentUriStatService implements AgentUriStatService {

    private final int BUCKET_SIZE = 8;
    private final int[] EMPTY_BUCKETS = new int[BUCKET_SIZE];
    private final UriStatDao uriStatDao;

    public PinotAgentUriStatService(UriStatDao uriStatDao) {
        this.uriStatDao = Objects.requireNonNull(uriStatDao, "uriStatDao");
    }

    @Override
    public void save(AgentUriStatBo agentUriStatBo) {
        ArrayList<UriStat> data = new ArrayList<>();
        final String serviceName = agentUriStatBo.getServiceName();
        final String applicationName = agentUriStatBo.getApplicationName();
        final String agentId = agentUriStatBo.getAgentId();
        final int version = agentUriStatBo.getBucketVersion();

        for (EachUriStatBo eachUriStatBo : agentUriStatBo.getEachUriStatBoList()) {
            final String uri = eachUriStatBo.getUri();
            final long timestamp = eachUriStatBo.getTimestamp();
            final UriStatHistogram totalHistogram = eachUriStatBo.getTotalHistogram();
            final UriStatHistogram failureHistogram = eachUriStatBo.getFailedHistogram();
            data.add(new UriStat(timestamp, serviceName, applicationName, agentId, uri, totalHistogram.getCount(), totalHistogram.getMax(),
                    totalHistogram.getTotal(), getHistogramArray(totalHistogram), getHistogramArray(failureHistogram), version));
        }
        uriStatDao.insert(data);
    }

    public int[] getHistogramArray(UriStatHistogram histogram) {
        if (histogram != null) {
            return histogram.getTimestampHistogram();
        }
        return EMPTY_BUCKETS;
    }

}
