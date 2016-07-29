package org.structr.payment.entity;

import org.structr.core.entity.OneToMany;

/**
 *
 */
public class PaymentItems extends OneToMany<PaymentNode, PaymentItemNode> {

	@Override
	public Class<PaymentNode> getSourceType() {
		return PaymentNode.class;
	}

	@Override
	public Class<PaymentItemNode> getTargetType() {
		return PaymentItemNode.class;
	}

	@Override
	public String name() {
		return "paymentItem";
	}
}
