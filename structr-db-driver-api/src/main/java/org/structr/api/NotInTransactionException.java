package org.structr.api;

/**
 *
 * @author Christian Morgner
 */
public class NotInTransactionException extends RuntimeException {

	public NotInTransactionException(final Throwable cause) {
		super(cause);
	}

	public NotInTransactionException(final String message) {
		super(message);
	}
}
