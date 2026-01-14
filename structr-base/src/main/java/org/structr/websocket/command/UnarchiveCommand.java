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
package org.structr.websocket.command;

import java.util.Map;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.io.InputStream;
import java.util.Set;

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

		final Set<String> supportedByArchiveStreamFactory = Set.of(
				ArchiveStreamFactory.AR,
				ArchiveStreamFactory.ARJ,
				ArchiveStreamFactory.CPIO,
				ArchiveStreamFactory.DUMP,
				ArchiveStreamFactory.JAR,
				ArchiveStreamFactory.TAR,
				ArchiveStreamFactory.SEVEN_Z,
				ArchiveStreamFactory.ZIP
		);

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final App app = StructrApp.getInstance(securityContext);

		try {

			final String id            = webSocketData.getId();
			String parentFolderId      = webSocketData.getNodeDataStringValue("parentFolderId");
			final boolean createFolder = webSocketData.getNodeDataBooleanValue("createFolder");

			final File file;
			final InputStream is;
			final String fileName;

			try (final Tx tx = app.tx()) {

				final NodeInterface fileNode = app.getNodeById(StructrTraits.FILE, id);

				if (fileNode == null) {
					getWebSocket().send(MessageBuilder.status().code(400).message("File not found: ".concat(id)).build(), true);
					return;
				}

				file     = fileNode.as(File.class);
				fileName = file.getName();

				final String fileExtension = StringUtils.substringAfterLast(fileName, ".");
				if (!supportedByArchiveStreamFactory.contains(fileExtension)) {

					getWebSocket().send(MessageBuilder.status().code(400).message("Unsupported archive format: ".concat(fileExtension)).build(), true);
					return;
				}

				is = file.getInputStream();

				if (is == null) {

					getWebSocket().send(MessageBuilder.status().code(400).message("Could not get input stream from file ".concat(fileName)).build(), true);
					return;
				}

				if (createFolder) {

					final String folderName = StringUtils.substringBeforeLast(fileName, ".");

					final NodeInterface newParentFolder = app.create(StructrTraits.FOLDER,
							new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), folderName),
							new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.PARENT_ID_PROPERTY), parentFolderId)
					);

					parentFolderId = newParentFolder.getUuid();
				}

				tx.success();
			}

			// send WS message to all clients about start of extraction
			getWebSocket().send(MessageBuilder.forName(getCommand()).data(Map.of(
					"id", file.getUuid(),
					"file", file.getPath(),
					"status", "START"
			)).build(), true);

			// no transaction here since this is a bulk command
			FileHelper.unarchive(securityContext, file, parentFolderId);

			getWebSocket().send(MessageBuilder.forName(getCommand()).data(Map.of(
					"id", file.getUuid(),
					"file", file.getPath(),
					"status", "SUCCESS"
			)).build(), true);

		} catch (Throwable t) {

			logger.warn("", t);

			final String msg = t.toString();

			try (final Tx tx = app.tx()) {

				// return error message
				getWebSocket().send(MessageBuilder.forName(getCommand()).data(Map.of(
						"id", webSocketData.getId(),
						"status", "ERROR",
						"error", "Could not extract archive: ".concat((msg != null) ? msg : "")
				)).build(), true);

				tx.success();

			} catch (FrameworkException ignore) {
			}
		}
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return true;
	}

	//~--- get methods ----------------------------------------------------
	@Override
	public String getCommand() {
		return "UNARCHIVE";
	}
}
