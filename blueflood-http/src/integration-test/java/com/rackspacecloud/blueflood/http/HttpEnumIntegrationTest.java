/*
 * Copyright 2013-2015 Rackspace
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.rackspacecloud.blueflood.http;

import com.rackspacecloud.blueflood.inputs.formats.JSONMetricsContainerTest;
import com.rackspacecloud.blueflood.inputs.handlers.wrappers.AggregatedPayload;
import com.rackspacecloud.blueflood.service.Configuration;
import com.rackspacecloud.blueflood.service.CoreConfig;
import com.rackspacecloud.blueflood.service.EnumValidator;
import com.rackspacecloud.blueflood.types.Locator;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

import static org.junit.Assert.assertTrue;

public class HttpEnumIntegrationTest extends HttpIntegrationTestBase {

    @Test
    public void testMetricIngestionWithEnum() throws Exception {

        // ingest and rollup metrics with enum values and verify CF points and elastic search indexes
        final String tenant_id = "333333";

        String prefix = getPrefix();
        final String metric_name = prefix + "enum_metric_test";

        Set<Locator> locators = new HashSet<Locator>();
        locators.add(Locator.createLocatorFromPathComponents(tenant_id, metric_name));

        // post enum metric for ingestion and verify
        HttpResponse response = postMetric(tenant_id, postAggregatedPath, "sample_enums_payload.json", prefix );
        assertEquals( "Should get status 200 from ingestion server for POST", 200, response.getStatusLine().getStatusCode() );
        EntityUtils.consume(response.getEntity());

        // execute EnumValidator
        EnumValidator enumValidator = new EnumValidator(locators);
        enumValidator.run();

        //Sleep for a while
        Thread.sleep(3000);

        // query for metric and assert results
        HttpResponse query_response = queryMetricIncludeEnum(tenant_id, metric_name);
        assertEquals( "Should get status 200 from query server for GET", 200, query_response.getStatusLine().getStatusCode() );

        // assert response content
        String responseContent = EntityUtils.toString(query_response.getEntity(), "UTF-8");
        assertEquals( String.format( "[{\"metric\":\"%s\",\"enum_values\":[\"v1\",\"v2\",\"v3\"]}]", metric_name ), responseContent );
        EntityUtils.consume(query_response.getEntity());
    }

    @Test
    public void testHttpEnumIngestionInvalidPastCollectionTime() throws IOException, URISyntaxException {

        long timestamp = System.currentTimeMillis() - TIME_DIFF - Configuration.getInstance().getLongProperty( CoreConfig.BEFORE_CURRENT_COLLECTIONTIME_MS );

        // ingest and rollup metrics with enum values and verify CF points and elastic search indexes
        final String tenant_id = "333333";

        String prefix = getPrefix();

        final String metric_name = prefix + "enum_metric_test";
        Set<Locator> locators = new HashSet<Locator>();
        locators.add(Locator.createLocatorFromPathComponents(tenant_id, metric_name));

        // post enum metric for ingestion and verify
        HttpResponse response = postMetric(tenant_id, postAggregatedPath, "sample_enums_payload.json", timestamp, prefix );

        String[] errors = getBodyArray( response );

        assertEquals( 400, response.getStatusLine().getStatusCode() );
        assertTrue( Pattern.matches( JSONMetricsContainerTest.PAST_COLLECTION_TIME_REGEX, errors[ 1 ] ) );
    }

    @Test
    public void testHttpEnumIngestionInvalidFutureCollectionTime() throws IOException, URISyntaxException {

        long timestamp = System.currentTimeMillis() + TIME_DIFF + Configuration.getInstance().getLongProperty( CoreConfig.AFTER_CURRENT_COLLECTIONTIME_MS );

        // ingest and rollup metrics with enum values and verify CF points and elastic search indexes
        final String tenant_id = "333333";

        String prefix = getPrefix();
        final String metric_name = prefix + "enum_metric_test";
        Set<Locator> locators = new HashSet<Locator>();
        locators.add(Locator.createLocatorFromPathComponents(tenant_id, metric_name));

        // post enum metric for ingestion and verify
        HttpResponse response = postMetric(tenant_id, postAggregatedPath, "sample_enums_payload.json", timestamp, prefix );

        String[] errors = getBodyArray( response );

        assertEquals( 400, response.getStatusLine().getStatusCode() );
        assertTrue( Pattern.matches( JSONMetricsContainerTest.FUTURE_COLLECTION_TIME_REGEX, errors[ 1 ] ) );
    }
}
