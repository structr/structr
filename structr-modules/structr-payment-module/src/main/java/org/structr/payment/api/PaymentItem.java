package org.structr.payment.api;

/**
 *
 */
public interface PaymentItem {

	int getAmount();
	int getQuantity();

	String getName();
	String getDescription();
	String getItemNumber();
	String getItemUrl();
}
