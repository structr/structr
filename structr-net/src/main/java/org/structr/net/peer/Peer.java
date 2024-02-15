/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.net.peer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.net.PeerListener;
import org.structr.net.data.RemoteTransaction;
import org.structr.net.data.TimeoutException;
import org.structr.net.data.time.Clock;
import org.structr.net.data.time.PseudoTemporalEnvironment;
import org.structr.net.data.time.PseudoTime;
import org.structr.net.data.time.ToplevelTemporalEnvironment;
import org.structr.net.protocol.*;
import org.structr.net.repository.DefaultRepositoryObject;
import org.structr.net.repository.InternalChangeListener;
import org.structr.net.repository.Repository;
import org.structr.net.repository.RepositoryObject;

/**
 * The main class of this peer-to-peer implementation. This class will
 * start three individual threads. Two that handle inbound and outbound
 * traffic and another one that acts on the protocol messages it receives.
 */
public final class Peer implements Runnable, Clock, InternalChangeListener {

	public static final int START_PORT = 5757;

	private static final Logger logger = LoggerFactory.getLogger(Peer.class.getName());

	private final Queue<Envelope> outputQueue         = new ConcurrentLinkedQueue<>();
	private final Queue<Envelope> inputQueue          = new ConcurrentLinkedQueue<>();
	private final ExecutorService executorService     = Executors.newCachedThreadPool();
	private final Map<String, PeerInfo> peers         = new ConcurrentHashMap<>();
	private final Map<String, Callback> callbacks     = new ConcurrentHashMap<>();
	private final Charset utf8                        = Charset.forName("utf-8");
	private final List<PeerListener> listeners        = new LinkedList<>();
	private Map<String, Object> data                  = new HashMap<>();
	private KeyPair keyPair                           = null;
	private PrivateKey privateKey                     = null;
	private PublicKey publicKey                       = null;
	private ToplevelTemporalEnvironment pte           = null;
	private Repository repository                     = null;
	private boolean initialized                       = false;
	private String initialPeer                        = null;
	private String bindAddress                        = null;
	private DatagramSocket serverSocket               = null;
	private long timeOffset                           = 0L;
	private int localPort                             = START_PORT;
	private int sent                                  = 0;
	private int received                              = 0;
	private boolean running                           = true;
	private boolean verbose                           = false;
	private int discoveryInterval                     = 1000;
	private int discoveryIntervalStep                 = 1000;
	private int finalDiscoveryInterval                = 6000;
	private int hightestTxNumber                      = 0;

	public Peer(final KeyPair keyPair, final Repository repository) {
		this(keyPair, repository, "0.0.0.0");
	}

	public Peer(final KeyPair keyPair, final Repository repository, final String bindAddress) {
		this(keyPair, repository, bindAddress, "255.255.255.255");
	}

	public Peer(final KeyPair keyPair, final Repository repository, final String bindAddress, final String initialPeer) {

		this.pte         = new ToplevelTemporalEnvironment(this);
		this.keyPair     = keyPair;
		this.bindAddress = bindAddress;
		this.initialPeer = initialPeer;

		//addListener(new PrintListener());
		this.repository  = repository;

		// listen for internal changes in repository
		repository.addInternalChangeListener(this);
	}

