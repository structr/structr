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
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.CreationContainer;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.NodeTrait;

public interface Favoritable extends NodeTrait {

	/*
	Property<Boolean> isFavoritableProperty      = new ConstantBooleanProperty("isFavoritable", true).partOfBuiltInSchema();
	Property<String> favoriteContentTypeProperty = new FavoriteContentTypeProperty("favoriteContentType").typeHint("String").partOfBuiltInSchema();
	Property<String> favoriteContentProperty     = new FavoriteContentProperty("favoriteContent").typeHint("String").partOfBuiltInSchema();
	Property<String> favoriteContextProperty     = new FavoriteContextProperty("favoriteContext").typeHint("String").partOfBuiltInSchema();
	Property<String> relationshipIdProperty      = new FunctionProperty("relationshipId").format("this._path.id").typeHint("String").readOnly().partOfBuiltInSchema();

	View defaultView = new View(Favoritable.class, PropertyView.Public, isFavoritableProperty);
	View uiView      = new View(Favoritable.class, PropertyView.Ui,     isFavoritableProperty);

	View favView     = new View(Favoritable.class, "fav",
		id, type, name, favoriteContentTypeProperty, favoriteContentProperty, favoriteContextProperty, relationshipIdProperty
	);
	*/

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
