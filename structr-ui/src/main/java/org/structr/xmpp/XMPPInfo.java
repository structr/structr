package org.structr.xmpp;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;


/**
 *
 * @author Christian Morgner
 */
public interface XMPPInfo {

	public String getUsername();
	public String getPassword();
	public String getService();
	public String getHostName();
	public int getPort();

	public String getUuid();

	public XMPPCallback<Message> getMessageCallback();
	public XMPPCallback<Presence> getPresenceCallback();
	public XMPPCallback<IQ> getIqCallback();

	public interface XMPPCallback<T> {

		public void onMessage(final T message);
	}
}
