package org.structr.payment.api;

import org.structr.common.error.FrameworkException;

/**
 *
 */
public interface PaymentProvider {

	BeginCheckoutResponse beginCheckout(final Payment payment, final String successUrl, final String cancelUrl) throws FrameworkException;
	ConfirmCheckoutResponse confirmCheckout(final Payment payment, final String notifyUrl, final String token, final String payerId) throws FrameworkException;

	void cancelCheckout(final Payment payment) throws FrameworkException;
}
