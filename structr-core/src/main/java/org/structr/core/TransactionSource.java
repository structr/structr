package org.structr.core;

/**
 * Identifies the source of a transaction. This interface exists in order to ne
 * able to differentiate between local and remote transactions (i.e. those
 * coming from a replication master).
 *
 * @author Christian Morgner
 */
public interface TransactionSource {

	public boolean isLocal();
	public boolean isRemote();

	public String getOriginAddress();
}
