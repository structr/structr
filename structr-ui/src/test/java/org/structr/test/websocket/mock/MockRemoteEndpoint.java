/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.eclipse.jetty.websocket.api.BatchMode;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.WriteCallback;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class MockRemoteEndpoint implements RemoteEndpoint {

	private final List<String> sentStrings = new LinkedList<>();

	public List<String> getSentStrings() {
		return sentStrings;
	}

	@Override
	public void sendBytes(ByteBuffer byteBuffer) throws IOException {

	}

	@Override
	public void sendBytes(ByteBuffer byteBuffer, WriteCallback writeCallback) {

	}

	@Override
	public void sendPartialBytes(ByteBuffer byteBuffer, boolean b) throws IOException {

	}

	@Override
	public void sendPartialBytes(ByteBuffer byteBuffer, boolean b, WriteCallback writeCallback) {

	}

	@Override
	public void sendString(String s) throws IOException {
		sentStrings.add(s);
	}

	@Override
	public void sendString(String s, WriteCallback writeCallback) {
		sentStrings.add(s);
	}

	@Override
	public void sendPartialString(String s, boolean b) throws IOException {

	}

	@Override
	public void sendPartialString(String s, boolean b, WriteCallback writeCallback) throws IOException {

	}

	@Override
	public void sendPing(ByteBuffer byteBuffer) throws IOException {

	}

	@Override
	public void sendPing(ByteBuffer byteBuffer, WriteCallback writeCallback) {

	}

	@Override
	public void sendPong(ByteBuffer byteBuffer) throws IOException {

	}

	@Override
	public void sendPong(ByteBuffer byteBuffer, WriteCallback writeCallback) {

	}

	@Override
	public BatchMode getBatchMode() {
		return null;
	}

	@Override
	public void setBatchMode(BatchMode batchMode) {

	}

	@Override
	public int getMaxOutgoingFrames() {
		return 0;
	}

	@Override
	public void setMaxOutgoingFrames(int i) {

	}

	@Override
	public SocketAddress getRemoteAddress() {
		return null;
	}

	@Override
	public void flush() throws IOException {

	}
}
