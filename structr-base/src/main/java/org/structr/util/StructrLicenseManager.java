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
package org.structr.util;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.service.Feature;
import org.structr.api.service.LicenseManager;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.NodeServiceCommand;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.concurrent.TimeUnit;

/**
 */
public class StructrLicenseManager implements LicenseManager {

	private static final Logger logger      = LoggerFactory.getLogger(LicenseManager.class);
	private static final String Certificate =
		"MIIDpzCCAo+gAwIBAgIEbgcR/TANBgkqhkiG9w0BAQ0FADCBgzELMAkGA1UEBhMC" +
		"REUxDzANBgNVBAgTBkhlc3NlbjEaMBgGA1UEBxMRRnJhbmtmdXJ0IGFtIE1haW4x" +
		"FTATBgNVBAoTDFN0cnVjdHIgR21iSDEUMBIGA1UECxMLRGV2ZWxvcG1lbnQxGjAY" +
		"BgNVBAMTEUNocmlzdGlhbiBNb3JnbmVyMB4XDTE3MDUxNzExMjExMloXDTI4MDQy" +
		"OTExMjExMlowgYMxCzAJBgNVBAYTAkRFMQ8wDQYDVQQIEwZIZXNzZW4xGjAYBgNV" +
		"BAcTEUZyYW5rZnVydCBhbSBNYWluMRUwEwYDVQQKEwxTdHJ1Y3RyIEdtYkgxFDAS" +
		"BgNVBAsTC0RldmVsb3BtZW50MRowGAYDVQQDExFDaHJpc3RpYW4gTW9yZ25lcjCC" +
		"ASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAI2i0WhHV9OauRZ6viefhuSK" +
		"OZaKlTvVYi0HLmk99/JlfIvAkkmS3Dlz+xwp9SUUPdfFMvPvMpgwXyfj9HlZGMg6" +
		"fdSGocZyJpozVSmzlM0D1xJB6MbU1k8lcvI52IYx6gUCa7jQIoPxkwb+8/NP2SXA" +
		"56RVIsNQFGpb31AwKNFViE5CqCHcF4hQ3uB/rcbxPUQ9t94R51dNLMWw37lKiqpq" +
		"iHGKYmriBOV9iivdgUFFFA0hNbclnRImXhYuUgCBaPmtjOGDywBfkRs5kbRBixR5" +
		"V41DOqaFnG5jOKTW+ycLMMmtvHnPt1dHckvYTnnM0YUS9FINbXLoxZBw2hP1mfkC" +
		"AwEAAaMhMB8wHQYDVR0OBBYEFBM6fjBbxeespRRG87mEHbqzfXCGMA0GCSqGSIb3" +
		"DQEBDQUAA4IBAQANnACQFo5r8gNK8ULZ5yvndx8Bv3YNbQEtXgHSnaQLWAgIgflG" +
		"E4GF8/0OwaxVuGxvyCd/ib74vjfCgOJvNe+XOHQQ+6o30JaILar2353QvVS5cqSU" +
		"HACOyFaBa7ndjgHSYdzabZHdhmpIIv/Tx2whClYdCCLTQi1sYjtXGMJ4FJyyJqjm" +
		"IjREcPU1KT8etnYQXaTvj2njM26lZVWc7DizsN83b+vL5h2m+z/4I5dAwVGEBMFZ" +
		"/u7r3lfqJ4h5bVTb6AMJ9gl/lOyob46gH1jNbx5ld53/ADgDFRcMW2vwLiZhay0p" +
		"Nhx1o8vT2VnQUzZxU1G4gnkdtiXN6knFTorl";

	public static final String DataEncryptionAlgorithm = "AES/CBC/PKCS5Padding";
	public static final String KeyEncryptionAlgorithm  = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
	public static final String SignatureAlgorithm      = "SHA512WithRSA";
	public static final String DatePattern             = "yyyy-MM-dd";
	public static final String KeystoreAlias           = "structr";
	public static final String KeyAlgorithm            = "RSA";
	public static final String CharSet                 = "UTF-8";
	public static final String NameKey                 = "name";
	public static final String DateKey                 = "date";
	public static final String StartKey                = "start";
	public static final String EndKey                  = "end";
	public static final String EditionKey              = "edition";
	public static final String ModulesKey              = "modules";
	public static final String MachineKey              = "machine";
	public static final String SignatureKey            = "key";
	public static final String ServersKey              = "servers";
	public static final String UsersKey                = "users";
	public static final String HostIdKey               = "hostId";
	public static final String LimitKey                = "limit";
	public static final String HostIdMappingKey        = "hostIdValidationAttempts";
	public static final int ServerPort                 = 5725;

