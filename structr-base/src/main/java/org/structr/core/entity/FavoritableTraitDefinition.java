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

import org.structr.core.property.*;
import org.structr.core.traits.AbstractTraitDefinition;
import org.structr.core.traits.TraitFactory;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.wrappers.FavoritableTraitWrapper;

import java.util.Map;
import java.util.Set;

public class FavoritableTraitDefinition extends AbstractTraitDefinition {

	Property<Boolean> isFavoritableProperty      = new ConstantBooleanProperty("isFavoritable", true).partOfBuiltInSchema();
	Property<String> favoriteContentTypeProperty = new FavoriteContentTypeProperty("favoriteContentType").typeHint("String").partOfBuiltInSchema();
	Property<String> favoriteContentProperty     = new FavoriteContentProperty("favoriteContent").typeHint("String").partOfBuiltInSchema();
	Property<String> favoriteContextProperty     = new FavoriteContextProperty("favoriteContext").typeHint("String").partOfBuiltInSchema();
	Property<String> relationshipIdProperty      = new FunctionProperty("relationshipId").format("this._path.id").typeHint("String").readOnly().partOfBuiltInSchema();

	public FavoritableTraitDefinition() {
		super("Favoritable");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {
		return Map.of();
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {
		return Map.of();
	}

	@Override
	public Map<Class, TraitFactory> getTraitFactories() {

		return Map.of(
			Favoritable.class, (traits, node) -> new FavoritableTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		return Set.of(
			favoriteContentTypeProperty,
			favoriteContentProperty,
			favoriteContextProperty,
			isFavoritableProperty,
			relationshipIdProperty
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	/*
	View defaultView = new View(FavoritableTraitDefinition.class, PropertyView.Public, isFavoritableProperty);
	View uiView      = new View(FavoritableTraitDefinition.class, PropertyView.Ui,     isFavoritableProperty);

	View favView     = new View(FavoritableTraitDefinition.class, "fav",
		id, type, name, favoriteContentTypeProperty, favoriteContentProperty, favoriteContextProperty, relationshipIdProperty
	);
	*/

}
