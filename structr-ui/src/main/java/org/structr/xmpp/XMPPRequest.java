package org.structr.xmpp;

import java.util.logging.Logger;
import org.jivesoftware.smack.packet.IQ.Type;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;

/**
 *
 * @author Christian Morgner
 */
public class XMPPRequest extends AbstractNode {

	private static final Logger logger = Logger.getLogger(XMPPRequest.class.getName());

	public static final Property<XMPPClient> client = new StartNode<>("client", XMPPClientRequest.class);
	public static final Property<String> sender     = new StringProperty("sender").indexed();
	public static final Property<String> content    = new StringProperty("content");
	public static final Property<Type> requestType  = new EnumProperty("requestType", Type.class);


	public static final View publicView = new View(XMPPRequest.class, PropertyView.Public,
		client, sender, content, requestType
	);

	public static final View uiView = new View(XMPPRequest.class, PropertyView.Ui,
		client, sender, content, requestType
	);
}
