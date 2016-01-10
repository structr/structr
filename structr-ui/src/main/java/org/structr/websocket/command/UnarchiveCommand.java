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
package org.structr.websocket.command;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.PathHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.dynamic.File;
import org.structr.web.common.FileHelper;
import org.structr.web.common.ImageHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Image;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- classes ----------------------------------------------------------------
/**
 * Websocket command for un-archiving archive files.
 *
 *
 */
public class UnarchiveCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(UnarchiveCommand.class.getName());

	static {

		StructrWebSocket.addCommand(UnarchiveCommand.class);

	}

	//~--- methods --------------------------------------------------------
	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final Set<String> supportedByArchiveStreamFactory = new HashSet<>(Arrays.asList(new String[]{
			ArchiveStreamFactory.AR,
			ArchiveStreamFactory.ARJ,
			ArchiveStreamFactory.CPIO,
			ArchiveStreamFactory.DUMP,
			ArchiveStreamFactory.JAR,
			ArchiveStreamFactory.TAR,
			ArchiveStreamFactory.ZIP
		}));

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final App app                         = StructrApp.getInstance(securityContext);

		try {

			final String id = (String) webSocketData.getId();
			final File file;

			try (final Tx tx = app.tx()) {

				file = app.get(File.class, id);

				if (file == null) {
					getWebSocket().send(MessageBuilder.status().code(400).message("File not found: ".concat(id)).build(), true);
					return;
				}

				final String fileExtension = StringUtils.substringAfterLast(file.getName(), ".");
				if (!supportedByArchiveStreamFactory.contains(fileExtension)) {

					getWebSocket().send(MessageBuilder.status().code(400).message("Unsupported archive format: ".concat(fileExtension)).build(), true);
					return;
				}

				tx.success();
			}

			// no transaction here since this is a bulk command
			unarchive(securityContext, file);


		} catch (Throwable t) {

			t.printStackTrace();

			String msg = t.toString();

			try (final Tx tx = app.tx()) {

				// return error message
				getWebSocket().send(MessageBuilder.status().code(400).message("Could not unarchive file: ".concat((msg != null) ? msg : "")).build(), true);

				tx.success();

			} catch (FrameworkException ignore) {}

		}
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	/**
	 * Return the folder node which corresponds with the parent path.
	 *
	 * @param path
	 */
	private Folder createOrGetParentFolder(final SecurityContext securityContext, final String path) throws FrameworkException {

		final String[] parts = PathHelper.getParts(path);

		// string is empty or a single file in root dir
		if (parts == null || parts.length == 1) {
			return null;
		}

		// Find root folder
		final App app = StructrApp.getInstance(securityContext);
		Folder folder = null;
		for (final Folder possibleRootFolder : app.nodeQuery(Folder.class).andName(parts[0])) {

			if (possibleRootFolder.getProperty(AbstractFile.parent) == null) {
				folder = possibleRootFolder;
				break;
			}

		}

		if (folder == null) {

			// Root folder doesn't exist, so create it and all child folders
			folder = app.create(Folder.class, parts[0]);
			logger.log(Level.INFO, "Created root folder {0}", new Object[]{parts[0]});

			for (int i = 1; i < parts.length - 1; i++) {
				Folder childFolder = app.create(Folder.class, parts[i]);
				childFolder.setProperty(AbstractFile.parent, folder);
				logger.log(Level.INFO, "Created {0} {1} with path {2}", new Object[]{childFolder.getType(), childFolder, FileHelper.getFolderPath(childFolder)});
				folder = childFolder;
			}

			return folder;

		}

		// Root folder exists, so walk over children and search for next path part
		for (int i = 1; i < parts.length - 1; i++) {

			Folder subFolder = null;

			for (AbstractFile child : folder.getProperty(Folder.children)) {

				if (child instanceof Folder && child.getName().equals(parts[i])) {
					subFolder = (Folder) child;
					break;
				}

			}

			if (subFolder == null) {

				// sub folder doesn't exist, so create it and all child folders
				subFolder = app.create(Folder.class, parts[i]);
				subFolder.setProperty(AbstractFile.parent, folder);
				logger.log(Level.INFO, "Created {0} {1} with path {2}", new Object[]{subFolder.getType(), subFolder, FileHelper.getFolderPath(subFolder)});

			}

			folder = subFolder;
		}

		return folder;

	}

	private void unarchive(final SecurityContext securityContext, final File file) throws ArchiveException, IOException, FrameworkException {

		final App app = StructrApp.getInstance(securityContext);
		final InputStream is;

		try (final Tx tx = app.tx()) {

			final String fileName = file.getName();

			logger.log(Level.INFO, "Unarchiving file {0}", fileName);

			is = file.getInputStream();
			tx.success();


			if (is == null) {

				getWebSocket().send(MessageBuilder.status().code(400).message("Could not get input stream from file ".concat(fileName)).build(), true);
				return;
			}
		}

		final ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream(new BufferedInputStream(is));
		ArchiveEntry entry          = in.getNextEntry();
		int overallCount            = 0;

		while (entry != null) {

			try (final Tx tx = app.tx()) {

				int count = 0;

				while (entry != null && count++ < 50) {

					final String entryPath = "/" + PathHelper.clean(entry.getName());
					logger.log(Level.INFO, "Entry path: {0}", entryPath);

					final AbstractFile f = FileHelper.getFileByAbsolutePath(securityContext, entryPath);
					if (f == null) {

						final Folder parentFolder = createOrGetParentFolder(securityContext, entryPath);
						final String name         = PathHelper.getName(entry.getName());

						if (StringUtils.isNotEmpty(name) && (parentFolder == null || !(FileHelper.getFolderPath(parentFolder).equals(entryPath)))) {

							AbstractFile fileOrFolder = null;

							if (entry.isDirectory()) {

								fileOrFolder = app.create(Folder.class, name);

							} else {

								fileOrFolder = ImageHelper.isImageType(name)
									? ImageHelper.createImage(securityContext, in, null, Image.class, name, false)
									: FileHelper.createFile(securityContext, in, null, File.class, name);
							}

							if (parentFolder != null) {
								fileOrFolder.setProperty(AbstractFile.parent, parentFolder);
							}

							logger.log(Level.INFO, "Created {0} {1} with path {2}", new Object[]{fileOrFolder.getType(), fileOrFolder, FileHelper.getFolderPath(fileOrFolder)});

							// create thumbnails while importing data
							if (fileOrFolder instanceof Image) {
								fileOrFolder.getProperty(Image.tnMid);
								fileOrFolder.getProperty(Image.tnSmall);
							}

						}
					}

					entry = in.getNextEntry();

					overallCount++;
				}

				logger.log(Level.INFO, "Committing transaction after {0} files..", overallCount);

				tx.success();
			}

		}

		in.close();

	}

	//~--- get methods ----------------------------------------------------
	@Override
	public String getCommand() {
		return "UNARCHIVE";
	}
}