	@Override
	public void run() {

		long currentTime   = 0L;
		long lastCleanup   = 0L;
		long lastDiscovery = 0L;

		// main loop of a peer is to listen for messages and react on it
		while (running) {

			try {
				currentTime = System.currentTimeMillis();

				// work on input queue, but interrupt for other tasks
				while (!inputQueue.isEmpty() && currentTime < lastDiscovery + discoveryInterval && currentTime < lastCleanup + discoveryIntervalStep) {

					currentTime = System.currentTimeMillis();

					final Envelope envelope = inputQueue.poll();
					if (envelope != null) {

						final AbstractMessage message = envelope.getMessage();

						// notify listeners
						onMessage(message);

						// re-broadcast to other peers
						final String ackKey = message.getId() + "-ack";

						// re-broadcast message if UUID was not seen before
						// (this causes the "wave" effect so that all peers
						// see the message, even if not connected directly)
						if (getData(ackKey) == null) {

							// process message
							message.onMessage(this, envelope.getPeer());

							// send message to other peers
							broadcast(message);
							setData(ackKey, true);
						}
					}
				}

				// send discovery request
				if (currentTime > lastDiscovery + discoveryInterval) {

					lastDiscovery = currentTime;

					// insert single discovery packet into output queue
					send(new PeerInfo(getPublicKey(), repository.getUuid(), initialPeer, START_PORT), new Discovery(getContentHash()));

					// adjust discovery interval in steps of 10 seconds
					// until it reaches 60 seconds
					if (discoveryInterval < finalDiscoveryInterval) {
						discoveryInterval += discoveryIntervalStep;
					}
				}

				// remove peers that have not (re)acted for some time
				if (currentTime > lastCleanup + discoveryIntervalStep) {

					lastCleanup = currentTime;

					// update list of peers
					for (final Iterator<PeerInfo> it = peers.values().iterator(); it.hasNext();) {

						final PeerInfo peer = it.next();
						final long time = peer.getLastSeen() + finalDiscoveryInterval + (discoveryIntervalStep * 2);

						if (currentTime > time) {

							onRemovePeer(peer);
							it.remove();
						}
					}
				}

				Thread.sleep(10L);

			} catch (Throwable t) {
				logger.warn("", t);
			}
		}

		// shut down
		executorService.shutdownNow();
	}

	public void start() {

		if (!initialized) {
			throw new IllegalStateException("Peer not initialized, did you call initializeServer?!");
		}

		try {

			executorService.submit(new InputHandler());
			executorService.submit(new OutputHandler());
			executorService.submit(this);

		} catch (RejectedExecutionException rex) {

			logger.warn("Unable to start peer, aborting.");
			executorService.shutdown();
		}
	}

	public void initializeServer() {

		boolean success = false;

		while (!success && localPort < START_PORT + 10) {

			try {

				serverSocket = new DatagramSocket(localPort, InetAddress.getByName(bindAddress));
				success = true;

			} catch (IOException ioex) {
				localPort++;
			} catch (Throwable t) {
				logger.warn("", t);
			}
		}

		if (!success) {

			System.out.println("Unable to bind to " + bindAddress + ", aborting.");

			// dont start at all
			running = false;

		} else {

			initialized = true;
		}
	}

	public void stop() {

		running = false;

		serverSocket.close();
	}

	public String getUuid() {
		return repository.getUuid();
	}

	public int getLocalPort() {
		return localPort;
	}

	public long getTimeOffset() {
		return timeOffset;
	}

	public int getTransactionNumber() {
		return hightestTxNumber;
	}

	public void setTransactionNumber(final int transactionNumber) {
		this.hightestTxNumber = transactionNumber;
	}

	public PseudoTemporalEnvironment getPseudoTemporalEnvironment() {
		return pte;
	}

	public void send(final PeerInfo recipient, final AbstractMessage message) {
		outputQueue.add(new Envelope(recipient, message));
	}

	public void onPeerDiscovery(final PeerInfo newPeer, final byte[] hash) {

		if (newPeer != null && !newPeer.getUuid().equals(getUuid())) {

			final boolean isNew      = addPeer(newPeer);
			final byte[] contentHash = getContentHash();
			final boolean hasChanged = !Arrays.equals(hash, contentHash);

			if (isNew || hasChanged) {

				if (isNew) {
					System.out.println("Peer is new, sending inventory..");
				}

				if (hasChanged) {
					System.out.println("Peer has different content hash, sending inventory..");

					System.out.println(printHash(hash) + " / " + printHash(contentHash));
				}

				// send inventory
				for (final RepositoryObject obj : repository.getObjects()) {

					log("Inventory(", obj.getUuid(), ", ", obj.getUserId(), ")");

					send(newPeer, new Inventory(repository.getUuid(), obj.getUuid(), obj.getDeviceId(), obj.getLastModificationTime()));
				}

			}
		}
	}

	public Collection<PeerInfo> getPeers() {
		return peers.values();
	}

	public Repository getRepository() {
		return repository;
	}

	public boolean isRunning() {
		return running;
	}

	public void addListener(final PeerListener listener) {
		listeners.add(listener);
	}

	public void removeListener(final PeerListener listener) {
		listeners.remove(listener);
	}

