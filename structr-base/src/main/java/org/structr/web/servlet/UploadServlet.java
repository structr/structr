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
package org.structr.web.servlet;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.io.QuietException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.RetryException;
import org.structr.api.config.Settings;
import org.structr.common.AccessMode;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.PathHelper;
import org.structr.core.GraphObject;
import org.structr.core.IJsonInput;
import org.structr.core.JsonInput;
import org.structr.core.JsonSingleInput;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.Authenticator;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.rest.JsonInputGSONAdapter;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.rest.exception.NotAllowedException;
import org.structr.rest.exception.NotFoundException;
import org.structr.rest.service.HttpServiceServlet;
import org.structr.rest.service.StructrHttpServiceConfig;
import org.structr.rest.servlet.AbstractServletBase;
import org.structr.schema.SchemaHelper;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Simple upload servlet.
 */
public class UploadServlet extends AbstractServletBase implements HttpServiceServlet {

	private static final Set<String> AllowedProperties = Set.of(
		GraphObjectTraitDefinition.TYPE_PROPERTY,
		NodeInterfaceTraitDefinition.NAME_PROPERTY,
		AbstractFileTraitDefinition.PARENT_ID_PROPERTY,
		AbstractFileTraitDefinition.PARENT_PROPERTY
	);

	private static final Logger logger                             = LoggerFactory.getLogger(UploadServlet.class.getName());
	private static final String REDIRECT_AFTER_UPLOAD_PARAMETER    = "redirectOnSuccess";
	private static final String APPEND_UUID_ON_REDIRECT_PARAMETER  = "appendUuidOnRedirect";
	private static final String UPLOAD_FOLDER_PATH_PARAMETER       = "uploadFolderPath";
	private static final String FILE_SCHEMA_TYPE_PARAMETER         = "type";
	private static final long MEGABYTE                              = 1024 * 1024;

	// non-static fields
	private final StructrHttpServiceConfig config = new StructrHttpServiceConfig();
	private final java.io.File filesDir           = null;

	public UploadServlet() {
	}

