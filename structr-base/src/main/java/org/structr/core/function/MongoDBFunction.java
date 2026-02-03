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
package org.structr.core.function;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MongoDBFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "mongodb";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("url, database, collection");
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
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${mongodb(url, database, collection)}. Example: ${mongodb('mongodb://localhost:27017', 'database1', 'collection1')}"),
			Usage.javaScript("Usage: ${{ $.mongodb(url, database, collection) }}. Example: ${{ $.mongodb('mongodb://localhost:27017', 'database1', 'collection1') }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Opens a connection to a MongoDB source and returns a MongoCollection which can be used to further query the Mongo database.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("url", "connection URL to MongoDB"),
			Parameter.mandatory("database", "name of the database to connect to"),
			Parameter.mandatory("collection", "name of the collection to fetch")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.javaScript("""
			{
				// Open the connection to mongo and return the testCollection
				let collection = $.mongodb('mongodb://localhost', 'testDatabase', 'testCollection');

				// Insert a record
				collection.insertOne($.bson({
					name: 'Test4'
				}));

				// Query all records with a give property set
				return collection.find($.bson({ name: 'Test4' }));
			}
			""", "Open connection, insert object and retrieve objects with identical name"),
			Example.javaScript("""
			{
				// Open the connection to mongo and return the testCollection
				let collection = $.mongodb('mongodb://localhost', 'testDatabase', 'testCollection');

				// Query all records with a give property set
				return collection.find($.bson({ name: { $regex: 'Test[0-9]' } }));
			}
			""", "Open connection and find objects with regex name"),
			Example.javaScript("""
			{
				// Open the connection to mongo and return the testCollection
				let collection = $.mongodb('mongodb://localhost', 'testDatabase', 'testCollection');

				 // Insert a record
				collection.insertOne($.bson({
					name: 'Test9',
					date: new Date(2018, 1, 1)
				}));

				return collection.find($.bson({ date: { $gte: new Date(2018, 1, 1) } }));
			}
			""", "Open connection, insert object with date and query all objects with dates greater than equal (gte) that date")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"The returned MongoCollection object has the functions exposed as described in https://mongodb.github.io/mongo-java-driver/4.2/apidocs/mongodb-driver-sync/com/mongodb/client/MongoCollection.html",
			"Native MongoDB operators (https://docs.mongodb.com/manual/reference/operator/) can be used.",
			"Every function without parameters or with Bson parameter can be used.",
			"Creating a Bson object is done with the `$.bson()` function which simple converts a JSON object to Bson.",
			"The result of a `collection.find()` is not a native JS array, so functions like `.filter()` or `.map()` are not available - the `for of` syntax applies.",
			"The records in a result are also not native JS objects, so the dot notation (i.e. `record.name`) does not work - the `record.get('name')` syntax applies.",
			"All examples assume a MongoDB instance has been locally started via Docker with the following command: docker run -ti -p 27017:27017 mongo"
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Database;
	}
}
