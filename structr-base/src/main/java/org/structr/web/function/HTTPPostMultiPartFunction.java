/*
 * Copyright (C) 2010-2023 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.function;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.StringProperty;
import org.structr.rest.common.HttpHelper;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HTTPPostMultiPartFunction extends UiAdvancedFunction {
    public static final String ERROR_MESSAGE_POST    = "Usage: ${POST_multi_part(URL, parts [, responseContentType])}. Example: ${POST('http://localhost:8082/structr/upload', '{name:\"Test\", file: find(\"AbstractFile\", \"name\", \"TestFile.txt\")}')}";
    public static final String ERROR_MESSAGE_POST_JS = "Usage: ${{Structr.POSTMultiPart(URL, parts[, responseContentType])}}. Example: ${{Structr.POST('http://localhost:8082/structr/rest/folders', '{name:\"Test\", file: find(\"AbstractFile\", \"name\", \"TestFile.txt\")}')}}";

    @Override
    public String getName() {
        return "POST_multi_part";
    }

    @Override
    public String getSignature() {
        return "url, parts";
    }

    @Override
    public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

        try {

            assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

            final String uri = sources[0].toString();
            final Map<String, Object> parts = (HashMap) sources[1];
            String contentType = "application/json";

            // override default content type
            if (sources.length >= 3 && sources[2] != null) {
                contentType = sources[2].toString();
            }

            final Map<String, String> responseData = this.postMultiPart(ctx, uri, parts, ctx.getHeaders(), ctx.isValidateCertificates());

            final int statusCode = Integer.parseInt(responseData.get("status"));
            responseData.remove("status");

            final String responseBody = responseData.get("body");
            responseData.remove("body");

            final GraphObjectMap response = new GraphObjectMap();

            if ("application/json".equals(contentType)) {

                final FromJsonFunction fromJsonFunction = new FromJsonFunction();
                response.setProperty(new GenericProperty<>("body"), fromJsonFunction.apply(ctx, caller, new Object[]{responseBody}));

            } else {

                response.setProperty(new StringProperty("body"), responseBody);
            }

            response.setProperty(new IntProperty("status"), statusCode);

            final GraphObjectMap map = new GraphObjectMap();

            for (final Map.Entry<String, String> entry : responseData.entrySet()) {

                map.put(new StringProperty(entry.getKey()), entry.getValue());
            }

            response.setProperty(new StringProperty("headers"), map);

            return response;

        } catch (IllegalArgumentException e) {

            logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
            return usage(ctx.isJavaScriptContext());
        }
    }

    @Override
    public String usage(boolean inJavaScriptContext) {
        return (inJavaScriptContext ? ERROR_MESSAGE_POST_JS : ERROR_MESSAGE_POST);
    }

    @Override
    public String shortDescription() {
        return "Sends an HTTP POST request to the given URL and returns the response body";
    }

    private MultipartEntityBuilder addFileToMultiPartBuilder(MultipartEntityBuilder builder, AbstractFile abstractFile, final String partKey) {

        if (abstractFile instanceof File) {

            final File file = (File) abstractFile;
            FileBody fileBody = new FileBody(file.getFileOnDisk(), ContentType.create(file.getContentType()), file.getName());
            builder.addPart(partKey, fileBody);

        } else if (abstractFile instanceof Folder) {

            final Folder folder = (Folder) abstractFile;
            for (File folderFile : folder.getFiles()) {
               builder = addFileToMultiPartBuilder(builder, folder, partKey);
            }
        }

        return builder;
    }

    private MultipartEntityBuilder addPartToBuilder(MultipartEntityBuilder builder, final String partKey, final Object partValue) throws UnsupportedEncodingException {

        if (partValue instanceof ArrayList) {

            for (Object collectionPart : (ArrayList) partValue) {
                builder = addPartToBuilder(builder, partKey, collectionPart);
            }

        } else if (partValue instanceof AbstractFile) {

            builder = this.addFileToMultiPartBuilder(builder, (AbstractFile) partValue, partKey);

        } else {

            builder.addPart(partKey, new StringBody((String) partValue));
        }

        return builder;
    }

    private Map<String, String> postMultiPart(final ActionContext ctx, final String address, final Map<String, Object> parts, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

        final Map<String, String> responseData = new HashMap<>();

        try {
            final URL url = new URL(address);
            HttpPost httppost = new HttpPost(url.toURI());
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();

            CloseableHttpClient client = HttpHelper.getClient(httppost, "UTF-8", null, null, null, null, null, null, ctx.getHeaders(), false, validateCertificates);

            for (Map.Entry<String, Object> entry : parts.entrySet()) {
                final String partKey = entry.getKey();
                final Object partValue = entry.getValue();

                 builder = addPartToBuilder(builder, partKey, partValue);
            }

            HttpEntity reqEntity = builder.build();
            httppost.setEntity(reqEntity);

            final CloseableHttpResponse response = client.execute(httppost);

            String content = IOUtils.toString(response.getEntity().getContent(), HttpHelper.charset(response));

            content = HttpHelper.skipBOMIfPresent(content);

            responseData.put("body", content);

            responseData.put("status", Integer.toString(response.getStatusLine().getStatusCode()));
            for (final Header header : response.getAllHeaders()) {

                responseData.put(header.getName(), header.getValue());
            }

        } catch (final Throwable t) {

            logger.error("Unable to issue POST request to address {}, {}", new Object[] { address, t.getMessage() });
            throw new FrameworkException(422, "Unable to issue POST request to address " + address + ": " + t.getCause() + " " + (t.getMessage() != null ? t.getMessage() : ""), t);
        }

        return responseData;
    }
}
