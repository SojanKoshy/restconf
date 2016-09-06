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

    public YdtToJsonListener(String rootName, JsonBuilder jsonBuilder) {
        this.jsonBuilder = jsonBuilder;
        this.rootName = rootName;
        this.isBegin = isNullOrEmpty(rootName);
    }

    @Override
    public void enterYdtNode(YdtContext ydtContext) {
        if (ydtContext.getName().equals(rootName)) {
            isBegin = true;
        }
        if (!isBegin) {
            return;
        }

        switch (ydtContext.getYdtType()) {

            case SINGLE_INSTANCE_NODE:
                jsonBuilder.addNodeTopHalf(ydtContext.getName(), JsonNodeType.OBJECT);
                break;
            case MULTI_INSTANCE_NODE:
                jsonBuilder.addNodeTopHalf(ydtContext.getName(), JsonNodeType.ARRAY);
                break;
            case SINGLE_INSTANCE_LEAF_VALUE_NODE:
                jsonBuilder.addNodeWithValueTopHalf(ydtContext.getName(), ydtContext.getValue());
                break;
            case MULTI_INSTANCE_LEAF_VALUE_NODE:
                jsonBuilder.addNodeWithSetTopHalf(ydtContext.getName(), ydtContext.getValueSet());
                break;
            default:
                throw new YdtParseException("unknown Ydt type"
                                                    + ydtContext.getYdtType().toString());
        }

    }

    @Override
    public void exitYdtNode(YdtContext ydtContext) {
        if (!isBegin) {
            return;
        }
        if (ydtContext.getName().equals(rootName)) {
            isBegin = false;
        }
        switch (ydtContext.getYdtType()) {

            case SINGLE_INSTANCE_NODE:
                jsonBuilder.addNodeBottomHalf(JsonNodeType.OBJECT);
                break;
            case MULTI_INSTANCE_NODE:
                jsonBuilder.addNodeBottomHalf(JsonNodeType.ARRAY);
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
    }
}
