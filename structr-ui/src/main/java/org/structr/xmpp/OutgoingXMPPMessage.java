package org.structr.xmpp;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.EndNode;
import org.structr.core.property.EntityNotionProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;

/**
 *
 * @author Christian Morgner
 */
public class OutgoingXMPPMessage extends AbstractNode {

	public static final Property<XMPPClient>  sentBy      = new StartNode<>("sentBy", XMPPClientOutMessage.class);
	public static final Property<XMPPContact> receivedBy  = new EndNode<>("receivedBy", XMPPMessageReceiver.class);
	public static final Property<String>      text        = new StringProperty("text");
	public static final Property<String>      sender      = new EntityNotionProperty("sender", sentBy, new PropertyNotion(XMPPClient.xmppHandle));
	public static final Property<String>      recipient   = new EntityNotionProperty("recipient", receivedBy, new PropertyNotion(XMPPContact.xmppHandle, true));
	public static final Property<Boolean>     sent        = new BooleanProperty("sent");

	public static final View defaultView = new View(OutgoingXMPPMessage.class, PropertyView.Public,
		text, sender, recipient, sent
	);

	public static final View uiView = new View(OutgoingXMPPMessage.class, PropertyView.Ui,
		text, sender, recipient, sent
	);


	@Override
	public boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if (super.isValid(errorBuffer)) {

			if (sender != null) {

				// send message
				final XMPPClient _sender = getProperty(sentBy);
				if (_sender != null) {

					final XMPPClientConnection connection = XMPPContext.getClientForId(_sender.getUuid());
					if (connection.isConnected()) {

						connection.sendMessage(getProperty(recipient), getProperty(text));
						setProperty(sent, true);

					} else {

						errorBuffer.add("base", new NotConnectedToken());
						return false;
					}
				}
			}

			return true;
		}

		return false;
	}
}
