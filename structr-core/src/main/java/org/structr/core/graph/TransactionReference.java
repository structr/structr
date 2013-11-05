package org.structr.core.graph;

import org.neo4j.graphdb.Lock;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Transaction;

/**
 *
 * @author Christian Morgner
 */
public class TransactionReference implements Transaction {

	private Transaction tx     = null;
	private int referenceCount = 0;
	
	public TransactionReference(final Transaction tx) {
		this.tx = tx;
	}
	
	public boolean isToplevel() {
		return referenceCount == 1;
	}

	public void begin() {
		referenceCount++;
	}
	
	public void end() {
		referenceCount--;
	}
	
	public int getReferenceCount() {
		return referenceCount;
	}
	
	// ----- interface Transaction -----
	@Override
	public void failure() {
		tx.failure();
	}

	@Override
	public void success() {
		tx.success();
	}

	@Override
	public void finish() {
		
		// only finish transaction if we are at root level
		if (--referenceCount == 0) {
			
			tx.finish();
		}
	}

	@Override
	public Lock acquireWriteLock(PropertyContainer entity) {
		return tx.acquireWriteLock(entity);
	}

	@Override
	public Lock acquireReadLock(PropertyContainer entity) {
		return tx.acquireReadLock(entity);
	}
}
