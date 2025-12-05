/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.test.rest.document;

import io.restassured.RestAssured;
import org.apache.commons.lang3.StringUtils;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonReferenceType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonSchema.Cascade;
import org.structr.api.schema.JsonType;
import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.schema.export.StructrSchema;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.testng.AssertJUnit.fail;

/**
 *
 *
 */
public class DocumentTest extends StructrRestTestBase {

	@Test
	public void testSchemaAutocreateNone() {

		final String projectNodeId     = createSchemaNode("Project", new PropertySpec("name", "String", true, true));
		final String taskNodeId        = createSchemaNode("Task",    new PropertySpec("name", "String", false, true));

		createSchemaRelationships(projectNodeId, taskNodeId, "TASK", "1", "*", "project", "tasks", Relation.NONE, Relation.SOURCE_TO_TARGET);

		// post document: expect success (Project -> Task)
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.body("{\"name\":\"Project1\",\"tasks\":[{\"name\":\"Task1\"}]}")
			.expect()
			.statusCode(422)
			.when()
			.post("/Project");

		// post document: expect failure (Task -> Project)
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.body("{\"name\":\"Task2\",\"project\":{\"name\":\"Project2\"}}")
			.expect()
			.statusCode(422)
			.when()
			.post("/Task");
	}

	@Test
	public void testSchemaAutocreateSourceToTarget() {

		final String projectNodeId     = createSchemaNode("Project", new PropertySpec("name", "String", true, true));
		final String taskNodeId        = createSchemaNode("Task",    new PropertySpec("name", "String", false, true));

		createSchemaRelationships(projectNodeId, taskNodeId, "TASK", "1", "*", "project", "tasks", Relation.SOURCE_TO_TARGET, Relation.SOURCE_TO_TARGET);

		// post document: expect success (Project -> Task)
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.body("{\"name\":\"Project1\",\"tasks\":[{\"name\":\"Task1\"}]}")
			.expect()
			.statusCode(201)
			.when()
			.post("/Project");

		// post document: expect failure (Task -> Project)
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.body("{\"name\":\"Task2\",\"project\":{\"name\":\"Project2\"}}")
			.expect()
			.statusCode(422)
			.when()
			.post("/Task");
	}

	@Test
	public void testSchemaAutocreateTargetToSource() {

		final String projectNodeId     = createSchemaNode("Project", new PropertySpec("name", "String", true, true));
		final String taskNodeId        = createSchemaNode("Task",    new PropertySpec("name", "String", false, true));

		createSchemaRelationships(projectNodeId, taskNodeId, "TASK", "1", "*", "project", "tasks", Relation.TARGET_TO_SOURCE, Relation.SOURCE_TO_TARGET);

		// post document: expect success (Project -> Task)
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.body("{\"name\":\"Project1\",\"tasks\":[{\"name\":\"Task1\"}]}")
			.expect()
			.statusCode(422)
			.when()
			.post("/Project");

		// post document: expect failure (Task -> Project)
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.body("{\"name\":\"Task2\",\"project\":{\"name\":\"Project2\"}}")
			.expect()
			.statusCode(201)
			.when()
			.post("/Task");
	}

	@Test
	public void testSchemaAutocreateAlways() {

		final String projectNodeId     = createSchemaNode("Project", new PropertySpec("name", "String", true, true));
		final String taskNodeId        = createSchemaNode("Task",    new PropertySpec("name", "String", false, true));

		createSchemaRelationships(projectNodeId, taskNodeId, "TASK", "1", "*", "project", "tasks", Relation.ALWAYS, Relation.SOURCE_TO_TARGET);

		// post document: expect success (Project -> Task)
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.body("{\"name\":\"Project1\",\"tasks\":[{\"name\":\"Task1\"}]}")
			.expect()
			.statusCode(201)
			.when()
			.post("/Project");

		// post document: expect failure (Task -> Project)
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.body("{\"name\":\"Task2\",\"project\":{\"name\":\"Project2\"}}")
			.expect()
			.statusCode(201)
			.when()
			.post("/Task");
	}

	@Test
	public void testSimpleDocumentWithSchema() {

		final String projectNodeId     = createSchemaNode("Project", new PropertySpec("name", "String", true, true), new ViewSpec("test", "name, tasks"));
		final String taskNodeId        = createSchemaNode("Task",    new PropertySpec("name", "String", false, true), new ViewSpec("test","name, project, subtasks, parentTask, worker"));
		final String workerNodeId      = createSchemaNode("Worker",  new PropertySpec("name", "String", true, true), new ViewSpec("test", "name, tasks"));

		// create relationships
		createSchemaRelationships(projectNodeId, taskNodeId, "TASK",    "1", "*", "project",    "tasks",    Relation.SOURCE_TO_TARGET, Relation.SOURCE_TO_TARGET);
		createSchemaRelationships(taskNodeId, taskNodeId,    "SUBTASK", "1", "*", "parentTask", "subtasks", Relation.SOURCE_TO_TARGET, Relation.SOURCE_TO_TARGET);
		createSchemaRelationships(workerNodeId, taskNodeId,  "WORKS",   "1", "*", "worker",     "tasks",    Relation.ALWAYS,           Relation.SOURCE_TO_TARGET);

		// post document
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.body("{\"name\":\"Project1\",\"tasks\":[{\"name\":\"Task1\",\"worker\":{\"name\":\"Worker1\"},\"subtasks\":[{\"name\":\"Subtask1.1\",\"worker\":{\"name\":\"Worker1\"}},{\"name\":\"Subtask1.2\",\"worker\":{\"name\":\"Worker2\"}}]}]}")
			.expect()
			.statusCode(201)
			.when()
			.post("/Project");

		// check result
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.body("result",			                    hasSize(1))
			.body("result_count",		                    equalTo(1))
			.body("result[0].name",                             equalTo("Project1"))
			.body("result[0].tasks[0].name",                    equalTo("Task1"))
			.body("result[0].tasks[0].worker.name",             equalTo("Worker1"))
			.body("result[0].tasks[0].subtasks[0].name",        equalTo("Subtask1.1"))
			.body("result[0].tasks[0].subtasks[0].worker.name", equalTo("Worker1"))
			.body("result[0].tasks[0].subtasks[1].name",        equalTo("Subtask1.2"))
			.body("result[0].tasks[0].subtasks[1].worker.name", equalTo("Worker2"))
			.when()
			.get("/Project/test?name=Project1&_sort=name");

		// post document
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.body("{\"name\":\"Project2\",\"tasks\":[{\"name\":\"Task2\",\"worker\":{\"name\":\"Worker2\"},\"subtasks\":[{\"name\":\"Subtask2.1\",\"worker\":{\"name\":\"Worker2\"}},{\"name\":\"Subtask2.2\",\"worker\":{\"name\":\"Worker3\"}}]}]}")
			.expect()
			.statusCode(201)
			.when()
			.post("/Project");

		// check result
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.body("result",			                    hasSize(1))
			.body("result_count",		                    equalTo(1))
			.body("result[0].name",                             equalTo("Project2"))
			.body("result[0].tasks[0].name",                    equalTo("Task2"))
			.body("result[0].tasks[0].worker.name",             equalTo("Worker2"))
			.body("result[0].tasks[0].subtasks[0].name",        equalTo("Subtask2.1"))
			.body("result[0].tasks[0].subtasks[0].worker.name", equalTo("Worker2"))
			.body("result[0].tasks[0].subtasks[1].name",        equalTo("Subtask2.2"))
			.body("result[0].tasks[0].subtasks[1].worker.name", equalTo("Worker3"))
			.when()
			.get("/Project/test?name=Project2&_sort=name");

		// check workers
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.body("result",			                    hasSize(1))
			.body("result_count",		                    equalTo(1))
			.body("result[0].name",                             equalTo("Worker2"))
			.body("result[0].tasks",                            hasSize(3))
			.body("result[0].tasks[0].name",                    equalTo("Subtask1.2"))
			.body("result[0].tasks[1].name",                    equalTo("Subtask2.1"))
			.body("result[0].tasks[2].name",                    equalTo("Task2"))
			.when()
			.get("/Worker/test?name=Worker2&_sort=name");
	}