	private final Set<String> communityModules          = new LinkedHashSet<>(Arrays.asList("base", "core", "rest", "ui"));
	private final Set<String> modules                   = new LinkedHashSet<>(communityModules);
	private final Set<String> classes                   = new LinkedHashSet<>();
	private Certificate certificate                     = null;
	private PublicKey publicKey                         = null;
	private boolean allModulesLicensed                  = false;
	private boolean licensePresent                      = false;
	private String edition                              = "Community";
	private String moduleString                         = null;
	private String licensee                             = null;
	private String numberOfUsersString                  = null;
	private Date startDate                              = null;
	private Date endDate                                = null;

	public StructrLicenseManager() {

		logger.info("Host ID is {}", createHash());
		logger.info("Checking Structr license..");

		// initialize certificate from static data above
		this.certificate = certFromBase64(Certificate);
		this.publicKey   = certificate.getPublicKey();

		initializeLicense();
	}

	private void initializeLicense() {

		final boolean licenseWasPresent = licensePresent;

		final String currentBase64LicenseKey = getBase64LicenseKey();
		final Map<String, String> properties = getLicenseKeyFromString(currentBase64LicenseKey);

		if (licensePresent) {

			if (licenseWasPresent) {

				final boolean editionEqual = edition      != null && edition.equals(properties.get(EditionKey));
				final boolean modulesEqual = moduleString != null && moduleString.equals(properties.get(ModulesKey));

				if (!editionEqual || !modulesEqual) {
					logger.warn("License Edition or Modules changed - unable to update license info without a restart. Please restart to update license info.");
					return;
				}
			}

			if (isValid(properties)) {

				edition                   = properties.get(EditionKey);
				moduleString              = properties.get(ModulesKey);
				licensee                  = properties.get(NameKey);
				numberOfUsersString       = properties.get(UsersKey);

				// init modules
				if (moduleString != null) {

					// remove default modules from Community Edition
					modules.clear();

					if ("*".equals(moduleString)) {

						allModulesLicensed = true;

					} else if (!"none".equals(moduleString)) {

						for (final String module : moduleString.split(",")) {

							modules.add(module.trim());
						}
					}
				}

			} else {

				// check license validation setting
				if (!Settings.LicenseAllowFallback.getValue(false)) {

					logger.info("No valid license found and {} has a value of false, exiting.", Settings.LicenseAllowFallback.getKey());
					System.exit(2);

				} else {

					logger.info("No valid license found, but {} has a value of true, continuing.", Settings.LicenseAllowFallback.getKey());
				}
			}
		}

		logLicenseInfo();
	}

	@Override
	public void logLicenseInfo() {

		logger.info("Running {} Edition with {}.", edition, allModulesLicensed ? "all modules" : "modules " + modules.toString());

		if (licensee != null) {

			logger.info("Licensed for {}", licensee);

		} else {

			logger.info("Evaluation License");
		}

		final int userLimit = getNumberOfUsers();
		if (userLimit > 0) {

			logger.info("Licensed for {} users", userLimit);
		}
	}

	@Override
	public void refresh(boolean readLicense) {

		if (readLicense) {

			if (licensePresent == true) {

				initializeLicense();

			} else {

				logger.warn("Unable to update license info without a restart because no previous license was configured!");
			}
		}

		if (endDate == null) {
			// nothing to do - was unlicensed and still is

		} else if (licenseExpired(endDate)) {

			final SimpleDateFormat format = new SimpleDateFormat(DatePattern);

			logger.error("License found in license file is not valid any more, license period ended {}.", format.format(endDate.getTime()));

			modules.clear();
			modules.addAll(communityModules);

			allModulesLicensed = false;

			edition = "Community";

			// check license validation setting
			if (!Settings.LicenseAllowFallback.getValue(false)) {

				logger.info("No valid license found and {} has a value of false, exiting.", Settings.LicenseAllowFallback.getKey());
				System.exit(2);

			} else {

				logger.info("No valid license found, but {} has a value of true, continuing.", Settings.LicenseAllowFallback.getKey());
			}
		}
	}

