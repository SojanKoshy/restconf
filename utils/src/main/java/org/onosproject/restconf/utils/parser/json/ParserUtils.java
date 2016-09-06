package org.onosproject.restconf.utils.parser.json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.onosproject.restconf.utils.exceptions.JsonParseException;
import org.onosproject.restconf.utils.parser.api.JsonBuilder;
import org.onosproject.restconf.utils.parser.api.JsonWalker;
import org.onosproject.yms.ydt.YdtBuilder;
import org.onosproject.yms.ydt.YdtContext;
import org.onosproject.yms.ydt.YdtContextOperationType;
import org.onosproject.yms.ydt.YdtListener;
import org.onosproject.yms.ydt.YdtType;
import org.onosproject.yms.ydt.YdtWalker;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utils to complete the conversion between JSON and YDT(YANG DATA MODEL).
 */
public final class ParserUtils {

    private static final Splitter SLASH_SPLITTER = Splitter.on('/');
    private static final Splitter COMMA_SPLITTER = Splitter.on(',');
    private static final String EQUAL = "=";
    private static final String COMMA = ",";
    private static final String COLON = ":";
    private static final String URI_ENCODING_CHAR_SET = "ISO-8859-1";

    private ParserUtils() {
        throw new RuntimeException("Parse utils class should not be instantiated");
    }

    /**
     * Converts  URI identifier to YDT builder.
     *
     * @param identifier the uri identifier from web request.
     * @param builder    the base ydt builder
     * @return the YDT builder with the tree info of identifier
     */
    public static YdtBuilder convertUriToYdt(String identifier, YdtBuilder builder,
                                             YdtContextOperationType ydtOpType) {
        checkNotNull(identifier, "uri identifier should not be null");
        List<String> segmentPaths = urlPathArgsDecode(SLASH_SPLITTER.split(identifier));
        if (segmentPaths.isEmpty()) {
            return null;
        }
        processPathSegments(segmentPaths, builder, ydtOpType);
        return builder;
    }

    /**
     * Returns the last segment of a URI identifier, it is the base node of a request.
     *
     * @param identifier the request URI identifier
     * @return the name of the last node in a URI
     */
    public static String getLastSegmentNodeName(String identifier) {
        List<String> segmentPaths = urlPathArgsDecode(SLASH_SPLITTER.split(identifier));
        if (segmentPaths.isEmpty()) {
            return null;
        }
        String lastSegment = segmentPaths.get(segmentPaths.size() - 1);
        String nodeName = lastSegment;
        if (lastSegment.indexOf(EQUAL) > 0) {
            nodeName = lastSegment.substring(0, lastSegment.indexOf(EQUAL));
        }
        return nodeName;
    }

    /**
     * Converts  JSON objectNode to YDT builder. The objectNode can be any standard JSON node, node
     * just for RESTconf payload.
     *
     * @param objectNode the objectNode from web request.
     * @param builder    the base ydt builder
     * @return the YDT builder with the tree info of identifier
     */
    public static YdtBuilder convertJsonToYdt(ObjectNode objectNode, YdtBuilder builder) {
        JsonWalker walker = new DefaultJsonWalker();
        JsonToYdtListener listener = new JsonToYdtListener(builder);
        walker.walk(listener, null, objectNode);
        return builder;
    }

    /**
     * Converts a Ydt context tree to a JSON object.
     *
     * @param ydtContext a abstract data model for YANG data.
     * @param walker     abstraction of an entity which provides interfaces for YDT walk.
     * @return the JSON node corresponding the YANG data
     */
    public static ObjectNode convertYdtToJson(String rootName, YdtContext ydtContext, YdtWalker walker) {
        JsonBuilder builder = new DefaultJsonBuilder();
        YdtListener listener = new YdtToJsonListener(rootName, builder);
        walker.walk(listener, ydtContext);
        return builder.getTreeNode();
    }

