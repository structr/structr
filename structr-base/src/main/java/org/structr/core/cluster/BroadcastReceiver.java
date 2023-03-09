package org.structr.core.cluster;

/**
 */

public interface BroadcastReceiver {

	void receive(final String sender, final StructrMessage message);
	String getNodeName();
}
