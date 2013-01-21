package org.structr.web.test;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Predicate;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.StructrTransaction;
import org.structr.web.common.DOMTest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**

 * @author Christian Morgner
 */
public class PerformanceTest extends DOMTest {
		
	public void testSiblingPerformance() {

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
		System.out.println(duration);


		// 13011
		// 15007


	}
}
