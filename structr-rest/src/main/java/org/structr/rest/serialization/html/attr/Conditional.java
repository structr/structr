package org.structr.rest.serialization.html.attr;

import org.neo4j.helpers.Predicate;
import org.structr.rest.serialization.html.Attr;

/**
 *
 * @author Christian Morgner
 */
public class Conditional extends Attr {

	private Predicate<Context> predicate = null;

	public Conditional(final Predicate<Context> predicate, Attr attr) {

		super(attr.getKey(), attr.getValue());

		this.predicate = predicate;
	}

	@Override
	public String format(final Context context) {

		if (predicate.accept(context)) {
			return super.format(context);
		}

		return "";
	}
}
