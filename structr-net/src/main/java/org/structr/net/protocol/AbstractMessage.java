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
package org.structr.net.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.net.peer.Peer;
import org.structr.net.peer.PeerInfo;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 *
 */
public abstract class AbstractMessage {

	private static final Logger logger = LoggerFactory.getLogger(AbstractMessage.class.getName());

	private static final Map<Integer, Class<? extends AbstractMessage>> CommandMap = new HashMap<>();
	private static final Map<Class, Integer> TypeMap                               = new HashMap<>();

	static {

		CommandMap.put( 0, Discovery.class);

		CommandMap.put( 2, BroadcastMessage.class);
		CommandMap.put( 3, DirectMessage.class);
		CommandMap.put( 4, Update.class);
		CommandMap.put( 5, Delete.class);
		CommandMap.put( 6, Update.class);
		CommandMap.put( 7, Get.class);
		CommandMap.put( 8, Value.class);
		CommandMap.put( 9, Set.class);
		CommandMap.put(10, BeginTx.class);

		CommandMap.put(12, Commit.class);
		CommandMap.put(13, Committed.class);
		CommandMap.put(14, Ack.class);
		CommandMap.put(15, GetHistory.class);
		CommandMap.put(16, History.class);
		CommandMap.put(17, Inventory.class);


		TypeMap.put(String.class,       1);
		TypeMap.put(Integer.class,      2);
		TypeMap.put(Long.class,         3);
		TypeMap.put(Boolean.class,      4);
		TypeMap.put(Double.class,       5);
		TypeMap.put(Float.class,        6);
		TypeMap.put(ArrayList.class, 100);

	}

	private String uuid     = null;
	private int command     = 0;
	private long timestamp  = 0;

	public abstract void onMessage(final Peer peer, final PeerInfo sender);
	public abstract void serialize(final DataOutputStream dos) throws IOException;
	public abstract void deserialize(final DataInputStream dis) throws IOException;

	protected AbstractMessage(final int command) {

		this.uuid    = UUID.randomUUID().toString().replaceAll("\\-", "");
		this.command = command;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	public void onSend(final Peer peer) {
	}

	public void reBroadcast(final Peer peer, final PeerInfo sender) {
	}

	public String getId() {
		return uuid;
	}

	public void setId(final String uuid) {
		this.uuid = uuid;
	}

	public int getCommand() {
		return command;
	}

	public long getSenderTimestamp() {
		return timestamp;
	}

	public void setSenderTimestamp(final long senderTimestamp) {
		this.timestamp = senderTimestamp;
	}

	public static Envelope receive(final Peer peer, final DatagramPacket packet) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {

		final PrivateKey privateKey = peer.getPrivateKey();
		if (privateKey != null) {

			final Cipher cipher = AbstractMessage.getCipher();
			cipher.init(Cipher.DECRYPT_MODE, peer.getPrivateKey());

			final byte[] data = decryptBlocks(packet.getData(), cipher, 256, 245);

			if (data.length == 0) {

				System.out.println("Decryption failed");

			} else {

				final DataInputStream dis = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(data), 1024));
				//final DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
				final int command         = dis.readInt();
				final String messageId    = deserializeUUID(dis);
				final String peerId       = deserializeUUID(dis);
				final long timestamp      = dis.readLong();
				AbstractMessage msg       = null;

				final Class<? extends AbstractMessage> type = CommandMap.get(command);
				if (type != null) {

					try {

						msg = type.newInstance();

						msg.setSenderTimestamp(timestamp);
						msg.setId(messageId);

						msg.deserialize(dis);

						// create envelope
						return new Envelope(new PeerInfo(peer.getPublicKey(), peerId, packet.getAddress().getHostAddress(), packet.getPort()), msg);

					} catch (Throwable t) {
						logger.warn("", t);
					}

				} else {

					System.out.println("Unknown command " + command);
				}
			}

		} else {

			System.out.println("Unable to decrypt packet, aborting");
		}

