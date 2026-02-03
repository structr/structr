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
package org.structr.rest.servlet;

import com.google.gson.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.io.QuietException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.RequestHeaders;
import org.structr.common.RequestParameters;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.JsonException;
import org.structr.core.IJsonInput;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.rest.JsonInputGSONAdapter;
import org.structr.rest.RestMethodResult;
import org.structr.rest.serialization.StreamingHtmlWriter;
import org.structr.rest.serialization.StreamingJsonWriter;
import org.structr.rest.service.HttpServiceServlet;
import org.structr.rest.service.StructrHttpServiceConfig;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 */
public abstract class AbstractDataServlet extends AbstractServletBase implements HttpServiceServlet {

	private static final Logger logger = LoggerFactory.getLogger(AbstractDataServlet.class);

	protected final StructrHttpServiceConfig config = new StructrHttpServiceConfig();

	@Override
	public StructrHttpServiceConfig getConfig() {
		return config;
	}

	// ----- public static methods -----
	public static String getTypeOrDefault(final NodeInterface obj, final String defaultType) {

		if (obj != null) {
			return obj.getType();
		}

		return defaultType;
	}

	// ----- protected methods -----
	protected void commitResponse(final SecurityContext securityContext, final HttpServletRequest request, final HttpServletResponse response, final RestMethodResult result, final String view, final boolean wrapSingleResultInArray) {

		final String serializeNullsSrc    = request.getParameter(RequestParameters.SerializeNulls.getName());
		final String outputDepthSrc       = request.getParameter(RequestParameters.OutputDepth.getName());
		final int outputDepth             = Services.parseInt(outputDepthSrc, config.getOutputNestingDepth());
		final boolean serializeNulls      = Services.parseBoolean(serializeNullsSrc, true);
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

				final PagingIterable resultIterable = new PagingIterable(request.toString(), content);
				if (result.getOverriddenResultCount() != null) {

					resultIterable.setOverriddenResultCount(result.getOverriddenResultCount());
				}

				writeJson(securityContext, response, resultIterable, baseUrl, view, outputDepth, wrapSingleResultInArray, serializeNulls);

			} else {

				final String message = result.getMessage();
				if (message != null) {

					writeStatus(response, result.getResponseCode(), message);

				} else {

					final Object nonGraphObjectResult = result.getNonGraphObjectResult();

					if (nonGraphObjectResult != null && nonGraphObjectResult instanceof Iterable) {

						writeJson(securityContext, response, new PagingIterable(request.toString(), (Iterable) (nonGraphObjectResult)), baseUrl, view, outputDepth, wrapSingleResultInArray, serializeNulls);

					} else {

						writeJson(securityContext, response, new PagingIterable(request.toString(), Arrays.asList(nonGraphObjectResult)), baseUrl, view, outputDepth, wrapSingleResultInArray, serializeNulls);
					}
				}

			}

		} catch (JsonIOException | IOException t) {

			logger.warn("Unable to commit Response", t);
		}
	}

	protected void processResult(final SecurityContext securityContext, final HttpServletRequest request, final HttpServletResponse response, final ResultStream result, final String view, final int outputDepth, final boolean wrapSingleResultInArray) throws ServletException, IOException {

		final String serializeNullsSrc = request.getParameter(RequestParameters.SerializeNulls.getName());
		final boolean serializeNulls   = Services.parseBoolean(serializeNullsSrc, true);
		final String baseUrl           = request.getRequestURI();

		try {

			final String accept = request.getHeader(RequestHeaders.Accept.getName());

			if (accept != null && accept.contains("text/html")) {

				writeHtml(securityContext, response, result, baseUrl, view, outputDepth, wrapSingleResultInArray, serializeNulls);

			} else {

				writeJson(securityContext, response, result, baseUrl, view, outputDepth, wrapSingleResultInArray, serializeNulls);
			}

			response.setStatus(HttpServletResponse.SC_OK);

		} catch (FrameworkException frameworkException) {

			writeException(response, frameworkException);

		} catch (JsonSyntaxException jsex) {

			logger.warn("JsonSyntaxException in GET", jsex);

			writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "JsonSyntaxException in GET: " + jsex.getMessage());

		} catch (JsonParseException jpex) {

			logger.warn("JsonParseException in GET", jpex);

			writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "JsonParseException in GET: " + jpex.getMessage());

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

				writeJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, t.getClass().getSimpleName() + " in GET: " + t.getMessage());

				// if sending the error creates an error, we can probably ignore that one
			} catch (Throwable ignore) { }

		} finally {

			try {
				response.getWriter().flush();
				response.getWriter().close();

			} catch (Throwable t) { }
		}
	}

	protected void writeHtml(final SecurityContext securityContext, final HttpServletResponse response, final ResultStream result, final String baseUrl, final String view, final int nestingDepth, final boolean wrapSingleResultInArray, final boolean serializeNulls) throws FrameworkException, IOException {

		final boolean indentJson               = Settings.JsonIndentation.getValue();
		final App app                          = StructrApp.getInstance(securityContext);
		final StreamingHtmlWriter htmlStreamer = new StreamingHtmlWriter(view, indentJson, nestingDepth, wrapSingleResultInArray, serializeNulls);
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

	protected void writeJson(final SecurityContext securityContext, final HttpServletResponse response, final ResultStream result, final String baseUrl, final String view, final int nestingDepth, final boolean wrapSingleResultInArray, final boolean serializeNulls) throws IOException {

		final boolean indentJson               = Settings.JsonIndentation.getValue();
		final StreamingJsonWriter jsonStreamer = new StreamingJsonWriter(view, indentJson, nestingDepth, wrapSingleResultInArray, serializeNulls);

		// no trailing semicolon so we dont trip MimeTypes.getContentTypeWithoutCharset
		response.setContentType("application/json; charset=utf-8");

		final Writer writer = response.getWriter();

		jsonStreamer.stream(securityContext, writer, result, baseUrl);
		writer.write(10);    // useful newline
		writer.flush();
	}

	protected void resetResponseBuffer(final HttpServletResponse response, final int statusCode) {

		if (response.isCommitted()) {

			logger.warn("Unable to reset response buffer. The response has already been committed due to streaming. Status code and response can not be changed. Status code was {} but should change to {}.", response.getStatus(), statusCode);

		} else {

			try {

				response.resetBuffer();

			} catch (IllegalStateException ise) {

				logger.warn("Unable to reset response buffer", ise);
			}
		}
	}

	protected void writeException(final HttpServletResponse response, final JsonException fex) throws IOException {

		resetResponseBuffer(response, fex.getStatus());

		final PrintWriter writer = response.getWriter();
		final Gson gson          = getGson();

		// set response headers (for Allow in 405)
		final Map<String, String> headers = fex.headers();
		for (final String header : headers.keySet()) {
			response.addHeader(header, headers.get(header));
		}

		// set status & write JSON output
		response.setStatus(fex.getStatus());
		gson.toJson(fex.toJSON(), writer);
		writer.println();
	}

	protected void writeJsonError(final HttpServletResponse response, final int statusCode, final String errorString) throws IOException {

		resetResponseBuffer(response, statusCode);

		response.setStatus(statusCode);
		response.getWriter().append(RestMethodResult.jsonError(statusCode, errorString));
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
}
