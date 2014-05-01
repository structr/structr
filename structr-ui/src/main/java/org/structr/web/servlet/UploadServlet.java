/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Structr is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr. If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import org.structr.common.*;
import org.structr.common.SecurityContext;
import org.structr.core.Services;

//~--- JDK imports ------------------------------------------------------------
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.Tx;
import org.structr.rest.service.HttpServiceServlet;
import org.structr.rest.service.StructrHttpServiceConfig;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.Image;

//~--- classes ----------------------------------------------------------------
/**
 * Simple upload servlet.
 *
 * @author Axel Morgner
 */
public class UploadServlet extends HttpServlet implements HttpServiceServlet {

	private static final Logger logger = Logger.getLogger(UploadServlet.class.getName());

	private static final int MEMORY_THRESHOLD = 1024 * 1024 * 10;  // above 10 MB, files are stored on disk
	private static final int MAX_FILE_SIZE = 1024 * 1024 * 100; // 100 MB
	private static final int MAX_REQUEST_SIZE = 1024 * 1024 * 120; // 120 MB

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
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {

		if (!ServletFileUpload.isMultipartContent(request)) {
			throw new ServletException("Content type is not multipart/form-data");
		}

		try (final Tx tx = StructrApp.getInstance().tx()) {

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

			uploader.setFileSizeMax(MAX_FILE_SIZE);
			uploader.setSizeMax(MAX_REQUEST_SIZE);

			response.setContentType("text/html");
			final PrintWriter out = response.getWriter();

			List<FileItem> fileItemsList = uploader.parseRequest(request);
			Iterator<FileItem> fileItemsIterator = fileItemsList.iterator();

			while (fileItemsIterator.hasNext()) {

				final FileItem fileItem = fileItemsIterator.next();

				try {

					String contentType = fileItem.getContentType();
					boolean isImage = (contentType != null && contentType.startsWith("image"));

					Class type = isImage ? Image.class : org.structr.web.entity.File.class;

					String name = fileItem.getName().replaceAll("\\\\", "/");

					org.structr.web.entity.File newFile = FileHelper.createFile(securityContext, IOUtils.toByteArray(fileItem.getInputStream()), contentType, type);
					newFile.setProperty(AbstractNode.name, PathHelper.getName(name));
					newFile.setProperty(AbstractNode.visibleToPublicUsers, true);
					newFile.setProperty(AbstractNode.visibleToAuthenticatedUsers, true);

					// Just write out the uuids of the new files
					out.write(newFile.getUuid());

				} catch (IOException ex) {
					logger.log(Level.WARNING, "Could not upload file", ex);
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