		return null;
	}

	public static DatagramPacket forSending(final String peerId, final PeerInfo recipient, final AbstractMessage message) throws UnknownHostException, IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {

		final ByteArrayOutputStream finalBuffer = new ByteArrayOutputStream();
		final GZIPOutputStream zos              = new GZIPOutputStream(finalBuffer, 1024);
		final DataOutputStream dos              = new DataOutputStream(zos);

		dos.writeInt(message.getCommand());		// Command
		serializeUUID(dos, message.getId());		// UUID
		serializeUUID(dos, peerId);			// peer UUID
		dos.writeLong(message.getSenderTimestamp());	// timestamp

		// let message do the rest
		message.serialize(dos);

		// flush and close
		dos.flush();
		dos.close();

		final PublicKey publicKey = recipient.getPublicKey();
		if (publicKey != null) {

			// encrypt with the public key of the recipient
			final Cipher cipher = getCipher();
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);

			final byte[] data    = encryptBlocks(finalBuffer.toByteArray(), cipher, 245);
			final String address = recipient.getAddress();
			final int port       = recipient.getPort();

			return new DatagramPacket(data, data.length, InetAddress.getByName(address), port);

		} else {

			System.out.println("Unable to encrypt packet, aborting.");
		}

		return null;
	}

	// ----- protected methods -----
	protected void serializeObject(final DataOutputStream dos, final Object value) throws IOException {

		if (value != null) {

			final Integer typeKey = TypeMap.get(value.getClass());
			if (typeKey != null) {

				dos.writeInt(typeKey);

				switch (typeKey) {

					case 1:
						dos.writeUTF((String)value);
						break;

					case 2:
						dos.writeInt((Integer)value);
						break;

					case 3:
						dos.writeLong((Long)value);
						break;

					case 4:
						dos.writeBoolean((Boolean)value);
						break;

					case 5:
						dos.writeDouble((Double)value);
						break;

					case 6:
						dos.writeFloat((Float)value);
						break;

					case 100:
						serializeList(dos, (List<Object>)value);
						break;

				}

			} else {

				System.out.println("Unknown type " + value.getClass() + ", cannot serialize!");
			}

		} else {

			dos.writeInt(0);
		}
	}

	public static void serializeUUID(final DataOutputStream dos, final String uuid) throws IOException {

		final UUID obj = toUUID(uuid);
		dos.writeLong(obj.getLeastSignificantBits());
		dos.writeLong(obj.getMostSignificantBits());
	}

	public static String deserializeUUID(final DataInputStream dis) throws IOException {

		final long lsb = dis.readLong();
		final long msb = dis.readLong();

		final UUID uuid = new UUID(msb, lsb);

		return uuid.toString().replaceAll("\\-", "");
	}

	protected Object deserializeObject(final DataInputStream dis) throws IOException {

		final int typeKey = dis.readInt();

		switch (typeKey) {

			default:
				return null;

			case 1:
				return dis.readUTF();

			case 2:
				return dis.readInt();

			case 3:
				return dis.readLong();

			case 4:
				return dis.readBoolean();

			case 5:
				return dis.readDouble();

			case 6:
				return dis.readFloat();

			case 100:
				return deserializeList(dis);

		}
	}

	private void serializeList(final DataOutputStream dos, final List<Object> list) throws IOException {

		dos.writeInt(list.size());

		for (final Object obj : list) {
			serializeObject(dos, obj);
		}
	}

	private List deserializeList(final DataInputStream dis) throws IOException {

		final ArrayList<Object> list = new ArrayList<>();
		final int count               = dis.readInt();

		for (int i=0; i<count; i++) {

			list.add(deserializeObject(dis));
		}

		return list;
	}

	private static Cipher getCipher() throws NoSuchAlgorithmException, NoSuchPaddingException {
		return Cipher.getInstance("RSA/ECB/PKCS1Padding");
	}

	private static UUID toUUID(final String id) {

		final StringBuilder buf = new StringBuilder(id);

		buf.insert(20, "-");
		buf.insert(16, "-");
		buf.insert(12, "-");
		buf.insert( 8, "-");

		return UUID.fromString(buf.toString());
	}

	private static byte[] encryptBlocks(final byte[] data, final Cipher cipher, final int dataSize) throws IOException {

		final ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
		final int count                 = (data.length / dataSize) + 1;
		int remaining                   = data.length;

		for (int i=0; i<count; i++) {

			final int offset = i*dataSize;
			final int length = Math.min(dataSize, remaining);

			final CipherOutputStream cos = new CipherOutputStream(bos, cipher);

			cos.write(data, offset, length);
			cos.flush();
			cos.close();

			remaining -= length;
		}

		return bos.toByteArray();
	}

	private static byte[] decryptBlocks(final byte[] data, final Cipher cipher, final int blockSize, final int dataSize) throws IOException {

		final ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
		final int count                 = (data.length / dataSize);
		final byte[] buffer             = new byte[dataSize];
		int remaining                   = data.length;

		for (int i=0; i<count; i++) {

			final int offset = i*blockSize;
			final int length = Math.min(blockSize, remaining);

			final byte[] range             = Arrays.copyOfRange(data, offset, offset + length);
			final ByteArrayInputStream bis = new ByteArrayInputStream(range);
			final CipherInputStream cis    = new CipherInputStream(bis, cipher);

			try {
				// clear buffer
				Arrays.fill(buffer, 0, dataSize, (byte)0);

				// copy data
				cis.read(buffer, 0, dataSize);
				bos.write(buffer);

				remaining -= Math.min(blockSize, remaining);

			} catch (IOException ignore) {
				break;
			}
		}

		return bos.toByteArray();
	}
}