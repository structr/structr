/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
