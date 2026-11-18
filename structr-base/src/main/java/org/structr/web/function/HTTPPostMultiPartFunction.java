/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.rest.common.HttpHelper;
import org.structr.common.error.ArgumentTypeException;
import org.structr.schema.action.ActionContext;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HTTPPostMultiPartFunction extends HttpPostFunction {

	@Override
	public String getName() {

		return "POSTMultiPart";
	}

	@Override
	public boolean isAsyncCapable() {

		// Undoes the true this would otherwise inherit from HttpPostFunction. Unlike the other verbs, this
		// one resolves File and Folder nodes and opens their storage providers to build the request body,
		// which is graph work and needs the calling thread's transaction.

		return false;
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			// max length as well as min, and only the mandatory arguments checked for null: the optional
			// ones may legitimately be passed as null to reach the options object behind them, which the
			// code below is written to handle. Asserting no nulls anywhere contradicted that.
			assertArrayHasMinLengthAndMaxLength(sources, 2, 3);

			for (int i = 0; i < 2; i++) {

				if (sources[i] == null) {

					throw new ArgumentTypeException("POSTMultiPart(): the url and the parts must not be null.");
				}
			}

			final String uri                = sources[0].toString();
			final Map<String, Object> parts = partsOf(sources[1]);
			final HttpOptions options       = HttpOptions.from("POSTMultiPart", sources, 2).accepting(ctx, "POSTMultiPart", HttpOptions.PARSE_RESPONSE);

			// a node is sent as the content of a file, or of all files of a folder - any other node is
			// refused before the request is made instead of being sent as something the receiver can't use
			for (final Map.Entry<String, Object> entry : parts.entrySet()) {

				final String invalidType = findNodeThatIsNotAFileOrFolder(entry.getValue());
				if (invalidType != null) {

					return throwExceptionIfSupportedElseLogWarningAndReturnNull(ctx, getName() + "(): part '" + entry.getKey() + "' must be a File or Folder node, was a node of type " + invalidType + ".");
				}
			}

			final String username = options.getString(HttpOptions.USERNAME);
			final String password = options.getString(HttpOptions.PASSWORD);

			// A file part is a stream, and a stream cannot answer a 401 challenge, because answering means
			// sending the body again. Refused up front like a File body in POST(), instead of failing later
			// as an apparently empty upload.
			if (username != null && !options.getBoolean(HttpOptions.PREEMPTIVE, false) && parts.values().stream().anyMatch(this::containsNode)) {

				throw new ArgumentTypeException(getName() + "(): a File or Folder part cannot be sent with username/password unless "
					+ "{ preemptive: true } is set. The credentials are otherwise sent only after a 401, which requires "
					+ "sending the parts a second time, and a file cannot be read twice.");
			}

			// checked before the parts are built, which opens the files
			HttpHelper.validateOutgoingAddress(uri);

			// no contentType parameter: a multipart request builds its own Content-Type, including the
			// boundary, so naming one here could only conflict with it
			final Map<String, Object> responseData = HttpHelper.post(uri, buildEntity(parts), username, password, null, null, null, null,
				options.mergeHeaders(ctx.getHeaders()), DEFAULT_CHARSET, options.getBoolean(HttpOptions.VALIDATE_CERTIFICATES, ctx.isValidateCertificates()),
				options.asRequestConfig(), null);

			return processResponseData(ctx, caller, responseData, options.getBoolean(HttpOptions.PARSE_RESPONSE, false));

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage());

			return null;
		}
	}

	/**
	 * The parts as a map: a JavaScript object arrives as a Map, a map from a function like from_json() as a
	 * GraphObjectMap, which is not a Map.
	 */
	private Map<String, Object> partsOf(final Object source) {

		if (source instanceof GraphObjectMap graphObjectMap) {

			return graphObjectMap.toMap();
		}

		if (source instanceof Map<?, ?> map) {

			final Map<String, Object> parts = new LinkedHashMap<>();

			for (final Map.Entry<?, ?> entry : map.entrySet()) {

				parts.put(String.valueOf(entry.getKey()), entry.getValue());
			}

			return parts;
		}

		throw new ArgumentTypeException(getName() + "(): the parts must be a map, was " + source.getClass().getSimpleName() + ".");
	}

	/** The type of the first node in the given part value (or its elements) that is neither a File nor a Folder, or null. */
	private String findNodeThatIsNotAFileOrFolder(final Object partValue) {

		if (partValue instanceof Iterable<?> iterable) {

			for (final Object element : iterable) {

				final String type = findNodeThatIsNotAFileOrFolder(element);
				if (type != null) {

					return type;
				}
			}

		} else if (partValue instanceof NodeInterface n && !n.is(StructrTraits.FILE) && !n.is(StructrTraits.FOLDER)) {

			return n.getType();
		}

		return null;
	}

	/**
	 * The content type of a file part. parse() rather than create(), which refuses parameters like a charset.
	 * A file without a usable content type is still sent, as application/octet-stream.
	 */
	private ContentType contentTypeOf(final File file) {

		final String contentType = file.getContentType();

		if (contentType != null && !contentType.isBlank()) {

			try {

				return ContentType.parse(contentType);

			} catch (Exception ex) {

				logger.warn("{}(): unable to parse content type '{}' of file {}, sending it as {}", getName(), contentType, file.getUuid(), ContentType.APPLICATION_OCTET_STREAM.getMimeType());
			}
		}

		return ContentType.APPLICATION_OCTET_STREAM;
	}

	private MultipartEntityBuilder addInputStreamToMultiPartBuilder(MultipartEntityBuilder builder, AbstractFile abstractFile, final String partKey) {

		if (abstractFile.is(StructrTraits.FILE)) {

			final File file = abstractFile.as(File.class);
			InputStreamBody inputStreamBody = new InputStreamBody(StorageProviderFactory.getStorageProvider(file).getInputStream(), contentTypeOf(file), file.getName());

			builder.addPart(partKey, inputStreamBody);

		} else if (abstractFile.is(StructrTraits.FOLDER)) {

			final Folder folder = abstractFile.as(Folder.class);

			for (File folderFile : folder.getFiles()) {

				builder = addInputStreamToMultiPartBuilder(builder, folderFile, partKey);
			}
		}

		return builder;
	}

	/** Whether the given part value is a node or contains one: after the type check, a File or a Folder. */
	private boolean containsNode(final Object partValue) {

		if (partValue instanceof Iterable<?> iterable) {

			for (final Object element : iterable) {

				if (containsNode(element)) {

					return true;
				}
			}

			return false;
		}

		return partValue instanceof NodeInterface;
	}

	private HttpEntity buildEntity(final Map<String, Object> parts) {

		MultipartEntityBuilder builder = MultipartEntityBuilder.create();

		for (final Map.Entry<String, Object> entry : parts.entrySet()) {

			builder = addPartToBuilder(builder, entry.getKey(), entry.getValue());
		}

		return builder.build();
	}

	private MultipartEntityBuilder addPartToBuilder(MultipartEntityBuilder builder, final String partKey, final Object partValue) {

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


	// ----- documentation -----
	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("url, partsMap [, options ]");
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("url", "URL to connect to"),
			Parameter.mandatory("partsMap", "map of part names to values: text, `File`, `Folder` or a list of these (see notes)"),
			Parameter.optional("options",
				"""
					object with optional settings\
					\
					<ul>\
						<li> `username`: username for basic authentication</li>\
						<li> `password`: password for basic authentication</li>\
						<li> `preemptive`: sends the credentials with the first request instead of waiting for a 401 challenge. Required when sending a `File` or `Folder` with credentials (default: `false`)</li>\
						<li> `headers`: map of request headers. Merged with the headers set with `addHeader()`, the headers given here win on conflicts</li>\
						<li> `timeout`: timeout in seconds (default: no timeout)</li>\
						<li> `redirects`: indicates if redirects should be followed (default: `false`)</li>\
						<li> `validateCertificates`: indicates if the server certificate should be validated (default: the setting of `validateCertificates()`, which is `true` unless changed)</li>\
						<li> `parseResponse`: indicates if the response body should be parsed as JSON (default: `false`)</li>\
					</ul>\
					"""
			)
		);
	}

	@Override
	public String getShortDescription() {

		return "Sends a multi-part HTTP POST request to the given URL and returns the response body.";
	}

	@Override
	public String getLongDescription() {

		return """
			This function can be used in a script to make a multi-part HTTP POST request **from within the Structr Server**, triggered by a frontend control like a button etc.

			The function returns a response object containing the response headers, body and status code. The object has the following structure:

			| Field | Description | Type |
			| --- | --- | --- |
			status | HTTP status of the request | Integer |
			headers | Response headers | Map |
			body | Response body | Map or String |

			The options object configures everything else, for example `{ timeout: 60, redirects: true }`. A multipart request builds its own Content-Type, including the boundary, so there is no contentType parameter.
			""";
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.javaScript("""
				${{
					let file = $.find('File', { name: 'report.pdf' })[0];

					let response = $.POSTMultiPart('https://api.example.com/upload', {
						file:        file,
						description: 'Monthly report'
					});

					$.log('Upload finished with status ' + response.status);
				}}
				""", "Upload a file together with a text field"),
			Example.javaScript("""
				${{
					let folder = $.find('Folder', { name: 'invoices' })[0];

					let response = $.POSTMultiPart('https://api.example.com/upload', {
						attachments: folder,
						tags:        ['invoice', '2026']
					}, {
						headers:       { Authorization: 'Bearer ...' },
						parseResponse: true
					});

					$.log('Created upload ' + response.body.id);
				}}
				""", "Upload every file of a folder as part 'attachments' and two parts 'tags', with an authorization header, and parse the JSON response"),
			Example.structrScript("""
				${POSTMultiPart('https://api.example.com/contact', from_json('{ "name": "Jane Doe", "message": "Hello" }')).status}
				""", "Send text fields from StructrScript, which has no map literal, so the parts come from `fromJson()`")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"The `POSTMultiPart()` function will **not** be executed in the security context of the current user. The request will be made **by the Structr server**, without any user authentication or additional information. To access protected resources, use the `username` and `password` options for HTTP Basic Auth, or the `headers` option or `addHeader()` for header-based authentication, e.g. a bearer token.",
			"As of Structr 6.0, it is possible to restrict HTTP calls based on a whitelist setting in structr.conf, `application.httphelper.urlwhitelist`. However the default behaviour in Structr is to allow all outgoing calls.",
			"Each entry of `partsMap` is sent as a part whose name is the entry's key. The value decides the content: text (or any other value that is not a node) is sent as a text part. A `File` is sent as a file part with the file's content and content type, and with the file's name as the `filename` of the part - the part itself is still named after the key. A `Folder` is sent as one such file part for each file in it, and a list as one part for each element, so several parts share the key as their name. For example, `{ attachments: [a, b] }` with two files named `a.pdf` and `b.pdf` sends two parts named `attachments`, one with the `filename` `a.pdf` and one with `b.pdf`.",
			"A node in `partsMap` that is neither a `File` nor a `Folder` is refused: the function throws an error in JavaScript, and in StructrScript it logs a warning and returns `null`. To send such a node, convert it explicitly, e.g. with `toJson()`.",
			"File and Folder parts cannot be sent with `username` and `password` unless `preemptive: true` is set: the credentials are otherwise only sent after a 401 challenge, which would require reading the files a second time."
		);
	}
}
