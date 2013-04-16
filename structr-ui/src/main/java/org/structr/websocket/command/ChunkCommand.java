/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.websocket.command;

import java.util.Date;
import org.apache.commons.codec.binary.Base64;

import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.web.entity.File;
import org.structr.websocket.StructrWebSocket;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class ChunkCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(ChunkCommand.class.getName());
	
	static {
		
		StructrWebSocket.addCommand(ChunkCommand.class);

	}

	//~--- methods --------------------------------------------------------

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		try {

			int sequenceNumber = Integer.parseInt((String) webSocketData.getNodeData().get("chunkId"));
			int chunkSize      = Integer.parseInt((String) webSocketData.getNodeData().get("chunkSize"));
			Object rawData     = webSocketData.getNodeData().get("chunk");
			String uuid        = webSocketData.getId();
			byte[] data        = null;

			if (rawData != null) {

				if (rawData instanceof String) {

					logger.log(Level.FINEST, "Raw data: {0}", rawData);

//                                      data = Base64.decodeBase64(((String)rawData).getBytes("UTF-8"));
					data = Base64.decodeBase64(((String) rawData));

					logger.log(Level.FINEST, "Decoded data: {0}", data);

				}

			}

			File file = (File) getNode(uuid);
			
			long size = (long)(sequenceNumber * chunkSize) + data.length;
			
			// Set proper size
			file.setProperty(File.size, size);
			
			long partSize = (long)(sequenceNumber * chunkSize) + chunkSize;
			
			getWebSocket().handleFileChunk(uuid, sequenceNumber, chunkSize, data);
			
			// This should trigger setting of lastModifiedDate in any case
			file.setChecksum(0L);
//			file.getChecksum();
			
			getWebSocket().send(MessageBuilder.status().code(200).message(size + " bytes of " + file.getName() + " successfully saved.").build(), true);

		} catch (Throwable t) {

			String msg = t.toString();

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
