/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.restconf.utils.parser.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.onosproject.restconf.utils.exceptions.JsonParseException;
import org.onosproject.restconf.utils.parser.api.JsonListener;
import org.onosproject.yms.ydt.YdtBuilder;
import org.onosproject.yms.ydt.YdtType;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Represents implementation of codec JSON listener.
 */
public class JsonToYdtListener implements JsonListener {

    private YdtBuilder ydtBuilder;

    public JsonToYdtListener(YdtBuilder ydtBuilder) {
        this.ydtBuilder = ydtBuilder;
    }

    @Override
    public void enterJsonNode(String fieldName, JsonNode node) {
        if (isNullOrEmpty(fieldName)) {
            return;
        }

        switch (node.getNodeType()) {
            case OBJECT:
                ydtBuilder.addChild(fieldName, null, YdtType.SINGLE_INSTANCE_NODE);
                break;
            case ARRAY:
                processArrayNode(fieldName, node);
                break;
            //TODO for now, just process the following three node type
            case STRING:
            case NUMBER:
            case BOOLEAN:
                ydtBuilder.addLeaf(fieldName, null, node.asText());
                break;
            case BINARY:
            case MISSING:
            case NULL:
            case POJO:
            default:
                throw new JsonParseException("Unsupported node type" + node.getNodeType()
                                                     + "filed name is %s" + fieldName);

        }

    }

    @Override
    public void exitJsonNode(JsonNodeType nodeType) {
        ydtBuilder.traverseToParent();
    }

    private void processArrayNode(String fieldName, JsonNode node) {
        ArrayNode arrayNode = (ArrayNode) node;
        Set<String> sets = new HashSet<>();
        Iterator<JsonNode> elements = arrayNode.elements();
        boolean isLeafList = true;
        while (elements.hasNext()) {
            JsonNode element = elements.next();
            JsonNodeType eleType = element.getNodeType();
            if (eleType == JsonNodeType.STRING ||
                    eleType == JsonNodeType.NUMBER
                    ) {
                sets.add(element.asText());
            } else {
                isLeafList = false;
            }
        }
        if (isLeafList) {
            ydtBuilder.addLeaf(fieldName, null, sets);
        } else {
            ydtBuilder.addChild(fieldName, null, YdtType.MULTI_INSTANCE_NODE);
        }
    }
}
