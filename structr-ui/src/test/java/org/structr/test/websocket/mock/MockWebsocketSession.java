/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.test.websocket.mock;

import com.google.gson.Gson;
import org.eclipse.jetty.websocket.api.*;

import java.net.SocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class MockWebsocketSession implements Session {

	private final MockRemoteEndpoint endpoint = new MockRemoteEndpoint();
	private final Gson gson;

	public MockWebsocketSession(final Gson gson) {
		this.gson = gson;
	}

	public Map<String, Object> getLastWebsocketResponse() {

		final List<String> messages = endpoint.getSentStrings();
		if (!messages.isEmpty()) {

			final String last = messages.get(messages.size() - 1);

			return gson.fromJson(last, Map.class);
		}

		return null;
	}

	@Override
	public void close() {
	}

	@Override
	public void close(final CloseStatus closeStatus) {

	}

	@Override
	public void close(int i, String s) {

	}

	@Override
	public void disconnect() {

	}

	@Override
	public SocketAddress getLocalAddress() {
		return null;
	}

	@Override
	public String getProtocolVersion() {
		return "";
	}

	@Override
	public RemoteEndpoint getRemote() {
		return endpoint;
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return null;
	}

	@Override
	public UpgradeRequest getUpgradeRequest() {
		return null;
	}

	@Override
	public UpgradeResponse getUpgradeResponse() {
		return null;
	}

	@Override
	public boolean isOpen() {
		return false;
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public SuspendToken suspend() {
		return null;
	}

	@Override
	public WebSocketBehavior getBehavior() {
		return null;
	}

	@Override
	public Duration getIdleTimeout() {
		return null;
	}

	@Override
	public int getInputBufferSize() {
		return 0;
	}

	@Override
	public int getOutputBufferSize() {
		return 0;
	}

	@Override
	public long getMaxBinaryMessageSize() {
		return 0;
	}

	@Override
	public long getMaxTextMessageSize() {
		return 0;
	}

	@Override
	public long getMaxFrameSize() {
		return 0;
	}

	@Override
	public boolean isAutoFragment() {
		return false;
	}

	@Override
	public void setIdleTimeout(Duration duration) {

	}

	@Override
	public void setInputBufferSize(int i) {

	}

	@Override
	public void setOutputBufferSize(int i) {

	}

	@Override
	public void setMaxBinaryMessageSize(long l) {

	}

	@Override
	public void setMaxTextMessageSize(long l) {

	}

	@Override
	public void setMaxFrameSize(long l) {

	}

	@Override
	public void setAutoFragment(boolean b) {

	}
}
