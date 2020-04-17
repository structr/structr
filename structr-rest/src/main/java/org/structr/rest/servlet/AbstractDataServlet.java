/**
 * Copyright (C) 2010-2020 Structr GmbH
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
package org.structr.rest.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.io.QuietException;
import org.structr.api.config.Settings;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import static org.structr.core.GraphObject.logger;
import org.structr.core.IJsonInput;
import org.structr.core.Services;
import org.structr.core.Value;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.rest.JsonInputGSONAdapter;
import org.structr.rest.ResourceProvider;
import org.structr.rest.RestMethodResult;
import org.structr.rest.resource.Resource;
import org.structr.rest.serialization.StreamingHtmlWriter;
import org.structr.rest.serialization.StreamingJsonWriter;
import org.structr.rest.service.HttpServiceServlet;
import org.structr.rest.service.StructrHttpServiceConfig;
import static org.structr.rest.servlet.JsonRestServlet.REQUEST_PARAMTER_OUTPUT_DEPTH;

/**
 *
 */
public abstract class AbstractDataServlet extends AbstractServletBase implements HttpServiceServlet {

	// final fields
	protected final Map<Pattern, Class<? extends Resource>> resourceMap = new LinkedHashMap<>();
	protected final StructrHttpServiceConfig config                     = new StructrHttpServiceConfig();
	protected Value<String> propertyView                                = null;
	protected String defaultPropertyView                                = null;

	@Override
	public void init() {

		// inject resources
		final ResourceProvider provider = config.getResourceProvider();
		if (provider != null) {

			resourceMap.putAll(provider.getResources());

		} else {

			logger.error("Unable to initialize JsonRestServlet, no resource provider found. Please check structr.conf for a valid resource provider class");
		}

		// initialize variables
		this.propertyView        = new ThreadLocalPropertyView();
		this.defaultPropertyView = config.getDefaultPropertyView();
	}

	@Override
	public StructrHttpServiceConfig getConfig() {
		return config;
	}

	// ----- protected methods -----
	protected void commitResponse(final SecurityContext securityContext, final HttpServletRequest request, final HttpServletResponse response, final RestMethodResult result, final boolean wrapSingleResultInArray) {

		final String outputDepthSrc       = request.getParameter(REQUEST_PARAMTER_OUTPUT_DEPTH);
		final int outputDepth             = Services.parseInt(outputDepthSrc, config.getOutputNestingDepth());
		final String baseUrl              = request.getRequestURI();
		final Map<String, String> headers = result.getHeaders();

		// set headers
		for (final Entry<String, String> header : headers.entrySet()) {
			response.setHeader(header.getKey(), header.getValue());
		}

		// set response code
		if (response.getStatus() != 200) {
			response.setStatus(response.getStatus());
		} else {
			response.setStatus(result.getResponseCode());
		}

		try {

			final List<Object> content = result.getContent();
			if (content != null) {

				writeJson(securityContext, response, new PagingIterable(content), baseUrl, outputDepth, wrapSingleResultInArray);

			} else {

				final String message = result.getMessage();
				if (message != null) {

					writeStatus(response, result.getResponseCode(), message);

				} else {

					final Object nonGraphObjectResult = result.getNonGraphObjectResult();
					if (nonGraphObjectResult != null && nonGraphObjectResult instanceof Iterable) {
						writeJson(securityContext, response, new PagingIterable((Iterable) (nonGraphObjectResult)), baseUrl, outputDepth, wrapSingleResultInArray);
					} else {
						writeJson(securityContext, response, new PagingIterable(Arrays.asList(nonGraphObjectResult)), baseUrl, outputDepth, wrapSingleResultInArray);
					}
				}

			}

		} catch (JsonIOException | IOException t) {

			logger.warn("Unable to commit HttpServletResponse", t);
		}
	}

