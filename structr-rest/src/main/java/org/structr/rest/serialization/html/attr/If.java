package org.structr.rest.serialization.html.attr;

import org.neo4j.helpers.Predicate;
import org.structr.rest.serialization.html.Attr;

/**
 *
 * @author Christian Morgner
 */
public class If extends Conditional {

	public If(final boolean condition, final Attr attr) {
		
		super(new Predicate<Context>() {

			@Override
			public boolean accept(Context item) {
				return condition;
			}
			
		}, attr);
	}
}
