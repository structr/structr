/**
 * Copyright (C) 2010-2015 Structr GmbH
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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.structr.common.AccessMode;
import org.structr.common.PathHelper;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.ThreadLocalMatcher;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.rest.service.HttpServiceServlet;
import org.structr.rest.service.StructrHttpServiceConfig;
import org.structr.schema.SchemaHelper;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.Image;
import org.structr.web.entity.VideoFile;

//~--- classes ----------------------------------------------------------------
/**
 * Simple upload servlet.
 *
 *
 */
public class UploadServlet extends HttpServlet implements HttpServiceServlet {

	private static final Logger logger = Logger.getLogger(UploadServlet.class.getName());
	private static final ThreadLocalMatcher threadLocalUUIDMatcher = new ThreadLocalMatcher("[a-fA-F0-9]{32}");

	private static final int MEGABYTE = 1024 * 1024;
	private static final int MEMORY_THRESHOLD = 10 * MEGABYTE;  // above 10 MB, files are stored on disk
	private static final String MAX_FILE_SIZE = "1000"; // unit is MB
	private static final String MAX_REQUEST_SIZE = "1000"; // unit is MB

	// non-static fields
	private ServletFileUpload uploader = null;
	private File filesDir = null;
	private final StructrHttpServiceConfig config = new StructrHttpServiceConfig();

	public UploadServlet() {
	}

	//~--- methods --------------------------------------------------------
	@Override
	public StructrHttpServiceConfig getConfig() {
		return config;
	}

	@Override
	public void init() {

		try (final Tx tx = StructrApp.getInstance().tx()) {
			DiskFileItemFactory fileFactory = new DiskFileItemFactory();
			fileFactory.setSizeThreshold(MEMORY_THRESHOLD);

			filesDir = new File(Services.getInstance().getConfigurationValue(Services.TMP_PATH)); // new File(Services.getInstance().getTmpPath());
			if (!filesDir.exists()) {
				filesDir.mkdir();
			}

			fileFactory.setRepository(filesDir);
			uploader = new ServletFileUpload(fileFactory);

			tx.success();

		} catch (FrameworkException t) {

			t.printStackTrace();
		}
	}

	@Override
	public void destroy() {
	}

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException {

		try (final Tx tx = StructrApp.getInstance().tx()) {

			if (!ServletFileUpload.isMultipartContent(request)) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getOutputStream().write("ERROR (400): Request does not contain multipart content.\n".getBytes("UTF-8"));
				return;
			}

			final SecurityContext securityContext = getConfig().getAuthenticator().initializeAndExamineRequest(request, response);

			if (securityContext.getUser(false) == null && Boolean.FALSE.equals(Boolean.parseBoolean(StructrApp.getConfigurationValue("UploadServlet.allowAnonymousUploads", "false")))) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				response.getOutputStream().write("ERROR (403): Anonymous uploads forbidden.\n".getBytes("UTF-8"));
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

			uploader.setFileSizeMax(MEGABYTE * Long.parseLong(StructrApp.getConfigurationValue("UploadServlet.maxFileSize", MAX_FILE_SIZE)));
			uploader.setSizeMax(MEGABYTE * Long.parseLong(StructrApp.getConfigurationValue("UploadServlet.maxRequestSize", MAX_REQUEST_SIZE)));

			response.setContentType("text/html");
			final PrintWriter out = response.getWriter();

			List<FileItem> fileItemsList = uploader.parseRequest(request);
			Iterator<FileItem> fileItemsIterator = fileItemsList.iterator();

			final Map<String, Object> params = new HashMap<>();

			while (fileItemsIterator.hasNext()) {

				final FileItem item = fileItemsIterator.next();

				if (item.isFormField()) {

					params.put(item.getFieldName(), item.getString());

				} else {

					try {

						final String contentType = item.getContentType();
						boolean isImage = (contentType != null && contentType.startsWith("image"));
						boolean isVideo = (contentType != null && contentType.startsWith("video"));

						// Override type from path info
						if (params.containsKey(NodeInterface.type.jsonName())) {
							type = (String) params.get(NodeInterface.type.jsonName());
						}

						final Class cls = type != null
							? SchemaHelper.getEntityClassForRawType(type)
							: (isImage
								? Image.class
								: (isVideo
									? VideoFile.class
									: org.structr.dynamic.File.class));

						final String name = item.getName().replaceAll("\\\\", "/");

						final org.structr.dynamic.File newFile = FileHelper.createFile(securityContext, IOUtils.toByteArray(item.getInputStream()), contentType, cls);
						newFile.setProperty(AbstractNode.name, PathHelper.getName(name));

						if (!newFile.validatePath(securityContext, null)) {
							newFile.setProperty(AbstractNode.name, name.concat("_").concat(FileHelper.getDateString()));
						}

						PropertyMap additionalProperties = PropertyMap.inputTypeToJavaType(securityContext, cls, params);

						for (PropertyKey key : additionalProperties.keySet()) {

							newFile.setProperty(key, additionalProperties.get(key));

						}

						// upload trigger
						newFile.notifyUploadCompletion();

						// Just write out the uuids of the new files
						out.write(newFile.getUuid());

					} catch (IOException ex) {
						logger.log(Level.WARNING, "Could not upload file", ex);
					}

				}

			}

			tx.success();

		} catch (FrameworkException | IOException | FileUploadException t) {

			t.printStackTrace();
			logger.log(Level.SEVERE, "Exception while processing request", t);
			UiAuthenticator.writeInternalServerError(response);
		}
	}

	@Override
	protected void doPut(final HttpServletRequest request, final HttpServletResponse response) throws ServletException {

		try (final Tx tx = StructrApp.getInstance().tx(false, false, false)) {

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

			uploader.setFileSizeMax(MEGABYTE * Long.parseLong(StructrApp.getConfigurationValue("UploadServlet.maxFileSize", MAX_FILE_SIZE)));
			uploader.setSizeMax(MEGABYTE * Long.parseLong(StructrApp.getConfigurationValue("UploadServlet.maxRequestSize", MAX_REQUEST_SIZE)));

			List<FileItem> fileItemsList = uploader.parseRequest(request);
			Iterator<FileItem> fileItemsIterator = fileItemsList.iterator();

			while (fileItemsIterator.hasNext()) {

				final FileItem fileItem = fileItemsIterator.next();

				try {

					final GraphObject node = StructrApp.getInstance().getNodeById(uuid);

					if (node == null) {

						response.setStatus(HttpServletResponse.SC_NOT_FOUND);
						response.getOutputStream().write("ERROR (404): File not found.\n".getBytes("UTF-8"));

					}

					if (node instanceof org.structr.web.entity.AbstractFile) {

						final org.structr.dynamic.File file = (org.structr.dynamic.File) node;
						if (file.isGranted(Permission.write, securityContext)) {

							FileHelper.writeToFile(file, fileItem.getInputStream());
							file.increaseVersion();

							// upload trigger
							file.notifyUploadCompletion();

						} else {

							response.setStatus(HttpServletResponse.SC_FORBIDDEN);
							response.getOutputStream().write("ERROR (403): Write access forbidden.\n".getBytes("UTF-8"));

						}
					}

				} catch (IOException ex) {
					logger.log(Level.WARNING, "Could not write to file", ex);
				}

			}

			tx.success();

		} catch (FrameworkException | IOException | FileUploadException t) {

			t.printStackTrace();
			logger.log(Level.SEVERE, "Exception while processing request", t);
			UiAuthenticator.writeInternalServerError(response);
		}
	}
}