	public synchronized void printInfo() {

		System.out.println("#########################################");
		System.out.println("Peer " + serverSocket.getLocalAddress() + ":" + localPort);
		System.out.println("UUID: " + getUuid());
		System.out.println("Time offset: " + timeOffset);
		System.out.println(received + " messages received, " + sent + " messages sent");
		System.out.println(peers.size() + " peers");

		for (final PeerInfo info : peers.values()) {
			System.out.println("    " + info);
		}

		for (final RepositoryObject obj : repository.getObjects()) {
			System.out.println("        ##### " + obj.getUuid());
			System.out.println("        " + obj.getType() + "(" + obj.getUserId() + "): " + obj.getProperties(pte.next()));
			((DefaultRepositoryObject)obj).printHistory();
		}

		System.out.println(outputQueue);

		System.out.flush();
	}

	public void setVerbose(final boolean verbose) {
		this.verbose = verbose;
	}

	public void log(final Object... values) {

		if (!verbose) {
			return;
		}

		for(final Object obj : values) {
			System.out.print(obj);
		}

		System.out.println();
	}

	public Object getData(final String key) {
		return data.get(key);
	}

	public void setData(final String key, final Object value) {
		data.put(key, value);
	}

	public void broadcast(final AbstractMessage message) {

		// make broadcasts to self
		inputQueue.add(new Envelope(new PeerInfo(getPublicKey(), repository.getUuid(), bindAddress, localPort), message));

		// send message to all peers
		for (final PeerInfo info : getPeers()) {
			send(info, message);
		}
	}

	public void set(final String objectId, final String key, final Object value) {

		final RepositoryObject sharedObject = repository.getObject(objectId);
		if (sharedObject != null) {

			try (final RemoteTransaction tx = beginTx(sharedObject)) {

				tx.setProperty(sharedObject, key, value);
				tx.commit();

			} catch (Exception tex) {
				System.out.println("Failed");
			}

		} else {

			System.out.println("No such object " + objectId);
		}
	}

	public Object get(final String objectId, final String key) {

		final RepositoryObject sharedObject = repository.getObject(objectId);
		if (sharedObject != null) {

			try (final RemoteTransaction tx = beginTx(sharedObject)) {

				return tx.getProperty(sharedObject, key);

			} catch (Exception tex) {
				System.out.println("Failed");
			}

		} else {

			System.out.println("No such object " + objectId);
		}

		return null;
	}

	public RemoteTransaction beginTx(final RepositoryObject sharedObject) throws TimeoutException {

		final RemoteTransaction tx = new RemoteTransaction(this);

		tx.begin(sharedObject);

		return tx;
	}

	public void registerCallback(final String uuid, final Callback callback) {
		callbacks.put(uuid, callback);
	}

	public void unregisterCallback(final String uuid) {
		callbacks.remove(uuid);
	}

	public void callback(final String uuid, final AbstractMessage message) {

		final Callback callback = callbacks.get(uuid);
		if (callback != null) {

			callback.callback(message);
			callbacks.remove(uuid);
		}
	}

	public long getCoordinatedTime() {
		return System.currentTimeMillis() + timeOffset;
	}

	public boolean knowsPeer(final String uuid) {
		return peers.containsKey(uuid);
	}

	public byte[] getContentHash() {

		MessageDigest digest = null;

		try {
			digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException nex) {
			logger.warn("", nex);
		}

		if (digest != null) {

			final long t0 = System.currentTimeMillis();

			final List<RepositoryObject> objects = new LinkedList<>(repository.getObjects());
			Collections.sort(objects, new UuidComparator());

			for (final RepositoryObject node : objects) {

				final String uuid = node.getUuid();
				final String type = node.getType();
				final String last = node.getLastModificationTime().toString();

				digest.update(uuid.getBytes(utf8));
				digest.update(type.getBytes(utf8));
				digest.update(last.getBytes(utf8));
			}

			final long t1 = System.currentTimeMillis();
			if ((t1-t0) > 100) {

				System.out.println("Creation of content hash took " + (System.currentTimeMillis() - t0) + " ms!");
			}

			return digest.digest();

		} else {

			System.out.println("Cannot create hash value, algorithms not available.");
		}

		return new byte[0];
	}

	public PrivateKey getPrivateKey() {
		return keyPair.getPrivate();
	}

	public PublicKey getPublicKey() {
		return keyPair.getPublic();
	}

	// ---- interface Clock -----
	@Override
	public long getTime() {
		return getCoordinatedTime();
	}