	@Test
	public void testComplexDocumentWithSchema() {

		final String projectNodeId     = createSchemaNode("Project", new PropertySpec("name", "String", true, true), new ViewSpec("test", "name, tasks"));
		final String taskNodeId        = createSchemaNode("Task",    new PropertySpec("name", "String", true, true), new ViewSpec("test", "name, subtasks, worker"));
		final String workerNodeId      = createSchemaNode("Worker",  new PropertySpec("name", "String", true, true), new ViewSpec("test", "name, tasks, company"));
		final String companyNodeId     = createSchemaNode("Company", new PropertySpec("name", "String", true, true), new ViewSpec("test", "name, workers"));

		// create relationships
		createSchemaRelationships(projectNodeId, taskNodeId,   "TASK",     "1", "*", "project",    "tasks",    Relation.ALWAYS, Relation.SOURCE_TO_TARGET);
		createSchemaRelationships(taskNodeId, taskNodeId,      "SUBTASK",  "1", "*", "parentTask", "subtasks", Relation.ALWAYS, Relation.SOURCE_TO_TARGET);
		createSchemaRelationships(workerNodeId, taskNodeId,    "WORKS_ON", "1", "*", "worker",     "tasks",    Relation.ALWAYS, Relation.SOURCE_TO_TARGET);
		createSchemaRelationships(workerNodeId, companyNodeId, "WORKS_AT", "*", "1", "workers",    "company",  Relation.ALWAYS, Relation.SOURCE_TO_TARGET);

		String jsonBody =
                "{"
                + "\n" + "   \"name\": \"Project1\","
                + "\n" + "   \"tasks\": ["
                + "\n" + "       {"
                + "\n" + "           \"name\": \"Task1\","
                + "\n" + "           \"worker\": {"
                + "\n" + "               \"name\": \"Worker1\","
                + "\n" + "               \"company\": { "
                + "\n" + "                   \"name\": \"Company1\""
                + "\n" + "               }"
                + "\n" + "           },"
                + "\n" + "           \"subtasks\": ["
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask1.1\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker1\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company1\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               },"
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask1.2\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker2\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company1\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               },"
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask1.3\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker2\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company1\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               },"
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask1.4\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker3\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company2\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               }"
                + "\n" + "           ]"
                + "\n" + "       },"
                + "\n" + "       {"
                + "\n" + "           \"name\": \"Task2\","
                + "\n" + "           \"worker\": {"
                + "\n" + "               \"name\": \"Worker2\","
                + "\n" + "               \"company\": { "
                + "\n" + "                   \"name\": \"Company1\""
                + "\n" + "               }"
                + "\n" + "           }"
                + "\n" + "       },"
                + "\n" + "       {"
                + "\n" + "           \"name\": \"Task3\","
                + "\n" + "           \"worker\": {"
                + "\n" + "               \"name\": \"Worker3\","
                + "\n" + "               \"company\": { "
                + "\n" + "                   \"name\": \"Company2\""
                + "\n" + "               }"
                + "\n" + "           }"
                + "\n" + "       },"
                + "\n" + "       {"
                + "\n" + "           \"name\": \"Task4\","
                + "\n" + "           \"worker\": {"
                + "\n" + "               \"name\": \"Worker4\","
                + "\n" + "               \"company\": { "
                + "\n" + "                   \"name\": \"Company3\""
                + "\n" + "               }"
                + "\n" + "           },"
                + "\n" + "           \"subtasks\": ["
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask4.1\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker4\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company3\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               },"
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask4.2\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker4\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company3\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               },"
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask4.3\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker4\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company3\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               },"
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask4.4\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker5\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company3\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               }"
                + "\n" + "           ]"
                + "\n" + "       },"
                + "\n" + "       {"
                + "\n" + "           \"name\": \"Task5\","
                + "\n" + "           \"worker\": {"
                + "\n" + "               \"name\": \"Worker5\","
                + "\n" + "               \"company\": { "
                + "\n" + "                   \"name\": \"Company3\""
                + "\n" + "               }"
                + "\n" + "           },"
                + "\n" + "           \"subtasks\": ["
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask5.1\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker4\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company3\""
                + "\n" + "                       }"
                + "\n" + "                   },"
                + "\n" + "                   \"subtasks\": ["
                + "\n" + "                       {"
                + "\n" + "                           \"name\": \"Subtask5.1.1\","
                + "\n" + "                           \"worker\": {"
                + "\n" + "                               \"name\": \"Worker4\","
                + "\n" + "                               \"company\": { "
                + "\n" + "                                   \"name\": \"Company3\""
                + "\n" + "                               }"
                + "\n" + "                           }"
                + "\n" + "                       },"
                + "\n" + "                       {"
                + "\n" + "                           \"name\": \"Subtask5.1.2\","
                + "\n" + "                           \"worker\": {"
                + "\n" + "                               \"name\": \"Worker4\","
                + "\n" + "                               \"company\": { "
                + "\n" + "                                   \"name\": \"Company3\""
                + "\n" + "                               }"
                + "\n" + "                           }"
                + "\n" + "                       }"
                + "\n" + "                   ]"
                + "\n" + "               },"
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask5.2\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker4\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company3\""
                + "\n" + "                       }"
                + "\n" + "                   },"
                + "\n" + "                   \"subtasks\": ["
                + "\n" + "                       {"
                + "\n" + "                           \"name\": \"Subtask5.2.1\","
                + "\n" + "                           \"worker\": {"
                + "\n" + "                               \"name\": \"Worker4\","
                + "\n" + "                               \"company\": { "
                + "\n" + "                                   \"name\": \"Company3\""
                + "\n" + "                               }"
                + "\n" + "                           }"
                + "\n" + "                       },"
                + "\n" + "                       {"
                + "\n" + "                           \"name\": \"Subtask5.2.2\","
                + "\n" + "                           \"worker\": {"
                + "\n" + "                               \"name\": \"Worker4\","
                + "\n" + "                               \"company\": { "
                + "\n" + "                                   \"name\": \"Company3\""
                + "\n" + "                               }"
                + "\n" + "                           }"
                + "\n" + "                       }"
                + "\n" + "                   ]"
                + "\n" + "               }"
                + "\n" + "           ]"
                + "\n" + "       }"
                + "\n" + "   ]"
                + "\n" + "}";

		// post document
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.body(jsonBody)
			.expect()
			.statusCode(201)
			.when()
			.post("/Project");



		// JSON output depth limits our ability to check the result here, so we need to check separately


		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.body("result",			                                        hasSize(1))
			.body("result_count",		                                        equalTo(1))
			.body("result[0].name",                                                 equalTo("Project1"))

			.body("result[0].tasks[0].name",                                        equalTo("Task1"))
			.body("result[0].tasks[0].worker.name",                                 equalTo("Worker1"))

			.body("result[0].tasks[1].name",                                        equalTo("Task2"))
			.body("result[0].tasks[1].worker.name",                                 equalTo("Worker2"))

			.body("result[0].tasks[2].name",                                        equalTo("Task3"))
			.body("result[0].tasks[2].worker.name",                                 equalTo("Worker3"))

			.body("result[0].tasks[3].name",                                        equalTo("Task4"))
			.body("result[0].tasks[3].worker.name",                                 equalTo("Worker4"))

			.body("result[0].tasks[4].name",                                        equalTo("Task5"))
			.body("result[0].tasks[4].worker.name",                                 equalTo("Worker5"))

			.when()
			.get("/Project/test?name=Project1&_sort=name");

		// check Task1
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.body("result",			                               hasSize(1))
			.body("result_count",		                               equalTo(1))

			.body("result[0].name",                                        equalTo("Task1"))
			.body("result[0].worker.name",                                 equalTo("Worker1"))
			.body("result[0].worker.company.name",                         equalTo("Company1"))

			.body("result[0].subtasks[0].name",                            equalTo("Subtask1.1"))
			.body("result[0].subtasks[0].worker.name",                     equalTo("Worker1"))
			.body("result[0].subtasks[0].worker.company.name",             equalTo("Company1"))
			.body("result[0].subtasks[1].name",                            equalTo("Subtask1.2"))
			.body("result[0].subtasks[1].worker.name",                     equalTo("Worker2"))
			.body("result[0].subtasks[1].worker.company.name",             equalTo("Company1"))
			.body("result[0].subtasks[2].name",                            equalTo("Subtask1.3"))
			.body("result[0].subtasks[2].worker.name",                     equalTo("Worker2"))
			.body("result[0].subtasks[2].worker.company.name",             equalTo("Company1"))
			.body("result[0].subtasks[3].name",                            equalTo("Subtask1.4"))
			.body("result[0].subtasks[3].worker.name",                     equalTo("Worker3"))
			.body("result[0].subtasks[3].worker.company.name",             equalTo("Company2"))

			.when()
			.get("/Task/test?name=Task1&_sort=name");

		// check Task2
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.body("result",			                               hasSize(1))
			.body("result_count",		                               equalTo(1))

			.body("result[0].name",                                        equalTo("Task2"))
			.body("result[0].worker.name",                                 equalTo("Worker2"))
			.body("result[0].worker.company.name",                         equalTo("Company1"))

			.when()
			.get("/Task/test?name=Task2&_sort=name");


		// check Task3
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.body("result",			                               hasSize(1))
			.body("result_count",		                               equalTo(1))

			.body("result[0].name",                                        equalTo("Task3"))
			.body("result[0].worker.name",                                 equalTo("Worker3"))
			.body("result[0].worker.company.name",                         equalTo("Company2"))

			.when()
			.get("/Task/test?name=Task3&_sort=name");


		// check Task4
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.body("result",			                               hasSize(1))
			.body("result_count",		                               equalTo(1))

			.body("result[0].name",                                        equalTo("Task4"))
			.body("result[0].worker.name",                                 equalTo("Worker4"))
			.body("result[0].worker.company.name",                         equalTo("Company3"))

			.body("result[0].subtasks[0].name",                            equalTo("Subtask4.1"))
			.body("result[0].subtasks[0].worker.name",                     equalTo("Worker4"))
			.body("result[0].subtasks[0].worker.company.name",             equalTo("Company3"))
			.body("result[0].subtasks[1].name",                            equalTo("Subtask4.2"))
			.body("result[0].subtasks[1].worker.name",                     equalTo("Worker4"))
			.body("result[0].subtasks[1].worker.company.name",             equalTo("Company3"))
			.body("result[0].subtasks[2].name",                            equalTo("Subtask4.3"))
			.body("result[0].subtasks[2].worker.name",                     equalTo("Worker4"))
			.body("result[0].subtasks[2].worker.company.name",             equalTo("Company3"))
			.body("result[0].subtasks[3].name",                            equalTo("Subtask4.4"))
			.body("result[0].subtasks[3].worker.name",                     equalTo("Worker5"))
			.body("result[0].subtasks[3].worker.company.name",             equalTo("Company3"))

			.when()
			.get("/Task/test?name=Task4&_sort=name");


		// check Task5
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.body("result",			                               hasSize(1))
			.body("result_count",		                               equalTo(1))

			.body("result[0].name",                                        equalTo("Task5"))
			.body("result[0].worker.name",                                 equalTo("Worker5"))
			.body("result[0].worker.company.name",                         equalTo("Company3"))

			.body("result[0].subtasks[0].name",                            equalTo("Subtask5.1"))
			.body("result[0].subtasks[0].worker.name",                     equalTo("Worker4"))
			.body("result[0].subtasks[0].worker.company.name",             equalTo("Company3"))
			.body("result[0].subtasks[0].subtasks[0].name",                equalTo("Subtask5.1.1"))
			.body("result[0].subtasks[0].subtasks[0].worker.name",         equalTo("Worker4"))
			.body("result[0].subtasks[0].subtasks[1].name",                equalTo("Subtask5.1.2"))
			.body("result[0].subtasks[0].subtasks[1].worker.name",         equalTo("Worker4"))
			.body("result[0].subtasks[1].name",                            equalTo("Subtask5.2"))
			.body("result[0].subtasks[1].worker.name",                     equalTo("Worker4"))
			.body("result[0].subtasks[1].worker.company.name",             equalTo("Company3"))
			.body("result[0].subtasks[1].subtasks[0].name",                equalTo("Subtask5.2.1"))
			.body("result[0].subtasks[1].subtasks[0].worker.name",         equalTo("Worker4"))
			.body("result[0].subtasks[1].subtasks[1].name",                equalTo("Subtask5.2.2"))
			.body("result[0].subtasks[1].subtasks[1].worker.name",         equalTo("Worker4"))

			.when()
			.get("/Task/test?name=Task5&_sort=name");


		// check Subtask5.1
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.body("result",			                               hasSize(1))
			.body("result_count",		                               equalTo(1))

			.body("result[0].name",                                        equalTo("Subtask5.1"))
			.body("result[0].worker.name",                                 equalTo("Worker4"))
			.body("result[0].worker.company.name",                         equalTo("Company3"))

			.body("result[0].subtasks[0].name",                            equalTo("Subtask5.1.1"))
			.body("result[0].subtasks[0].worker.name",                     equalTo("Worker4"))
			.body("result[0].subtasks[0].worker.company.name",             equalTo("Company3"))
			.body("result[0].subtasks[1].name",                            equalTo("Subtask5.1.2"))
			.body("result[0].subtasks[1].worker.name",                     equalTo("Worker4"))
			.body("result[0].subtasks[1].worker.company.name",             equalTo("Company3"))

			.when()
			.get("/Task/test?name=Subtask5.1&_sort=name");


		// check Subtask5.2
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.body("result",			                               hasSize(1))
			.body("result_count",		                               equalTo(1))

			.body("result[0].name",                                        equalTo("Subtask5.2"))
			.body("result[0].worker.name",                                 equalTo("Worker4"))
			.body("result[0].worker.company.name",                         equalTo("Company3"))

			.body("result[0].subtasks[0].name",                            equalTo("Subtask5.2.1"))
			.body("result[0].subtasks[0].worker.name",                     equalTo("Worker4"))
			.body("result[0].subtasks[0].worker.company.name",             equalTo("Company3"))
			.body("result[0].subtasks[1].name",                            equalTo("Subtask5.2.2"))
			.body("result[0].subtasks[1].worker.name",                     equalTo("Worker4"))
			.body("result[0].subtasks[1].worker.company.name",             equalTo("Company3"))

			.when()
			.get("/Task/test?name=Subtask5.2&_sort=name");


		// check companies
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.body("result",		hasSize(3))
			.body("result_count",	equalTo(3))

			.body("result[0].name", equalTo("Company1"))
			.body("result[1].name", equalTo("Company2"))
			.body("result[2].name", equalTo("Company3"))

			.when()
			.get("/Company/test?_sort=name");

		// check workers
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.body("result",			hasSize(5))
			.body("result_count",		equalTo(5))

			.body("result[0].name",         equalTo("Worker1"))
			.body("result[0].company.name", equalTo("Company1"))
			.body("result[1].name",         equalTo("Worker2"))
			.body("result[1].company.name", equalTo("Company1"))
			.body("result[2].name",         equalTo("Worker3"))
			.body("result[2].company.name", equalTo("Company2"))
			.body("result[3].name",         equalTo("Worker4"))
			.body("result[3].company.name", equalTo("Company3"))
			.body("result[4].name",         equalTo("Worker5"))
			.body("result[4].company.name", equalTo("Company3"))

			.when()
			.get("/Worker/test?_sort=name");

	}

	@Test
	public void testComplexDocumentUpdate() {

		final String projectNodeId     = createSchemaNode("Project", new PropertySpec("name", "String", true, true), new PropertySpec("description", "String"), new ViewSpec("test", "name, description, tasks"));
		final String taskNodeId        = createSchemaNode("Task",    new PropertySpec("name", "String", true, true), new PropertySpec("description", "String"), new ViewSpec("test", "name, description, subtasks, worker"));
		final String workerNodeId      = createSchemaNode("Worker",  new PropertySpec("name", "String", true, true), new PropertySpec("description", "String"), new ViewSpec("test", "name, description, tasks, company"));
		final String companyNodeId     = createSchemaNode("Company", new PropertySpec("name", "String", true, true), new PropertySpec("description", "String"), new ViewSpec("test", "name, description, workers"));

		// create relationships
		createSchemaRelationships(projectNodeId, taskNodeId,   "TASK",     "1", "*", "project",    "tasks",    Relation.ALWAYS, Relation.SOURCE_TO_TARGET);
		createSchemaRelationships(taskNodeId, taskNodeId,      "SUBTASK",  "1", "*", "parentTask", "subtasks", Relation.ALWAYS, Relation.SOURCE_TO_TARGET);
		createSchemaRelationships(workerNodeId, taskNodeId,    "WORKS_ON", "1", "*", "worker",     "tasks",    Relation.ALWAYS, Relation.SOURCE_TO_TARGET);
		createSchemaRelationships(workerNodeId, companyNodeId, "WORKS_AT", "*", "1", "workers",    "company",  Relation.ALWAYS, Relation.SOURCE_TO_TARGET);

		String jsonBody1 =
                "{"
                + "\n" + "   \"name\": \"Project1\","
                + "\n" + "   \"description\": \"projectDescription1\","
                + "\n" + "   \"tasks\": ["
                + "\n" + "       {"
                + "\n" + "           \"name\": \"Task1\","
                + "\n" + "           \"description\": \"taskDescription1\","
                + "\n" + "           \"worker\": {"
                + "\n" + "               \"name\": \"Worker1\","
                + "\n" + "               \"company\": { "
                + "\n" + "                   \"name\": \"Company1\""
                + "\n" + "               }"
                + "\n" + "           },"
                + "\n" + "           \"subtasks\": ["
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask1.1\","
                + "\n" + "                   \"description\": \"subtaskDescription1.1\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker1\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company1\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               },"
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask1.2\","
                + "\n" + "                   \"description\": \"subtaskDescription1.2\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker2\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company1\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               },"
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask1.3\","
                + "\n" + "                   \"description\": \"subtaskDescription1.3\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker2\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company1\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               },"
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask1.4\","
                + "\n" + "                   \"description\": \"subtaskDescription1.4\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker3\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company2\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               }"
                + "\n" + "           ]"
                + "\n" + "       },"
                + "\n" + "       {"
                + "\n" + "           \"name\": \"Task2\","
                + "\n" + "           \"description\": \"taskDescription2\","
                + "\n" + "           \"worker\": {"
                + "\n" + "               \"name\": \"Worker2\","
                + "\n" + "               \"company\": { "
                + "\n" + "                   \"name\": \"Company1\""
                + "\n" + "               }"
                + "\n" + "           }"
                + "\n" + "       },"
                + "\n" + "       {"
                + "\n" + "           \"name\": \"Task3\","
                + "\n" + "           \"description\": \"taskDescription3\","
                + "\n" + "           \"worker\": {"
                + "\n" + "               \"name\": \"Worker3\","
                + "\n" + "               \"company\": { "
                + "\n" + "                   \"name\": \"Company2\""
                + "\n" + "               }"
                + "\n" + "           }"
                + "\n" + "       },"
                + "\n" + "       {"
                + "\n" + "           \"name\": \"Task4\","
                + "\n" + "           \"description\": \"taskDescription4\","
                + "\n" + "           \"worker\": {"
                + "\n" + "               \"name\": \"Worker4\","
                + "\n" + "               \"company\": { "
                + "\n" + "                   \"name\": \"Company3\""
                + "\n" + "               }"
                + "\n" + "           },"
                + "\n" + "           \"subtasks\": ["
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask4.1\","
                + "\n" + "                   \"description\": \"subtaskDescription4.1\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker4\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company3\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               },"
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask4.2\","
                + "\n" + "                   \"description\": \"subtaskDescription4.2\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker4\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company3\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               },"
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask4.3\","
                + "\n" + "                   \"description\": \"subtaskDescription4.3\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker4\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company3\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               },"
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask4.4\","
                + "\n" + "                   \"description\": \"subtaskDescription4.4\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker5\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company3\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               }"
                + "\n" + "           ]"
                + "\n" + "       }"
                + "\n" + "   ]"
                + "\n" + "}";

		// post document
		final String projectId = getUuidFromLocation(RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.body(jsonBody1)
			.expect()
			.statusCode(201)
			.when()
			.post("/Project")
			.getHeader("Location"));

		String jsonBody2 =
                "{"
                + "\n" + "   \"name\": \"Project1\","
                + "\n" + "   \"description\": \"new projectDescription1\","
                + "\n" + "   \"tasks\": ["
                + "\n" + "       {"
                + "\n" + "           \"name\": \"Task1\","
                + "\n" + "           \"description\": \"new taskDescription1\","
                + "\n" + "           \"worker\": {"
                + "\n" + "               \"name\": \"Worker1\","
                + "\n" + "               \"description\": \"new workerDescription1\","
                + "\n" + "               \"company\": { "
                + "\n" + "                   \"name\": \"Company1\","
                + "\n" + "                   \"description\": \"new companyDescription1\""
                + "\n" + "               }"
                + "\n" + "           },"
                + "\n" + "           \"subtasks\": ["
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask1.1\","
                + "\n" + "                   \"description\": \"new subtaskDescription1.1\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker1\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company1\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               },"
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask1.2\","
                + "\n" + "                   \"description\": \"new subtaskDescription1.2\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker2\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company1\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               },"
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask1.3\","
                + "\n" + "                   \"description\": \"new subtaskDescription1.3\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker2\","
                + "\n" + "                       \"description\": \"new workerDescription2\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company1\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               },"
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask1.4\","
                + "\n" + "                   \"description\": \"new subtaskDescription1.4\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker3\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company2\","
                + "\n" + "                           \"description\": \"new companyDescription2\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               }"
                + "\n" + "           ]"
                + "\n" + "       },"
                + "\n" + "       {"
                + "\n" + "           \"name\": \"Task2\","
                + "\n" + "           \"description\": \"new taskDescription2\","
                + "\n" + "           \"worker\": {"
                + "\n" + "               \"name\": \"Worker2\","
                + "\n" + "               \"company\": { "
                + "\n" + "                   \"name\": \"Company1\""
                + "\n" + "               }"
                + "\n" + "           }"
                + "\n" + "       },"
                + "\n" + "       {"
                + "\n" + "           \"name\": \"Task3\","
                + "\n" + "           \"description\": \"new taskDescription3\","
                + "\n" + "           \"worker\": {"
                + "\n" + "               \"name\": \"Worker3\","
                + "\n" + "               \"description\": \"new workerDescription3\","
                + "\n" + "               \"company\": { "
                + "\n" + "                   \"name\": \"Company2\""
                + "\n" + "               }"
                + "\n" + "           }"
                + "\n" + "       },"
                + "\n" + "       {"
                + "\n" + "           \"name\": \"Task4\","
                + "\n" + "           \"description\": \"new taskDescription4\","
                + "\n" + "           \"worker\": {"
                + "\n" + "               \"name\": \"Worker4\","
                + "\n" + "               \"company\": { "
                + "\n" + "                   \"name\": \"Company3\""
                + "\n" + "               }"
                + "\n" + "           },"
                + "\n" + "           \"subtasks\": ["
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask4.1\","
                + "\n" + "                   \"description\": \"new subtaskDescription4.1\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker4\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company3\","
                + "\n" + "                           \"description\": \"new companyDescription3\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               },"
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask4.2\","
                + "\n" + "                   \"description\": \"new subtaskDescription4.2\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker4\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company3\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               },"
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask4.3\","
                + "\n" + "                   \"description\": \"new subtaskDescription4.3\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker4\","
                + "\n" + "                       \"description\": \"new workerDescription4\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company3\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               },"
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask4.4\","
                + "\n" + "                   \"description\": \"new subtaskDescription4.4\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker5\","
                + "\n" + "                       \"description\": \"new workerDescription5\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company3\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               }"
                + "\n" + "           ]"
                + "\n" + "       }"
                + "\n" + "   ]"
                + "\n" + "}";

		// update document
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.body(jsonBody2)
			.expect()
			.statusCode(200)
			.when()
			.put("/Project/" + projectId);

		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.body("result",			                                        hasSize(1))
			.body("result_count",		                                        equalTo(1))
			.body("result[0].name",                                                 equalTo("Project1"))
			.body("result[0].description",                                          equalTo("new projectDescription1"))

			.body("result[0].tasks[0].name",                                        equalTo("Task1"))
			.body("result[0].tasks[0].description",                                 equalTo("new taskDescription1"))
			.body("result[0].tasks[0].worker.name",                                 equalTo("Worker1"))
			.body("result[0].tasks[0].worker.description",                          equalTo("new workerDescription1"))

			.body("result[0].tasks[1].name",                                        equalTo("Task2"))
			.body("result[0].tasks[1].description",                                 equalTo("new taskDescription2"))
			.body("result[0].tasks[1].worker.name",                                 equalTo("Worker2"))
			.body("result[0].tasks[1].worker.description",                          equalTo("new workerDescription2"))

			.body("result[0].tasks[2].name",                                        equalTo("Task3"))
			.body("result[0].tasks[2].description",                                 equalTo("new taskDescription3"))
			.body("result[0].tasks[2].worker.name",                                 equalTo("Worker3"))
 			.body("result[0].tasks[2].worker.description",                          equalTo("new workerDescription3"))

			.body("result[0].tasks[3].name",                                        equalTo("Task4"))
			.body("result[0].tasks[3].description",                                 equalTo("new taskDescription4"))
			.body("result[0].tasks[3].worker.name",                                 equalTo("Worker4"))
			.body("result[0].tasks[3].worker.description",                          equalTo("new workerDescription4"))

			.when()
			.get("/Project/test?name=Project1&_sort=name");

		// check Task1
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.body("result",			                               hasSize(1))
			.body("result_count",		                               equalTo(1))

			.body("result[0].name",                                        equalTo("Task1"))
			.body("result[0].description",                                 equalTo("new taskDescription1"))
			.body("result[0].worker.name",                                 equalTo("Worker1"))
			.body("result[0].worker.description",                          equalTo("new workerDescription1"))
			.body("result[0].worker.company.name",                         equalTo("Company1"))
			.body("result[0].worker.company.description",                  equalTo("new companyDescription1"))

			.body("result[0].subtasks[0].name",                            equalTo("Subtask1.1"))
			.body("result[0].subtasks[0].description",                     equalTo("new subtaskDescription1.1"))
			.body("result[0].subtasks[0].worker.name",                     equalTo("Worker1"))
			.body("result[0].subtasks[0].worker.description",              equalTo("new workerDescription1"))
			.body("result[0].subtasks[0].worker.company.name",             equalTo("Company1"))
			.body("result[0].subtasks[0].worker.company.description",      equalTo("new companyDescription1"))
			.body("result[0].subtasks[1].name",                            equalTo("Subtask1.2"))
			.body("result[0].subtasks[1].description",                     equalTo("new subtaskDescription1.2"))
			.body("result[0].subtasks[1].worker.name",                     equalTo("Worker2"))
			.body("result[0].subtasks[1].worker.description",              equalTo("new workerDescription2"))
			.body("result[0].subtasks[1].worker.company.name",             equalTo("Company1"))
			.body("result[0].subtasks[1].worker.company.description",      equalTo("new companyDescription1"))
			.body("result[0].subtasks[2].name",                            equalTo("Subtask1.3"))
			.body("result[0].subtasks[2].description",                     equalTo("new subtaskDescription1.3"))
			.body("result[0].subtasks[2].worker.name",                     equalTo("Worker2"))
			.body("result[0].subtasks[2].worker.description",              equalTo("new workerDescription2"))
			.body("result[0].subtasks[2].worker.company.name",             equalTo("Company1"))
			.body("result[0].subtasks[2].worker.company.description",      equalTo("new companyDescription1"))
			.body("result[0].subtasks[3].name",                            equalTo("Subtask1.4"))
			.body("result[0].subtasks[3].description",                     equalTo("new subtaskDescription1.4"))
			.body("result[0].subtasks[3].worker.name",                     equalTo("Worker3"))
			.body("result[0].subtasks[3].worker.description",              equalTo("new workerDescription3"))
			.body("result[0].subtasks[3].worker.company.name",             equalTo("Company2"))
			.body("result[0].subtasks[3].worker.company.description",      equalTo("new companyDescription2"))

			.when()
			.get("/Task/test?name=Task1&_sort=name");

		// check Task2
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.body("result",			                               hasSize(1))
			.body("result_count",		                               equalTo(1))

			.body("result[0].name",                                        equalTo("Task2"))
			.body("result[0].description",                                 equalTo("new taskDescription2"))
			.body("result[0].worker.name",                                 equalTo("Worker2"))
			.body("result[0].worker.description",                          equalTo("new workerDescription2"))
			.body("result[0].worker.company.name",                         equalTo("Company1"))
			.body("result[0].worker.company.description",                  equalTo("new companyDescription1"))

			.when()
			.get("/Task/test?name=Task2&_sort=name");


		// check Task3
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.body("result",			                               hasSize(1))
			.body("result_count",		                               equalTo(1))

			.body("result[0].name",                                        equalTo("Task3"))
			.body("result[0].description",                                 equalTo("new taskDescription3"))
			.body("result[0].worker.name",                                 equalTo("Worker3"))
			.body("result[0].worker.description",                          equalTo("new workerDescription3"))
			.body("result[0].worker.company.name",                         equalTo("Company2"))
			.body("result[0].worker.company.description",                  equalTo("new companyDescription2"))

			.when()
			.get("/Task/test?name=Task3&_sort=name");


		// check Task4
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.body("result",			                               hasSize(1))
			.body("result_count",		                               equalTo(1))

			.body("result[0].name",                                        equalTo("Task4"))
			.body("result[0].description",                                 equalTo("new taskDescription4"))
			.body("result[0].worker.name",                                 equalTo("Worker4"))
			.body("result[0].worker.description",                          equalTo("new workerDescription4"))
			.body("result[0].worker.company.name",                         equalTo("Company3"))
			.body("result[0].worker.company.description",                  equalTo("new companyDescription3"))

			.body("result[0].subtasks[0].name",                            equalTo("Subtask4.1"))
			.body("result[0].subtasks[0].description",                     equalTo("new subtaskDescription4.1"))
			.body("result[0].subtasks[0].worker.name",                     equalTo("Worker4"))
			.body("result[0].subtasks[0].worker.description",              equalTo("new workerDescription4"))
			.body("result[0].subtasks[0].worker.company.name",             equalTo("Company3"))
			.body("result[0].subtasks[0].worker.company.description",      equalTo("new companyDescription3"))
			.body("result[0].subtasks[1].name",                            equalTo("Subtask4.2"))
			.body("result[0].subtasks[1].description",                     equalTo("new subtaskDescription4.2"))
			.body("result[0].subtasks[1].worker.name",                     equalTo("Worker4"))
			.body("result[0].subtasks[1].worker.description",              equalTo("new workerDescription4"))
			.body("result[0].subtasks[1].worker.company.name",             equalTo("Company3"))
			.body("result[0].subtasks[1].worker.company.description",      equalTo("new companyDescription3"))
			.body("result[0].subtasks[2].name",                            equalTo("Subtask4.3"))
			.body("result[0].subtasks[2].description",                     equalTo("new subtaskDescription4.3"))
			.body("result[0].subtasks[2].worker.name",                     equalTo("Worker4"))
			.body("result[0].subtasks[2].worker.description",              equalTo("new workerDescription4"))
			.body("result[0].subtasks[2].worker.company.name",             equalTo("Company3"))
			.body("result[0].subtasks[2].worker.company.description",      equalTo("new companyDescription3"))
			.body("result[0].subtasks[3].name",                            equalTo("Subtask4.4"))
			.body("result[0].subtasks[3].description",                     equalTo("new subtaskDescription4.4"))
			.body("result[0].subtasks[3].worker.name",                     equalTo("Worker5"))
			.body("result[0].subtasks[3].worker.description",              equalTo("new workerDescription5"))
			.body("result[0].subtasks[3].worker.company.name",             equalTo("Company3"))
			.body("result[0].subtasks[3].worker.company.description",      equalTo("new companyDescription3"))

			.when()
			.get("/Task/test?name=Task4&_sort=name");

		// check companies
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.body("result",		hasSize(3))
			.body("result_count",	equalTo(3))

			.body("result[0].name",        equalTo("Company1"))
			.body("result[0].description", equalTo("new companyDescription1"))
			.body("result[1].name",        equalTo("Company2"))
			.body("result[1].description", equalTo("new companyDescription2"))
			.body("result[2].name",        equalTo("Company3"))
			.body("result[2].description", equalTo("new companyDescription3"))

			.when()
			.get("/Company/test?_sort=name");

		// check workers
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.body("result",			hasSize(5))
			.body("result_count",		equalTo(5))

			.body("result[0].name",                equalTo("Worker1"))
			.body("result[0].description",         equalTo("new workerDescription1"))
			.body("result[0].company.name",        equalTo("Company1"))
			.body("result[0].company.description", equalTo("new companyDescription1"))
			.body("result[1].name",                equalTo("Worker2"))
			.body("result[1].description",         equalTo("new workerDescription2"))
			.body("result[1].company.name",        equalTo("Company1"))
			.body("result[1].company.description", equalTo("new companyDescription1"))
			.body("result[2].name",                equalTo("Worker3"))
			.body("result[2].description",         equalTo("new workerDescription3"))
			.body("result[2].company.name",        equalTo("Company2"))
			.body("result[2].company.description", equalTo("new companyDescription2"))
			.body("result[3].name",                equalTo("Worker4"))
			.body("result[3].description",         equalTo("new workerDescription4"))
			.body("result[3].company.name",        equalTo("Company3"))
			.body("result[3].company.description", equalTo("new companyDescription3"))
			.body("result[4].name",                equalTo("Worker5"))
			.body("result[4].description",         equalTo("new workerDescription5"))
			.body("result[4].company.name",        equalTo("Company3"))
			.body("result[4].company.description", equalTo("new companyDescription3"))

			.when()
			.get("/Worker/test?_sort=name");

	}

	@Test
	public void testMergeOnNestedProperties() {

		final String projectNodeId     = createSchemaNode("Project", new PropertySpec("name", "String", true, true), new PropertySpec("description", "String"));
		final String taskNodeId        = createSchemaNode("Task",    new PropertySpec("name", "String", true, true), new PropertySpec("description", "String"));
		final String workerNodeId      = createSchemaNode("Worker",  new PropertySpec("name", "String", true, true), new PropertySpec("description", "String"));

		// create relationships
		createSchemaRelationships(projectNodeId, taskNodeId,   "TASK",     "*", "*", "project",    "tasks",    Relation.ALWAYS, Relation.SOURCE_TO_TARGET);
		createSchemaRelationships(workerNodeId, taskNodeId,    "WORKS_ON", "*", "*", "worker",     "tasks",    Relation.ALWAYS, Relation.SOURCE_TO_TARGET);

		RestAssured.given().contentType("application/json; charset=UTF-8")
			.body("{\"name\":\"Project1\",\"tasks\":[{\"name\":\"Task1\",\"worker\":[{\"name\":\"Worker1\"}]}]}")
			.expect()
			.statusCode(201)
			.when()
			.post("/Project");


		RestAssured.given().contentType("application/json; charset=UTF-8")
			.header("Structr-Force-Merge-Of-Nested-Properties", "enabled")
			.body("{\"name\":\"Project2\",\"tasks\":[{\"name\":\"Task1\",\"worker\":[{\"name\":\"Worker2\"}]}]}")
			.expect()
			.statusCode(201)
			.when()
			.post("/Project");

		RestAssured.given().contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)

			.body("result",                     hasSize(1))
			.body("result_count",               equalTo(1))

			.body("result[0].name",             equalTo("Task1"))
			.body("result[0].project[0].name",  equalTo("Project1"))
			.body("result[0].project[1].name",  equalTo("Project2"))
			.body("result[0].worker[0].name",   equalTo("Worker1"))
			.body("result[0].worker[1].name",   equalTo("Worker2"))

			.when()
			.get("/Task/custom?_sort=name");
	}

	@Test
	public void testNestedUpdate() {

		final String projectNodeId     = createSchemaNode("Project", new PropertySpec("name", "String", true, true), new PropertySpec("description", "String"), new ViewSpec("test", "id, type, name, tasks"));
		final String taskNodeId        = createSchemaNode("Task",    new PropertySpec("name", "String", true, true), new PropertySpec("description", "String"), new ViewSpec("test", "id, type, name, workers"));
		final String workerNodeId      = createSchemaNode("Worker",  new PropertySpec("name", "String", true, true), new PropertySpec("description", "String"), new ViewSpec("test", "id, type, name"));

		// create relationships
		createSchemaRelationships(projectNodeId, taskNodeId,   "TASK",     "*", "*", "project",    "tasks",    Relation.NONE, Relation.SOURCE_TO_TARGET);
		createSchemaRelationships(workerNodeId, taskNodeId,    "WORKS_ON", "*", "*", "workers",    "tasks",    Relation.NONE, Relation.SOURCE_TO_TARGET);

		final String project1 = createEntity("/Project", "{ name: Project1 }");
		final String task1    = createEntity("/Task", "{ name: Task1 }");
		final String worker1  = createEntity("/Worker", "{ name: Worker1 }");
		final String worker2  = createEntity("/Worker", "{ name: Worker2 }");
		final String worker3  = createEntity("/Worker", "{ name: Worker3 }");
		final String worker4  = createEntity("/Worker", "{ name: Worker4 }");

		// add a task
		RestAssured.given().contentType("application/json; charset=UTF-8")
			.body("{ tasks: [" + task1 + "] }")
			.expect()
			.statusCode(200)
			.when()
			.put("/Project/" + project1);

		// check result
		RestAssured.given().contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)

			.body("result",                     hasSize(1))
			.body("result_count",               equalTo(1))

			.body("result[0].name",             equalTo("Project1"))
			.body("result[0].tasks[0].name",    equalTo("Task1"))
			.body("result[0].tasks[0].workers", hasSize(0))

			.when()
			.get("/Project/test?_sort=name");

		// add a single worker using nested PUT
		RestAssured.given().contentType("application/json; charset=UTF-8")
			.body("{ tasks: [ { id: '" + task1 + "', workers: [ " + worker1 + "] } ] }")
			.expect()
			.statusCode(200)
			.when()
			.put("/Project/" + project1);

		// check result
		RestAssured.given().contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)

			.body("result",                             hasSize(1))
			.body("result_count",                       equalTo(1))

			.body("result[0].name",                     equalTo("Project1"))
			.body("result[0].tasks[0].name",            equalTo("Task1"))
			.body("result[0].tasks[0].workers[0].name", equalTo("Worker1"))

			.when()
			.get("/Project/test?_sort=name");

		// add a second worker
		RestAssured.given().contentType("application/json; charset=UTF-8")
			.body("{ tasks: [ { id: '" + task1 + "', workers: [ " + worker1 + ", " + worker2 + " ] } ] }")
			.expect()
			.statusCode(200)
			.when()
			.put("/Project/" + project1);

		// check result
		RestAssured.given().contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)

			.body("result",                             hasSize(1))
			.body("result_count",                       equalTo(1))

			.body("result[0].name",                     equalTo("Project1"))
			.body("result[0].tasks[0].name",            equalTo("Task1"))
			.body("result[0].tasks[0].workers[0].name", equalTo("Worker1"))
			.body("result[0].tasks[0].workers[1].name", equalTo("Worker2"))

			.when()
			.get("/Project/test?_sort=name");

		// replace workers with worker3
		RestAssured.given().contentType("application/json; charset=UTF-8")
			.body("{ tasks: [ { id: '" + task1 + "', workers: [ " + worker3 + " ] } ] }")
			.expect()
			.statusCode(200)
			.when()
			.put("/Project/" + project1);

		// check result
		RestAssured.given().contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)

			.body("result",                             hasSize(1))
			.body("result_count",                       equalTo(1))

			.body("result[0].name",                     equalTo("Project1"))
			.body("result[0].tasks[0].name",            equalTo("Task1"))
			.body("result[0].tasks[0].workers[0].name", equalTo("Worker3"))

			.when()
			.get("/Project/test?_sort=name");

	}

	@Test
	public void testForeignPropertiesInOneToOne() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonObjectType project = schema.addType("Project");
			final JsonObjectType task    = schema.addType("Task");
			final JsonReferenceType rel  = project.relate(task, "HAS", Cardinality.OneToOne, "project", "task").setCascadingCreate(Cascade.sourceToTarget);

			rel.addStringProperty("role", "public");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception");
		}

		final String projectId = createEntity("Project", "{ name: 'Project1' }");
		final String taskId    = createEntity("Task",    "{ name: 'Task1' }");


		// create data
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.body("{ task: { id: '" + taskId + "', role: 'Role1' } }")

			.expect()
				.statusCode(200)

			.when()
				.put("/Project/" + projectId);


		// check result
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)
				.body("result[0].role", equalTo("Role1"))

			.when()
				.get("/ProjectHASTask");
	}

	@Test
	public void testForeignPropertiesInOneToMany() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonObjectType project = schema.addType("Project");
			final JsonObjectType task    = schema.addType("Task");
			final JsonReferenceType rel  = project.relate(task, "HAS", Cardinality.OneToMany, "project", "tasks").setCascadingCreate(Cascade.sourceToTarget);

			rel.addStringProperty("role", "public");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception");
		}

		final String projectId = createEntity("Project", "{ name: 'Project1' }");
		final String taskId    = createEntity("Task",    "{ name: 'Task1' }");


		// create data
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.body("{ tasks: [ { id: '" + taskId + "', role: 'Role1' } ] }")

			.expect()
				.statusCode(200)

			.when()
	 			.put("/Project/" + projectId);

		// check result
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)
				.body("result[0].role", equalTo("Role1"))

			.when()
				.get("/ProjectHASTask");

	}

	@Test
	public void testForeignPropertiesOnRelationship() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.getType(StructrTraits.GROUP_CONTAINS_PRINCIPAL);

			type.addStringProperty("test", PropertyView.Public);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception");
		}

		final String parent = createEntity(StructrTraits.GROUP, "{ name: 'parent' }");
		final String child  = createEntity(StructrTraits.GROUP, "{ name: 'child' }");


		// create data
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.body("{ members: [ { id: '" + child + "', test: 'success!!' } ] }")

			.expect()
				.statusCode(200)

			.when()
				.put("/Group/" + parent);

		// check result
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)
				.body("result[0].test", equalTo("success!!"))

			.when()
				.get("/GroupCONTAINSPrincipal");

	}

	@Test
	public void testAdvancedObjectCreationResult() {

		final String projectNodeId     = createSchemaNode("Project", new PropertySpec("name", "String", true, true), new PropertySpec("description", "String"), new ViewSpec("test", "name, description, tasks"));
		final String taskNodeId        = createSchemaNode("Task",    new PropertySpec("name", "String", true, true), new PropertySpec("description", "String"), new ViewSpec("test", "name, description, subtasks, worker"));
		final String workerNodeId      = createSchemaNode("Worker",  new PropertySpec("name", "String", true, true), new PropertySpec("description", "String"), new ViewSpec("test", "name, description, tasks, company"));
		final String companyNodeId     = createSchemaNode("Company", new PropertySpec("name", "String", true, true), new PropertySpec("description", "String"), new ViewSpec("test", "name, description, workers"));

		// create relationships
		createSchemaRelationships(projectNodeId, taskNodeId,    "TASK",     "1", "*", "project",    "tasks",    Relation.ALWAYS, Relation.SOURCE_TO_TARGET);
		createSchemaRelationships(taskNodeId,    taskNodeId,    "SUBTASK",  "1", "*", "parentTask", "subtasks", Relation.ALWAYS, Relation.SOURCE_TO_TARGET);
		createSchemaRelationships(workerNodeId,  taskNodeId,    "WORKS_ON", "1", "*", "worker",     "tasks",    Relation.ALWAYS, Relation.SOURCE_TO_TARGET);
		createSchemaRelationships(workerNodeId,  companyNodeId, "WORKS_AT", "*", "1", "workers",    "company",  Relation.ALWAYS, Relation.SOURCE_TO_TARGET);

		String jsonBody1 =
                "{"
                + "\n" + "   \"name\": \"Project1\","
                + "\n" + "   \"description\": \"projectDescription1\","
                + "\n" + "   \"tasks\": ["
                + "\n" + "       {"
                + "\n" + "           \"name\": \"Task1\","
                + "\n" + "           \"description\": \"taskDescription1\","
                + "\n" + "           \"worker\": {"
                + "\n" + "               \"name\": \"Worker1\","
                + "\n" + "               \"company\": { "
                + "\n" + "                   \"name\": \"Company1\""
                + "\n" + "               }"
                + "\n" + "           },"
                + "\n" + "           \"subtasks\": ["
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask1.1\","
                + "\n" + "                   \"description\": \"subtaskDescription1.1\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker1\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company1\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               },"
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask1.2\","
                + "\n" + "                   \"description\": \"subtaskDescription1.2\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker2\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company1\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               },"
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask1.3\","
                + "\n" + "                   \"description\": \"subtaskDescription1.3\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker2\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company1\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               },"
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask1.4\","
                + "\n" + "                   \"description\": \"subtaskDescription1.4\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker3\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company2\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               }"
                + "\n" + "           ]"
                + "\n" + "       },"
                + "\n" + "       {"
                + "\n" + "           \"name\": \"Task2\","
                + "\n" + "           \"description\": \"taskDescription2\","
                + "\n" + "           \"worker\": {"
                + "\n" + "               \"name\": \"Worker2\","
                + "\n" + "               \"company\": { "
                + "\n" + "                   \"name\": \"Company1\""
                + "\n" + "               }"
                + "\n" + "           }"
                + "\n" + "       },"
                + "\n" + "       {"
                + "\n" + "           \"name\": \"Task3\","
                + "\n" + "           \"description\": \"taskDescription3\","
                + "\n" + "           \"worker\": {"
                + "\n" + "               \"name\": \"Worker3\","
                + "\n" + "               \"company\": { "
                + "\n" + "                   \"name\": \"Company2\""
                + "\n" + "               }"
                + "\n" + "           }"
                + "\n" + "       },"
                + "\n" + "       {"
                + "\n" + "           \"name\": \"Task4\","
                + "\n" + "           \"description\": \"taskDescription4\","
                + "\n" + "           \"worker\": {"
                + "\n" + "               \"name\": \"Worker4\","
                + "\n" + "               \"company\": { "
                + "\n" + "                   \"name\": \"Company3\""
                + "\n" + "               }"
                + "\n" + "           },"
                + "\n" + "           \"subtasks\": ["
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask4.1\","
                + "\n" + "                   \"description\": \"subtaskDescription4.1\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker4\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company3\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               },"
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask4.2\","
                + "\n" + "                   \"description\": \"subtaskDescription4.2\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker4\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company3\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               },"
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask4.3\","
                + "\n" + "                   \"description\": \"subtaskDescription4.3\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker4\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company3\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               },"
                + "\n" + "               {"
                + "\n" + "                   \"name\": \"Subtask4.4\","
                + "\n" + "                   \"description\": \"subtaskDescription4.4\","
                + "\n" + "                   \"worker\": {"
                + "\n" + "                       \"name\": \"Worker5\","
                + "\n" + "                       \"company\": { "
                + "\n" + "                           \"name\": \"Company3\""
                + "\n" + "                       }"
                + "\n" + "                   }"
                + "\n" + "               }"
                + "\n" + "           ]"
                + "\n" + "       }"
                + "\n" + "   ]"
                + "\n" + "}";

		// post document
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.header("Structr-Return-Details-For-Created-Objects", true)
			.body(jsonBody1)
			.expect()
			.statusCode(201)
			.body("result[0].type", equalTo("Company"))
			.body("result[1].type", equalTo("Worker"))
			.body("result[2].type", equalTo("Task"))
			.body("result[3].type", equalTo("Worker"))
			.body("result[4].type", equalTo("Task"))
			.body("result[5].type", equalTo("Task"))
			.body("result[6].type", equalTo("Company"))
			.body("result[7].type", equalTo("Worker"))
			.body("result[8].type", equalTo("Task"))
			.body("result[9].type", equalTo("Task"))
			.body("result[10].type", equalTo("Task"))
			.body("result[11].type", equalTo("Task"))
			.body("result[12].type", equalTo("Company"))
			.body("result[13].type", equalTo("Worker"))
			.body("result[14].type", equalTo("Task"))
			.body("result[15].type", equalTo("Task"))
			.body("result[16].type", equalTo("Task"))
			.body("result[17].type", equalTo("Worker"))
			.body("result[18].type", equalTo("Task"))
			.body("result[19].type", equalTo("Task"))
			.body("result[20].type", equalTo("Project"))
			.when()
			.post("/Project");
	}

	@Test
	public void testAssignmentWithWrongTypeOneToOne() {

		final String projectNodeId     = createSchemaNode("Project", new PropertySpec("name", "String", true, true));
		final String taskNodeId        = createSchemaNode("Task",    new PropertySpec("name", "String", false, true));

		createSchemaRelationships(projectNodeId, taskNodeId, "TASK", "1", "1", "project", "task", Relation.NONE, Relation.SOURCE_TO_TARGET);

		final String projectId = createEntity("Project", "{ name: 'Project1' }");
		final String testId    = createEntity("MailTemplate",    "{ name: 'Task1' }");

		// create data
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.body("{ task: " + testId + " }")

			.expect()
			.statusCode(422)

			.when()
			.put("/Project/" + projectId);

	}

	@Test
	public void testAssignmentWithWrongTypeOneToMany() {

		final String projectNodeId     = createSchemaNode("Project", new PropertySpec("name", "String", true, true));
		final String taskNodeId        = createSchemaNode("Task",    new PropertySpec("name", "String", false, true));

		createSchemaRelationships(projectNodeId, taskNodeId, "TASK", "1", "*", "project", "tasks", Relation.NONE, Relation.SOURCE_TO_TARGET);

		final String projectId = createEntity("Project", "{ name: 'Project1' }");
		final String testId    = createEntity("MailTemplate",    "{ name: 'Task1' }");

		// create data
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.body("{ tasks: [ " + testId + " ] }")

			.expect()
			.statusCode(422)

			.when()
			.put("/Project/" + projectId);
	}

	@Test
	public void testAssignmentWithWrongTypeManyToOne() {

		final String projectNodeId     = createSchemaNode("Project", new PropertySpec("name", "String", true, true));
		final String taskNodeId        = createSchemaNode("Task",    new PropertySpec("name", "String", false, true));

		createSchemaRelationships(projectNodeId, taskNodeId, "TASK", "*", "1", "projects", "task", Relation.NONE, Relation.SOURCE_TO_TARGET);

		final String projectId = createEntity("Project", "{ name: 'Project1' }");
		final String testId    = createEntity("MailTemplate",    "{ name: 'Task1' }");

		// create data
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.body("{ task: " + testId + "}")

			.expect()
			.statusCode(422)

			.when()
			.put("/Project/" + projectId);
	}

	@Test
	public void testAssignmentWithWrongTypeManyToMany() {

		final String projectNodeId     = createSchemaNode("Project", new PropertySpec("name", "String", true, true));
		final String taskNodeId        = createSchemaNode("Task",    new PropertySpec("name", "String", false, true));

		createSchemaRelationships(projectNodeId, taskNodeId, "TASK", "*", "*", "projects", "tasks", Relation.NONE, Relation.SOURCE_TO_TARGET);

		final String projectId = createEntity("Project", "{ name: 'Project1' }");
		final String testId    = createEntity("MailTemplate",    "{ name: 'Task1' }");

		// create data
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.body("{ tasks: [ " + testId + " ] }")

			.expect()
			.statusCode(422)

			.when()
			.put("/Project/" + projectId);
	}

	// ----- private methods -----
	private String createSchemaNode(final String name, SchemaSpec... specs) {

		final StringBuilder buf              = new StringBuilder();
		final Map<String, Set<String>> views = new LinkedHashMap<>();
		boolean first                        = true;

		// append name
		buf.append("{ \"name\": \"");
		buf.append(name);
		buf.append("\", schemaProperties: [");

		for (final SchemaSpec spec : specs) {

			if (spec.isView) {

				final Set<String> view = views.computeIfAbsent(spec.key, k -> new LinkedHashSet<>());
				for (final String p : spec.type.split("[, ]+")) {

					view.add(p.trim());
				}

			} else {

				views.computeIfAbsent("public", k -> new LinkedHashSet<>()).add(spec.key);

				if (!first) {
					buf.append(", ");
				}

				buf.append("{ name: \"");
				buf.append(spec.key);
				buf.append("\", propertyType: ");
				buf.append(spec.type);
				buf.append(", ");
				buf.append("unique: ");
				buf.append(spec.unique);
				buf.append(", notNull: ");
				buf.append(spec.notNull);
				buf.append(" }");

				first = false;
			}
		}

		// append view as well
		buf.append("], schemaViews: [");

		first = true;

		for (final Map.Entry<String, Set<String>> entry : views.entrySet()) {

			final String viewName = entry.getKey();

			if (!first) {
				buf.append(", ");
			}

			buf.append("{ name: ");
			buf.append(viewName);
			buf.append(", nonGraphProperties: \"");
			buf.append(StringUtils.join(entry.getValue(), ","));
			buf.append("\" }");

			first = false;
		}

		//System.out.println(buf);

		buf.append("] }");

		return createEntity("/SchemaNode", buf.toString());
	}

	private String createSchemaRelationships(final String sourceId, final String targetId, final String relationshipType, final String sourceMultiplicity, final String targetMultiplicity, final String sourceJsonName, final String targetJsonName, final int autocreate, final int cascadingDelete) {

		return createEntity(
			"/SchemaRelationshipNode",
			"{ \"sourceId\": \"" + sourceId + "\"" +
			", \"targetId\": \"" + targetId + "\"" +
			", \"relationshipType\": \"" + relationshipType + "\"" +
			", \"sourceMultiplicity\": \"" + sourceMultiplicity + "\"" +
			", \"targetMultiplicity\": \"" + targetMultiplicity + "\"" +
			", \"sourceJsonName\": \"" + sourceJsonName + "\"" +
			", \"targetJsonName\": \"" + targetJsonName + "\"" +
			", \"cascadingDeleteFlag\": " + cascadingDelete +
			", \"autocreationFlag\": " + autocreate +
			" }");
	}

	private static class PropertySpec extends SchemaSpec {

		public PropertySpec(final String key, final String type) {
			this(key, type, false, false);
		}

		public PropertySpec(final String key, final String type, final boolean unique, final boolean notNull) {
			super(false, key, type, unique, notNull);
		}
	}

	private static class ViewSpec extends SchemaSpec {

		public ViewSpec(final String key, final String properties) {
			super(true, key, properties, false, false);
		}
	}

	private static class SchemaSpec {

		String key      = null;
		String type     = null;
		boolean unique  = false;
		boolean notNull = false;
		boolean isView  = false;

		public SchemaSpec(final boolean isView, final String key, final String type) {
			this(isView, key, type, false, false);
		}

		public SchemaSpec(final boolean isView, final String key, final String type, final boolean unique, final boolean notNull) {

			this.key     = key;
			this.type    = type;
			this.unique  = unique;
			this.notNull = notNull;
			this.isView  = isView;
		}
	}
}
