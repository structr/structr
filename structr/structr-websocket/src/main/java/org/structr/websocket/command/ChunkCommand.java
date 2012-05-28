/*
 *  Copyright (C) 2010-2012 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.websocket.command;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Base64;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

/**
 *
 * @author Christian Morgner
 */
public class ChunkCommand extends AbstractCommand {

	private static final Logger logger  = Logger.getLogger(ChunkCommand.class.getName());

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		try {
			int sequenceNumber = Integer.parseInt((String)webSocketData.getNodeData().get("chunkId"));
			int chunkSize = Integer.parseInt((String)webSocketData.getNodeData().get("chunkSize"));
			Object rawData = webSocketData.getNodeData().get("chunk");
			String uuid = webSocketData.getId();
			byte[] data = null;

			if(rawData != null) {

				if(rawData instanceof String) {

					logger.log(Level.FINEST, "Raw data: {0}", rawData);

//					data = Base64.decodeBase64(((String)rawData).getBytes("UTF-8"));
					data = Base64.decodeBase64(((String)rawData));

					logger.log(Level.FINEST, "Decoded data: {0}", data);

				}
			}

			getWebSocket().handleFileChunk(uuid, sequenceNumber, chunkSize, data);

		} catch(Throwable t) {

			String msg = t.getMessage();
			// return error message
			getWebSocket().send(MessageBuilder.status().code(400).message("Invalid chunk: ".concat(msg != null ? msg : "")).build(), true);
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