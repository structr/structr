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
package org.structr.core.function.search;

import org.structr.common.error.FrameworkException;
import org.structr.core.function.AdvancedScriptingFunction;
import org.structr.core.function.SearchFunction;
import org.structr.docs.Example;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.Collection;
import java.util.List;

public class FindAnyFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "find.any";
	}

	@Override
	public String getDisplayName(boolean includeParameters) {
		return "predicate.any";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			final Object value = sources[0];

			if (value instanceof Collection collection) {
				return new AnyPredicate(collection);
			} else {
				throw new FrameworkException(422, "find.any: first parameter must be a collection");
			}

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${{ $.predicate.any(collection). Example: ${{ $.find('Group', $.predicate.equals('name', $.predicate.any(['Group 1', 'Group 2']))) }}"),
			Usage.structrScript("Usage: ${any(collection). Example: ${find('Group', equals('name', any(merge('Group 1', 'Group 2'))))}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns a query predicate that can be used with find() and search() .";
	}

	@Override
	public String getLongDescription() {
		return """
			The function takes a collection as a parameter. The query returns all nodes that match any of the given values.

			The main use case for predicate.any is remote properties but it can also be used for local properties (not array properties at the moment).

			predicate.any is always used in conjunction with `predicate.equals` or `predicate.contains` and the elements of `listOfOptions` must be of the same type as the property that is being searched.

			Examples for different property types:
			```
			// String property
			$.predicate.equals('name', $.predicate.any( [ 'value 1', 'value 2', 'value 3', ... ] ))
			$.predicate.contains('name', $.predicate.any( [ 'Jack', 'Jane', 'John', ... ] ))

			// Integer Property
			$.predicate.equals('age', $.predicate.any( [ 1, 50, 30, ... ] ))

			// Date Property
			// Since dates are stored as long values internally, we use .getTime()
			$.predicate.equals('createdDate', $.predicate.any( [ date1.getTime(), date2.getTime() ] ))

			// Remote property (cardinality 1)
			$.predicate.equals('bornIn', $.predicate.any( [ country1, country2, country3, ... ] ))

			// Remote property (cardinality n) - exact search
			$.predicate.equals('hasSkills', $.predicate.any( [ [skill1, skill2, skill3], [skill2, skill3, skill4], ... ] ))

			// Remote property (cardinality n) - contains search
			$.predicate.contains('hasSkills', $.predicate.any( [ [skill1, skill2, skill3],  [skill4],  [skill1, skill4], ... ] ))
			```

			**Remote Attributes / Linked nodes**

			For remote attributes with **cardinality 1**, only predicate.equals can be used:

			```
			// find all users born in Germany, France or the UK
			let germany = ...;
			let france  = ...;
			let uk      = ...;
			$.find('User', $.predicate.equals('bornIn', $.predicate.any( [ germany, france, uk ] )));
			```

			For remote attributes with **cardinality n**, both predicate.equals and predicate.contains can be used.

			For situations where an exact match is required, we use `predicate.equals`. In this example `User.jobSkills` is a list, meaning that the argument passed to predicate.any must be a list of lists:

			```
			// find all waiters and cooks (users with either of those exact skillsets)
			let waiterSkills     = [ take_orders, serve_food, serve_drinks ];
			let pizzaMakerSkills = [ knead_dough, put_toppings, bake_pizza ];

			$.find('User', $.predicate.equals('jobSkills', $.predicate.any( [ waiterSkills, pizzaMakerSkills ] )));
			```

			The previous example finds users with the **exact** skillset required for a job. This would find perfect matches but it would leave out anyone who has more than the exact skills. For such situations `predicate.contains` can be used. `User.jobSkills` is still a list, meaning that the argument passed to predicate.any must still be a list of lists.

			```
			// find all users that have the required skills and maybe even more
			let waiterSkills     = [ take_orders, serve_food, serve_drinks ];
			let pizzaMakerSkills = [ knead_dough, put_toppings, bake_pizza ];

			$.find('User', $.predicate.contains('jobSkills', $.predicate.any( [ waiterSkills, pizzaMakerSkills ] )));
			```

			""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.javaScript("""
				{
					let projects = $.find('Project', $.predicate.equals('status', $.predicate.any(['IN_PROGRESS', 'WAITING'])));
				}""", "Fetch projects whose status matches any value in the provided list"),
				Example.javaScript("""
				{
					let myTasks = $.me.assignedTasks;

					// for a "contains" search on a remote collection ("tasks"), we need a list of lists
					// ==> wrap every task in a single array so we can search for each task individually
					let mappedTasks = myTasks.map(task => [task]);

					let project = $.find('Project', $.predicate.contains('tasks', $.predicate.any([ mappedTasks ])));
				}""", "Fetch projects where the current user has tasks")
		);
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("collection");
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Predicate;
	}
}
