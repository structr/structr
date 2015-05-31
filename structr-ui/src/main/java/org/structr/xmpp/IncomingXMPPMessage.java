package org.structr.xmpp;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.AbstractNode;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.EndNode;
import org.structr.core.property.EntityNotionProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;

/**
 *
 * @author Christian Morgner
 */
public class IncomingXMPPMessage extends AbstractNode {

	public static final Property<XMPPClient>  receivedBy = new StartNode<>("receivedBy", XMPPClientInMessage.class);
	public static final Property<XMPPContact> sentBy     = new EndNode<>("sentBy", XMPPMessageSender.class);
	public static final Property<String>     text        = new StringProperty("text");
	public static final Property<String>     recipient   = new EntityNotionProperty("recipient", receivedBy, new PropertyNotion(XMPPClient.xmppHandle));
	public static final Property<String>     sender      = new EntityNotionProperty("sender", sentBy, new PropertyNotion(XMPPContact.xmppHandle, true));

	public static final View defaultView = new View(IncomingXMPPMessage.class, PropertyView.Public,
		text, sender, recipient
	);

	public static final View uiView = new View(IncomingXMPPMessage.class, PropertyView.Ui,
		text, sender, recipient
	);
}
