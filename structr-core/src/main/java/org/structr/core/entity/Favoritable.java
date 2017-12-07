/**
 * Copyright (C) 2010-2017 Structr GmbH
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

import java.net.URI;
import org.structr.api.Predicate;
import org.structr.common.ConstantBooleanTrue;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.CreationContainer;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.ConstantBooleanProperty;
import org.structr.core.property.FunctionProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;

public interface Favoritable extends NodeInterface {

	static class Impl { static {

		final JsonSchema schema        = SchemaService.getDynamicSchema();
		final JsonObjectType principal = (JsonObjectType)schema.addType("Principal");
		final JsonObjectType type      = schema.addType("Favoritable");

		type.setIsInterface();
		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Favoritable"));

		type.addBooleanProperty("isFavoritable").addTransformer(ConstantBooleanTrue.class.getName());

		if (principal != null) {
			principal.relate(type, "FAVORITE", Relation.Cardinality.ManyToMany, "favoriteUsers", "favorites");
		}
	}}


	public static final Property<String>  favoriteContentType = new FavoriteContentTypeProperty("favoriteContentType");
	public static final Property<String>  favoriteContent     = new FavoriteContentProperty("favoriteContent");
	public static final Property<String>  favoriteContext     = new FavoriteContextProperty("favoriteContext");
	public static final Property<String>  relIdProperty       = new FunctionProperty("relationshipId").readFunction("this._path.id");
	public static final Property<Boolean> isFavoritable       = new ConstantBooleanProperty("isFavoritable", true);

	public static final View favView = new View(Favoritable.class, "fav",
		id, name, type, favoriteContext, favoriteContent, favoriteContentType, relIdProperty
	);

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
