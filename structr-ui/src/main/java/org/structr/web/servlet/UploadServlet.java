/*
 * Copyright (C) 2010-2021 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.servlet;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.RetryException;
import org.structr.api.config.Settings;
import org.structr.common.AccessMode;
import org.structr.common.PathHelper;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.ThreadLocalMatcher;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.IJsonInput;
import org.structr.core.JsonInput;
import org.structr.core.JsonSingleInput;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.rest.JsonInputGSONAdapter;
import org.structr.rest.service.HttpServiceServlet;
import org.structr.rest.service.StructrHttpServiceConfig;
import org.structr.rest.servlet.AbstractServletBase;
import org.structr.schema.SchemaHelper;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.Folder;
import org.structr.web.entity.File;
import org.structr.web.entity.Image;

/**
 * Simple upload servlet.
 */
public class UploadServlet extends AbstractServletBase implements HttpServiceServlet {

	private static final Logger logger                             = LoggerFactory.getLogger(UploadServlet.class.getName());
	private static final ThreadLocalMatcher threadLocalUUIDMatcher = new ThreadLocalMatcher("[a-fA-F0-9]{32}");
	private static final String REDIRECT_AFTER_UPLOAD_PARAMETER    = "redirectOnSuccess";
	private static final String APPEND_UUID_ON_REDIRECT_PARAMETER  = "appendUuidOnRedirect";
	private static final String UPLOAD_FOLDER_PATH_PARAMETER       = "uploadFolderPath";
	private static final long MEGABYTE                              = 1024 * 1024;

	// non-static fields
	private final StructrHttpServiceConfig config = new StructrHttpServiceConfig();
	private ServletFileUpload uploader            = null;
	private java.io.File filesDir                 = null;

	public UploadServlet() {
	}

	@Override
	public StructrHttpServiceConfig getConfig() {
		return config;
	}

	@Override
	public String getModuleName() {
		return "ui";
	}

	@Override
	public void init() {
		uploader = new ServletFileUpload();
	}

	@Override
	public void destroy() {
	}

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException {

		try {

			assertInitialized();

		} catch (FrameworkException fex) {

			try {
				response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
				response.getOutputStream().write(fex.getMessage().getBytes("UTF-8"));

			} catch (IOException ioex) {

				logger.warn("Unable to send response", ioex);
			}

			return;
		}

		setCustomResponseHeaders(response);

		try {

			if (!ServletFileUpload.isMultipartContent(request)) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getOutputStream().write("ERROR (400): Request does not contain multipart content.\n".getBytes("UTF-8"));
				return;
			}

		} catch (IOException ioex) {
			logger.warn("Unable to send response", ioex);
		}

		SecurityContext securityContext = null;
		String redirectUrl              = null;
		boolean appendUuidOnRedirect    = false;
		String path                     = null;

		// isolate request authentication in a transaction
		try (final Tx tx = StructrApp.getInstance().tx()) {

			try {
				securityContext = getConfig().getAuthenticator().initializeAndExamineRequest(request, response);

			} catch (AuthenticationException ae) {

				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				response.getOutputStream().write("ERROR (401): Invalid user or password.\n".getBytes("UTF-8"));
				return;
			}

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("Unable to examine request", fex);
		} catch (IOException ioex) {
			logger.warn("Unable to send response", ioex);
		}

		// something went wrong, but we don't know what...
		if (securityContext == null) {
			logger.warn("No SecurityContext, aborting.");
			return;
		}

		final App app              = StructrApp.getInstance(securityContext);

