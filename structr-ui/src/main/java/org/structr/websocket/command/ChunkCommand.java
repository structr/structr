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

import java.io.IOException;

import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.dynamic.File;
import org.structr.util.Base64;
import org.structr.web.common.FileHelper;
import org.structr.websocket.StructrWebSocket;

//~--- classes ----------------------------------------------------------------

/**
 *
 *
 */
public class ChunkCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(ChunkCommand.class.getName());

	static {

		StructrWebSocket.addCommand(ChunkCommand.class);

	}

	//~--- methods --------------------------------------------------------

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		try {

			int sequenceNumber = ((Long) webSocketData.getNodeData().get("chunkId")).intValue();
			int chunkSize      = ((Long) webSocketData.getNodeData().get("chunkSize")).intValue();
			Object rawData     = webSocketData.getNodeData().get("chunk");
			int chunks         = ((Long) webSocketData.getNodeData().get("chunks")).intValue();
			String uuid        = webSocketData.getId();
			byte[] data        = null;

			if (rawData != null) {

				if (rawData instanceof String) {

					logger.log(Level.FINEST, "Raw data: {0}", rawData);

//                                        data = Base64.decodeBase64(((String)rawData).getBytes("UTF-8"));
					data = Base64.decode(((String) rawData));

					logger.log(Level.FINEST, "Decoded data: {0}", data);

				}

			}

			final File file = (File) getNode(uuid);

			if (!file.isGranted(Permission.write, securityContext)) {

				logger.log(Level.WARNING, "No write permission for {0} on {1}", new Object[] {getWebSocket().getCurrentUser().toString(), file.toString()});
				getWebSocket().send(MessageBuilder.status().message("No write permission").code(400).build(), true);
				return;

			}

			getWebSocket().handleFileChunk(uuid, sequenceNumber, chunkSize, data, chunks);

			if (sequenceNumber+1 == chunks) {

				final long checksum = FileHelper.getChecksum(file);
				final long size     = FileHelper.getSize(file);

				file.unlockReadOnlyPropertiesOnce();
				file.setProperty(File.checksum, checksum);

				file.unlockReadOnlyPropertiesOnce();
				file.setProperty(File.size, size);

				file.increaseVersion();

				getWebSocket().removeFileUploadHandler(uuid);

				logger.log(Level.FINE, "File upload finished. Checksum: {0}, size: {1}", new Object[]{ checksum, size });

			}

			final long currentSize = (long)(sequenceNumber * chunkSize) + data.length;

			// This should trigger setting of lastModifiedDate in any case
			getWebSocket().send(MessageBuilder.status().code(200).message("{\"id\":\"" + file.getUuid() + "\", \"name\":\"" + file.getName() + "\",\"size\":" + currentSize + "}").build(), true);

		} catch (IOException | FrameworkException ex) {

			String msg = ex.toString();

			// return error message
			getWebSocket().send(MessageBuilder.status().code(400).message("Could not process chunk data: ".concat((msg != null)
				? msg
				: "")).build(), true);
		}
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {
		return "CHUNK";
	}
}



/**
 * { "command" : "CHUNK", "id" : <uuid>, "data" : { "chunk" : "ölasdkfjoifhp9wea8hisaghsakjgf", "chunkId" : 3 } }
 */
