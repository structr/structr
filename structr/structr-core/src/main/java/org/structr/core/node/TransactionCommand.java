/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.node;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

/**
 *
 * @author cmorgner
 */
public class TransactionCommand extends NodeServiceCommand {

	@Override
	public Object execute(Object... parameters) {

		Object ret = null;
		GraphDatabaseService graphDb = (GraphDatabaseService)arguments.get("graphDb");

		if(parameters.length > 0 && parameters[0] instanceof StructrTransaction) {

			StructrTransaction transaction = (StructrTransaction)parameters[0];
			if(graphDb != null) {
				Transaction tx = graphDb.beginTx();
				try {
					ret = transaction.execute();
					tx.success();

				} catch(Throwable t) {

					//t.printStackTrace();
					transaction.setCause(t);
					tx.failure();

				} finally {
					tx.finish();
				}
			}

		} else if(parameters.length > 0 && parameters[0] instanceof BatchTransaction) {

			BatchTransaction transaction = (BatchTransaction)parameters[0];
			Transaction tx = graphDb.beginTx();
			try {
				ret = transaction.execute(tx);
				tx.success();

			} catch(Throwable t) {
				// t.printStackTrace();
				transaction.setCause(t);
				tx.failure();

			} finally {
				tx.finish();
			}

		}

		return ret;
	}
}
