package org.structr.cloud;

import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;

/**
 *
 * @author Christian Morgner
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
	public void transmissionProgress(int current, int total) {
		websocket.send(MessageBuilder.progress().code(200).message("{\"key\":\"" + key + "\", \"current\":" + current + ", \"total\":" + total + "}").build(), true);
	}
}
