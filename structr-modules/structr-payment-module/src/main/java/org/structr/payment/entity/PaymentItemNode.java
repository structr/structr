package org.structr.payment.entity;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.schema.SchemaService;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.payment.api.PaymentItem;

/**
 *
 */
public class PaymentItemNode extends AbstractNode implements PaymentItem {

	static {

		SchemaService.registerBuiltinTypeOverride("PaymentItemNode", PaymentItemNode.class.getName());
	}

	public static final Property<PaymentNode> payment           = new StartNode<>("payment", PaymentItems.class);
	public static final Property<Integer>     amount            = new IntProperty("amount").indexed();
	public static final Property<Integer>     quantity          = new IntProperty("quantity").indexed();
	public static final Property<String>      description       = new StringProperty("description");
	public static final Property<String>      number            = new StringProperty("number");
	public static final Property<String>      url               = new StringProperty("url");

	public static final View defaultView = new View(PaymentItemNode.class, PropertyView.Public,
		name, amount, quantity, description, number, url
	);

	public static final View uiView = new View(PaymentItemNode.class, PropertyView.Ui,
		name, amount, quantity, description, number, url
	);

	@Override
	public int getAmount() {
		return getProperty(amount);
	}

	@Override
	public int getQuantity() {
		return getProperty(quantity);
	}

	@Override
	public String getDescription() {
		return getProperty(description);
	}

	@Override
	public String getItemNumber() {
		return getProperty(number);
	}

	@Override
	public String getItemUrl() {
		return getProperty(url);
	}
}