	@Override
	public String getEdition() {
		return edition;
	}

	@Override
	public boolean isModuleLicensed(final String module) {
		return allModulesLicensed || modules.contains(module);
	}

	@Override
	public boolean isClassLicensed(final String fqcn) {
		return allModulesLicensed || classes.contains(fqcn);
	}

	@Override
	public String getLicensee() {
		return licensee;
	}

	@Override
	public String getHardwareFingerprint() {
		return createHash();
	}

	@Override
	public Date getStartDate() {
		return startDate;
	}

	@Override
	public Date getEndDate() {
		return endDate;
	}

	@Override
	public int getNumberOfUsers() {

		if (numberOfUsersString != null) {

			try {

				return Integer.valueOf(numberOfUsersString);

			} catch (Throwable t) {
				logger.error("Invalid value for number of users in license key: {}: {}", numberOfUsersString, t.getMessage());
			}
		}

		return -1;
	}

	@Override
	public boolean isValid(final Feature feature) {

		if (feature != null) {

			final String moduleName = feature.getModuleName();
			return moduleName != null && isModuleLicensed(moduleName);
		}

		return false;
	}

	/**
	 *
	 * @param codeSigners
	 * @return
	 */
	@Override
	public boolean isValid(final CodeSigner[] codeSigners) {

		if (codeSigners != null && codeSigners.length > 0) {

			for (final CodeSigner codeSigner : codeSigners) {

				for (final Certificate cert : codeSigner.getSignerCertPath().getCertificates()) {

					try {

						cert.verify(publicKey);
						return true;

					} catch (Throwable ignore) {}
				}
			}
		}

		// none of the code signer certificates could be verified using our key => not valid
		return false;
	}

	@Override
	public void addLicensedClass(final String fqcn) {
		classes.add(fqcn);
	}

	// ----- private methods -----
	private boolean isValid(final Map<String, String> properties) {

		if (!licensePresent) {
			return false;
		}

		final SimpleDateFormat format = new SimpleDateFormat(DatePattern);
		final String src              = collectLicenseFieldsForSignature(properties);
		final String name             = properties.get(NameKey);
		final String key              = properties.get(SignatureKey);
		final String hostId           = properties.get(MachineKey);
		final String thisHostId       = createHash();
		final String edition          = properties.get(EditionKey);
		final String modules          = properties.get(ModulesKey);
		final String dateString       = properties.get(DateKey);
		final String startDateString  = properties.get(StartKey);
		final String endDateString    = properties.get(EndKey);
		final String serversString    = properties.get(ServersKey);
		final String usersString      = properties.get(UsersKey); // unused (cannot be verified here)
		final Date now                = new Date();

		if (StringUtils.isEmpty(key)) {

			logger.error("Unable to read key from license file.");
			return false;
		}

		final Date licenseStartDate = parseDate(startDateString);
		final Date licenseEndDate   = parseDate(endDateString);

		// verify that the license is valid for the current date
		if (licenseStartDate != null && now.before(licenseStartDate) && !now.equals(licenseStartDate)) {

			logger.error("License found in license file is not yet valid, license period starts {}.", format.format(licenseStartDate.getTime()));
			return false;
		}

		// verify that the license is valid for the current date
		if (licenseExpired(licenseEndDate)) {

			logger.error("License found in license file is not valid any more, license period ended {}.", format.format(licenseEndDate.getTime()));
			return false;
		}

		try {

			final byte[] data      = src.getBytes(CharSet);
			final Signature signer = Signature.getInstance(SignatureAlgorithm);
			final byte[] signature = Hex.decodeHex(key.toCharArray());

			signer.initVerify(certificate);
			signer.update(data);

			if (!signer.verify(signature)) {

				logger.error("License signature verification failed, license is not valid.");
				return false;
			}

		} catch (Throwable t) {

			logger.error("Unable to verify license.", t);
			return false;
		}

		if (StringUtils.isEmpty(name)) {

			logger.error("License file doesn't contain licensee name.");
			return false;
		}

		if (StringUtils.isEmpty(edition)) {

			logger.error("License file doesn't contain edition.");
			return false;
		}

		if (StringUtils.isEmpty(modules)) {

			logger.error("License file doesn't contain modules.");
			return false;
		}

		if (StringUtils.isEmpty(hostId)) {

			logger.error("License file doesn't contain host ID.");
			return false;
		}

		if (StringUtils.isEmpty(dateString)) {

			logger.error("License file doesn't contain license date.");
			return false;
		}

		if (StringUtils.isEmpty(startDateString)) {

			logger.error("License file doesn't contain start date.");
			return false;
		}

		if (StringUtils.isEmpty(endDateString)) {

			logger.error("License file doesn't contain end date.");
			return false;
		}

		// verify host ID
		if (!thisHostId.equals(hostId) && !"*".equals(hostId)) {

			logger.error("Host ID found in license ({}) file does not match current host ID.", hostId);
			return false;
		}

		startDate = licenseStartDate;
		endDate   = licenseEndDate;

		if ("*".equals(hostId)) {

			// check volume license against server addresses
			if (StringUtils.isNotBlank(serversString)) {

				// send HostID to server
				properties.put(HostIdKey, thisHostId);

				return checkVolumeLicense(properties, serversString);
			}

			final Calendar issuedAtPlusOneMonth = GregorianCalendar.getInstance();
			final Calendar cal                  = GregorianCalendar.getInstance();

			// set issuedAt to license date plus one month
			issuedAtPlusOneMonth.setTime(parseDate(dateString));
			issuedAtPlusOneMonth.add(Calendar.MONTH, 1);

			// check that the license file was issued not more than one month ago
			if (cal.after(issuedAtPlusOneMonth)) {

				logger.error("Development license found in license file is not valid any more, license period ended {}.", format.format(issuedAtPlusOneMonth.getTime()));
				return false;
			}
		}

		return true;
	}