		try {

			if (securityContext.getUser(false) == null && !Settings.UploadAllowAnonymous.getValue()) {

				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				response.getOutputStream().write("ERROR (401): Anonymous uploads forbidden.\n".getBytes("UTF-8"));

				return;
			}

			// Ensure access mode is frontend
			securityContext.setAccessMode(AccessMode.Frontend);

			request.setCharacterEncoding("UTF-8");

			// Important: Set character encoding before calling response.getWriter() !!, see Servlet Spec 5.4
			response.setCharacterEncoding("UTF-8");

			// don't continue on redirects
			if (response.getStatus() == 302) {
				return;
			}

			final String pathInfo = request.getPathInfo();
			String type = null;

			if (StringUtils.isNotBlank(pathInfo)) {

				type = SchemaHelper.normalizeEntityName(StringUtils.stripStart(pathInfo.trim(), "/"));

			}

			uploader.setFileSizeMax((long) MEGABYTE * Settings.UploadMaxFileSize.getValue());
			uploader.setSizeMax((long) MEGABYTE * Settings.UploadMaxRequestSize.getValue());

			response.setContentType("text/html");

			final FileItemIterator fileItemsIterator = uploader.getItemIterator(request);
			final Map<String, Object> params         = new HashMap<>();
			String uuid                              = null;

			while (fileItemsIterator.hasNext()) {

				final FileItemStream item = fileItemsIterator.next();

				if (item.isFormField()) {

					final String fieldName = item.getFieldName();
					final String fieldValue = IOUtils.toString(item.openStream(), "UTF-8");

					if (REDIRECT_AFTER_UPLOAD_PARAMETER.equals(fieldName)) {

						redirectUrl = fieldValue;

					} else if (APPEND_UUID_ON_REDIRECT_PARAMETER.equals(fieldName)) {

						appendUuidOnRedirect = "true".equalsIgnoreCase(fieldValue);

					} else if (UPLOAD_FOLDER_PATH_PARAMETER.equals(fieldName)) {

						path = fieldValue;

					} else {

						try {

							final IJsonInput jsonInput = cleanAndParseJsonString(app, "{" + fieldName + ":" + fieldValue + "}");
							for (final JsonInput input : jsonInput.getJsonInputs()) {
								params.put(fieldName, convertPropertySetToMap(input).get(fieldName));
							}

						} catch (final FrameworkException fex) {
							params.put(fieldName, fieldValue);
						}
					}

				} else {

					final String contentType = item.getContentType();
					boolean isImage = (contentType != null && contentType.startsWith("image"));
					boolean isVideo = (contentType != null && contentType.startsWith("video"));

					// Override type from path info
					if (params.containsKey(NodeInterface.type.jsonName())) {
						type = (String) params.get(NodeInterface.type.jsonName());
					}

					Class cls = null;
					if (type != null) {

						cls = SchemaHelper.getEntityClassForRawType(type);

					}

					if (cls == null) {

						if (isImage) {

							cls = Image.class;

						} else if (isVideo) {

							cls = SchemaHelper.getEntityClassForRawType("VideoFile");
							if (cls == null) {

								logger.warn("Unable to create entity of type VideoFile, class is not defined.");
							}

						} else {

							cls = File.class;
						}
					}

					if (cls != null) {
						type = cls.getSimpleName();
					}

					final String name = item.getName().replaceAll("\\\\", "/");
					File newFile      = null;
					boolean retry     = true;

					while (retry) {

						retry = false;

						final String defaultUploadFolderConfigValue = Settings.DefaultUploadFolder.getValue();
						Folder uploadFolder                         = null;

						// If a path attribute was sent, create all folders on the fly.
						if (path != null) {

							uploadFolder = getOrCreateFolderPath(securityContext, path);

						} else if (StringUtils.isNotBlank(defaultUploadFolderConfigValue)) {

							uploadFolder = getOrCreateFolderPath(SecurityContext.getSuperUserInstance(), defaultUploadFolderConfigValue);

						}

						try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

							try (final InputStream is = item.openStream()) {

								newFile = FileHelper.createFile(securityContext, is, contentType, cls, name, uploadFolder);
								AbstractFile.validateAndRenameFileOnce(newFile, securityContext, null);

								final PropertyMap changedProperties = new PropertyMap();

								changedProperties.putAll(PropertyMap.inputTypeToJavaType(securityContext, cls, params));

								// Update type as it could have changed
								changedProperties.put(AbstractNode.type, type);

								newFile.unlockSystemPropertiesOnce();
								newFile.setProperties(securityContext, changedProperties);

								uuid = newFile.getUuid();

							} catch (IOException ex) {
								logger.warn("Could not store file: {}", ex.getMessage());
							}

							tx.success();

						} catch (RetryException rex) {
							retry = true;
						}
					}

					// since the transaction can be repeated, we need to make sure that
					// only the actual existing file creates a UUID output
					if (newFile != null) {

						// upload trigger
						newFile.notifyUploadCompletion();

						// store uuid
						uuid = newFile.getUuid();
					}
				}
			}

