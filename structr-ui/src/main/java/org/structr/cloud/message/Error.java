/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.cloud.message;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.ExportContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.SyncCommand;

/**
 *
 * @author Christian Morgner
 */
public class Error extends Message {

	private String message = null;
	private int errorCode  = 0;

	public Error() {}

	public Error(final int errorCode, final String message) {
		this.errorCode = errorCode;
		this.message   = message;
	}

	@Override
	public String toString() {
		return "Error(" + message + ")";
	}

	@Override
	public void onRequest(CloudConnection serverConnection, ExportContext context) throws IOException, FrameworkException {
		serverConnection.setError(errorCode, message);
		context.progress();
	}

	@Override
	public void onResponse(CloudConnection clientConnection, ExportContext context) throws IOException, FrameworkException {
		clientConnection.setError(errorCode, message);
		context.progress();
	}

	@Override
	public void afterSend(CloudConnection connection) {
	}

	@Override
	protected void deserializeFrom(Reader reader) throws IOException {
		this.message   = (String)SyncCommand.deserialize(reader);
		this.errorCode = (Integer)SyncCommand.deserialize(reader);
	}

	@Override
	protected void serializeTo(Writer writer) throws IOException {
		SyncCommand.serialize(writer, message);
		SyncCommand.serialize(writer, errorCode);
	}
}
