package org.structr.core.property;

import org.structr.api.Predicate;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.Favoritable;

public class FavoriteContextProperty extends StringProperty {

	public FavoriteContextProperty(final String name) {
		super(name);
	}

	@Override
	public String getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter, final Predicate<GraphObject> predicate) {

		final Favoritable favoritable = Favoritable.getFavoritable(obj);
		if (favoritable != null) {

			return favoritable.getContext();

		}

		throw new IllegalStateException("Cannot use Favoritable.getContext() on type " + obj.getClass().getName());
	}

	@Override
	public Object setProperty(final SecurityContext securityContext, final GraphObject obj, final String value) throws FrameworkException {
		throw new FrameworkException(422, "Cannot set context via Favoritable interface.");
	}
}
