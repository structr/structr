/**
 * Copyright (C) 2010-2020 Structr GmbH
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PathHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.web.common.FileHelper;
import org.structr.web.common.ImageHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Image;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;
import org.structr.web.entity.File;

/**
 * Websocket command for un-archiving archive files.
 */
public class UnarchiveCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(UnarchiveCommand.class.getName());

	static {

		StructrWebSocket.addCommand(UnarchiveCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		final Set<String> supportedByArchiveStreamFactory = new HashSet<>(Arrays.asList(new String[]{
			ArchiveStreamFactory.AR,
			ArchiveStreamFactory.ARJ,
			ArchiveStreamFactory.CPIO,
			ArchiveStreamFactory.DUMP,
			ArchiveStreamFactory.JAR,
			ArchiveStreamFactory.TAR,
			ArchiveStreamFactory.SEVEN_Z,
			ArchiveStreamFactory.ZIP
		}));

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final App app = StructrApp.getInstance(securityContext);

		try {

			final String id = (String) webSocketData.getId();
			final String parentFolderId = webSocketData.getNodeDataStringValue("parentFolderId");

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

	private void unarchive(final SecurityContext securityContext, final File file, final String parentFolderId) throws ArchiveException, IOException, FrameworkException {

		final App app = StructrApp.getInstance(securityContext);
		final InputStream is;

		Folder existingParentFolder = null;

		final String fileName = file.getName();

		try (final Tx tx = app.tx(true, true, true)) {

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

		final BufferedInputStream bufferedIs = new BufferedInputStream(is);


		switch (ArchiveStreamFactory.detect(bufferedIs)) {

			// 7z doesn't support streaming
			case ArchiveStreamFactory.SEVEN_Z: {
				int overallCount = 0;

				logger.info("7-Zip archive format detected");

				try (final Tx outertx = app.tx()) {
					SevenZFile sevenZFile = new SevenZFile(file.getFileOnDisk());

					SevenZArchiveEntry sevenZEntry = sevenZFile.getNextEntry();

					while (sevenZEntry != null) {

						try (final Tx tx = app.tx(true, true, false)) {

							int count = 0;

							while (sevenZEntry != null && count++ < 50) {

								final String entryPath = "/" + PathHelper.clean(sevenZEntry.getName());
								logger.info("Entry path: {}", entryPath);

								if (sevenZEntry.isDirectory()) {

									handleDirectory(securityContext, existingParentFolder, entryPath);

								} else {

									byte[] buf = new byte[(int) sevenZEntry.getSize()];
									sevenZFile.read(buf, 0, buf.length);

									try (final ByteArrayInputStream in = new ByteArrayInputStream(buf)) {
										handleFile(securityContext, in, existingParentFolder, entryPath);
									}
								}

								sevenZEntry = sevenZFile.getNextEntry();

								overallCount++;
							}

							logger.info("Committing transaction after {} entries.", overallCount);
							tx.success();
						}

					}

					logger.info("Unarchived {} files.", overallCount);
					outertx.success();
				}

				break;
			}

			// ZIP needs special treatment to support "unsupported feature data descriptor"
			case ArchiveStreamFactory.ZIP: {

				logger.info("Zip archive format detected");

				try (final ZipArchiveInputStream in = new ZipArchiveInputStream(bufferedIs, null, false, true)) {

					handleArchiveInputStream(in, app, securityContext, existingParentFolder);
				}

				break;
			}

			default: {

				logger.info("Default archive format detected");

				try (final ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream(bufferedIs)) {

					handleArchiveInputStream(in, app, securityContext, existingParentFolder);
				}
			}
		}

		getWebSocket().send(MessageBuilder.finished().callback(callback).data("success", true).data("filename", fileName).build(), true);
	}

	private void handleArchiveInputStream(final ArchiveInputStream in, final App app, final SecurityContext securityContext, final Folder existingParentFolder) throws FrameworkException, IOException {

		int overallCount = 0;

		ArchiveEntry entry = in.getNextEntry();

		while (entry != null) {

			try (final Tx tx = app.tx(true, true, false)) { // don't send notifications for bulk commands

				int count = 0;

				while (entry != null && count++ < 50) {

					final String entryPath = "/" + PathHelper.clean(entry.getName());
					logger.info("Entry path: {}", entryPath);

					if (entry.isDirectory()) {

						handleDirectory(securityContext, existingParentFolder, entryPath);

					} else {

						handleFile(securityContext, in, existingParentFolder, entryPath);
					}

					entry = in.getNextEntry();

					overallCount++;
				}

				logger.info("Committing transaction after {} entries.", overallCount);

				tx.success();
			}
		}

		logger.info("Unarchived {} entries.", overallCount);
	}

	private void handleDirectory(final SecurityContext securityContext, final Folder existingParentFolder, final String entryPath) throws FrameworkException {

		final String folderPath = (existingParentFolder != null ? existingParentFolder.getPath() : "") + PathHelper.PATH_SEP + entryPath;

		FileHelper.createFolderPath(securityContext, folderPath);
	}

	private void handleFile(final SecurityContext securityContext, final InputStream in, final Folder existingParentFolder, final String entryPath) throws FrameworkException, IOException {

		final PropertyKey<Folder> parentKey     = StructrApp.key(AbstractFile.class, "parent");
		final PropertyKey<Boolean> hasParentKey = StructrApp.key(AbstractFile.class, "hasParent");
		final String filePath                   = (existingParentFolder != null ? existingParentFolder.getPath() : "") + PathHelper.PATH_SEP + PathHelper.clean(entryPath);
		final String name                       = PathHelper.getName(entryPath);
		final AbstractFile newFile              = ImageHelper.isImageType(name)
					? ImageHelper.createImage(securityContext, in, null, Image.class, name, false)
					: FileHelper.createFile(securityContext, in, null, File.class, name);

		final String folderPath = StringUtils.substringBeforeLast(filePath, PathHelper.PATH_SEP);
		final Folder parentFolder = FileHelper.createFolderPath(securityContext, folderPath);

		if (parentFolder != null) {

			final PropertyMap properties = new PropertyMap();

			properties.put(parentKey,    parentFolder);
			properties.put(hasParentKey, true);

			newFile.setProperties(securityContext, properties);
		}
	}

	//~--- get methods ----------------------------------------------------
	@Override
	public String getCommand() {
		return "UNARCHIVE";
	}
}
