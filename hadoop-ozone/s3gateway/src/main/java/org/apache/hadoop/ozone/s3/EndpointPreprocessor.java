/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.ozone.s3;

import java.io.IOException;
import java.net.URI;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.Provider;
import org.apache.hadoop.ozone.s3.endpoint.vectors.VectorEndpoint;
import org.apache.hadoop.ozone.s3.util.S3Consts;

/**
 * Filter to adjust request headers for compatible reasons.
 *
 * It should be executed AFTER signature check (VirtualHostStyleFilter) as the
 * original Content-Type could be part of the base of the signature.
 */
@Provider
@PreMatching
@Priority(VirtualHostStyleFilter.PRIORITY
    + S3GatewayHttpServer.FILTER_PRIORITY_DO_AFTER * 2)
public class EndpointPreprocessor implements ContainerRequestFilter {

  @Override
  public void filter(ContainerRequestContext requestContext) throws
      IOException {
    String path = requestContext.getUriInfo().getPath();
    String newPath;
    if (VectorEndpoint.isMatch(path)) {
      newPath = S3Consts.S3_VECTORS_PATH;
    } else {
      newPath = S3Consts.S3_OBJECTS_PATH;
    }
    newPath += "/";
    URI baseURI = requestContext.getUriInfo().getBaseUri();
    String currentPath = requestContext.getUriInfo().getPath();


    MultivaluedMap<String, String> queryParams = requestContext.getUriInfo()
        .getQueryParameters();
    UriBuilder requestAddrBuilder = UriBuilder.fromUri(baseURI).path(newPath).path(currentPath);
    queryParams.forEach((k, v) -> requestAddrBuilder.queryParam(k,
        v.toArray()));
    URI requestAddr = requestAddrBuilder.build();
    requestContext.setRequestUri(baseURI, requestAddr);
  }

}
