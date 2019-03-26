/**
 * Copyright (C) 2010-2018 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.jar;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.security.DigestOutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;
import org.structr.web.function.UiFunction;

/**
 *
 */
public class CreateJarFileFunction extends UiFunction {

	@Override
	public String getName() {
		return "create_jar_file";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

			if (sources[0] instanceof OutputStream) {

				try {

					final String algorithm       = "SHA1";
					final String signAlgorithm   = "SHA1withRSA";
					final String keygenAlgorithm = "RSA";
					final String srngAlgorithm   = "SHA1PRNG";

					final JarOutputStream jos       = new JarOutputStream((OutputStream)sources[0]);
					final MessageDigest md          = MessageDigest.getInstance(algorithm);
					final Manifest manifest         = new Manifest();
					final Attributes mainAttributes = manifest.getMainAttributes();

					final PrivateKey privateKey = getOrCreatePrivateKey(keygenAlgorithm, srngAlgorithm, signAlgorithm);
					final X509Certificate cert  = getOrCreateCertificate(keygenAlgorithm, srngAlgorithm, signAlgorithm);

					System.out.println("This is the fingerprint of the keystore: " + hex(cert));

//							if (false) {
//
//								// this code loads an existing keystore
//								final String keystorePath     = StructrApp.getConfigurationValue("application.keystore.path", null);
//								final String keystorePassword = StructrApp.getConfigurationValue("application.keystore.password", null);
//
//								X509Certificate cert       = null;
//								PrivateKey privateKey      = null;
//
//								if (StringUtils.isNoneBlank(keystorePath, keystorePassword)) {
//
//									try (final FileInputStream fis = new FileInputStream(keystorePath)) {
//
//										final KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
//
//										keystore.load(fis, keystorePassword.toCharArray());
//
//										for (final Enumeration<String> aliases = keystore.aliases(); aliases.hasMoreElements();) {
//
//											final String alias = aliases.nextElement();
//
//											if (keystore.isCertificateEntry(alias)) {
//
//												System.out.println("Using certificate entry " + alias);
//												cert = (X509Certificate)keystore.getCertificate(alias);
//
//											} else if (keystore.isKeyEntry(alias)) {
//
//												System.out.println("Using private key entry " + alias);
//												privateKey = (PrivateKey)keystore.getKey(alias, keystorePassword.toCharArray());
//
//											}
//										}
//
//
//									} catch (Throwable t) {
//
//										logger.warn("", t);
//									}
//								}
//							}
					// maximum compression
					jos.setLevel(9);

					// initialize manifest
					mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");

					// add entries from scripting context
					for (final Object source : sources) {

						if (source != null && source instanceof NameAndContent) {

							final NameAndContent content = (NameAndContent)source;
							final JarEntry entry = new JarEntry(content.getName());
							final byte[] data = content.getContent().getBytes("utf-8");

							entry.setTime(System.currentTimeMillis());

							// write JarEntry
							jos.putNextEntry(entry);
							jos.write(data);
							jos.closeEntry();
							jos.flush();

							// update message digest with data
							md.update(data);

							// create new attribute with the entry's name
							Attributes attr = manifest.getAttributes(entry.getName());
							if (attr == null) {

								attr = new Attributes();
								manifest.getEntries().put(entry.getName(), attr);
							}

							// store SHA1-Digest for the new entry
							attr.putValue(algorithm + "-Digest", new String(Base64.encode(md.digest()), "ASCII"));
						}
					}

					// add manifest entry
					jos.putNextEntry(new JarEntry(JarFile.MANIFEST_NAME));
					manifest.write(jos);

					// add signature entry
					final byte[] signedData = getSignatureForManifest(manifest, algorithm);
					jos.putNextEntry(new JarEntry("META-INF/CERT.SF"));
					jos.write(signedData);

					if (privateKey != null && cert != null) {

						// add certificate entry
						jos.putNextEntry(new JarEntry("META-INF/CERT." + privateKey.getAlgorithm()));
						writeSignatureBlock(jos, algorithm, new CMSProcessableByteArray(signedData), cert, privateKey);

					} else {

						System.out.println("No certificate / key found, signinig disabled.");
					}

					// use finish() here to avoid an "already closed" exception later
					jos.flush();
					jos.finish();

				} catch (Throwable t) {

					logException(caller, t, sources);

				}

			} else {

				logger.warn("First parameter of create_jar_file() must be an output stream. Parameters: {}", getParametersAsString(sources));
				return "First parameter of create_jar_file() must be an output stream.";
			}

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return "create_jar_file";
	}

	@Override
	public String shortDescription() {
		return "Creates a signed JAR file from the given contents.";
	}

	// ----- private methods -----
	private byte[] getSignatureForManifest(final Manifest forManifest, final String algorithm) throws IOException, GeneralSecurityException {

		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final Manifest signatureFile    = new Manifest();
		final Attributes main           = signatureFile.getMainAttributes();
		final MessageDigest md          = MessageDigest.getInstance(algorithm);
		final PrintStream print         = new PrintStream(new DigestOutputStream(new ByteArrayOutputStream(), md), true, "UTF-8");

		main.putValue("Signature-Version", "1.0");

		forManifest.write(print);
		print.flush();

		main.putValue(algorithm + "-Digest-Manifest", new String(Base64.encode(md.digest()), "ASCII"));

		final Map<String, Attributes> entries = forManifest.getEntries();

		for (Map.Entry<String, Attributes> entry : entries.entrySet()) {

			// Digest of the manifest stanza for this entry.
			print.print("Name: " + entry.getKey() + "\r\n");

			for (Map.Entry<Object, Object> att : entry.getValue().entrySet()) {
				print.print(att.getKey() + ": " + att.getValue() + "\r\n");
			}

			print.print("\r\n");
			print.flush();

			final Attributes sfAttr = new Attributes();
			sfAttr.putValue(algorithm + "-Digest", new String(Base64.encode(md.digest()), "ASCII"));

			signatureFile.getEntries().put(entry.getKey(), sfAttr);
		}

		signatureFile.write(bos);

		return bos.toByteArray();
	}

