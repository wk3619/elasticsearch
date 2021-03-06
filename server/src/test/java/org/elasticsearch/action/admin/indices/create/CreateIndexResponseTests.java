/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.action.admin.indices.create;

import org.elasticsearch.Version;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.test.AbstractStreamableXContentTestCase;
import org.elasticsearch.test.EqualsHashCodeTestUtils;

import java.io.IOException;

public class CreateIndexResponseTests extends AbstractStreamableXContentTestCase<CreateIndexResponse> {

    @Override
    protected CreateIndexResponse createTestInstance() {
        boolean acknowledged = randomBoolean();
        boolean shardsAcknowledged = acknowledged && randomBoolean();
        String index = randomAlphaOfLength(5);
        return new CreateIndexResponse(acknowledged, shardsAcknowledged, index);
    }

    @Override
    protected CreateIndexResponse createBlankInstance() {
        return new CreateIndexResponse();
    }

    @Override
    protected EqualsHashCodeTestUtils.MutateFunction<CreateIndexResponse> getMutateFunction() {
        return response -> {
            if (randomBoolean()) {
                if (randomBoolean()) {
                    boolean acknowledged = response.isAcknowledged() == false;
                    boolean shardsAcknowledged = acknowledged && response.isShardsAcknowledged();
                    return new CreateIndexResponse(acknowledged, shardsAcknowledged, response.index());
                } else {
                    boolean shardsAcknowledged = response.isShardsAcknowledged() == false;
                    boolean acknowledged = shardsAcknowledged || response.isAcknowledged();
                    return new CreateIndexResponse(acknowledged, shardsAcknowledged, response.index());
                }
            } else {
                return new CreateIndexResponse(response.isAcknowledged(), response.isShardsAcknowledged(),
                            response.index() + randomAlphaOfLengthBetween(2, 5));
            }
        };
    }

    @Override
    protected CreateIndexResponse doParseInstance(XContentParser parser) {
        return CreateIndexResponse.fromXContent(parser);
    }

    public void testSerializationWithOldVersion() throws IOException {
        Version oldVersion = Version.V_5_4_0;
        CreateIndexResponse response = new CreateIndexResponse(true, true, "foo");

        try (BytesStreamOutput output = new BytesStreamOutput()) {
            output.setVersion(oldVersion);
            response.writeTo(output);

            try (StreamInput in = output.bytes().streamInput()) {
                in.setVersion(oldVersion);
                CreateIndexResponse serialized = new CreateIndexResponse();
                serialized.readFrom(in);
                assertEquals(response.isShardsAcknowledged(), serialized.isShardsAcknowledged());
                assertEquals(response.isAcknowledged(), serialized.isAcknowledged());
                assertNull(serialized.index());
            }
        }
    }

    public void testToXContent() {
        CreateIndexResponse response = new CreateIndexResponse(true, false, "index_name");
        String output = Strings.toString(response);
        assertEquals("{\"acknowledged\":true,\"shards_acknowledged\":false,\"index\":\"index_name\"}", output);
    }

    public void testToAndFromXContentIndexNull() throws IOException {
        CreateIndexResponse response = new CreateIndexResponse(true, false, null);
        String output = Strings.toString(response);
        assertEquals("{\"acknowledged\":true,\"shards_acknowledged\":false,\"index\":null}", output);
        try (XContentParser parser = createParser(JsonXContent.jsonXContent, output)) {
            CreateIndexResponse parsedResponse = CreateIndexResponse.fromXContent(parser);
            assertNull(parsedResponse.index());
            assertTrue(parsedResponse.isAcknowledged());
            assertFalse(parsedResponse.isShardsAcknowledged());
        }
    }
}
