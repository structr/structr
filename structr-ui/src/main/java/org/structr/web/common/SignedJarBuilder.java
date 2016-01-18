/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.web.common;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.encoders.Base64;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.DigestOutputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * A Jar file builder with signature support.
 */
public class SignedJarBuilder {

	private final byte[] buffer             = new byte[4096];
	private JarOutputStream jarOutputStream = null;
	private PrivateKey privateKey           = null;
	private X509Certificate certificate     = null;
	private Manifest manifest               = null;
	private MessageDigest messageDigest     = null;

	public SignedJarBuilder(final OutputStream out, final PrivateKey key, final X509Certificate certificate) throws IOException, NoSuchAlgorithmException {

		this.jarOutputStream = new JarOutputStream(new BufferedOutputStream(out));
		this.privateKey  = key;
		this.certificate = certificate;

		this.jarOutputStream.setLevel(9);

		if (privateKey != null && certificate != null) {

			manifest = new Manifest();
			Attributes main = manifest.getMainAttributes();
			main.putValue("Manifest-Version", "1.0");

			messageDigest = MessageDigest.getInstance("SHA1");
		}
	}

	/**
	 * Writes a new {@link File} into the archive.
	 *
	 * @param inputFile the {@link File} to write.
	 * @param jarPath the filepath inside the archive.
	 * @throws IOException
	 */
	public void writeFile(File inputFile, String jarPath) throws IOException {

		// Get an input stream on the file.
		FileInputStream fis = new FileInputStream(inputFile);
		try {
			// create the zip entry
			JarEntry entry = new JarEntry(jarPath);
			entry.setTime(inputFile.lastModified());
			writeEntry(fis, entry);

		} finally {
			// close the file stream used to read the file
			fis.close();
		}
	}

	public void close() throws IOException {

		if (manifest != null) {

			// write the manifest to the jar file
			jarOutputStream.putNextEntry(new JarEntry(JarFile.MANIFEST_NAME));
			manifest.write(jarOutputStream);

			try {

				final byte[] signedData = getSignature(manifest);

				jarOutputStream.putNextEntry(new JarEntry("META-INF/CERT.SF"));
				jarOutputStream.write(signedData);

				jarOutputStream.putNextEntry(new JarEntry("META-INF/CERT." + privateKey.getAlgorithm()));
				writeSignatureBlock(jarOutputStream, new CMSProcessableByteArray(signedData), certificate, privateKey);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		jarOutputStream.close();
		jarOutputStream = null;
	}

	/**
	 * Clean up of the builder for interrupted workflow. This does nothing if {@link #close()} was called successfully.
	 */
	public void cleanUp() {

		if (jarOutputStream != null) {
			try {
				jarOutputStream.close();
			} catch (IOException e) {
				// pass
			}
		}
	}

	/**
	 * Adds an entry to the output jar, and write its content from the {@link InputStream}
	 *
	 * @param input The input stream from where to write the entry content.
	 * @param entry the entry to write in the jar.
	 * @throws IOException
	 */
	private void writeEntry(final InputStream input, final JarEntry entry) throws IOException {

		// add the entry to the jar archive
		jarOutputStream.putNextEntry(entry);

		// read the content of the entry from the input stream, and write it into the archive.
		int count;

		while ((count = input.read(buffer)) != -1) {

			jarOutputStream.write(buffer, 0, count);

			if (messageDigest != null) {
				messageDigest.update(buffer, 0, count);
			}
		}

		jarOutputStream.closeEntry();

		if (manifest != null) {

			Attributes attr = manifest.getAttributes(entry.getName());
			if (attr == null) {

				attr = new Attributes();
				manifest.getEntries().put(entry.getName(), attr);
			}

			attr.putValue("SHA1-Digest", new String(Base64.encode(messageDigest.digest()), "ASCII"));
		}
	}

	/**
	 * Writes a .SF file with a digest to the manifest.
	 */
	private byte[] getSignature(final Manifest forManifest) throws IOException, GeneralSecurityException {

		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final Manifest signatureFile    = new Manifest();
		final Attributes main           = signatureFile.getMainAttributes();
		final MessageDigest md          = MessageDigest.getInstance("SHA1");
		final PrintStream print         = new PrintStream(new DigestOutputStream(new ByteArrayOutputStream(), md), true, "UTF-8");

		main.putValue("Signature-Version", "1.0");

		// Digest of the entire manifest
		forManifest.write(print);
		print.flush();

		main.putValue("SHA1-Digest-Manifest", new String(Base64.encode(md.digest()), "ASCII"));

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
			sfAttr.putValue("SHA1-Digest", new String(Base64.encode(md.digest()), "ASCII"));

			signatureFile.getEntries().put(entry.getKey(), sfAttr);
		}

		signatureFile.write(bos);

		return bos.toByteArray();
	}

	/**
	 * Write the certificate file with a digital signature.
	 */
	private void writeSignatureBlock(final JarOutputStream jos, final CMSTypedData data, final X509Certificate publicKey, final PrivateKey privateKey) throws IOException, CertificateEncodingException, OperatorCreationException, CMSException {

		final List<X509Certificate> certList = new ArrayList<>();
		certList.add(publicKey);

		final JcaCertStore certs         = new JcaCertStore(certList);
		final CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
		final ContentSigner sha1Signer   = new JcaContentSignerBuilder("SHA1with" + privateKey.getAlgorithm()).build(privateKey);

		gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().build()).setDirectSignature(true).build(sha1Signer, publicKey));
		gen.addCertificates(certs);

		final CMSSignedData sigData = gen.generate(data, false);
		final ASN1InputStream asn1  = new ASN1InputStream(sigData.getEncoded());
		final DEROutputStream dos   = new DEROutputStream(jos);

		dos.writeObject(asn1.readObject());
	}
}
