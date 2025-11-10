/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.core.function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.docs.Signature;
import org.structr.schema.action.ActionContext;

import java.util.Iterator;
import java.util.List;

public class DeleteFunction extends CoreFunction implements BatchableFunction {

	private static final Logger logger = LoggerFactory.getLogger(DeleteFunction.class);

	public static final String ERROR_MESSAGE_DELETE = "Usage: ${delete(objectOrList)}. Example: ${delete(this)}";

	private boolean batched = false;
	private int batchSize   = -1;

	@Override
	public String getName() {
		return "delete";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("objectOrList");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources != null) {

			final App app = StructrApp.getInstance(ctx.getSecurityContext());
			for (final Object obj : sources) {

				deleteObject(app, obj);
			}
		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_DELETE;
	}

	@Override
	public String getShortDescription() {
		return "Deletes the given entity from the database";
	}

	// ----- interface BatchableFunction -----
	@Override
	public void setBatched(boolean isBatched) {
		this.batched = isBatched;
	}

	@Override
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	// ----- private methods -----
	private void deleteObject(final App app, final Object obj) throws FrameworkException {

		if (obj instanceof NodeInterface) {

			app.delete((NodeInterface)obj);
		}

		if (obj instanceof RelationshipInterface) {

			app.delete((RelationshipInterface)obj);
		}

		if (obj instanceof Iterable) {

			if (batched) {

				final Iterable iterable = (Iterable)obj;
				final Iterator iterator = iterable.iterator();
				int count               = 0;

				while (iterator.hasNext()) {

					try (final Tx tx = app.tx()) {

						while (iterator.hasNext()) {

							deleteObject(app, iterator.next());

							if ((++count % batchSize) == 0) {
								break;
							}
						}

						tx.success();
					}

					logger.debug("Committing batch after {} objects", count);

					// reset count
					count = 0;
				}


			} else {

				for (final Object o : (Iterable)obj) {

					deleteObject(app, o);
				}
			}
		}
	}
}