	private void writeSignatureBlock(final JarOutputStream jos, final String algorithm, final CMSTypedData data, final X509Certificate publicKey, final PrivateKey privateKey) throws IOException, CertificateEncodingException, OperatorCreationException, CMSException {

		final List<X509Certificate> certList = new ArrayList<>();
		certList.add(publicKey);

		final JcaCertStore certs                = new JcaCertStore(certList);
		final CMSSignedDataGenerator gen        = new CMSSignedDataGenerator();
		final ContentSigner signer              = new JcaContentSignerBuilder(algorithm + "with" + privateKey.getAlgorithm()).build(privateKey);
		final SignerInfoGenerator infoGenerator = new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().build()).setDirectSignature(true).build(signer, publicKey);

		gen.addSignerInfoGenerator(infoGenerator);
		gen.addCertificates(certs);

		final CMSSignedData sigData = gen.generate(data, false);
		final ASN1InputStream asn1  = new ASN1InputStream(sigData.getEncoded());
		final DEROutputStream dos   = new DEROutputStream(jos);
		final ASN1Primitive obj     = asn1.readObject();

		dos.writeObject(obj);
	}

	private PrivateKey getOrCreatePrivateKey(final String keygenAlgorithm, final String srngAlgorithm, final String signAlgorithm) {

		final KeyStore keyStore   = getOrCreateKeystore(keygenAlgorithm, srngAlgorithm, signAlgorithm);
		final String keystorePass = "test";

		if (keyStore != null) {

			try {
				return (PrivateKey)keyStore.getKey("priv", keystorePass.toCharArray());

			} catch (Throwable t) {
				logger.warn("", t);
			}
		}

		return null;
	}

	private X509Certificate getOrCreateCertificate(final String keygenAlgorithm, final String srngAlgorithm, final String signAlgorithm) {

		final KeyStore keyStore = getOrCreateKeystore(keygenAlgorithm, srngAlgorithm, signAlgorithm);
		if (keyStore != null) {

			try {
				return (X509Certificate)keyStore.getCertificate("cert");

			} catch (Throwable t) {
				logger.warn("", t);
			}
		}

		return null;
	}

	private KeyStore getOrCreateKeystore(final String keygenAlgorithm, final String srngAlgorithm, final String signAlgorithm) {

		final String keystorePath       = "test.keystore";
		final String keystorePass       = "test";
		final java.io.File keystoreFile = new java.io.File(keystorePath);

		if (keystoreFile.exists()) {

			try (final FileInputStream fis = new FileInputStream(keystoreFile)) {

				final KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());

				keystore.load(fis, keystorePass.toCharArray());

				return keystore;

			} catch (Throwable t) {

				logger.warn("", t);
			}

		} else {

			try (final FileOutputStream fos = new FileOutputStream(keystoreFile)) {

				final KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
				keystore.load(null, keystorePass.toCharArray());

				final KeyPairGenerator gen = KeyPairGenerator.getInstance(keygenAlgorithm);
				gen.initialize(1024, SecureRandom.getInstance(srngAlgorithm));

				final KeyPair keyPair                    = gen.generateKeyPair();
				final SimpleDateFormat dateFormat        = new SimpleDateFormat("dd.MM.yyyy");
				final Date startDate                     = dateFormat.parse("01.01.2015");
				final Date expiryDate                    = dateFormat.parse("01.01.2017");
				final BigInteger serialNumber            = BigInteger.valueOf(1234);
				final X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
				final X500Principal dnName               = new X500Principal("CN=Test CA Certificate");

				certGen.setSerialNumber(serialNumber);
				certGen.setIssuerDN(dnName);
				certGen.setNotBefore(startDate);
				certGen.setNotAfter(expiryDate);
				certGen.setSubjectDN(dnName);
				certGen.setPublicKey(keyPair.getPublic());
				certGen.setSignatureAlgorithm(signAlgorithm);

				final X509Certificate cert = certGen.generate(keyPair.getPrivate());

				keystore.setCertificateEntry("cert", cert);
				keystore.setKeyEntry("priv", keyPair.getPrivate(), keystorePass.toCharArray(), new Certificate[] { cert } );

				keystore.store(fos, keystorePass.toCharArray());

				fos.flush();

				return keystore;

			} catch (Throwable t) {

				logger.warn("", t);
			}
		}

		return null;

	}

	public String hex(final Certificate cert) {

		byte[] encoded;
		try {

			encoded = cert.getEncoded();

		} catch (CertificateEncodingException e) {

			encoded = new byte[0];
		}

		return hex(encoded);
	}

	public String hex(byte[] sig) {

		byte[] csig = new byte[sig.length * 2];

		for (int j = 0; j < sig.length; j++) {

			byte v = sig[j];
			int d = (v >> 4) & 0xf;
			csig[j * 2] = (byte) (d >= 10 ? ('a' + d - 10) : ('0' + d));
		   	d = v & 0xf;
			csig[j * 2 + 1] = (byte) (d >= 10 ? ('a' + d - 10) : ('0' + d));
		}

		return new String(csig);
	}

}
