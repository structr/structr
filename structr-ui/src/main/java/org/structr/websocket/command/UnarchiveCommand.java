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
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PathHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.dynamic.File;
import org.structr.web.common.FileHelper;
import org.structr.web.common.ImageHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.FileBase;
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

	private static final Logger logger = LoggerFactory.getLogger(UnarchiveCommand.class.getName());

	static {

		StructrWebSocket.addCommand(UnarchiveCommand.class);

	}

	//~--- methods --------------------------------------------------------
	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

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
		final App app = StructrApp.getInstance(securityContext);

		try {

			final String id = (String) webSocketData.getId();
			final String parentFolderId = (String) webSocketData.getNodeData().get("parentFolderId");

			final FileBase file;

			try (final Tx tx = app.tx()) {

				file = app.get(FileBase.class, id);

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
			unarchive(securityContext, file, parentFolderId);

		} catch (Throwable t) {

			logger.warn("", t);

			String msg = t.toString();

			try (final Tx tx = app.tx()) {

				// return error message
				getWebSocket().send(MessageBuilder.status().code(400).message("Could not unarchive file: ".concat((msg != null) ? msg : "")).build(), true);
				getWebSocket().send(MessageBuilder.finished().callback(callback).data("success", false).build(), true);

				tx.success();

			} catch (FrameworkException ignore) {
			}

		}
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	private void unarchive(final SecurityContext securityContext, final FileBase file, final String parentFolderId) throws ArchiveException, IOException, FrameworkException {

		final App app = StructrApp.getInstance(securityContext);
		final InputStream is;

		Folder existingParentFolder = null;

		final String fileName = file.getName();

		try (final Tx tx = app.tx()) {

			// search for existing parent folder
			existingParentFolder = app.get(Folder.class, parentFolderId);
			String parentFolderName = null;

			String msgString = "Unarchiving file {}";
			if (existingParentFolder != null) {

				parentFolderName = existingParentFolder.getName();
				msgString += " into existing folder {}.";
			}

			logger.info(msgString, new Object[]{fileName, parentFolderName});

			is = file.getInputStream();
			tx.success();

			if (is == null) {

				getWebSocket().send(MessageBuilder.status().code(400).message("Could not get input stream from file ".concat(fileName)).build(), true);
				return;
			}

			tx.success();
		}

		final ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream(new BufferedInputStream(is));
		ArchiveEntry entry = in.getNextEntry();
		int overallCount = 0;

		while (entry != null) {

			try (final Tx tx = app.tx(true, true, false)) { // don't send notifications for bulk commands

				int count = 0;

				while (entry != null && count++ < 50) {

					final String entryPath = "/" + PathHelper.clean(entry.getName());
					logger.info("Entry path: {}", entryPath);

					if (entry.isDirectory()) {

						final String folderPath = (existingParentFolder != null ? existingParentFolder.getPath() : "") + PathHelper.PATH_SEP + entryPath;
						final Folder newFolder = FileHelper.createFolderPath(securityContext, folderPath);

						logger.info("Created folder {} with path {}", new Object[]{newFolder, FileHelper.getFolderPath(newFolder)});

					} else {

						final String filePath = (existingParentFolder != null ? existingParentFolder.getPath() : "") + PathHelper.PATH_SEP + entryPath;

						final String name = PathHelper.getName(entryPath);

						AbstractFile newFile = ImageHelper.isImageType(name)
									? ImageHelper.createImage(securityContext, in, null, Image.class, name, false)
									: FileHelper.createFile(securityContext, in, null, File.class, name);

						final String folderPath = StringUtils.substringBeforeLast(filePath, PathHelper.PATH_SEP);
						final Folder parentFolder = FileHelper.createFolderPath(securityContext, folderPath);

						if (parentFolder != null) {
							newFile.setProperties(securityContext, new PropertyMap(AbstractFile.parent, parentFolder));
						}
						// create thumbnails while importing data
//						if (newFile instanceof Image) {
//							newFile.getProperty(Image.tnMid);
//							newFile.getProperty(Image.tnSmall);
//						}

						logger.info("Created {} file {} with path {}", new Object[]{newFile.getType(), newFile, FileHelper.getFolderPath(newFile)});

					}


					entry = in.getNextEntry();

					overallCount++;
				}

				logger.info("Committing transaction after {} files.", overallCount);

				tx.success();

				logger.info("Unarchived {} files.", overallCount);
			}

		}

		getWebSocket().send(MessageBuilder.finished().callback(callback).data("success", true).data("filename", fileName).build(), true);

		in.close();

	}

	//~--- get methods ----------------------------------------------------
	@Override
	public String getCommand() {
		return "UNARCHIVE";
	}
}
