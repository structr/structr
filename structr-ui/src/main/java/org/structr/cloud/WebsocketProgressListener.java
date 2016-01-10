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
package org.structr.cloud;

import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;

/**
 *
 *
 */
public class WebsocketProgressListener implements CloudListener {

	private StructrWebSocket websocket = null;
	private String key                 = null;

	public WebsocketProgressListener(final StructrWebSocket websocket, final String key) {
		this.websocket = websocket;
		this.key       = key;
	}

	@Override
	public void transmissionStarted() {
		websocket.send(MessageBuilder.status().code(200).message("Transmission started").build(), true);
	}

	@Override
	public void transmissionFinished() {
		websocket.send(MessageBuilder.status().code(200).message("Transmission finished").build(), true);
	}

	@Override
	public void transmissionAborted() {
		websocket.send(MessageBuilder.status().code(200).message("Transmission aborted").build(), true);
	}

	@Override
	public void transmissionProgress(final String message) {

		websocket.send(
			MessageBuilder.progress()
				.code(200)
				.message("{\"key\":\"" + key + "\", \"message\":\"" + message + "\"}")
			.build(),
			true
		);
	}
}
