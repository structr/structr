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

import org.neo4j.graphdb.Transaction;

/**
 * Batch transaction carries the database transaction.
 *
 * A batch transaction should be commited after several write operations to
 * flush to disk and avoid OutOfMemory errors.
 *
 * See: http://wiki.neo4j.org/content/FAQ#Why_do_I_get_an_OutOfMemoryError_injecting_data.3F
 *
 * To commit in between, just execute
 * 
 * <code>
 *   tx.success();
 *   tx.finish();
 *   System.out.println("Database transaction commited.");
 *   tx = graphDb.beginTx();
 * </code>
 *
 * @author amorgner
 */
public interface BatchTransaction
{
	public Object execute(Transaction tx) throws Throwable;
}
