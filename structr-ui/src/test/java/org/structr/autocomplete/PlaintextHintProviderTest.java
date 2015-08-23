package org.structr.autocomplete;

import java.util.List;
import static junit.framework.TestCase.assertEquals;
import org.junit.Test;
import org.structr.core.GraphObject;


/**
 *
 * @author Christian Morgner
 */
public class PlaintextHintProviderTest {

	@Test
	public void testPlaintextHintProviderWithoutEntity() {

		final PlaintextHintProvider instance = new PlaintextHintProvider();

		assertEquals("Invalid hint for plaintext mode", "current", getHint(0, instance.getHints(null, null, "", "", "", 0, -1)));
		assertEquals("Invalid hint for plaintext mode", "request", getHint(1, instance.getHints(null, null, "", "", "", 0, -1)));
		assertEquals("Invalid hint for plaintext mode", "this",    getHint(2, instance.getHints(null, null, "", "", "", 0, -1)));

		assertEquals("Invalid hints for plaintext mode", 104, instance.getHints(null, null, "", "", "", 0, -1).size());
		assertEquals("Invalid hints for plaintext mode", 104, instance.getHints(null, null, ".", "", "", 0, -1).size());
		assertEquals("Invalid hints for plaintext mode", 104, instance.getHints(null, null, "(", "", "", 0, -1).size());
		assertEquals("Invalid hints for plaintext mode", 104, instance.getHints(null, null, "((", "", "", 0, -1).size());
		assertEquals("Invalid hints for plaintext mode", 104, instance.getHints(null, null, "((((((", "", "", 0, -1).size());
		assertEquals("Invalid hints for plaintext mode", 104, instance.getHints(null, null, "${", "", "", 0, -1).size());

		assertEquals("Invalid hints for plaintext mode", 11, instance.getHints(null, null, "${c", "", "", 0, -1).size());
		assertEquals("Invalid hints for plaintext mode", 1,  instance.getHints(null, null, "${cl", "", "", 0, -1).size());

		assertEquals("Invalid hint for plaintext mode", "his", getFirstHint(instance.getHints(null, null, "t", "", "", 0, -1)));
		assertEquals("Invalid hint for plaintext mode", "urrent", getFirstHint(instance.getHints(null, null, "c", "", "", 0, -1)));

		assertEquals("Invalid hint for plaintext mode", 104, instance.getHints(null, null, "${this.", "", "", 0, -1).size());

		assertEquals("Invalid hint for plaintext mode", 104, instance.getHints(null, null, "clean()", "", "", 0, 6).size());

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
			assertEquals("Invalid hints for plaintext mode", index+1, hints.size());
		}

		return hints.get(index).getProperty(AbstractHintProvider.text);
	}
}
