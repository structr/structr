package org.structr.xmpp;

import java.util.List;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNodes;
import org.structr.core.property.StringProperty;

/**
 *
 * @author Christian Morgner
 */
public class XMPPContact extends AbstractNode {

	public static final Property<List<IncomingXMPPMessage>> messagesFrom = new StartNodes<>("messagesFrom", XMPPMessageSender.class);
	public static final Property<List<OutgoingXMPPMessage>> messagesTo   = new StartNodes<>("messagesTo", XMPPMessageReceiver.class);

	public static final Property<String>  xmppHandle   = new StringProperty("xmppHandle").indexed();
	public static final Property<Mode> presenceMode    = new EnumProperty("presenceMode", Mode.class, Mode.available);


	public static final View publicView = new View(XMPPContact.class, PropertyView.Public,
		xmppHandle, presenceMode, messagesFrom, messagesTo
	);

	public static final View uiView = new View(XMPPContact.class, PropertyView.Ui,
		xmppHandle, presenceMode, messagesFrom, messagesTo
	);
}
