/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.diff.websocket;

import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.diff.compare.Delta;
import org.structr.diff.compare.DiffReport;
import org.structr.diff.compare.Matcher;
import org.structr.diff.export.ExportSource;
import org.structr.diff.export.ZipExportSource;
import org.structr.diff.model.Entity;
import org.structr.diff.parse.ExportParser;
import org.structr.web.entity.File;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.command.AbstractCommand;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Compares two exports held in the virtual filesystem and returns the diff.
 *
 * Takes two File uuids rather than paths, because an export is a File node once it is ingested and
 * a uuid is what the UI already has in hand. Reading them needs nothing but
 * {@code getRawInputStream()}, so this works on any storage backend and does not wait on a
 * filesystem provider for the VFS.
 *
 * <p>The whole comparison happens inside the request. It is affordable: parsing a large real export
 * measured around 180ms, and the two parses plus the match come in under half a second, which is
 * why there is no job, no progress channel and no partial result to reason about.
 */
public class CompareExportsCommand extends AbstractCommand {

	public static void register() {

		StructrWebSocket.addCommand(CompareExportsCommand.class);
	}

	@Override
	public String getCommand() {

		return "COMPARE_EXPORTS";
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) throws FrameworkException {

		setDoTransactionNotifications(false);

		final String leftId  = webSocketData.getNodeDataStringValue("leftId");
		final String rightId = webSocketData.getNodeDataStringValue("rightId");

		if (leftId == null || rightId == null) {

			getWebSocket().send(MessageBuilder.status().code(422)
				.message("COMPARE_EXPORTS needs leftId and rightId, the uuids of two export files.").build(), true);

			return;
		}

		final App app = StructrApp.getInstance(getWebSocket().getSecurityContext());

		try (final Tx tx = app.tx()) {

			final File left  = file(app, leftId);
			final File right = file(app, rightId);

			if (left == null || right == null) {

				getWebSocket().send(MessageBuilder.status().code(404)
					.message("No such export file: " + (left == null ? leftId : rightId)).build(), true);

				return;
			}

			final List<Entity> a     = parse(left);
			final List<Entity> b     = parse(right);
			final List<Delta> deltas = new Matcher(a, b).getDeltas();

			webSocketData.setNodeData(DiffReport.of(left.getName(), right.getName(), a, b, deltas));

			getWebSocket().send(webSocketData, true);

			tx.success();

		} catch (IOException ioex) {

			// An unreadable archive is a user-facing answer, not a server fault: the wrong file was
			// picked, or it is not an export at all.
			getWebSocket().send(MessageBuilder.status().code(422)
				.message("Could not read an export: " + ioex.getMessage()).build(), true);
		}
	}

	private File file(final App app, final String uuid) throws FrameworkException {

		final NodeInterface node = app.getNodeById(StructrTraits.FILE, uuid);

		return node != null ? node.as(File.class) : null;
	}

	private List<Entity> parse(final File file) throws IOException, FrameworkException {

		try (final InputStream in = file.getRawInputStream()) {

			final ExportSource source = ZipExportSource.read(in, file.getName());

			return new ExportParser(source).getEntities();
		}
	}
}
