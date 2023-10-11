/*
 * Copyright (C) 2010-2023 Structr GmbH
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
let websocket;
let pingInterval;
let reconnectIntervalId;

self.onmessage = (e) => {

	switch (e.data.type) {
		case 'connect': {
			connect(e.data);
			break;
		}
		case 'reconnect': {
			reconnect(e.data);
			break;
		}
		case 'stopReconnect': {
			stopReconnect();
			break;
		}
		case 'startPing': {
			startPing(e.data);
			break;
		}
		case 'stopPing': {
			stopPing(e.data);
			break;
		}
		case 'server': {
			try {
				websocket.send(e.data.message);
			} catch (e) {}
			break;
		}
	}
};

let connect = (data) => {

	let isEnc   = (self.location.protocol === 'https:');
	let wsHost  = `ws${isEnc ? 's' : ''}://${self.location.host}`;
	let wsUrl   = wsHost + data.wsPath;
	let wsClass = data.wsClass;

	try {

		if (!websocket || websocket.readyState > 1) {
			// closed websocket

			if (websocket) {
				websocket.onopen    = undefined;
				websocket.onclose   = undefined;
				websocket.onmessage = undefined;
				websocket           = undefined;
			}

			if ('WebSocket' === wsClass) {

				try {

					websocket = new WebSocket(wsUrl, 'structr');

				} catch (e) {}

			} else if ('MozWebSocket' === wsClass) {

				try {

					websocket = new MozWebSocket(wsUrl, 'structr');

				} catch (e) {}

				return false;
			}

			websocket.onopen = (event) => {
				self.postMessage({ type: 'onopen' });
			};

			websocket.onclose = (event) => {
				self.postMessage({ type: 'onclose' });
			};

			websocket.onmessage = (message) => {
				self.postMessage({
					type: 'onmessage',
					message: message.data
				});
			};
		}

	} catch (exception) {

		if (websocket) {
			websocket.close();
			websocket = null;
		}
	}
};

let reconnect = (data) => {

	if (!reconnectIntervalId) {

		stopPing();
		stopReconnect();

		try {

			websocket?.close();
			websocket = null;

		} catch (e) {}

		// keep setInterval in worker to prevent background throttling
		reconnectIntervalId = setInterval(() => {

			connect(data);
		}, 1000);
	}
};

let stopReconnect = () => {
	clearInterval(reconnectIntervalId);
	reconnectIntervalId = undefined;
};

let startPing = (e) => {

	if (pingInterval) {
		clearInterval(pingInterval);
	}

	// keep setInterval in worker to prevent background throttling
	pingInterval = setInterval(() => {

		self.postMessage({ type: 'ping' });
	}, 1000);
};

let stopPing = () => {

	clearInterval(pingInterval);
};