	private boolean licenseExpired(final Date licenseEndDate) {

		final Date now = new Date();

		if (licenseEndDate != null) {

			final Calendar cal = Calendar.getInstance();

			cal.setTime(licenseEndDate);
			cal.add(Calendar.DAY_OF_YEAR, 1);

			final Date dayAfterLicenseExpiryDate = cal.getTime();

			return (licenseEndDate != null && now.after(dayAfterLicenseExpiryDate));
		}

		return true;
	}

	private String readFileToString(final String fileName) {

		final Path source      = Paths.get(Settings.getBasePath());
		final Path licenseFile = source.resolve(fileName);

		if (Files.exists(licenseFile)) {

			try {

				return new String(Files.readAllBytes(licenseFile));

			} catch (IOException ioex) {

				logger.warn("Error reading license file: {}", ioex.getMessage());
			}
		}

		return null;
	}

	private String getBase64LicenseKey() {

		final String licenseKeyFromFile = readFileToString("license.key");

		if (licenseKeyFromFile != null) {
			return licenseKeyFromFile;
		}

		final String licenseKey = Settings.LicenseKey.getValue();

		if (StringUtils.isNotBlank(licenseKey)) {
			return licenseKey;
		}

		return null;
	}

	private Map<String, String> getLicenseKeyFromString(final String licenseKey) {

		if (StringUtils.isNotBlank(licenseKey)) {

			final Decoder base64Decoder = java.util.Base64.getMimeDecoder();
			final String decodedKey     = new String(base64Decoder.decode(licenseKey));

			licensePresent = true;

			final Map<String, String> properties = new LinkedHashMap<>();
			decodedKey.lines().forEach(line -> {

				final int pos = line.indexOf("=");
				if (pos >= 0) {

					final String key   = line.substring(0, pos).trim();
					final String value = line.substring(pos+1).trim();

					properties.put(key, value);
				}
			});

			return properties;

		} else {
			return Collections.emptyMap();
		}
	}

