package org.structr.xmpp;

import java.util.List;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.EndNodes;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.FunctionProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;

/**
 *
 * @author Christian Morgner
 */
public class XMPPClient extends AbstractNode implements XMPPInfo {

	public static final Property<List<IncomingXMPPMessage>> receivedMessages = new EndNodes<>("receivedMessages", XMPPClientInMessage.class);
	public static final Property<List<OutgoingXMPPMessage>> sentMessages     = new EndNodes<>("sentMessages", XMPPClientOutMessage.class);

	public static final Property<String>  xmppHandle   = new FunctionProperty("xmppHandle").format("concat(this.xmppUsername, '@', this.xmppHost)").indexed();
	public static final Property<String>  xmppUsername = new StringProperty("xmppUsername").indexed();
	public static final Property<String>  xmppPassword = new StringProperty("xmppPassword");
	public static final Property<String>  xmppService  = new StringProperty("xmppService");
	public static final Property<String>  xmppHost     = new StringProperty("xmppHost");
	public static final Property<Integer> xmppPort     = new IntProperty("xmppPort");

	public static final Property<Mode> presenceMode    = new EnumProperty("presenceMode", Mode.class, Mode.available);

	public static final Property<Boolean> isEnabled    = new BooleanProperty("isEnabled");
	public static final Property<Boolean> isConnected  = new BooleanProperty("isConnected");


	public static final View publicView = new View(XMPPClient.class, PropertyView.Public,
		xmppHandle, xmppUsername, xmppService, xmppHost, xmppPort, presenceMode, isEnabled, isConnected, receivedMessages, sentMessages
	);

	public static final View uiView = new View(XMPPClient.class, PropertyView.Ui,
		xmppHandle, xmppUsername, xmppService, xmppHost, xmppPort, presenceMode, isEnabled, isConnected, receivedMessages, sentMessages
	);



	@Override
	public boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if (getProperty(isEnabled)) {
			XMPPContext.connect(this);
		}

		return super.onCreation(securityContext, errorBuffer);
	}

	@Override
	public boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		XMPPClientConnection connection = XMPPContext.getClientForId(getUuid());
		boolean enabled                 = getProperty(isEnabled);
		if (!enabled) {

			if (connection != null && connection.isConnected()) {
				connection.disconnect();
			}

		} else {

			if (connection == null || !connection.isConnected()) {
				XMPPContext.connect(this);
			}

			connection = XMPPContext.getClientForId(getUuid());
			if (connection != null) {

				if (connection.isConnected()) {

					setProperty(isConnected, true);
					connection.setPresence(getProperty(presenceMode));

				} else {

					setProperty(isConnected, false);
				}
			}
		}

		return super.onModification(securityContext, errorBuffer);
	}

	@Override
	public boolean onDeletion(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {

		final String uuid = properties.get(id);
		if (uuid != null) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(uuid);
			if (connection != null) {

				connection.disconnect();
			}
		}

		return super.onDeletion(securityContext, errorBuffer, properties);
	}

	@Override
	public String getUsername() {
		return getProperty(xmppUsername);
	}

	@Override
	public String getPassword() {
		return getProperty(xmppPassword);
	}

	@Override
	public String getService() {
		return getProperty(xmppService);
	}

	@Override
	public String getHostName() {
		return getProperty(xmppHost);
	}

	@Override
	public int getPort() {
		return getProperty(xmppPort);
	}

	@Override
	public XMPPCallback<Message> getMessageCallback() {

		return new MessageCallback<Message>(getUuid()) {

			@Override
			public void onMessage(final Message message) {

				final App app = StructrApp.getInstance();

				try (final Tx tx = app.tx()) {

					final XMPPClient client = getClient();
					if (client != null) {

						app.create(IncomingXMPPMessage.class,
							new NodeAttribute(IncomingXMPPMessage.sender, cleanXMPPUserName(message.getFrom())),
							new NodeAttribute(IncomingXMPPMessage.recipient, cleanXMPPUserName(message.getTo())),
							new NodeAttribute(IncomingXMPPMessage.text, message.getBody()),
							new NodeAttribute(IncomingXMPPMessage.owner, client.getProperty(AbstractNode.owner))
						);
					}

					tx.success();

				} catch (FrameworkException fex) {
					fex.printStackTrace();
				}
			}
		};
	}

	@Override
	public XMPPCallback<Presence> getPresenceCallback() {

		return new MessageCallback<Presence>(getUuid()) {

			@Override
			public void onMessage(final Presence message) {

				System.out.println("PRESENCE: " + message);

//				try (final Tx tx = StructrApp.getInstance().tx()) {
//
//					final XMPPClient client = getClient();
//					if (client != null) {
//
//
//					}
//
//					tx.success();
//
//				} catch (FrameworkException fex) {
//					fex.printStackTrace();
//				}
			}
		};
	}

	@Override
	public XMPPCallback<IQ> getIqCallback() {

		return new MessageCallback<IQ>(getUuid()) {

			@Override
			public void onMessage(final IQ message) {

				System.out.println("IQ: " + message);
//
//				try (final Tx tx = StructrApp.getInstance().tx()) {
//
//					final XMPPClient client = getClient();
//					if (client != null) {
//
//
//					}
//
//					tx.success();
//
//				} catch (FrameworkException fex) {
//					fex.printStackTrace();
//				}
			}
		};
	}

	// ----- private methods -----
	private String cleanXMPPUserName(final String source) {

		if (source != null) {

			if (source.contains("/")) {
				return source.substring(0, source.lastIndexOf("/"));
			}
		}

		return source;
	}

	// ----- nested classes -----
	private static abstract class MessageCallback<T> implements XMPPCallback<T> {

		private String uuid = null;

		public MessageCallback(final String uuid) {
			this.uuid = uuid;
		}

		protected XMPPClient getClient() throws FrameworkException {
			return StructrApp.getInstance().get(XMPPClient.class, uuid);
		}
	}
}
