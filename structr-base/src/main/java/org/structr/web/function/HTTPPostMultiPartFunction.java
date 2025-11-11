/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.rest.common.HttpHelper;
import org.structr.schema.action.ActionContext;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HTTPPostMultiPartFunction extends HttpPostFunction {

	@Override
	public String getName() {
		return "POST_multi_part";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("url, partsMap, contentType");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			final String uri                = sources[0].toString();
			final Map<String, Object> parts = (HashMap) sources[1];
			final String contentType        = (sources.length >= 3 && sources[2] != null) ? sources[2].toString() : DEFAULT_CONTENT_TYPE;

			final Map<String, Object> responseData = this.postMultiPart(uri, parts, ctx.getHeaders(), ctx.isValidateCertificates());

			final GraphObjectMap response = processResponseData(ctx, caller, responseData, contentType);

			return response;

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${POST_multi_part(URL, partsMap [, responseContentType])}. Example: ${POST_multi_part('http://localhost:8082/structr/upload', { name: \"Test\", file: first(find(\"AbstractFile\", \"name\", \"TestFile.txt\")) })}"),
			Usage.javaScript("Usage: ${{ $.POSTMultiPart(URL, partsMap[, responseContentType]) }}. Example: ${{ $.POSTMultiPart('http://localhost:8082/structr/rest/folders', { name: \"Test\", file: find(\"AbstractFile\", \"name\", \"TestFile.txt\")[0] }) }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Sends a multi-part HTTP POST request to the given URL and returns the response body";
	}

	private MultipartEntityBuilder addInputStreamToMultiPartBuilder(MultipartEntityBuilder builder, AbstractFile abstractFile, final String partKey) {

		if (abstractFile.is(StructrTraits.FILE)) {

			final File file = (File) abstractFile;
			InputStreamBody inputStreamBody = new InputStreamBody(StorageProviderFactory.getStorageProvider(file).getInputStream(), ContentType.create(file.getContentType()), file.getName());
			builder.addPart(partKey, inputStreamBody);

		} else if (abstractFile.is(StructrTraits.FOLDER)) {

			final Folder folder = (Folder) abstractFile;
			for (File folderFile : folder.getFiles()) {
				builder = addInputStreamToMultiPartBuilder(builder, folderFile, partKey);
			}
		}

		return builder;
	}

	private MultipartEntityBuilder addPartToBuilder(MultipartEntityBuilder builder, final String partKey, final Object partValue) throws UnsupportedEncodingException {

		if (partValue instanceof Iterable) {

			for (Object collectionPart : (Iterable) partValue) {
				builder = addPartToBuilder(builder, partKey, collectionPart);
			}

		} else if (partValue instanceof NodeInterface n) {

			builder = this.addInputStreamToMultiPartBuilder(builder, n.as(AbstractFile.class), partKey);

		} else if (partValue != null) {

			builder.addPart(partKey, new StringBody(partValue.toString(), ContentType.create("text/plain", "UTF-8")));
		}

		return builder;
	}

	private Map<String, Object> postMultiPart(final String address, final Map<String, Object> parts, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		final Map<String, Object> responseData = new HashMap<>();

		try {

			final URI uri      = new URL(address).toURI();
			final HttpPost req = new HttpPost(uri);

			MultipartEntityBuilder builder = MultipartEntityBuilder.create();

			CloseableHttpClient client = HttpHelper.getClient(req, DEFAULT_CHARSET, null, null, null, null, null, null, headers, false, validateCertificates);

			for (Map.Entry<String, Object> entry : parts.entrySet()) {

				final String partKey   = entry.getKey();
				final Object partValue = entry.getValue();

				 builder = addPartToBuilder(builder, partKey, partValue);
			}

			HttpEntity reqEntity = builder.build();
			req.setEntity(reqEntity);

			final CloseableHttpResponse response = client.execute(req);

			String content = IOUtils.toString(response.getEntity().getContent(), HttpHelper.charset(response));

			content = HttpHelper.skipBOMIfPresent(content);

			responseData.put(HttpHelper.FIELD_BODY, content);
			responseData.put(HttpHelper.FIELD_STATUS, Integer.toString(response.getStatusLine().getStatusCode()));
			responseData.put(HttpHelper.FIELD_HEADERS, HttpHelper.getHeadersAsMap(response));

		} catch (final Throwable t) {

			logger.error("Unable to issue POST request to address {}, {}", address, t.getMessage());
			throw new FrameworkException(422, "Unable to issue POST request to address " + address + ": " + t.getCause() + " " + (t.getMessage() != null ? t.getMessage() : ""), t);
		}

		return responseData;
	}
}
