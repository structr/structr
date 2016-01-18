package org.structr.api;

/**
 *
 * @author Christian Morgner
 */
public class DeadlockException extends RuntimeException {

	public DeadlockException(final Throwable cause) {
		super(cause);
	}
}