	protected void processResult(final SecurityContext securityContext, final HttpServletRequest request, final HttpServletResponse response, final ResultStream result, final int outputDepth, final boolean wrapSingleResultInArray) throws ServletException, IOException {

		final String baseUrl = request.getRequestURI();

		try {

			final String accept = request.getHeader("Accept");

			if (accept != null && accept.contains("text/html")) {

				writeHtml(securityContext, response, result, baseUrl, outputDepth, wrapSingleResultInArray);

			} else {

				writeJson(securityContext, response, result, baseUrl, outputDepth, wrapSingleResultInArray);
			}

			response.setStatus(HttpServletResponse.SC_OK);

		} catch (FrameworkException frameworkException) {

			writeException(response, frameworkException);

		} catch (JsonSyntaxException jsex) {

			logger.warn("JsonSyntaxException in GET", jsex);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(RestMethodResult.jsonError(code, "Json syntax exception in GET: " + jsex.getMessage()));

		} catch (JsonParseException jpex) {

			logger.warn("JsonParseException in GET", jpex);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(RestMethodResult.jsonError(code, "Parser exception in GET: " + jpex.getMessage()));

		} catch (Throwable t) {

			try {

				if (t instanceof QuietException || t.getCause() instanceof QuietException) {
					// ignore exceptions which (by jettys standards) should be handled less verbosely
				} else if (t instanceof IllegalStateException && t.getCause() == null && t.getMessage() == null) {
					// ignore exception. it is probably caused by a canceled request/closed connection which caused the JsonWriter to tilt
				} else {
					logger.warn("Exception in GET (URI: {})", securityContext != null ? securityContext.getCompoundRequestURI() : "(null SecurityContext)");
					logger.warn(" => Error thrown: ", t);
				}

				int code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

				response.setStatus(code);
				response.getWriter().append(RestMethodResult.jsonError(code, "Exception in GET: " + t.getMessage()));

				// if sending the error creates an error, we can probably ignore that one
			} catch (Throwable ignore) { }

		} finally {

			try {
				response.getWriter().flush();
				response.getWriter().close();

			} catch (Throwable t) { }
		}
	}

	protected void writeHtml(final SecurityContext securityContext, final HttpServletResponse response, final ResultStream result, final String baseUrl, final int nestingDepth, final boolean wrapSingleResultInArray) throws FrameworkException, IOException {

		final boolean indentJson               = Settings.JsonIndentation.getValue();
		final App app                          = StructrApp.getInstance(securityContext);
		final StreamingHtmlWriter htmlStreamer = new StreamingHtmlWriter(this.propertyView, indentJson, nestingDepth, wrapSingleResultInArray);
		// isolate write output
		try (final Tx tx = app.tx()) {

			// no trailing semicolon so we dont trip MimeTypes.getContentTypeWithoutCharset
			response.setContentType("text/html; charset=utf-8");

			final Writer writer = response.getWriter();

			htmlStreamer.stream(securityContext, writer, result, baseUrl);
			writer.append("\n");    // useful newline

			tx.success();
		}
	}

	protected void writeJson(final SecurityContext securityContext, final HttpServletResponse response, final ResultStream result, final String baseUrl, final int nestingDepth, final boolean wrapSingleResultInArray) throws IOException {

		final boolean indentJson               = Settings.JsonIndentation.getValue();
		final StreamingJsonWriter jsonStreamer = new StreamingJsonWriter(this.propertyView, indentJson, nestingDepth, wrapSingleResultInArray);

		// no trailing semicolon so we dont trip MimeTypes.getContentTypeWithoutCharset
		response.setContentType("application/json; charset=utf-8");

		final Writer writer = response.getWriter();

		jsonStreamer.stream(securityContext, writer, result, baseUrl);
		writer.write(10);    // useful newline
		writer.flush();
	}

	protected void writeException(final HttpServletResponse response, final FrameworkException fex) throws IOException {

		final PrintWriter writer = response.getWriter();
		final Gson gson          = getGson();

		// set status & write JSON output
		response.setStatus(fex.getStatus());
		gson.toJson(fex.toJSON(), writer);
		writer.println();
	}

	protected void writeStatus(final HttpServletResponse response, final int statusCode, final String message) throws IOException {

		final PrintWriter writer = response.getWriter();
		final Gson gson          = getGson();
		final JsonObject obj     = new JsonObject();

		obj.addProperty("code", statusCode);
		obj.addProperty("message", message);

		// set status & write JSON output
		response.setStatus(statusCode);
		gson.toJson(obj, writer);
		writer.println();
	}

	protected Gson getGson() {

		final JsonInputGSONAdapter jsonInputAdapter = new JsonInputGSONAdapter();

		// create GSON serializer
		final GsonBuilder gsonBuilder = new GsonBuilder()
			.setPrettyPrinting()
			.serializeNulls()
			.registerTypeAdapter(IJsonInput.class, jsonInputAdapter);

		final boolean lenient = Settings.JsonLenient.getValue();
		if (lenient) {

			// Serializes NaN, -Infinity, Infinity, see http://code.google.com/p/google-gson/issues/detail?id=378
			gsonBuilder.serializeSpecialFloatingPointValues();
		}

		return gsonBuilder.create();
	}

	protected String coalesce(final String... sources) {

		for (final String source : sources) {

			if (source != null) {

				return source;
			}
		}

		return null;
	}

	// ----- nested classes -----
	private class ThreadLocalPropertyView extends ThreadLocal<String> implements Value<String> {

		@Override
		protected String initialValue() {
			return config.getDefaultPropertyView();
		}

		@Override
		public void set(SecurityContext securityContext, String value) {
			set(value);
		}

		@Override
		public String get(SecurityContext securityContext) {
			return get();
		}
	}
}
