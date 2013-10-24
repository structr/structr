package org.structr.rest.serialization.html.attr;

import org.structr.rest.serialization.html.Attr;

/**
 *
 * @author Christian Morgner
 */
public class Css extends Attr {

	public Css(final String css) {
		super("class", css);
	}
}