	private Certificate certFromBase64(final String src) {

		try {

			final byte[] byteKey = Base64.decode(src.getBytes());

			return CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(byteKey));

		} catch (Throwable t) {
			logger.warn("Unable to decode public key.", t);
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

				final SimpleDateFormat format = new SimpleDateFormat(DatePattern);
				return alignToDay(format.parse(dateOrNull));

			} catch (Throwable ignore) {}
		}

		return new Date(0L);
	}

	private Date alignToDay(final Date date) {

		try {

			final SimpleDateFormat format = new SimpleDateFormat(DatePattern);
			return format.parse(format.format(date));

		} catch (Throwable ignore) {}

		return null;
	}

	// ----- private static methods -----
	public static String collectLicenseFieldsForSignature(final Map<String, String> properties) {

		final StringBuilder buf = new StringBuilder();

		buf.append(properties.get(NameKey));
		buf.append(properties.get(DateKey));
		buf.append(properties.get(StartKey));
		buf.append(properties.get(EndKey));
		buf.append(properties.get(EditionKey));
		buf.append(properties.get(ModulesKey));
		buf.append(properties.get(MachineKey));

		// optional values
		final String servers = properties.get(ServersKey);
		if (StringUtils.isNotBlank(servers)) {

			buf.append(servers);
		}

		final String users = properties.get(UsersKey);
		if (StringUtils.isNotBlank(users)) {

			buf.append(users);
		}

		return buf.toString();
	}

	private static void create(final String name, final String start, final String end, final String edition, final String modules, final String hostId, final String servers, final String users, final String keystoreFileName, final String password, final String outputFileName) {

		final Map<String, String> properties = new LinkedHashMap<>();
		final SimpleDateFormat format        = new SimpleDateFormat(DatePattern);

		properties.put(NameKey,    name.trim());
		properties.put(DateKey,    format.format(System.currentTimeMillis()));
		properties.put(StartKey,   start.trim());
		properties.put(EndKey,     end.trim());
		properties.put(EditionKey, edition.trim());
		properties.put(ModulesKey, modules.trim());
		properties.put(MachineKey, hostId.trim());

		if (StringUtils.isNotBlank(servers)) {
			properties.put(ServersKey, servers.trim());
		}

		if (StringUtils.isNotBlank(users)) {
			properties.put(UsersKey, users.trim());
		}

		sign(properties, keystoreFileName, password);
		write(properties, outputFileName);
	}

	private static void sign(final Map<String, String> properties, final String keystoreFileName, final String password) {

		final String src = collectLicenseFieldsForSignature(properties);

		try {

			final byte[] data       = src.getBytes(CharSet);
			final Signature signer  = Signature.getInstance(SignatureAlgorithm);
			final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

			try (final InputStream is = new FileInputStream(keystoreFileName)) {

				keyStore.load(is, password.toCharArray());

				final Key key = keyStore.getKey(KeystoreAlias, password.toCharArray());

				signer.initSign((PrivateKey)key);
				signer.update(data);

				properties.put(SignatureKey, Hex.encodeHexString(signer.sign()));
			}

		} catch (Throwable t) {
			logger.warn("Unable to sign license.", t);
		}
	}

	private static void write(final Map<String, String> properties, final String fileName) {

		final Encoder base64Encoder = java.util.Base64.getEncoder();

		try (final Writer writer = new OutputStreamWriter(base64Encoder.wrap(new FileOutputStream(fileName)))) {

			writer.write(write(properties));

		} catch (IOException ioex) {
			logger.warn("Unable to write file.", ioex);
		}
	}

	private static String write(final Map<String, String> properties) {

		final StringBuilder buf = new StringBuilder();

		properties.forEach((key, value) -> {

			buf.append(key);
			buf.append(" = ");
			buf.append(value);
			buf.append("\n");
		});

		return buf.toString();
	}

	private boolean checkVolumeLicense(final Map<String, String> properties, final String serversString) {

		try {

			final KeyGenerator kgen = KeyGenerator.getInstance("AES");
			final byte[] data       = write(properties).getBytes("utf-8");
			final String name       = properties.get(NameKey);
			final byte[] expected   = name.getBytes("utf-8");

			kgen.init(128);

			for (final String part : serversString.split("[, ]+")) {

				final String address = part.trim();

				if (StringUtils.isNotBlank(address)) {

					try {

						logger.info("Trying to verify volume license with server {}", address);

						final long t0              = System.currentTimeMillis();
						final SecretKey aesKey     = kgen.generateKey(); // symmetric stream key
						final byte[] ivspec        = RandomUtils.nextBytes(16); // initialization vector for stream cipher
						final byte[] key           = encryptSessionKey(aesKey.getEncoded());
						final byte[] encryptedIV   = encryptSessionKey(ivspec);
						final byte[] encryptedData = encryptData(data, aesKey, ivspec);
						final byte[] response      = sendAndReceive(address, key, encryptedIV, encryptedData);
						final boolean result       = verify(expected, response);

						if (result == true) {
							logger.info("License verified in {} ms", System.currentTimeMillis() - t0);
						}

						return result;

					} catch (Throwable t) {
						logger.warn("Unable to verify volume license: {}", t.getMessage());
					}
				}
			}

		} catch (Throwable t) {
			logger.error("", t);
		}

		return false;
	}

	private byte[] encryptData(final byte[] data, final SecretKey sessionKey, final byte[] ivSpec) throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, UnsupportedEncodingException, InvalidAlgorithmParameterException {

		// setup
		final Cipher cipher = Cipher.getInstance(DataEncryptionAlgorithm);

		cipher.init(Cipher.ENCRYPT_MODE, sessionKey, new IvParameterSpec(ivSpec));

		return cipher.doFinal(data);
	}

	private byte[] encryptSessionKey(final byte[] sessionKey) throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, UnsupportedEncodingException {

		// setup
		final Cipher cipher = Cipher.getInstance(KeyEncryptionAlgorithm);

		cipher.init(Cipher.ENCRYPT_MODE, certificate);

		return cipher.doFinal(sessionKey);
	}

	private byte[] sendAndReceive(final String address, final byte[] key, final byte[] ivspec, final byte[] data) {

		if (address != null && address.toLowerCase().startsWith("http://")) {

			return sendAndReceiveHttp(address, key, ivspec, data);

		} else {

			return sendAndReceiveBinary(address, key, ivspec, data);
		}

	}

	private byte[] sendAndReceiveBinary(final String address, final byte[] key, final byte[] ivspec, final byte[] data) {

		final int timeoutMilliseconds = Long.valueOf(TimeUnit.SECONDS.toMillis(Settings.LicenseValidationTimeout.getValue(10))).intValue();
		final int retries             = 3;

		// try to connect to the license server 3 times..
		for (int i=0; i<retries; i++) {

			final byte[] result = new byte[256];

			try(final Socket socket = new java.net.Socket(address, ServerPort)) {

				socket.getOutputStream().write(key);
				socket.getOutputStream().flush();

				socket.getOutputStream().write(ivspec);
				socket.getOutputStream().flush();

				socket.getOutputStream().write(data);
				socket.getOutputStream().flush();

				socket.setSoTimeout(timeoutMilliseconds);

				// read exactly 256 bytes (size of expected signature response)
				socket.getInputStream().read(result, 0, 256);

				return result;

			} catch (ConnectException cex) {

				logger.warn("Unable to verify volume license: {}, attempt {} of {}", cex.getMessage(), (i+1), retries);

				// wait some time..
				try { Thread.sleep(1234 * i); } catch (Throwable t) {}

				if ((i+1) == retries) {
					throw new RuntimeException("no connection to license server");
				}

			} catch (IOException ioex) {

				logger.warn("Unable to verify volume license: {}, attempt {} of {}", ioex.getMessage(), (i+1), retries);

				if ((i+1) == retries) {
					throw new RuntimeException("verification failed");
				}
			}
		}

		return null;
	}

	private byte[] sendAndReceiveHttp(final String address, final byte[] key, final byte[] ivspec, final byte[] data) {

		final int timeoutMilliseconds = Long.valueOf(TimeUnit.SECONDS.toMillis(Settings.LicenseValidationTimeout.getValue(10))).intValue();
		final int retries             = 3;

		// try to connect to the license server 3 times..
		for (int i=0; i<retries; i++) {

			try {
				final URL url = URI.create(address).toURL();
				URLConnection connection;

				final String proxyUrl = Settings.HttpProxyUrl.getValue();
				final int proxyPort = Settings.HttpProxyPort.getValue(80);

				if (StringUtils.isNotBlank(proxyUrl)) {
					logger.info("Using configured Proxy {}:{} for license check request", proxyUrl, proxyPort);
					Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyUrl, proxyPort));
					connection = url.openConnection(proxy);
 				} else {
					connection = url.openConnection();
				}

				final HttpURLConnection http   = (HttpURLConnection)connection;

				http.setDoInput(true);
				http.setDoOutput(true);
				http.setUseCaches(false);
				http.setReadTimeout(timeoutMilliseconds);
				http.setFollowRedirects(true);
				http.setRequestMethod("POST");
				http.setRequestProperty("Content-Type", "application/octet-stream");

				http.connect();

				// write data
				http.getOutputStream().write(key);
				http.getOutputStream().flush();
				http.getOutputStream().write(ivspec);
				http.getOutputStream().flush();
				http.getOutputStream().write(data);
				http.getOutputStream().flush();
				http.getOutputStream().close();

				// read response
				final byte[] response = http.getInputStream().readNBytes(256);

				http.getInputStream().close();
				http.disconnect();

				return response;

			} catch (MalformedURLException | ProtocolException mex) {

				// don't retry if the URL is invalid..
				i = 3;

				// log error
				mex.printStackTrace();

			} catch (ConnectException cex) {

				logger.warn("Unable to verify volume license: {}, attempt {} of {}", cex.getMessage(), (i+1), retries);

				// wait some time..
				try { Thread.sleep(1234 * i); } catch (Throwable t) {}

				if ((i+1) == retries) {
					throw new RuntimeException("no connection to license server");
				}

			} catch (IOException ioex) {

				logger.warn("Unable to verify volume license: {}, attempt {} of {}", ioex.getMessage(), (i+1), retries);

				if ((i+1) == retries) {
					throw new RuntimeException("verification failed");
				}
			}
		}

		return null;
	}

	private boolean verify(final byte[] data, final byte[] signatureData) {

		try {

			final Signature verifier = Signature.getInstance(SignatureAlgorithm);

			verifier.initVerify(certificate);
			verifier.update(data);

			if (verifier.verify(signatureData)) {

				return true;
			}

		} catch (Throwable t) {
			logger.warn("Unable to verify volume license: {}", t.getMessage());
		}

		logger.error("License verification failed, license is not valid.");

		return false;
	}

	// ----- nested classes ------
	public static class CreateLicenseCommand extends NodeServiceCommand implements MaintenanceCommand {

		private String licenseContent = "";
		private boolean success = true;

		@Override
		public void execute(final Map<String, Object> attributes) throws FrameworkException {

			final String name     = (String)attributes.get(NameKey);
			final String start    = (String)attributes.get(StartKey);
			final String end      = (String)attributes.get(EndKey);
			final String edition  = (String)attributes.get(EditionKey);
			final String modules  = (String)attributes.get(ModulesKey);
			final String hostId   = (String)attributes.get(MachineKey);
			final String servers  = (String)attributes.get(ServersKey);
			final String users    = (String)attributes.get(UsersKey);
			final String keystore = (String)attributes.get("keystore");
			final String password = (String)attributes.get("password");

			// outfile, fallback to license.key
			String outFile = (String)attributes.get("outFile");
			if (outFile == null) {
				outFile = "license.key";
			}

			if (name == null || start == null || end == null || edition == null || modules == null || hostId == null || keystore == null || password == null) {

				success = false;
				logger.warn("Cannot create license file, missing parameter. Parameters are: name, start, end, edition, modules, machine, keystore, password, outFile (optional).");

			} else {

				StructrLicenseManager.create(name, start, end, edition, modules, hostId, servers, users, keystore, password, outFile);
				logger.info("Successfully created license file {}.", outFile);

				try {

					licenseContent = Files.readString(Paths.get(outFile));

				} catch (IOException ioex) {

					success = false;
				}
			}
		}

		@Override
		public boolean requiresEnclosingTransaction() {
			return false;
		}

		@Override
		public boolean requiresFlushingOfCaches() {
			return false;
		}

		@Override
		public Object getCommandResult() {

			final Map<String, Object> result = new HashMap<>();
			result.put("success", success);

			if (success) {

				result.put("license", licenseContent);

			} else {

				result.put("error", "Unable to create license");
			}

			return result;
		}
	}


	public static class UpdateLicenseCommand extends NodeServiceCommand implements MaintenanceCommand {

		@Override
		public void execute(final Map<String, Object> attributes) throws FrameworkException {

			Services.getInstance().getLicenseManager().refresh(true);
		}

		@Override
		public boolean requiresEnclosingTransaction() {
			return false;
		}

		@Override
		public boolean requiresFlushingOfCaches() {
			return false;
		}
	}
}
