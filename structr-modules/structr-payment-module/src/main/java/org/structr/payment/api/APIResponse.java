package org.structr.payment.api;

import java.util.List;

/**
 *
 */
public interface APIResponse {

	CheckoutState getCheckoutState();
	List<APIError> getErrors();
}
