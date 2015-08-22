package org.structr.autocomplete;

import java.util.List;
import static junit.framework.TestCase.assertEquals;
import org.junit.Test;
import org.structr.core.GraphObject;

/**
 *
 * @author Christian Morgner
 */
public class JavascriptHintProviderTest {

	@Test
	public void testJavascriptHintProviderWithoutEntity() {

		final JavascriptHintProvider instance = new JavascriptHintProvider();

		assertEquals("Invalid hint for javascript mode", "current", getHint(0, instance.getHints(null, null, "", "", 0, -1)));
		assertEquals("Invalid hint for javascript mode", "request", getHint(1, instance.getHints(null, null, "", "", 0, -1)));
		assertEquals("Invalid hint for javascript mode", "this",    getHint(2, instance.getHints(null, null, "", "", 0, -1)));
	}


	private String getSingleHint(final List<GraphObject> hints) {
		return getSingleHint(hints, true, 0);
	}

	private String getFirstHint(final List<GraphObject> hints) {
		return getSingleHint(hints, false, 0);
	}

	private String getHint(final int index, final List<GraphObject> hints) {
		return getSingleHint(hints, false, index);
	}

	private String getSingleHint(final List<GraphObject> hints, final boolean checkSize, final int index) {

		if (checkSize) {
			assertEquals("Invalid hints for Javascript mode", index+1, hints.size());
		}

		return hints.get(index).getProperty(JavascriptHintProvider.text);
	}
}