	// ----- interface PeerListener -----
	public void onMessage(final AbstractMessage msg) {

		for (final PeerListener listener : listeners) {
			listener.onMessage(msg);
		}
	}

	public void onAddPeer(final PeerInfo info) {

		for (final PeerListener listener : listeners) {
			listener.onAddPeer(info);
		}
	}

	public void onRemovePeer(final PeerInfo info) {

		for (final PeerListener listener : listeners) {
			listener.onRemovePeer(info);
		}
	}

	// ----- private methods -----
	private synchronized boolean addPeer(final PeerInfo peer) {

		final String uuid = peer.getUuid();

		if (!peers.containsKey(uuid)) {

			peers.put(uuid, peer);

			peer.setLastSeen(System.currentTimeMillis());
			onAddPeer(peer);

			return true;
		}

		// not added
		return false;
	}

	private synchronized void updatePeer(final String uuid, final long latency) {

		final PeerInfo peer = peers.get(uuid);
		if (peer != null) {

			peer.setLastSeen(System.currentTimeMillis());
			peer.setLatency(latency);
		}
	}

	@Override
	public void onObjectCreation(final RepositoryObject source, final Map<String, Object> data) {

		log("Create(", source.getUuid(), ")");

		broadcast(new Update(repository.getUuid(), source.getUuid(), source.getType(), source.getUserId(), source.getCreationTime(), source.getLastModificationTime(), data));
	}

	@Override
	public void onObjectModification(final RepositoryObject source, final Map<String, Object> data) {

		log("Update(", source.getUuid(), ")");

		broadcast(new Update(repository.getUuid(), source.getUuid(), source.getType(), source.getUserId(), source.getCreationTime(), source.getLastModificationTime(), data));
	}

	@Override
	public void onObjectDeletion(final String uuid) {

		log("Delete(", uuid, ")");

		broadcast(new Delete(repository.getUuid(), uuid, PseudoTime.now(this)));
	}


	// ----- private methods -----
	private String printHash(final byte[] array) {


		final StringBuilder buf = new StringBuilder();

		for (final byte b : array) {

			final int val = b & 0xff;

			if (val < 16) {
				buf.append("0");
			}

			buf.append(Integer.toHexString(val));
		}

		return buf.toString();
	}

	// ----- nested classes -----
	private class InputHandler implements Runnable {

		private final byte[] buffer = new byte[2048];

		@Override
		public void run() {

			while (running) {

				try {

					final DatagramPacket packet = new DatagramPacket(buffer, 2048);

					serverSocket.receive(packet);

					final Envelope envelope = AbstractMessage.receive(Peer.this, packet);
					if (envelope != null) {

						final AbstractMessage msg  = envelope.getMessage();
						final long senderTimestamp = msg.getSenderTimestamp();
						final long current         = System.currentTimeMillis();
						final long delta           = senderTimestamp - current;

						// adjust time offset to be in sync with other peers,
						// the group's value will be the maximum of all peers
						if (delta > timeOffset) {
							timeOffset = delta;
						}

						// update last seen time
						updatePeer(envelope.getPeer().getUuid(), (current + timeOffset) - senderTimestamp);

						inputQueue.add(envelope);

						received++;

					}

				} catch (Throwable t) {
					logger.warn("", t);
				}
			}
		}
	}

	private class OutputHandler implements Runnable {

		@Override
		public void run() {

			while (running) {

				try {

					while (!outputQueue.isEmpty()) {

						final Envelope envelope = outputQueue.poll();
						if (envelope != null) {

							final AbstractMessage message = envelope.getMessage();
							final PeerInfo recipient      = envelope.getPeer();

							message.setSenderTimestamp(System.currentTimeMillis() + timeOffset);
							message.onSend(Peer.this);

							serverSocket.send(AbstractMessage.forSending(Peer.this.getUuid(), recipient, message));

							sent++;
						}
					}

					Thread.sleep(10L);

				} catch (IOException ignore1) {
				} catch (InterruptedException ignore2) {
				} catch (Throwable t) {
					logger.warn("", t);
				}
			}
		}
	}

	private class UuidComparator implements Comparator<RepositoryObject> {

		@Override
		public int compare(final RepositoryObject o1, final RepositoryObject o2) {
			return o1.getUuid().compareTo(o2.getUuid());
		}
	}
}
