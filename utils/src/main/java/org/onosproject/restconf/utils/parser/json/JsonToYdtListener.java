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
import org.onosproject.yms.ydt.YdtContext;
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
    private static final String INPUT_FIELD_NAME = "input";
    private static final int INPUT_FIELD_LENGTH = 2;
    private YdtContext rpcModule;

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
                //for input, the filed name is something like
                String[] segments = fieldName.split(":");
                Boolean isLastInput = segments[INPUT_FIELD_LENGTH - 1].equals(INPUT_FIELD_NAME);

                if (segments.length == INPUT_FIELD_LENGTH && isLastInput) {
                    ydtBuilder.addChild(segments[0], null, YdtType.SINGLE_INSTANCE_NODE);
                    rpcModule = ydtBuilder.getCurNode();
                    ydtBuilder.addChild(segments[1], null, YdtType.SINGLE_INSTANCE_NODE);
                } else {
                    ydtBuilder.addChild(fieldName, null, YdtType.SINGLE_INSTANCE_NODE);
                }
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
    public void exitJsonNode(JsonNode jsonNode) {
        ydtBuilder.traverseToParent();
        //if the current node is the RPC node, then should go to the father
        //for we have enter the RPC node and Input node at the same time
        //and the input is the only child of RPC node.
        String curNodeName = ydtBuilder.getCurNode().getName();
        if (rpcModule != null && curNodeName.equals(rpcModule.getName())) {
            ydtBuilder.traverseToParent();
        }
    }

    private void processArrayNode(String fieldName, JsonNode node) {
        ArrayNode arrayNode = (ArrayNode) node;
        Set<String> sets = new HashSet<>();
        Iterator<JsonNode> elements = arrayNode.elements();
        boolean isLeafList = true;
        while (elements.hasNext()) {
            JsonNode element = elements.next();
            JsonNodeType eleType = element.getNodeType();

            if (eleType == JsonNodeType.STRING
                    || eleType == JsonNodeType.NUMBER
                    || eleType == JsonNodeType.BOOLEAN) {
                sets.add(element.asText());
            } else {
                isLeafList = false;
            }
        }
        if (isLeafList) {
            //leaf-list
            ydtBuilder.addLeaf(fieldName, null, sets);
        } else {
            //need to go into the array node
            ydtBuilder.addChild(fieldName, null, YdtType.MULTI_INSTANCE_NODE);
        }
    }
}
