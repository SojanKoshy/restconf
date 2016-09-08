/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.restconf.restconfmgr;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.glassfish.jersey.server.ChunkedOutput;
import org.onosproject.restconf.api.RestconfException;
import org.onosproject.restconf.api.RestconfService;
import org.onosproject.restconf.utils.parser.json.ParserUtils;
import org.onosproject.yms.ydt.YdtBuilder;
import org.onosproject.yms.ydt.YdtContext;
import org.onosproject.yms.ydt.YdtContextOperationType;
import org.onosproject.yms.ydt.YdtResponse;
import org.onosproject.yms.ydt.YmsOperationExecutionStatus;
import org.onosproject.yms.ydt.YmsOperationType;
import org.onosproject.yms.ymsm.YmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;

/*
 * Skeletal ONOS RESTCONF Server application. The RESTCONF Manager
 * implements the main logic of the RESTCONF Server.
 *
 * The design of the RESTCONF subsystem contains contains 2 major bundles:
 *
 * 1. RESTCONF Protocol Proxy (RPP). This bundle is implemented as a JAX-RS application.
 * It acts as the frond-end of the the RESTCONF server. It handles
 * HTTP requests that are sent to the RESTCONF Root Path. It then calls the RESTCONF Manager
 * to process the requests.
 *
 * 2. RESTCONF Manager. This is the back-end. It provides the main logic of the RESTCONF server.
 * It calls the YMS (YANG Management System) to operate on the YANG data objects.
 */
@Component(immediate = true)
@Service
public class RestconfManager implements RestconfService {

    private static final String RESTCONF_ROOT = "/onos/restconf";


    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected YmsService ymsService;


    @Activate
    protected void activate() {
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public ObjectNode doGetOperation(String identifier) throws RestconfException {
        //Get a root ydtBuilder
        YdtBuilder ydtBuilder = ymsService.getYdtBuilder(getRestconfRootPath(), null,
                                                         YmsOperationType.QUERY_REQUEST);
        //Convert the URI to ydtBuilder
        ParserUtils.convertUriToYdt(identifier, ydtBuilder, YdtContextOperationType.NONE);
        //Execute the query operation
        YdtResponse ydtResponse = ymsService.executeOperation(ydtBuilder);
        //TODO implement the exception process when YMS is ready
        YmsOperationExecutionStatus executionStatus = ydtResponse.getYmsOperationResult();
        if (executionStatus != YmsOperationExecutionStatus.EXECUTION_SUCCESS) {
            throw new RestconfException("YMS query operation failed",
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
        //this is a root node, need to find the query node.
        YdtContext rootNode = ydtResponse.getRootNode();
        String requestNodeName = ParserUtils.getLastSegmentNodeName(identifier);

        return ParserUtils.convertYdtToJson(requestNodeName, rootNode, ymsService.getYdtWalker());
    }

    @Override
    public void doPostOperation(String identifier, ObjectNode rootNode) {
        //Get a root ydtBuilder
        YdtBuilder ydtBuilder = ymsService.getYdtBuilder(getRestconfRootPath(), null,
                                                         YmsOperationType.EDIT_CONFIG_REQUEST);
        //Convert the URI to ydtBuilder
        ParserUtils.convertUriToYdt(identifier, ydtBuilder, YdtContextOperationType.CREATE);

        //set default operation type for the payload node
        ydtBuilder.setDefaultEditOperationType(YdtContextOperationType.CREATE);
        ParserUtils.convertJsonToYdt(rootNode, ydtBuilder, YdtContextOperationType.CREATE);
        //Execute the query operation
        YdtResponse ydtResponse = ymsService.executeOperation(ydtBuilder);
        YmsOperationExecutionStatus executionStatus = ydtResponse.getYmsOperationResult();
        if (executionStatus != YmsOperationExecutionStatus.EXECUTION_SUCCESS) {
            throw new RestconfException("YMS post operation failed.",
                                        Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void doPutOperation(String identifier, ObjectNode rootNode) throws RestconfException {
        //Get a root ydtBuilder
        YdtBuilder ydtBuilder = ymsService.getYdtBuilder(getRestconfRootPath(), null,
                                                         YmsOperationType.EDIT_CONFIG_REQUEST);
        //Convert the URI to ydtBuilder
        ParserUtils.convertUriToYdt(identifier, ydtBuilder, YdtContextOperationType.REPLACE);
        ydtBuilder.setDefaultEditOperationType(YdtContextOperationType.REPLACE);
        ParserUtils.convertJsonToYdt(rootNode, ydtBuilder, YdtContextOperationType.REPLACE);
        //Execute the query operation
        YdtResponse ydtResponse = ymsService.executeOperation(ydtBuilder);
        YmsOperationExecutionStatus executionStatus = ydtResponse.getYmsOperationResult();
        if (executionStatus != YmsOperationExecutionStatus.EXECUTION_SUCCESS) {
            throw new RestconfException("YMS put operation failed.",
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void doDeleteOperation(String identifier) throws RestconfException {
        //Get a root ydtBuilder
        YdtBuilder ydtBuilder = ymsService.getYdtBuilder(getRestconfRootPath(), null,
                                                         YmsOperationType.EDIT_CONFIG_REQUEST);
        //Convert the URI to ydtBuilder
        ParserUtils.convertUriToYdt(identifier, ydtBuilder, YdtContextOperationType.DELETE);
        //Execute the query operation
        YdtResponse ydtResponse = ymsService.executeOperation(ydtBuilder);
        YmsOperationExecutionStatus executionStatus = ydtResponse.getYmsOperationResult();
        if (executionStatus != YmsOperationExecutionStatus.EXECUTION_SUCCESS) {
            throw new RestconfException("YMS put operation failed.",
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void doPatchOperation(String identifier, ObjectNode rootNode) throws RestconfException {
        //Get a root ydtBuilder
        YdtBuilder ydtBuilder = ymsService.getYdtBuilder(getRestconfRootPath(), null,
                                                         YmsOperationType.EDIT_CONFIG_REQUEST);
        //Convert the URI to ydtBuilder
        ParserUtils.convertUriToYdt(identifier, ydtBuilder, YdtContextOperationType.MERGE);
        ydtBuilder.setDefaultEditOperationType(YdtContextOperationType.MERGE);
        ParserUtils.convertJsonToYdt(rootNode, ydtBuilder, YdtContextOperationType.MERGE);
        //Execute the query operation
        YdtResponse ydtResponse = ymsService.executeOperation(ydtBuilder);
        YmsOperationExecutionStatus executionStatus = ydtResponse.getYmsOperationResult();
        if (executionStatus != YmsOperationExecutionStatus.EXECUTION_SUCCESS) {
            throw new RestconfException("YMS put operation failed.",
                                        Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public String getRestconfRootPath() {
        return RESTCONF_ROOT;
    }


    @Override
    public void subscribeEventStream(String streamId, ChunkedOutput<String> output) throws RestconfException {
        //TODO implement the subscribe
    }
}
