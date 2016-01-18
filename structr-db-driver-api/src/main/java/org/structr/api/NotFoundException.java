package org.structr.api;

/**
 *
 * @author Christian Morgner
 */
public class NotFoundException extends RuntimeException {

	public NotFoundException(final Throwable cause) {
		super(cause);
	}

	public NotFoundException(final String message) {
		super(message);
	}
}
