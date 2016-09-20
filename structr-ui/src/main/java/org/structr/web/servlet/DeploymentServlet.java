/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import com.google.common.io.Files;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.io.IOUtils;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.ThreadLocalMatcher;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.graph.Tx;
import org.structr.rest.service.HttpServiceServlet;
import org.structr.rest.service.StructrHttpServiceConfig;
import org.structr.schema.SchemaHelper;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.maintenance.DeployCommand;

//~--- classes ----------------------------------------------------------------
/**
 * Endpoint for deployment file upload
 */
public class DeploymentServlet extends HttpServlet implements HttpServiceServlet {

	private static final Logger logger = Logger.getLogger(DeploymentServlet.class.getName());
	private static final ThreadLocalMatcher threadLocalUUIDMatcher = new ThreadLocalMatcher("[a-fA-F0-9]{32}");

	private static final int MEGABYTE = 1024 * 1024;
	private static final int MEMORY_THRESHOLD = 10 * MEGABYTE;  // above 10 MB, files are stored on disk
	private static final String MAX_FILE_SIZE = "1000"; // unit is MB
	private static final String MAX_REQUEST_SIZE = "1000"; // unit is MB

	// non-static fields
	private ServletFileUpload uploader = null;
	private File filesDir = null;
	private final StructrHttpServiceConfig config = new StructrHttpServiceConfig();

	public DeploymentServlet() {
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

			logger.log(Level.WARNING, "", t);
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

			final SecurityContext securityContext;
			try {
				securityContext = getConfig().getAuthenticator().initializeAndExamineRequest(request, response);

			} catch (AuthenticationException ae) {

				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				response.getOutputStream().write("ERROR (401): Invalid user or password.\n".getBytes("UTF-8"));
				return;
			}

			if (securityContext.getUser(false) == null && Boolean.FALSE.equals(Boolean.parseBoolean(StructrApp.getConfigurationValue("DeploymentServlet.allowAnonymousUploads", "false")))) {
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

			uploader.setFileSizeMax(MEGABYTE * Long.parseLong(StructrApp.getConfigurationValue("DeploymentServlet.maxFileSize", MAX_FILE_SIZE)));
			uploader.setSizeMax(MEGABYTE * Long.parseLong(StructrApp.getConfigurationValue("DeploymentServlet.maxRequestSize", MAX_REQUEST_SIZE)));

			response.setContentType("text/html");
			final PrintWriter out = response.getWriter();

			List<FileItem> fileItemsList = uploader.parseRequest(request);
			Iterator<FileItem> fileItemsIterator = fileItemsList.iterator();

			final Map<String, Object> params = new HashMap<>();

			while (fileItemsIterator.hasNext()) {

				final FileItem item = fileItemsIterator.next();

				try {
					final String directoryPath = "/tmp/" + UUID.randomUUID();
					final String filePath      = directoryPath + ".zip";

					File file = new File(filePath);
					Files.write(IOUtils.toByteArray(item.getInputStream()), file);
					
					unzip(file, directoryPath);
					
					DeployCommand deployCommand = StructrApp.getInstance(securityContext).command(DeployCommand.class);

					final Map<String, Object> attributes = new HashMap<>();
					attributes.put("source", directoryPath  + "/" + StringUtils.substringBeforeLast(item.getName(), "."));

					deployCommand.execute(attributes);
					
					file.deleteOnExit();
					File dir = new File(directoryPath);
					
					dir.deleteOnExit();

				} catch (IOException ex) {
					logger.log(Level.WARNING, "Could not upload file", ex);
				}

			}

			tx.success();

		} catch (FrameworkException | IOException | FileUploadException t) {


			logger.log(Level.SEVERE, "Exception while processing request", t);
			UiAuthenticator.writeInternalServerError(response);
		}
	}
	
	/**
	 * Unzip given file to given output directory.
	 * 
	 * @param file
	 * @param outputDir
	 * @throws IOException 
	 */
	private void unzip(final File file, final String outputDir) throws IOException {
	
		try (final ZipFile zipFile = new ZipFile(file)) {
			
			final Enumeration<? extends ZipEntry> entries = zipFile.entries();
			
			while (entries.hasMoreElements()) {
				
				final ZipEntry entry = entries.nextElement();
				final File targetFile = new File(outputDir, entry.getName());
				
				if (entry.isDirectory()) {
				
					targetFile.mkdirs();
				
				} else {
					
					targetFile.getParentFile().mkdirs();
					InputStream in = zipFile.getInputStream(entry);

					try (OutputStream out = new FileOutputStream(targetFile)) {

						IOUtils.copy(in, out);
						IOUtils.closeQuietly(in);
					}
				}
			}
		}
	}

}

