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

import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.onosproject.restconf.utils.exceptions.YdtParseException;
import org.onosproject.restconf.utils.parser.api.JsonBuilder;
import org.onosproject.yms.ydt.YdtContext;
import org.onosproject.yms.ydt.YdtListener;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Represents implementation of codec YDT listener.
 */
public class YdtToJsonListener implements YdtListener {

    private JsonBuilder jsonBuilder;
    //the root name of the json
    //the input YdtContext is usually a total tree of a YANG resource
    //this property is used to mark the start of the request node.
    private String rootName;
    //the parse state
    private boolean isBegin;
    private boolean isOver;

    public YdtToJsonListener(String rootName, JsonBuilder jsonBuilder) {
        this.jsonBuilder = jsonBuilder;
        this.rootName = rootName;
        this.isBegin = isNullOrEmpty(rootName);
        this.isOver = false;
    }

    @Override
    public void enterYdtNode(YdtContext ydtContext) {
        String name = ydtContext.getName();

        if (isOver) {
            return;
        }
        if (name.equals(rootName)) {
            isBegin = true;
        }
        if (!isBegin) {
            return;
        }

        switch (ydtContext.getYdtType()) {

            case SINGLE_INSTANCE_NODE:
                jsonBuilder.addNodeTopHalf(name, JsonNodeType.OBJECT);
                break;
            case MULTI_INSTANCE_NODE:
                YdtContext preNode = ydtContext.getPreviousSibling();
                if (preNode == null || !preNode.getName().equals(name)) {
                    jsonBuilder.addNodeTopHalf(name, JsonNodeType.ARRAY);
                }
                jsonBuilder.addNodeTopHalf("", JsonNodeType.OBJECT);
                break;
            case SINGLE_INSTANCE_LEAF_VALUE_NODE:
                jsonBuilder.addNodeWithValueTopHalf(name, ydtContext.getValue());
                break;
            case MULTI_INSTANCE_LEAF_VALUE_NODE:
                jsonBuilder.addNodeWithSetTopHalf(name, ydtContext.getValueSet());
                break;
            default:
                throw new YdtParseException("unknown Ydt type"
                                                    + ydtContext.getYdtType().toString());
        }

    }

    @Override
    public void exitYdtNode(YdtContext ydtContext) {
        String curName = ydtContext.getName();

        if (!isBegin) {
            return;
        }
        if (curName.equals(rootName)) {
            isBegin = false;
        }
        switch (ydtContext.getYdtType()) {

            case SINGLE_INSTANCE_NODE:
                jsonBuilder.addNodeBottomHalf(JsonNodeType.OBJECT);
                break;
            case MULTI_INSTANCE_NODE:
                YdtContext nextNode = ydtContext.getNextSibling();
                if (nextNode == null || !nextNode.getName().equals(curName)) {
                    jsonBuilder.addNodeBottomHalf(JsonNodeType.OBJECT);
                    jsonBuilder.addNodeBottomHalf(JsonNodeType.ARRAY);
                } else {
                    jsonBuilder.addNodeBottomHalf(JsonNodeType.OBJECT);
                }
                break;
            case SINGLE_INSTANCE_LEAF_VALUE_NODE:
                jsonBuilder.addNodeBottomHalf(JsonNodeType.STRING);
                break;
            case MULTI_INSTANCE_LEAF_VALUE_NODE:
                jsonBuilder.addNodeBottomHalf(JsonNodeType.ARRAY);
                break;
            default:
                throw new YdtParseException("unknown Ydt type"
                                                    + ydtContext.getYdtType().toString());
        }
        if (curName.equals(rootName)) {
            isOver = true;
        }
    }
}
