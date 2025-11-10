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

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.schema.action.ActionContext;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MongoDBFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE    = "Usage: ${mongodb(url, database, collection)}. Example: ${mongodb(\"mongodb://localhost:27017\", \"database1\", \"collection1\")}";

	@Override
	public String getName() {
		return "mongodb";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("url, database, collection");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 3);

			final List<Map<String, Object>> data = new LinkedList<>();
			final String url                     = (String)sources[0];
			final String db                      = (String)sources[1];
			final String coll                    = (String)sources[2];

			try (final MongoClient mongoClient = MongoClients.create(url)) {

				final MongoDatabase database               = mongoClient.getDatabase(db);
				final MongoCollection<Document> collection = database.getCollection(coll);

				return collection.withDocumentClass(Map.class);

			} catch (Throwable t) {

				logException(t, "{}(): Encountered exception '{}' for input: {}", new Object[] { getName(), t.getMessage(), sources });
			}

			return data;

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(final boolean inJavaScriptContext) {
		return ERROR_MESSAGE;
	}

	@Override
	public String getShortDescription() {
		return "Opens and returns a connection to an external MongoDB instance";
	}
}
