/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
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

		final Set<String> supportedByArchiveStreamFactory = new HashSet<>(Arrays.asList(new String[] {
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
			final InputStream is;
			final String fileName;

			try (final Tx tx = app.tx()) {

				final NodeInterface fileNode = app.get("File", id);

				if (fileNode == null) {
					getWebSocket().send(MessageBuilder.status().code(400).message("File not found: ".concat(id)).build(), true);
					return;
				}

				file = fileNode.as(File.class);

				final String fileExtension = StringUtils.substringAfterLast(file.getName(), ".");
				if (!supportedByArchiveStreamFactory.contains(fileExtension)) {

					getWebSocket().send(MessageBuilder.status().code(400).message("Unsupported archive format: ".concat(fileExtension)).build(), true);
					return;
				}

				fileName = file.getName();
				is       = file.getInputStream();

				if (is == null) {

					getWebSocket().send(MessageBuilder.status().code(400).message("Could not get input stream from file ".concat(fileName)).build(), true);
					return;
				}


				tx.success();
			}

			// no transaction here since this is a bulk command
			FileHelper.unarchive(securityContext, file, parentFolderId);

			getWebSocket().send(MessageBuilder.finished().callback(callback).data("success", true).data("filename", fileName).build(), true);


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
		return true;
	}

	//~--- get methods ----------------------------------------------------
	@Override
	public String getCommand() {
		return "UNARCHIVE";
	}
}
