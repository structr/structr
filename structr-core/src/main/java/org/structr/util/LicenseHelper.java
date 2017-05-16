/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class LicenseHelper {

	private static final Logger logger             = LoggerFactory.getLogger(LicenseHelper.class);
	private static final String PublicKey          = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC+EWUOSHqg5RteV+CE7TJK8H3E1OHu6dtRf6KPFePU018gOEkWMYu32SXlzRMU/QvvrDql5tmX6gkHyynuhvDsCQz1JrWd4CMwbv0SzXecNmIFuA++wFRElk9a98fGkNb+aI6p7OuDBB0dWmzHXy/zmZ6RtkCHBQkA1CMEO3keiQIDAQAB";
	private static final String SignatureAlgorithm = "SHA512WithRSA";
	private static final String KeyAlgorithm       = "RSA";
	private static final String CharSet            = "UTF-8";
	private static final String NameKey            = "name";
	private static final String DateKey            = "date";
	private static final String StartKey           = "start";
	private static final String EndKey             = "end";
	private static final String EditionKey         = "edition";
	private static final String ModulesKey         = "modules";
	private static final String MachineKey         = "machine";
	private static final String SignatureKey       = "key";

	private final SimpleDateFormat format        = new SimpleDateFormat("yyyy-MM-dd");
	private final Map<String, String> properties = new LinkedHashMap<>();
	private Set<String> modules                  = new LinkedHashSet<>();

	public LicenseHelper(final String licenseFileName) {

		logger.info("Looking for license file..");

		// read license file (if present)
		this.properties.putAll(read(licenseFileName));

		// read modules
		final String licensedModules = this.properties.get(ModulesKey);
		if (licensedModules != null) {

			for (final String module : licensedModules.split(",")) {

				modules.add(module.trim());
			}
		}
	}

	public boolean isModuleLicensed(final String module) {
		return modules.contains(module);
	}

	public boolean isValid(final String edition) {

		final String src             = collect(properties);
		final String key             = properties.get(SignatureKey);
		final String hostId          = properties.get(MachineKey);
		final String licensedEdition = properties.get(EditionKey);
		final String thisHostId      = createHash();
		final Date startDate         = parseDate(properties.get(StartKey));
		final Date endDate           = parseDate(properties.get(EndKey));
		final Date now               = new Date();

		// verify edition
		if (!licensedEdition.equals(edition)) {

			logger.error("Edition found in license file does not match current editon.");
			return false;
		}

		// verify host ID
		if (!hostId.equals(thisHostId)) {

			logger.error("Host ID found in license file does not match current host ID.");
			return false;
		}

		// verify that the license is valid for the current date
		if (now.before(startDate) && !now.equals(startDate)) {

			logger.error("License found in license file is not yet valid.");
			return false;
		}

		// verify that the license is valid for the current date
		if (now.after(endDate) && !now.equals(endDate)) {

			logger.error("License found in license file is not valid any more.");
			return false;
		}

		try {

			final byte[] data      = src.getBytes(CharSet);
			final Signature signer = Signature.getInstance(SignatureAlgorithm);
			final byte[] signature = Hex.decodeHex(key.toCharArray());

			signer.initVerify(publicFromBase64(PublicKey));
			signer.update(data);

			return signer.verify(signature);

		} catch (Throwable t) {
			logger.warn("Unable to verify license.", t);
		}

		return false;
	}

	public void create(final String name, final Date start, final Date end, final String edition, final String modules, final String hostId, final String privateKeyFileName, final String outputFileName) {

		final Map<String, String> properties = new LinkedHashMap<>();

		properties.put(NameKey,    name);
		properties.put(DateKey,    format.format(System.currentTimeMillis()));
		properties.put(StartKey,   format.format(start.getTime()));
		properties.put(EndKey,     format.format(end.getTime()));
		properties.put(EditionKey, edition);
		properties.put(ModulesKey, modules);
		properties.put(MachineKey, hostId);

		sign(properties, privateKeyFileName);
		write(properties, outputFileName);
	}

	public String getHardwareFingerprint() {
		return createHash();
	}

	// ----- private methods -----
	private void sign(final Map<String, String> properties, final String privateKeyFileName) {

		final String src = collect(properties);

		try {

			final byte[] data      = src.getBytes(CharSet);
			final Signature signer = Signature.getInstance(SignatureAlgorithm);

			signer.initSign(privateFromBase64(readFile(privateKeyFileName)));
			signer.update(data);

			properties.put(SignatureKey, Hex.encodeHexString(signer.sign()));

		} catch (Throwable t) {
			logger.warn("Unable to sign license.", t);
		}
	}

	private void write(final Map<String, String> properties, final String fileName) {

		try (final Writer writer = new FileWriter(fileName)) {

			properties.forEach((key, value) -> {

				try {
					writer.write(key);
					writer.write(" = ");
					writer.write(value);
					writer.write("\n");

				} catch (IOException ioex) {
					ioex.printStackTrace();
				}
			});

		} catch (IOException ioex) {
			logger.warn("Unable to write file.", ioex);
		}
	}

	private Map<String, String> read(final String fileName) {

		final Map<String, String> properties = new LinkedHashMap<>();

		// set some default values
		properties.put(NameKey, "Evaluation License");
		properties.put(EditionKey, "Community");
		properties.put(ModulesKey, "none");

		try (final BufferedReader reader = new BufferedReader(new FileReader(fileName))) {

			String line = reader.readLine();
			while (line != null) {

				final int pos = line.indexOf("=");
				if (pos >= 0) {

					final String key   = line.substring(0, pos).trim();
					final String value = line.substring(pos+1).trim();

					properties.put(key, value);
				}

				line = reader.readLine();
			}

		} catch (IOException ioex) {}

		return properties;
	}

	private String collect(final Map<String, String> properties) {

		final StringBuilder buf = new StringBuilder();

		buf.append(properties.get(NameKey));
		buf.append(properties.get(DateKey));
		buf.append(properties.get(StartKey));
		buf.append(properties.get(EndKey));
		buf.append(properties.get(EditionKey));
		buf.append(properties.get(ModulesKey));
		buf.append(properties.get(MachineKey));

		return buf.toString();
	}

	private String readFile(final String name) {

		try (final InputStream is = new FileInputStream(name)) {

			return IOUtils.toString(is);

		} catch (IOException ioex) {
			logger.warn("Unable to read file.", ioex);
		}

		return null;
	}

	private PrivateKey privateFromBase64(final String src) {

		try {

			final byte[] byteKey = Base64.decode(src.getBytes());
			final KeySpec key    = new PKCS8EncodedKeySpec(byteKey);
			final KeyFactory kf  = KeyFactory.getInstance(KeyAlgorithm);

			return kf.generatePrivate(key);

		} catch (Throwable t) {
			logger.warn("Unable to decode private key.", t);
		}

		return null;
	}

	private PublicKey publicFromBase64(final String src) {

		try {

			final byte[] byteKey = Base64.decode(src.getBytes());
			final KeySpec key    = new X509EncodedKeySpec(byteKey);
			final KeyFactory kf  = KeyFactory.getInstance(KeyAlgorithm);

			return kf.generatePublic(key);

		} catch (Throwable t) {
			logger.warn("Unable to decode public key.", t);
		}

		return null;
	}

	private String toBase64(final Key key) {

		try {

			return Base64.encodeToString(key.getEncoded(), true);

		} catch (Throwable t) {
			logger.warn("Unable to encode key.", t);
		}

		return null;
	}

	private String createHash() {

		try {

			final MessageDigest digest = MessageDigest.getInstance("MD5");

			// use network interface hardware addresses for host identification
			for (final NetworkInterface iface : getNetworkInterfaces()) {

				try {
					final byte[] hardwareAddress = iface.getHardwareAddress();
					if (hardwareAddress != null) {

						digest.update(hardwareAddress);
					}

				} catch (SocketException ex) {}
			}

			return Hex.encodeHexString(digest.digest());

		} catch (NoSuchAlgorithmException ex) {
			logger.warn("Unable to create hardware hash.", ex);
		}

		return null;
	}

	private byte[] toBytes(final String stringOrNull) {

		if (stringOrNull != null) {

			try {

				return stringOrNull.getBytes(CharSet);

			} catch (UnsupportedEncodingException ex) {}
		}

		return new byte[0];
	}

	private byte[] toBytes(final int value) {

		try {

			return Integer.toString(value).getBytes(CharSet);

		} catch (UnsupportedEncodingException ex) {}

		return new byte[0];
	}

	private Iterable<NetworkInterface> getNetworkInterfaces() {

		final List<NetworkInterface> interfaces = new LinkedList<>();

		try {
			for (final Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces(); enumeration.hasMoreElements();) {

				interfaces.add(enumeration.nextElement());
			}

		} catch (SocketException ex) {}

		return interfaces;
	}

	private Date parseDate(final String dateOrNull) {

		if (dateOrNull != null) {

			try {
				return alignToDay(format.parse(dateOrNull));

			} catch (Throwable ignore) {}
		}

		return new Date(0L);
	}

	private Date alignToDay(final Date date) {

		try {
			return format.parse(format.format(date));

		} catch (Throwable ignore) {}

		return null;
	}
}
