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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * JPMS SPI preflight (build gate). Companion to ModulePathPartitioner.
 *
 * WHY: some third-party jars register java.util.ServiceLoader providers via META-INF/services whose
 * constructors fail when the jar is an AUTOMATIC MODULE on the module path, yet succeed on the class
 * path. The archetype is the legacy Sun "JAI Image I/O Tools" (javax.media:jai_imageio): its
 * com.sun.media.imageioimpl.* ImageIO SPIs read their vendor name from the jar manifest, which resolves
 * to null for an automatic module -> every SPI constructor throws IllegalArgumentException("vendorName ==
 * null!"). For the eagerly-scanned ImageIO categories a SINGLE failing provider aborts the whole plugin
 * scan, so an unrelated feature (e.g. the barcode() function's ImageIO.write(...,"PNG",...)) breaks. This
 * is invisible at build time and only surfaces at runtime -> this preflight replays ServiceLoader for the
 * eagerly-scanned, side-effect-free SPI categories against the partitioned distribution and fails the
 * build if any provider cannot be instantiated, pointing at the fix (usually: quarantine the jar in
 * build/island-seed.txt so it lands on the class path).
 *
 * MUST be launched WITH the partitioned module path so it observes real runtime resolution:
 *   java --module-path <libDir> --add-modules ALL-MODULE-PATH -cp <island jars> SpiPreflight.java <libDir> [<islandDir>]
 * (args are accepted for symmetry/logging; the actual scan uses the module path the JVM was launched with.)
 *
 * Only categories where ONE bad provider poisons the whole scan are checked, and only categories whose
 * providers are cheap and side-effect-free to instantiate (SPI metadata objects, JDBC drivers, charset
 * providers). We deliberately do NOT instantiate arbitrary providers (Kafka/Pulsar/etc. have heavy or
 * blocking constructors).
 */
public class SpiPreflight {

	// Eagerly-scanned SPI categories: one failing provider breaks the entire scan for all consumers.
	private static final String[] CATEGORIES = {
		"javax.imageio.spi.ImageReaderSpi",
		"javax.imageio.spi.ImageWriterSpi",
		"javax.imageio.spi.ImageInputStreamSpi",
		"javax.imageio.spi.ImageOutputStreamSpi",
		"javax.imageio.spi.ImageTranscoderSpi",
		"java.sql.Driver",
		"java.nio.charset.spi.CharsetProvider",
	};

	public static void main(final String[] args) {

		final List<String> failures = new ArrayList<>();
		int tested = 0;

		for (final String category : CATEGORIES) {

			final Class<?> categoryClass;
			try {
				categoryClass = Class.forName(category);
			} catch (final Throwable notOnThisPath) {
				// category type not present in this distribution - nothing to check
				continue;
			}

			final ServiceLoader<?> loader;
			try {
				loader = ServiceLoader.load(categoryClass);
			} catch (final Throwable notAccessible) {
				continue;
			}

			tested++;

			// Enumerate provider handles (loads provider classes but does NOT instantiate) ...
			final List<ServiceLoader.Provider<?>> providers = new ArrayList<>();
			final Iterator<? extends ServiceLoader.Provider<?>> it = loader.stream().iterator();
			while (true) {
				try {
					if (!it.hasNext()) {
						break;
					}
					providers.add(it.next());
				} catch (final Throwable lookupError) {
					failures.add("  [" + category + "] provider LOOKUP failed: " + rootCause(lookupError));
					break;
				}
			}

			// ... then instantiate each (this is where the jai_imageio "vendorName == null!" failure fires).
			for (final ServiceLoader.Provider<?> provider : providers) {
				try {
					provider.get();
				} catch (final Throwable instantiationError) {
					failures.add("  [" + category + "] " + provider.type().getName()
						+ " could not be instantiated: " + rootCause(instantiationError));
				}
			}
		}

		if (failures.isEmpty()) {
			System.out.println("[spi-preflight] OK - " + tested + " eagerly-scanned SPI categories, all providers instantiate cleanly.");
			// Force termination: instantiating some providers (or logback init) starts non-daemon threads
			// that would otherwise keep this short-lived preflight JVM alive after main() returns.
			System.exit(0);
		}

		System.err.println("[spi-preflight] FATAL: " + failures.size()
			+ " ServiceLoader provider(s) fail to instantiate on the JPMS module path:");
		for (final String failure : failures) {
			System.err.println(failure);
		}
		System.err.println();
		System.err.println("  These providers work on the class path but not as automatic modules on the module path,");
		System.err.println("  so ImageIO/JDBC/charset plugin scans abort at runtime and break unrelated features.");
		System.err.println("  FIX: quarantine the offending jar to the class-path island by adding its glob to");
		System.err.println("       structr-app/src/main/resources/build/island-seed.txt (see the jai_imageio-*.jar entry).");
		System.exit(1);
	}

	private static String rootCause(final Throwable t) {
		Throwable r = t;
		while (r.getCause() != null && r.getCause() != r) {
			r = r.getCause();
		}
		return r.getClass().getName() + ": " + r.getMessage();
	}
}
