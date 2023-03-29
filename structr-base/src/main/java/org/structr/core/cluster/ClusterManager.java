/*
 * Copyright (C) 2010-2023 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.cluster;

import org.jgroups.*;
import org.jgroups.protocols.*;
import org.jgroups.protocols.kubernetes.KUBE_PING;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.protocols.pbcast.NAKACK2;
import org.jgroups.protocols.pbcast.STABLE;
import org.jgroups.stack.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 */
public class ClusterManager {

	private static final Logger logger   = LoggerFactory.getLogger(ClusterManager.class);
	private final String clusterName     = "structr";
	private final boolean loggingEnabled = false;
	private JChannel channel;
	private String name;

	public void start(final BroadcastReceiver receiver) throws Exception {

		System.setProperty("jgroups.bind_addr",     "match-interface:eth1");
		System.setProperty("jgroups.external_addr", "match-interface:eth1");

		final Protocol[] prot_stack = {
			new TCP().setBindPort(7800),
			new KUBE_PING()
				.setValue("namespace",  System.getenv("NAMESPACE"))
				.setValue("masterHost", System.getenv("KUBERNETES_SERVICE_HOST"))
				.setValue("masterPort", 443),
			new MERGE3(),
			new FD_SOCK(),
			new FD_ALL(),
			new VERIFY_SUSPECT(),
			new BARRIER(),
			new NAKACK2(),
			new UNICAST3(),
			new RSVP(),
			new STABLE(),
			new GMS(),
			new FRAG2()
		};

		logger.info("Connecting to cluster {}..", clusterName);

		name    = receiver.getNodeName();
		channel = new JChannel(prot_stack).name(name).setReceiver(new InternalReceiver(receiver)).connect(clusterName);

		// we are not interested in our own messages
		channel.setDiscardOwnMessages(true);

		logger.info("Connected to cluster {}.", channel.clusterName());
	}

	public boolean isConnected() {
		return channel.isConnected();
	}

	public boolean isCoordinator() {

		final View view = channel.getView();

		// are we the coordinator of this cluster?
		return channel.getAddress().equals(view.getCoord());
	}

	public void requestCoordinatorStatus() throws Exception {

		final View view     = channel.getView();
		final Address coord = view.getCoord();

		channel.send(new ObjectMessage(coord, new StructrMessage("status-requested")));
	}

	public void broadcast(final String msg, final Object payload) throws Exception {
		this.broadcast(msg, payload, false);
	}

	public void broadcast(final String msg, final Object payload, final boolean waitForDelivery) throws Exception {

		final Message message = new ObjectMessage(null, new StructrMessage(msg, payload));

		if (waitForDelivery) {

			// force the send() method to wait for delivery
			message.setFlag(Message.Flag.RSVP);
		}

		if (loggingEnabled) {
			logger.info("[{}] sending {}", name, msg);
		}

		channel.send(message);

		if (loggingEnabled) {
			logger.info("[{}] {} sent.", name, msg);
		}
	}

	private class InternalReceiver implements Receiver {

		private final BroadcastReceiver receiver;

		protected InternalReceiver(final BroadcastReceiver receiver) {
			this.receiver = receiver;
		}

		@Override
		public void receive(final Message msg) {

			try {

				if (loggingEnabled) {
					logger.info("[{}] msg from {}", name, msg.src());
				}

				final Object data = msg.getObject();
				if (data instanceof StructrMessage) {

					final StructrMessage message = msg.getObject();

					receiver.receive(msg.getSrc().toString(), message);

				} else {

					logger.warn("[{}] received unknown message of type {} from {}", name, data.getClass().getSimpleName(), msg.src());
				}

			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		public void viewAccepted(final View v) {

			try {

				if (loggingEnabled) {
					logger.info("[{}] new view: {}", name, v);
				}

			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
}