	@Override
	public void configureServletHolder(final ServletHolder servletHolder) {
		MultipartConfigElement multipartConfigElement = new MultipartConfigElement("", MEGABYTE * Settings.UploadMaxFileSize.getValue(), MEGABYTE * Settings.UploadMaxRequestSize.getValue(), (int)MEGABYTE);
		servletHolder.getRegistration().setMultipartConfig(multipartConfigElement);
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
	public void destroy() {
	}

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException {

		try {

			assertInitialized();

		} catch (FrameworkException fex) {

			try {
				response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
				response.getOutputStream().write(fex.getMessage().getBytes(StandardCharsets.UTF_8));

			} catch (IOException ioex) {

				logger.warn("Unable to send response", ioex);
			}

			return;
		}

		setCustomResponseHeaders(response);

		try {

			if (!request.getContentType().startsWith("multipart/form-data") || request.getParts().size() <= 0) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getOutputStream().write("ERROR (400): Request does not contain multipart content.\n".getBytes(StandardCharsets.UTF_8));
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
				response.getOutputStream().write("ERROR (401): Invalid user or password.\n".getBytes(StandardCharsets.UTF_8));
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

		final App app = StructrApp.getInstance(securityContext);

		try {

			try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

				if (securityContext.getUser(false) == null && !Settings.UploadAllowAnonymous.getValue()) {

					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					response.getOutputStream().write("ERROR (401): Anonymous uploads forbidden.\n".getBytes(StandardCharsets.UTF_8));

					return;
				}

				tx.success();
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

			response.setContentType("text/html");

			final Map<String, Object> params = new HashMap<>();
			String uuid = null;

			// 1. Collect non-file parts
			final Collection<Part> parts = request.getParts();
			for (Part p : parts) {

				if (p.getSubmittedFileName() == null) {

					final String fieldValue = IOUtils.toString(p.getInputStream(), StandardCharsets.UTF_8);
					final String fieldName  = p.getName();

					if (REDIRECT_AFTER_UPLOAD_PARAMETER.equals(fieldName)) {

						redirectUrl = fieldValue;

					} else if (APPEND_UUID_ON_REDIRECT_PARAMETER.equals(fieldName)) {

						appendUuidOnRedirect = "true".equalsIgnoreCase(fieldValue);

					} else if (UPLOAD_FOLDER_PATH_PARAMETER.equals(fieldName)) {

						path = fieldValue;

					} else if (FILE_SCHEMA_TYPE_PARAMETER.equals(fieldName)) {

						type = fieldValue;

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
				}
			}

			// 2. Handle file parts
			for (Part p : parts) {

				if (p.getSubmittedFileName() != null) {

					final String contentType = p.getContentType();

					Traits cls = null;
					if (type != null) {

						if (Traits.exists(type)) {

							cls = Traits.of(type);

						} else {

							logger.warn("Unable to create entity of type '" + type + "', class is not defined.");
						}
					}

					if (cls == null) {

						boolean isImage = (contentType != null && contentType.startsWith("image"));
						boolean isVideo = (contentType != null && contentType.startsWith("video"));

						if (isImage) {

							cls = Traits.of(StructrTraits.IMAGE);

						} else if (isVideo) {

							cls = Traits.of(StructrTraits.VIDEO_FILE);

						} else {

							cls = Traits.of(StructrTraits.FILE);
						}
					}

					if (cls != null) {

						type = cls.getName();
					}

					final Set<String> forbiddenProperties = new LinkedHashSet<>();

					// check parameters against whitelist (and allow dynamic properties to be set)
					// (this needs to be done here because we need the target type)
					for (final Iterator<String> it = params.keySet().iterator(); it.hasNext(); ) {

						final String propertyName = it.next();
						if (cls.hasKey(propertyName)) {

							final PropertyKey key = cls.key(propertyName);

							if (!AllowedProperties.contains(propertyName) && !key.isDynamic()) {

								forbiddenProperties.add(propertyName);
							}
						}
					}

					if (!forbiddenProperties.isEmpty()) {
						throw new FrameworkException(422, "Additional file properties found which are not allowed to be set during upload: '" + forbiddenProperties + "'. Only custom properties and the following properties may be set: '" + AllowedProperties + "'");
					}

					Folder uploadFolder                         = null;
					final String defaultUploadFolderConfigValue = getDefaultUploadFolderPathValue();

					if (params.containsKey(AbstractFileTraitDefinition.PARENT_PROPERTY)) {

						uploadFolder = getFolderParent(securityContext, params.get(AbstractFileTraitDefinition.PARENT_PROPERTY).toString());
					}

					if (params.containsKey(AbstractFileTraitDefinition.PARENT_ID_PROPERTY)) {

						uploadFolder = getFolderParent(securityContext, params.get(AbstractFileTraitDefinition.PARENT_ID_PROPERTY).toString());
					}

					final String name = (p.getSubmittedFileName() != null ? p.getSubmittedFileName() : p.getName()).replaceAll("\\\\", "/");
					File newFile = null;
					boolean retry = true;

					while (retry) {

						retry = false;

						// If a path attribute was sent, create all folders on the fly.
						if (uploadFolder == null) {

							if (path != null) {

								final boolean isUnderneathDefaultFolder = path.startsWith(defaultUploadFolderConfigValue);

								if (isUnderneathDefaultFolder && securityContext.getUser(false) != null) {

									uploadFolder = getOrCreateFolderPath(securityContext, path);

								} else {

									throw new NotAllowedException("Using the '" + UPLOAD_FOLDER_PATH_PARAMETER + "' parameter is only allowed for authenticated users and only underneath the default upload folder.");
								}

							} else if (StringUtils.isNotBlank(defaultUploadFolderConfigValue)) {

								uploadFolder = getOrCreateFolderPath(SecurityContext.getSuperUserInstance(), defaultUploadFolderConfigValue);
							}
						}

						try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

							try (final InputStream is = p.getInputStream()) {

								newFile = FileHelper.createFile(securityContext, is, contentType, type, name, uploadFolder).as(File.class);

								final PropertyMap changedProperties = new PropertyMap();

								changedProperties.putAll(PropertyMap.inputTypeToJavaType(securityContext, type, params));

								// Update type as it could have changed
								changedProperties.put(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.TYPE_PROPERTY), type);

								newFile.unlockSystemPropertiesOnce();
								newFile.setProperties(securityContext, changedProperties, true);

								// validate and rename file after setting all properties (as the folder might have changed)
								newFile.validateAndRenameFileOnce(securityContext, null);

								uuid = newFile.getUuid();

							} catch (IOException ex) {

								logger.warn("Unable to store file: {}", ex.getMessage());
							}

							tx.success();

						} catch (RetryException rex) {

							retry = true;
						}
					}

					// since the transaction can be repeated, we need to make sure that
					// only the actual existing file creates a UUID output
					if (newFile != null) {

						// all property access must be wrapped in a transaction
						try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

							// upload trigger
							newFile.notifyUploadCompletion();

							newFile.callOnUploadHandler(securityContext);

							// store uuid
							uuid = newFile.getUuid();

							tx.success();
						}
					}
				}
			}

			// send redirect to allow form-based file upload without JavaScript...
			if (StringUtils.isNotBlank(redirectUrl)) {

				if (appendUuidOnRedirect) {

					sendRedirectHeader(response, redirectUrl + (redirectUrl.endsWith("/") ? "" : "/") + uuid, false);    // user-provided, should be already prefixed

				} else {

					sendRedirectHeader(response, redirectUrl, false);    // user-provided, should be already prefixed
				}

			} else {

				// Just write out the uuids of the new files
				if (uuid != null) {

					response.getWriter().write(uuid);
				}
			}

		} catch (Throwable t) {

			final String content;

			if (t instanceof FrameworkException fex) {

				logger.error(fex.toString());
				content = errorPage(fex);

				// set response status accordingly
				response.setStatus(fex.getStatus());

			} else {

				if (t instanceof QuietException || t.getCause() instanceof QuietException) {
					// ignore exceptions which (by jettys standards) should be handled less verbosely
				} else {
					logger.error("Exception while processing upload request", t);
				}

				content = errorPage(t);

				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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
				response.getOutputStream().write(fex.getMessage().getBytes(StandardCharsets.UTF_8));

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
				response.getOutputStream().write("URL path doesn't end with UUID.\n".getBytes(StandardCharsets.UTF_8));
				return;
			}

			if (!Settings.isValidUuid(uuid)) {

				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getOutputStream().write("ERROR (400): URL path doesn't end with UUID.\n".getBytes(StandardCharsets.UTF_8));
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

			for (Part p : request.getParts()) {

				try {

					final GraphObject node = StructrApp.getInstance().getNodeById(uuid);

					if (node == null) {

						response.setStatus(HttpServletResponse.SC_NOT_FOUND);
						response.getOutputStream().write("ERROR (404): File not found.\n".getBytes(StandardCharsets.UTF_8));
					}

					if (node instanceof NodeInterface n && n.is(StructrTraits.FILE)) {

						final File file = n.as(File.class);
						if (n.isGranted(Permission.write, securityContext)) {

							try (final InputStream is = p.getInputStream()) {

								FileHelper.writeToFile(file, is);
								file.increaseVersion();

								// upload trigger
								file.notifyUploadCompletion();
							}

						} else {

							response.setStatus(HttpServletResponse.SC_FORBIDDEN);
							response.getOutputStream().write("ERROR (403): Write access forbidden.\n".getBytes(StandardCharsets.UTF_8));
						}
					}

				} catch (IOException ex) {
					logger.warn("Could not write to file", ex);
				}
			}

			tx.success();

		} catch (FrameworkException | IOException  t) {

			logger.error("Exception while processing request", t);
			UiAuthenticator.writeInternalServerError(response);
		}
	}

	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response) {

		setCustomResponseHeaders(response);

		try {

			assertInitialized();

			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=utf-8");

			// check if this is a CORS preflight request
			final String origin      = request.getHeader("Origin");
			final String corsHeaders = request.getHeader("Access-Control-Request-Headers");
			final String corsMethod  = request.getHeader("Access-Control-Request-Method");
			int statusCode           = HttpServletResponse.SC_OK;

			if (origin != null && corsHeaders != null && corsMethod != null) {

				final Authenticator auth = getConfig().getAuthenticator();
				// Ensure CORS settings apply by letting the authenticator examine the request.
				auth.initializeAndExamineRequest(request, response);

			} else {

				// OPTIONS is not allowed for non-CORS requests
				statusCode = HttpServletResponse.SC_METHOD_NOT_ALLOWED;
			}

			response.setContentLength(0);
			response.setStatus(statusCode);

		} catch (Throwable t) {

			logger.warn("Exception in UploadServlet OPTIONS", t);


		} finally {

			try {

				response.getWriter().close();

			} catch (Throwable t) {

				logger.warn("Unable to flush and close response: {}", t.getMessage());
			}
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

			jsonInput = gson.fromJson(input, IJsonInput.class);
			tx.success();

		} catch (JsonSyntaxException jsx) {

			throw new FrameworkException(400, jsx.getMessage());
		}

		if (jsonInput == null) {

			if (StringUtils.isBlank(input)) {

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

	private synchronized Folder getOrCreateFolderPath(final SecurityContext securityContext, final String path) {

		try (final Tx tx = StructrApp.getInstance().tx()) {

			final NodeInterface newFolder = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), path);
			Folder folder = newFolder != null ? newFolder.as(Folder.class) : null;

			// prevent folders from being committed to the DB if creating them should not be allowed
			checkUploadFolderRightsIfNecessary(securityContext, folder);

			tx.success();

			return folder;

		} catch (FrameworkException ex) {
			logger.warn("", ex);
		}

		return null;
	}

	/**
	 * Checks the given targetFolder if it is underneath the default upload folder.
	 * If yes, no write rights are necessary.
	 * If no, the current user must have write rights to the folder.
	 *
	 * To get the full folder path of the folder, it is re-fetched from database in a privileged context
	 *
	 * This targets 2 scenarios:
	 * - upload data contains "parent" or "parentId" attribute: If this folder is not underneath the default upload folder, the user must be allowed to write to it
	 * - upload data contains "uploadFolderPath" where a complete folder path is being created: If it is not underneath the default upload folder, the user must be allowed to write to it
	 * @param securityContext
	 * @param targetFolder
	 * @throws FrameworkException
	 * @throws NotAllowedException
	 */
	private void checkUploadFolderRightsIfNecessary(final SecurityContext securityContext, final Folder targetFolder) throws FrameworkException, NotAllowedException {

		final App privilegedApp = StructrApp.getInstance();
		try (final Tx tx = privilegedApp.tx()) {

			final Folder folder_privileged          = privilegedApp.getNodeById(StructrTraits.FOLDER, targetFolder.getUuid()).as(Folder.class);
			final String givenFolderPath            = folder_privileged.getPath();
			final boolean isUnderneathDefaultFolder = givenFolderPath.startsWith(getDefaultUploadFolderPathValue());
			final boolean requiredWriteRights       = (false == isUnderneathDefaultFolder);

			if (requiredWriteRights) {

				final boolean isAllowed = targetFolder.isGranted(Permission.write, securityContext);

				if (!isAllowed) {

					throw new NotAllowedException("User is not allowed to write to given folder.");
				}
			}
		}
	}

	private Folder getFolderParent(final SecurityContext securityContext, final String parentId) throws FrameworkException {

		Folder folder = null;
		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			NodeInterface node = app.getNodeById(parentId);

			if (node == null) {
				throw new NotFoundException("Upload folder not found: " + parentId);
			}

			folder = node.as(Folder.class);
		}

		checkUploadFolderRightsIfNecessary(securityContext, folder);

		return folder;
	}

	private String getDefaultUploadFolderPathValue () {
		return Settings.DefaultUploadFolder.getValue() + (Settings.DefaultUploadFolder.getValue().endsWith("/") ? "" : "/");
	}

	private String errorPage(final FrameworkException t) {
		return "<html><head><title>Error in Upload</title></head><body><h1>Error in Upload</h1><p>" + t.toJSON() + "</p></body></html>";
	}

	private String errorPage(final Throwable t) {
		return "<html><head><title>Error in Upload</title></head><body><h1>Error in Upload</h1><p>" + t.getMessage() + "</p></body></html>";
	}
}
