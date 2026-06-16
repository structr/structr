/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.web.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.MatchToken;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.JavaMethod;
import org.structr.core.entity.Relation;
import org.structr.core.function.ServerLogFunction;
import org.structr.core.property.*;
import org.structr.core.traits.*;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.Scratchpad;
import org.structr.web.traits.wrappers.ScratchpadTraitWrapper;

import java.util.Date;
import java.util.Map;
import java.util.Set;

public class ScratchpadTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String SOURCE_PROPERTY             = "source";
	public static final String RESULT_PROPERTY             = "result";
	public static final String LOG_PROPERTY                = "log";
	public static final String LAST_RUN_TIMESTAMP_PROPERTY = "lastRunTimestamp";
	public static final String COLLAPSED_PROPERTY          = "collapsed";

	public ScratchpadTraitDefinition() {
		super(StructrTraits.SCRATCHPAD);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return Set.of(

			new JavaMethod("prepareNextRun", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

					entity.as(Scratchpad.class).setLastRunTimestamp(new Date().getTime());

					return null;
				}

				@Override
				public String getDescription() {
					return "Prepares next run and returns log string that will be used during it.";
				}
			},

			new JavaMethod("run", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

					return entity.as(Scratchpad.class).run(actionContext);
				}

				@Override
				public String getDescription() {
					return "Runs the scratchpad and returns the result.";
				}
			},

			new JavaMethod("getServerLog", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) {

					return entity.as(Scratchpad.class).getServerLog();
				}

				@Override
				public String getDescription() {
					return "Runs the scratchpad and returns the result.";
				}
			}
		);
	}

	@Override
	public Map<Class, RelationshipTraitFactory> getRelationshipTraitFactories() {
		return Map.of();
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			Scratchpad.class, (traits, node) -> new ScratchpadTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<String> sourceProperty         = new StringProperty(SOURCE_PROPERTY).defaultValue("");
		final Property<String> resultProperty         = new StringProperty(RESULT_PROPERTY).defaultValue("");
		final Property<String> logProperty            = new StringProperty(LOG_PROPERTY).defaultValue("");
		final Property<Long> lastRunTimestampProperty = new LongProperty(LAST_RUN_TIMESTAMP_PROPERTY);
		final Property<Boolean> collapsedProperty     = new BooleanProperty(COLLAPSED_PROPERTY).defaultValue(false);

		return Set.of(
				sourceProperty,
				resultProperty,
				logProperty,
				lastRunTimestampProperty,
				collapsedProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Ui,
			newSet(
					SOURCE_PROPERTY, RESULT_PROPERTY, LOG_PROPERTY, LAST_RUN_TIMESTAMP_PROPERTY, COLLAPSED_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	@Override
	public boolean includeInDocumentation() {
		return true;
	}
}
