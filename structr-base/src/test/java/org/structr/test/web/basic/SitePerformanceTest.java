/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.test.web.basic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.traits.definitions.SiteTraitDefinition;
import org.structr.web.traits.definitions.dom.PageTraitDefinition;
import org.testng.annotations.Test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Measures what the Site lookup in HtmlServlet costs per request.
 *
 * A page that names a site is answered from its own relationships, while a page without one has to ask
 * whether any site claims the requested host, which is the only path that grows with the number of sites.
 *
 * Both paths are timed alternately against the same warm connection and the cost is taken as the difference
 * within each pair, so drift affects both halves equally and cancels. Measuring the two phases one after the
 * other instead lets JIT warm-up swamp the signal, which is several hundred microseconds against a request
 * of a few milliseconds. Which of the two is measured first is swapped every other pair for the same reason.
 */
public class SitePerformanceTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(SitePerformanceTest.class);

	// enough sites to lift the scan clear of the noise, few enough to keep the test quick
	private static final int MANY_SITES = 200;
	private static final int SAMPLES    = 250;
	private static final int WARMUP     = 150;

	// the scan is one node query plus a hostname and port comparison per site, measured at roughly 2 microseconds
	private static final double BUDGET_MICROS_PER_SITE = 50.0;

	@Test
	public void test01ScanCostGrowsOnlyForPagesWithoutASite() {

		createPagesAndFirstSite();

		final double scanAtOne = medianScanCostMillis();

		addSites(MANY_SITES);

		final double scanAtMany = medianScanCostMillis();
		final double perSiteMicros = ((scanAtMany - scanAtOne) * 1000.0) / (MANY_SITES - 1);

		logger.info("Site lookup, median over {} pairs of requests after {} warmup requests:", SAMPLES, WARMUP);
		logger.info("  the scan costs {} ms at 1 site and {} ms at {} sites", String.format("%.3f", scanAtOne), String.format("%.3f", scanAtMany), MANY_SITES);
		logger.info("  that is {} microseconds per site, budget {}", String.format("%.2f", perSiteMicros), BUDGET_MICROS_PER_SITE);

		// a measurement that cannot see the scan at all would pass every upper bound, so it has to fail instead
		assertTrue("The scan of " + MANY_SITES + " sites measured " + String.format("%.3f", scanAtMany)
			+ " ms, which is not above the cost at a single site. The measurement lost its signal, so it cannot"
			+ " tell a regression from noise.", scanAtMany > scanAtOne);

		assertTrue("Scanning the sites costs " + String.format("%.2f", perSiteMicros) + " microseconds per site, budget is "
			+ BUDGET_MICROS_PER_SITE + ". Either the per-site comparison got more expensive, or the single query per"
			+ " request turned into one query per site.", perSiteMicros < BUDGET_MICROS_PER_SITE);
	}

	/**
	 * Times a request that scans and a request that does not, alternately, and returns the median difference.
	 */
	private double medianScanCostMillis() {

		final double[] samples = new double[SAMPLES];

		try (final KeepAliveClient client = new KeepAliveClient(host, httpPort)) {

			for (int i = 0; i < WARMUP; i++) {

				expectOk(client, "/nosite", "unclaimed.example.com");
				expectOk(client, "/withsite", "claimed0.example.com");
			}

			for (int i = 0; i < SAMPLES; i++) {

				// the two are swapped every other pair, so being measured first is worth the same to each of them
				final boolean scanFirst = (i % 2 == 0);
				final long start   = System.nanoTime();

				expectOk(client, scanFirst ? "/nosite" : "/withsite", scanFirst ? "unclaimed.example.com" : "claimed0.example.com");
				final long between = System.nanoTime();
				expectOk(client, scanFirst ? "/withsite" : "/nosite", scanFirst ? "claimed0.example.com" : "unclaimed.example.com");
				final long end     = System.nanoTime();
				final double first  = (between - start) / 1_000_000.0;
				final double second = (end - between) / 1_000_000.0;

				samples[i] = scanFirst ? (first - second) : (second - first);
			}

		} catch (IOException ioex) {

			ioex.printStackTrace();
			fail("Unexpected exception while measuring: " + ioex.getMessage());
		}

		Arrays.sort(samples);

		return samples[samples.length / 2];
	}

	private void expectOk(final KeepAliveClient client, final String path, final String hostHeader) throws IOException {

		final int status = client.get(path, hostHeader);
		if (status != 200) {

			fail("Expected " + path + " on host " + hostHeader + " to be served, but the status was " + status);
		}
	}

	private void createPagesAndFirstSite() {

		try (final Tx tx = app.tx()) {

			// both pages are built the same way, so rendering cancels out of the difference
			final Page withSite = createPublicPage("withsite");
			createPublicPage("nosite");

			withSite.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.SITES_PROPERTY), List.of(createSite(0)));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	private void addSites(final int total) {

		try (final Tx tx = app.tx()) {

			// site 0 already exists and carries the page, so only the fillers are added here
			for (int i = 1; i < total; i++) {

				createSite(i);
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	private NodeInterface createSite(final int index) throws FrameworkException {

		final Traits traits      = Traits.of(StructrTraits.SITE);
		final NodeInterface site = createTestNode(StructrTraits.SITE,
			new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "site" + index)
		);

		// no site claims the host the site-less page is asked for, so every one of them is compared
		site.setProperty(traits.key(SiteTraitDefinition.HOSTNAME_PROPERTY), "claimed" + index + ".example.com");
		site.setProperty(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);
		site.setProperty(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true);

		return site;
	}

	private Page createPublicPage(final String name) throws FrameworkException {

		final Page page = Page.createSimplePage(securityContext, name);

		makePublicRecursively(page);

		page.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.POSITION_PROPERTY), 10);

		return page;
	}

	private void makePublicRecursively(final DOMNode node) throws FrameworkException {

		node.setProperty(Traits.of(StructrTraits.DOM_NODE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);
		node.setProperty(Traits.of(StructrTraits.DOM_NODE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true);

		for (final DOMNode child : node.getChildren()) {

			makePublicRecursively(child);
		}
	}

	/**
	 * A minimal HTTP/1.1 client on one socket. An HTTP client library adds more overhead per request than the
	 * thing being measured, and the Host header this needs is one that java.net.http refuses to set.
	 */
	private static final class KeepAliveClient implements AutoCloseable {

		private final Socket socket;
		private final OutputStream out;
		private final InputStream in;
		private final byte[] scratch = new byte[8192];

		KeepAliveClient(final String host, final int port) throws IOException {

			socket = new Socket(host, port);
			socket.setTcpNoDelay(true);
			socket.setSoTimeout(30_000);

			out = socket.getOutputStream();
			in  = new BufferedInputStream(socket.getInputStream());
		}

		int get(final String path, final String hostHeader) throws IOException {

			out.write(("GET " + path + " HTTP/1.1\r\nHost: " + hostHeader + "\r\nConnection: keep-alive\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
			out.flush();

			return readResponse();
		}

		private int readResponse() throws IOException {

			final String statusLine = readLine();
			if (statusLine == null) {

				throw new IOException("server closed the connection");
			}

			final String[] parts = statusLine.split(" ");
			if (parts.length < 2) {

				throw new IOException("unreadable status line: " + statusLine);
			}

			final int status  = Integer.parseInt(parts[1]);
			int contentLength = -1;
			boolean chunked   = false;

			while (true) {

				final String header = readLine();
				if (header == null || header.isEmpty()) {

					break;
				}

				final String lower = header.toLowerCase();
				if (lower.startsWith("content-length:")) {

					contentLength = Integer.parseInt(header.substring(header.indexOf(':') + 1).trim());

				} else if (lower.startsWith("transfer-encoding:") && lower.contains("chunked")) {

					chunked = true;
				}
			}

			if (chunked) {

				while (true) {

					final String sizeLine = readLine();
					final int size        = Integer.parseInt(sizeLine.trim().split(";")[0], 16);

					if (size == 0) {

						readLine();
						break;
					}

					readFully(size);
					readLine();
				}

			} else if (contentLength > 0) {

				readFully(contentLength);

			} else if (contentLength < 0) {

				// without a length and without chunks the body ends at the close, which would break keep-alive
				throw new IOException("response carries neither Content-Length nor chunked encoding");
			}

			return status;
		}

		private String readLine() throws IOException {

			final StringBuilder line = new StringBuilder(64);

			while (true) {

				final int c = in.read();
				if (c == -1) {

					return line.length() == 0 ? null : line.toString();
				}

				if (c == '\n') {

					final int length = line.length();
					if (length > 0 && line.charAt(length - 1) == '\r') {

						line.setLength(length - 1);
					}

					return line.toString();
				}

				line.append((char) c);
			}
		}

		private void readFully(final int count) throws IOException {

			int remaining = count;

			while (remaining > 0) {

				final int read = in.read(scratch, 0, Math.min(remaining, scratch.length));
				if (read == -1) {

					throw new IOException("server closed the connection with " + remaining + " bytes outstanding");
				}

				remaining -= read;
			}
		}

		@Override
		public void close() throws IOException {

			socket.close();
		}
	}
}