			// send redirect to allow form-based file upload without JavaScript..
			if (StringUtils.isNotBlank(redirectUrl)) {

				if (appendUuidOnRedirect) {

					response.sendRedirect(redirectUrl + uuid);

				} else {

					response.sendRedirect(redirectUrl);
				}

			} else {

				// Just write out the uuids of the new files
				response.getWriter().write(uuid);
			}


		} catch (Throwable t) {

			final String content;

			if (t instanceof FrameworkException) {

				final FrameworkException fex = (FrameworkException) t;
				logger.error(fex.toString());
				content = errorPage(fex);

				// set response status accordingly
				response.setStatus(fex.getStatus());

			} else {

				logger.error("Exception while processing upload request", t);
				content = errorPage(t);

				// set response status to 500
				response.setStatus(500);
			}

			try {
				final ServletOutputStream out = response.getOutputStream();
				IOUtils.write(content, out, "utf-8");

			} catch (IOException ex) {
				logger.error("Could not write to response", ex);
			}
		}
	}

	@Override
	protected void doPut(final HttpServletRequest request, final HttpServletResponse response) throws ServletException {

		try {

			assertInitialized();

		} catch (FrameworkException fex) {

			try {
				response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
				response.getOutputStream().write(fex.getMessage().getBytes("UTF-8"));

			} catch (IOException ioex) {

				logger.warn("Unable to send response", ioex);
			}

			return;
		}

		setCustomResponseHeaders(response);

		try (final Tx tx = StructrApp.getInstance().tx(true, false, false)) {

			final String uuid = PathHelper.getName(request.getPathInfo());

			if (uuid == null) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getOutputStream().write("URL path doesn't end with UUID.\n".getBytes("UTF-8"));
				return;
			}

			Matcher matcher = threadLocalUUIDMatcher.get();
			matcher.reset(uuid);

			if (!matcher.matches()) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getOutputStream().write("ERROR (400): URL path doesn't end with UUID.\n".getBytes("UTF-8"));
				return;
			}

			final SecurityContext securityContext = getConfig().getAuthenticator().initializeAndExamineRequest(request, response);

			// Ensure access mode is frontend
			securityContext.setAccessMode(AccessMode.Frontend);

			request.setCharacterEncoding("UTF-8");

			// Important: Set character encoding before calling response.getWriter() !!, see Servlet Spec 5.4
			response.setCharacterEncoding("UTF-8");

			// don't continue on redirects
			if (response.getStatus() == 302) {
				return;
			}

			uploader.setFileSizeMax(MEGABYTE * Settings.UploadMaxFileSize.getValue());
			uploader.setSizeMax(MEGABYTE * Settings.UploadMaxRequestSize.getValue());

			FileItemIterator fileItemsIterator = uploader.getItemIterator(request);

			while (fileItemsIterator.hasNext()) {

				final FileItemStream fileItem = fileItemsIterator.next();

				try {
					final GraphObject node = StructrApp.getInstance().getNodeById(uuid);

					if (node == null) {

						response.setStatus(HttpServletResponse.SC_NOT_FOUND);
						response.getOutputStream().write("ERROR (404): File not found.\n".getBytes("UTF-8"));

					}

					if (node instanceof org.structr.web.entity.File) {

						final File file = (File) node;
						if (file.isGranted(Permission.write, securityContext)) {

							try (final InputStream is = fileItem.openStream()) {

								FileHelper.writeToFile(file, is);
								file.increaseVersion();

								// upload trigger
								file.notifyUploadCompletion();
							}

						} else {

							response.setStatus(HttpServletResponse.SC_FORBIDDEN);
							response.getOutputStream().write("ERROR (403): Write access forbidden.\n".getBytes("UTF-8"));

						}
					}

				} catch (IOException ex) {
					logger.warn("Could not write to file", ex);
				}

			}

			tx.success();

		} catch (FrameworkException | IOException | FileUploadException t) {

			logger.error("Exception while processing request", t);
			UiAuthenticator.writeInternalServerError(response);
		}
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

	private IJsonInput cleanAndParseJsonString(final App app, final String input) throws FrameworkException {

		final Gson gson      = getGson();
		IJsonInput jsonInput = null;

		// isolate input parsing (will include read and write operations)
		try (final Tx tx = app.tx()) {

			jsonInput   = gson.fromJson(input, IJsonInput.class);
			tx.success();

		} catch (JsonSyntaxException jsx) {
			//logger.warn("", jsx);
			throw new FrameworkException(400, jsx.getMessage());
		}

		if (jsonInput == null) {

			if (org.tuckey.web.filters.urlrewrite.utils.StringUtils.isBlank(input)) {

				try (final Tx tx = app.tx()) {

					jsonInput   = gson.fromJson("{}", IJsonInput.class);
					tx.success();
				}

			} else {

				jsonInput = new JsonSingleInput();
			}
		}

		return jsonInput;

	}

	private Map<String, Object> convertPropertySetToMap(JsonInput propertySet) {

		if (propertySet != null) {
			return propertySet.getAttributes();
		}

		return new LinkedHashMap<>();
	}

	private synchronized Folder getOrCreateFolderPath(SecurityContext securityContext, String path) {

		try (final Tx tx = StructrApp.getInstance().tx()) {

			Folder folder = FileHelper.createFolderPath(securityContext, path);
			tx.success();

			return folder;

		} catch (FrameworkException ex) {
			logger.warn("", ex);
		}

		return null;
	}

	private String errorPage(final FrameworkException t) {
		return "<html><head><title>Error in Upload</title></head><body><h1>Error in Upload</h1><p>" + t.toJSON() + "</p>\n</body></html>";
	}

	private String errorPage(final Throwable t) {
		return "<html><head><title>Error in Upload</title></head><body><h1>Error in Upload</h1><p>" + t.getMessage() + "</p>\n</body></html>";
	}
}
