/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity;

import org.structr.api.Predicate;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.ConstantBooleanTrue;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.CreationContainer;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.StringProperty;
import org.structr.schema.SchemaService;

import java.net.URI;

public interface Favoritable extends NodeInterface {

	static class Impl { static {

		final JsonSchema schema        = SchemaService.getDynamicSchema();
		final JsonObjectType type      = schema.addType("Favoritable");

		type.setIsInterface();
		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Favoritable"));
		type.setCategory("core");

		type.addBooleanProperty("isFavoritable", PropertyView.Public, PropertyView.Ui).setReadOnly(true).addTransformer(ConstantBooleanTrue.class.getName());

		type.addCustomProperty("favoriteContentType", FavoriteContentTypeProperty.class.getName(), "fav").setTypeHint("String");
		type.addCustomProperty("favoriteContent",    FavoriteContentProperty.class.getName(),      "fav").setTypeHint("String");
		type.addCustomProperty("favoriteContext",    FavoriteContextProperty.class.getName(),      "fav").setTypeHint("String");

		// add relationshipId to public and fav view
		type.addFunctionProperty("relationshipId", "fav").setReadFunction("this._path.id").setTypeHint("String").setReadOnly(true);

		type.addViewProperty("fav", "id");
		type.addViewProperty("fav", "name");
		type.addViewProperty("fav", "type");
	}}

	String getContext();
	String getFavoriteContent();
	String getFavoriteContentType();
	void setFavoriteContent(final String content) throws FrameworkException;

	class FavoriteContentProperty extends StringProperty {

		public FavoriteContentProperty(final String name) {
			super(name);
		}

		@Override
		public String getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter, final Predicate<GraphObject> predicate) {

			final Favoritable favoritable = Favoritable.getFavoritable(obj);
			if (favoritable != null) {

				return favoritable.getFavoriteContent();

			}

			throw new IllegalStateException("Cannot use Favoritable.getFavoriteContent() on type " + obj.getClass().getName());
		}

		@Override
		public Object setProperty(final SecurityContext securityContext, final GraphObject obj, final String value) throws FrameworkException {

			final Favoritable favoritable = Favoritable.getFavoritable(obj);
			if (favoritable != null) {

				favoritable.setFavoriteContent(value);

			} else {

				throw new IllegalStateException("Cannot use Favoritable.setFavoriteContent() on type " + obj.getClass().getName());
			}

			return null;
		}
	}

	class FavoriteContentTypeProperty extends StringProperty {

		public FavoriteContentTypeProperty(final String name) {
			super(name);
		}

		@Override
		public String getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter, final Predicate<GraphObject> predicate) {

			final Favoritable favoritable = Favoritable.getFavoritable(obj);
			if (favoritable != null) {

				return favoritable.getFavoriteContentType();

			}

			throw new IllegalStateException("Cannot use Favoritable.getFavoriteContentType() on type " + obj.getClass().getName());
		}

		@Override
		public Object setProperty(final SecurityContext securityContext, final GraphObject obj, final String value) throws FrameworkException {
			throw new FrameworkException(422, "Cannot set content type via Favoritable interface.");
		}
	}

	class FavoriteContextProperty extends StringProperty {

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

	static Favoritable getFavoritable(final GraphObject obj) {

		if (obj == null) {
			return null;
		}

		if (obj instanceof Favoritable) {
			return (Favoritable)obj;
		}

		if (obj instanceof CreationContainer) {

			final GraphObject wrapped = ((CreationContainer)obj).getWrappedObject();
			if (wrapped instanceof Favoritable) {

				return (Favoritable)wrapped;
			}
		}

		return null;
	}
}
