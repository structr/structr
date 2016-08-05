package org.structr.payment.api;

/**
 *
 */
public enum CheckoutState {
	Success, Failure, Warning, SuccessWithWarning, FailureWithWarning, PartialSuccess, Custom
}
