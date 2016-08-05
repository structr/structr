package org.structr.payment.api;

/**
 *
 */
public interface APIError {

	String getShortMessage();
	String getLongMessage();
	String getErrorCode();
}