    /**
     * Converts a list of path segments to a YDT builder tree.
     *
     * @param builder the base YDT builder
     * @param paths   the list of path segments split from URI
     * @return the YDT builder with the tree info of paths
     */
    private static YdtBuilder processPathSegments(List<String> paths, YdtBuilder builder,
                                                  YdtContextOperationType ydtOpType) {
        if (paths.isEmpty()) {
            return builder;
        }
        boolean isLastNode = paths.size() == 1;
        YdtContextOperationType opTypeForThisNode = isLastNode ? ydtOpType : null;

        final String path = paths.iterator().next();
        if (path.contains(COLON)) {
            addModule(builder, path);
            addNode(path, builder, opTypeForThisNode);
        } else if (path.contains(EQUAL)) {
            addListOrLeafList(path, builder);
        } else {
            return addLeaf(path, builder, opTypeForThisNode);
        }

        if (paths.size() == 1) {
            return builder;
        }
        List<String> remainPaths = paths.subList(1, paths.size());
        processPathSegments(remainPaths, builder, ydtOpType);

        return builder;
    }


    private static YdtBuilder addModule(YdtBuilder builder, String path) {
        String moduleName = getPreSegment(path, COLON);
        if (moduleName == null) {
            throw new JsonParseException("Illegal URI, First node should be in " +
                                                 "format \"moduleName:nodeName\"");
        }
        builder.addChild(moduleName, null, YdtType.SINGLE_INSTANCE_NODE);
        return builder;
    }

    private static YdtBuilder addNode(String path, YdtBuilder builder, YdtContextOperationType ydtOpType) {
        String nodeName = getPostSegment(path, COLON);
        builder.addChild(nodeName, null, YdtType.SINGLE_INSTANCE_NODE, ydtOpType);
        return builder;
    }

    private static YdtBuilder addListOrLeafList(String path, YdtBuilder builder) {
        String nodeName = getPreSegment(path, EQUAL);
        String keyStr = getPostSegment(path, EQUAL);
        if (keyStr == null) {
            throw new JsonParseException("Illegal URI, List/Leaf-list node should " +
                                                 "be in format \"nodeName=key\"or \"nodeName=instance-value\"");
        }
        if (keyStr.contains(COMMA)) {
            List<String> keys = Lists.newArrayList(COMMA_SPLITTER.split(keyStr));
            builder.addChild(nodeName, null, YdtType.MULTI_INSTANCE_NODE);
            //builder.addKeyLeafs(keys);
        } else {
            //TODO need to check the interface,
            //TODO for it should be a MULTI_INSTANCE_LEAF_VALUE_NODE here
            builder.addLeaf(nodeName, null, keyStr);
        }
        return builder;
    }

    private static YdtBuilder addLeaf(String path, YdtBuilder builder, YdtContextOperationType ydtOpType) {
        checkNotNull(path);
        builder.addChild(path, null, ydtOpType);
        return builder;
    }

    private static String getPreSegment(final String path, String splitChar) {
        final int idx = path.indexOf(splitChar);
        if (idx == -1) {
            return null;
        }

        if (path.indexOf(':', idx + 1) != -1) {
            return null;
        }

        return path.substring(0, idx);
    }

    private static String getPostSegment(final String path, String splitChar) {
        final int idx = path.indexOf(splitChar);
        if (idx == -1) {
            return path;
        }

        if (path.indexOf(splitChar, idx + 1) != -1) {
            return null;
        }

        return path.substring(idx + 1);
    }

    /**
     * Converts a list of path from the original format to ISO-8859-1 code.
     *
     * @param paths the original paths
     * @return list of decoded paths
     */
    public static List<String> urlPathArgsDecode(Iterable<String> paths) {
        try {
            final List<String> decodedPathArgs = new ArrayList<String>();
            for (final String pathArg : paths) {
                final String decode = URLDecoder.decode(pathArg, URI_ENCODING_CHAR_SET);
                decodedPathArgs.add(decode);
            }
            return decodedPathArgs;
        } catch (final UnsupportedEncodingException e) {
            throw new JsonParseException("Invalid URL path arg '" + paths + "': ", e);
        }
    }
}
