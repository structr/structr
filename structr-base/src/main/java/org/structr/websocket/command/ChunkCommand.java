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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.storage.StorageProviderFactory;
import org.structr.util.Base64;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.io.IOException;

/**
 *
 */
public class ChunkCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(ChunkCommand.class.getName());

	static {

		StructrWebSocket.addCommand(ChunkCommand.class);

	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		try {

			final String uuid  = webSocketData.getId();
			int sequenceNumber = webSocketData.getNodeDataIntegerValue("chunkId");
			int chunkSize      = webSocketData.getNodeDataIntegerValue("chunkSize");
			int chunks         = webSocketData.getNodeDataIntegerValue("chunks");
			Object rawData     = webSocketData.getNodeData().get("chunk");

			byte[] data        = new byte[0];

			if (rawData != null) {

				if (rawData instanceof String) {

					logger.debug("Raw data: {}", rawData);

					data = Base64.decode(((String) rawData));

					logger.debug("Decoded data: {}", data);

				}

			}

			//org.structr.core.graph.search.SearchCommand.prefetch(AbstractFile.class, uuid);

			final File file = (File) getNode(uuid);
			if (file.isTemplate()) {

				logger.warn("No write permission, file is in template mode: {}", new Object[] {file.toString()});
				getWebSocket().send(MessageBuilder.status().message("No write permission, file is in template mode").code(400).build(), true);
				return;

			}

			if (!file.isGranted(Permission.write, securityContext)) {

				logger.warn("No write permission for {} on {}", new Object[] {getWebSocket().getCurrentUser().toString(), file.toString()});
				getWebSocket().send(MessageBuilder.status().message("No write permission").code(400).build(), true);
				return;

			}

			getWebSocket().handleFileChunk(uuid, sequenceNumber, chunkSize, data, chunks);

			if (sequenceNumber+1 == chunks) {

				FileHelper.updateMetadata(file);

				file.increaseVersion();

				getWebSocket().removeFileUploadHandler(uuid);

				logger.debug("File upload finished. Checksum: {}, size: {}", new Object[]{ file.getChecksum(), StorageProviderFactory.getStorageProvider(file).size()});

			}

			final long currentSize = (long)(sequenceNumber * chunkSize) + data.length;

			// This should trigger setting of lastModifiedDate in any case
			getWebSocket().send(MessageBuilder.status().code(200).message("{\"id\":\"" + file.getUuid() + "\", \"name\":\"" + file.getName() + "\",\"size\":" + currentSize + "}").build(), true);

			if (sequenceNumber+1 == chunks) {

				TransactionCommand.registerNodeCallback((NodeInterface) file, callback);

			}

		} catch (IOException | FrameworkException ex) {

			String msg = ex.toString();

			// return error message
			getWebSocket().send(MessageBuilder.status().code(400).message("Could not process chunk data: ".concat((msg != null)
				? msg
				: "")).build(), true);
		}
	}

	@Override
	public String getCommand() {
		return "CHUNK";
	}
}
/**
 * { "command" : "CHUNK", "id" : <uuid>, "data" : { "chunk" : "ölasdkfjoifhp9wea8hisaghsakjgf", "chunkId" : 3 } }
 */
