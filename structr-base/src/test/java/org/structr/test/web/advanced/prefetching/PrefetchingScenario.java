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
package org.structr.test.web.advanced.prefetching;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/**
 * A kind of request of the sample application, as a client sends it. The n-th request of a scenario is always the
 * same, so the n-th request with and without prefetching can be compared.
 *
 * @param name        short name for the report
 * @param description what the request does and why it is interesting for prefetching
 * @param user        the user who sends it, ADMIN or MEMBER
 * @param readOnly    whether the request leaves the data unchanged, so that its responses can be compared
 * @param request     the n-th request
 */
record PrefetchingScenario(String name, String description, String user, boolean readOnly, IntFunction<Request> request) {

	static final String ADMIN  = "admin";
	static final String MEMBER = "member";

	private static final int TASK_PAGE_SIZE = 150;

	record Request(String method, String path) {
	}

	/**
	 * All scenarios of the sample application.
	 */
	static List<PrefetchingScenario> all(final PrefetchingSampleData.Data data) {

		final List<PrefetchingScenario> scenarios = new ArrayList<>();

		scenarios.add(new PrefetchingScenario("rest-list-nested",
			"REST: all projects in a view nested four levels deep (tasks, assignee, team, comments, tags). Uses every task of the database, the case prefetching is made for.",
			ADMIN, true, n -> get("/structr/rest/Project/report?_sort=name")
		));

		scenarios.add(new PrefetchingScenario("rest-list-nested-member",
			"The same request by a non-admin user who reads the data through a schema grant to a group, so every node also costs permission lookups.",
			MEMBER, true, n -> get("/structr/rest/Project/report?_sort=name")
		));

		scenarios.add(new PrefetchingScenario("rest-detail-small",
			"REST: one small project in the nested view, a different one each time. The hint is the same for all of them (the UUID is normalized), and a type-wide pattern loads all tasks for the few of one project.",
			ADMIN, true, n -> get("/structr/rest/Project/" + data.smallProjects().get(n % data.smallProjects().size()) + "/report")
		));

		scenarios.add(new PrefetchingScenario("rest-detail-mixed",
			"REST: every project in turn, small and large ones, under the same hint. What is learned depends on which project comes first, and what pays off differs from project to project.",
			ADMIN, true, n -> get("/structr/rest/Project/" + data.projects().get(n % data.projects().size()) + "/report")
		));

		scenarios.add(new PrefetchingScenario("rest-task-page",
			"REST: one page of " + TASK_PAGE_SIZE + " tasks, sorted by name, each page in turn. Enough lookups to learn a pattern, but on a large data set a small share of what the pattern loads.",
			ADMIN, true, n -> get("/structr/rest/Task/report?_sort=name&_pageSize=" + TASK_PAGE_SIZE + "&_page=" + (1 + n % taskPages(data)))
		));

		scenarios.add(new PrefetchingScenario("rest-task-page-sometimes-empty",
			"Like rest-task-page, but every third request asks for the page after the last one, as a client does when it pages to the end. The hint is the same for both: an empty page fails the evaluation of every pattern, but only database.prefetching.maxfailures failures in a row deactivate one.",
			ADMIN, true, n -> get("/structr/rest/Task/report?_sort=name&_pageSize=" + TASK_PAGE_SIZE + "&_page=" + (n % 3 == 2 ? taskPages(data) + 1 : 1 + n % taskPages(data)))
		));

		scenarios.add(new PrefetchingScenario("page-nested-repeaters",
			"HTML: the page " + PrefetchingSampleData.PAGE_NAME + " with three nested repeaters over customers, projects and tasks.",
			ADMIN, true, n -> get("/" + PrefetchingSampleData.PAGE_NAME)
		));

		scenarios.add(new PrefetchingScenario("page-nested-repeaters-member", "The same page for the non-admin user.", MEMBER, true, n -> get("/" + PrefetchingSampleData.PAGE_NAME)));

		scenarios.add(new PrefetchingScenario("method-project-summary",
			"Schema method: a report over the tasks of a project, each project in turn.",
			ADMIN, true, n -> post("/structr/rest/Project/" + data.projects().get(n % data.projects().size()) + "/taskSummary")
		));

		scenarios.add(new PrefetchingScenario("method-customer-overview",
			"Schema method: the tasks of all projects of a customer counted by team, each customer in turn.",
			ADMIN, true, n -> post("/structr/rest/Customer/" + data.customers().get(n % data.customers().size()) + "/overview")
		));

		scenarios.add(new PrefetchingScenario("method-project-touch",
			"Schema method that writes: updates every task of a project and creates a comment, which ends the prefetch for the rest of the transaction.",
			ADMIN, false, n -> post("/structr/rest/Project/" + data.largeProjects().get(n % data.largeProjects().size()) + "/touch")
		));

		return scenarios;
	}

	private static int taskPages(final PrefetchingSampleData.Data data) {

		return Math.max(1, data.tasks() / TASK_PAGE_SIZE);
	}

	private static Request get(final String path) {

		return new Request("GET", path);
	}

	private static Request post(final String path) {

		return new Request("POST", path);
	}
}
