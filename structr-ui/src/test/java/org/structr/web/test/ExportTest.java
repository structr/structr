package org.structr.web.test;

import org.structr.web.common.DOMTest;

/**
 *
 * @author Christian Morgner
 */
public class ExportTest extends DOMTest {

	/*
	public void testPDFExport() {

		String templateFileName = "src/main/resources/xslfo/test.xslfo";
		String outputFileName = "test.pdf";
		
		OutputStream out = null;

		try {
		

			Page page = Page.createNewPage(securityContext, "page 01");
			DOMElement html   = (DOMElement)page.createElement("html");
			DOMElement head   = (DOMElement)page.createElement("head");
			DOMElement body   = (DOMElement)page.createElement("body");
			DOMElement title  = (DOMElement)page.createElement("title");
			DOMElement h1     = (DOMElement)page.createElement("h1");
			
			try {
				// add HTML element to page
				page.appendChild(html);

				// add HEAD and BODY elements to HTML
				html.appendChild(head);
				html.appendChild(body);

				// add TITLE element to HEAD
				head.appendChild(title);
				title.appendChild(page.createTextNode("Test Page"));

				// add H1 element to BODY
				body.appendChild(h1);
				h1.appendChild(page.createTextNode("Page Title"));
				
			} catch (DOMException dex) {

				throw new FrameworkException(422, dex.getMessage());
			}

			
			
			// render PDF using FOP
			out = new FileOutputStream(new File(outputFileName));
			
			FopFactory fopFactory = FopFactory.newInstance();
			TransformerFactory tFactory = TransformerFactory.newInstance();

			Templates templates = tFactory.newTemplates(new StreamSource(new File(templateFileName)));

			//First run (to /dev/null)
			FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
			Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, out);

			Transformer transformer = templates.newTransformer();
			
			transformer.setParameter("page-count", "#");
			transformer.transform(new DOMSource(page), new SAXResult(fop.getDefaultHandler()));

			
		} catch (Throwable t) {
			
			t.printStackTrace();
			
		} finally {

			try { out.close(); } catch (Throwable t) {}
		}
	}
	*/
}
