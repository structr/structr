package org.structr.core.property;

import org.apache.lucene.search.SortField;
import org.neo4j.helpers.Predicate;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.parser.Functions;
import org.structr.schema.action.ActionContext;

/**
 *
 * @author Christian Morgner
 */
public class FunctionProperty<T> extends AbstractReadOnlyProperty<T> {

	private String expression = null;

	public FunctionProperty(final String name, final String expression) {

		super(name);
		this.expression = expression;
	}

	@Override
	public Class relatedType() {
		return null;
	}

	@Override
	public T getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public T getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter, Predicate<GraphObject> predicate) {

		if (obj instanceof AbstractNode) {

			try {

				return (T)Functions.evaluate(securityContext, new ActionContext(), obj, expression);

			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		return null;
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public Integer getSortType() {
		return SortField.INT;
	}
}
