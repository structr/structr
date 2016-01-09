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
package org.structr.web.test;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Predicate;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.web.common.DOMTest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 *
 */
public class PerformanceTest extends DOMTest {

	public void testSiblingPerformance() {

		try (final Tx tx = app.tx()) {

			// create document
			final Document document = getDocument();
			assertNotNull(document);

			// create div
			final Element div = document.createElement("div");
			assertNotNull(div);

			try {

				NodeServiceCommand.bulkTransaction(securityContext, 1000, new StructrTransaction() {

					@Override
					public Object execute() throws FrameworkException {

						div.appendChild(document.createTextNode("test"));

						return null;
					}

				}, new Predicate<Long>() {

					@Override
					public boolean evaluate(SecurityContext securityContext, Long... obj) {

						return obj[0].longValue() > 100;
					}

				});

			} catch (Throwable t) {

				fail("Unexpected exception: " + t.getMessage());
			}

			// iterate over all siblings using the nextSibling method
			long t0 = System.currentTimeMillis();

			Node it = div.getFirstChild();
			while (it != null) {
				it = it.getNextSibling();
			}

			long t1 = System.currentTimeMillis();
			long duration = t1 - t0;

			assertTrue("Iteration of 100 nodes via getNextSibling should not take longer than 50ms, took " + duration + "!", duration < 50);

			tx.success();
			
		} catch (FrameworkException fex) {

			fex.printStackTrace();

			fail("Unexpected exception");

		}

	}

	/*
	 public void testForcedVsUnforcedTransactions() {

	 try {
	 int num = 1000;

	 long t0 = System.currentTimeMillis();
			
	 for (int i=0; i<num; i++) {
	 createTestNodes(Folder.class, 1, false);
	 }
			
	 long t1 = System.currentTimeMillis();
			
	 for (int i=0; i<num; i++) {
	 createTestNodes(Folder.class, 1, true);
	 }
			
	 long t2 = System.currentTimeMillis();

	 System.out.println("Forced:   " + (t1-t0) + " ms");
	 System.out.println("Unforced: " + (t2-t1) + " ms");
			
	 } catch (Throwable t) {
			
	 fail("Unexpected exception: " + t.getMessage());
	 }
	 }
	 */
}
