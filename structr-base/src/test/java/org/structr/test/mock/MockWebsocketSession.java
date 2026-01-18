/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.test.mock;

import com.google.gson.Gson;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MockWebsocketSession implements Session {

	private final List<String> messages = new LinkedList<>();
	private final Gson gson;

	public MockWebsocketSession(final Gson gson) {
		this.gson = gson;
	}

	public Map<String, Object> getLastWebsocketResponse() {

		if (!messages.isEmpty()) {

			final String last = messages.get(messages.size() - 1);

			return gson.fromJson(last, Map.class);
		}

		return null;
	}

	@Override
	public void demand() {
	}

	@Override
	public void sendBinary(ByteBuffer buffer, Callback callback) {

	}

	@Override
	public void sendPartialBinary(ByteBuffer buffer, boolean last, Callback callback) {

	}

	@Override
	public void sendText(String text, Callback callback) {
		messages.add(text);
	}

	@Override
	public void sendPartialText(String text, boolean last, Callback callback) {

	}

	@Override
	public void sendPing(ByteBuffer applicationData, Callback callback) {

	}

	@Override
	public void sendPong(ByteBuffer applicationData, Callback callback) {

	}

	@Override
	public void close(int statusCode, String reason, Callback callback) {

	}

	@Override
	public void disconnect() {

	}

	@Override
	public SocketAddress getLocalSocketAddress() {
		return null;
	}

	@Override
	public SocketAddress getRemoteSocketAddress() {
		return null;
	}

	@Override
	public String getProtocolVersion() {
		return "";
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
	public Duration getIdleTimeout() {
		return null;
	}

	@Override
	public void setIdleTimeout(Duration duration) {

	}

	@Override
	public int getInputBufferSize() {
		return 0;
	}

	@Override
	public void setInputBufferSize(int size) {

	}

	@Override
	public int getOutputBufferSize() {
		return 0;
	}

	@Override
	public void setOutputBufferSize(int size) {

	}

	@Override
	public long getMaxBinaryMessageSize() {
		return 0;
	}

	@Override
	public void setMaxBinaryMessageSize(long size) {

	}

	@Override
	public long getMaxTextMessageSize() {
		return 0;
	}

	@Override
	public void setMaxTextMessageSize(long size) {

	}

	@Override
	public long getMaxFrameSize() {
		return 0;
	}

	@Override
	public void setMaxFrameSize(long maxFrameSize) {

	}

	@Override
	public boolean isAutoFragment() {
		return false;
	}

	@Override
	public void setAutoFragment(boolean autoFragment) {

	}

	@Override
	public int getMaxOutgoingFrames() {
		return 0;
	}

	@Override
	public void setMaxOutgoingFrames(int maxOutgoingFrames) {

	}
}
