/*
 *  Copyright (C) 2011 Axel Morgner
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

import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

/**
 *
 * @author Christian Morgner
 */
public class ChunkCommand extends AbstractCommand {

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		try {
			int sequenceNumber = Integer.parseInt((String)webSocketData.getData().get("chunkId"));
			Object rawData = webSocketData.getData().get("chunk");
			String uuid = webSocketData.getId();
			byte[] data = null;

			if(rawData != null) {

				if(rawData instanceof String) {

					data = ((String)rawData).getBytes("UTF-8");
				}
			}

			getWebSocket().handleFileChunk(uuid, sequenceNumber, data);

		} catch(Throwable t) {

			// return error message
			getWebSocket().send(MessageBuilder.status().code(400).message("Invalid chunk: ".concat(t.getMessage())).build(), true);
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