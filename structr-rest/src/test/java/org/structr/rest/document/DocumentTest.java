/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.rest.document;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import org.junit.Test;
import org.structr.core.entity.Relation;
import org.structr.rest.common.StructrRestTest;

/**
 *
 *
 */
public class DocumentTest extends StructrRestTest {

	@Test
	public void testSchemaAutocreateNone() {

		final String projectNodeId     = createSchemaNode("Project", new Pair("_name", "+String!"));
		final String taskNodeId        = createSchemaNode("Task",    new Pair("_name", "+String"));

		createSchemaRelationships(projectNodeId, taskNodeId, "TASK", "1", "*", "project", "tasks", Relation.NONE, Relation.SOURCE_TO_TARGET);

		// post document: expect success (Project -> Task)
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.body("{\"name\":\"Project1\",\"tasks\":[{\"name\":\"Task1\"}]}")
			.expect()
			.statusCode(422)
			.when()
			.post("/projects");

		// post document: expect failure (Task -> Project)
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.body("{\"name\":\"Task2\",\"project\":{\"name\":\"Project2\"}}")
			.expect()
			.statusCode(422)
			.when()
			.post("/tasks");

	}

	@Test
	public void testSchemaAutocreateSourceToTarget() {

		final String projectNodeId     = createSchemaNode("Project", new Pair("_name", "+String!"));
		final String taskNodeId        = createSchemaNode("Task",    new Pair("_name", "+String"));

		createSchemaRelationships(projectNodeId, taskNodeId, "TASK", "1", "*", "project", "tasks", Relation.SOURCE_TO_TARGET, Relation.SOURCE_TO_TARGET);

		// post document: expect success (Project -> Task)
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.body("{\"name\":\"Project1\",\"tasks\":[{\"name\":\"Task1\"}]}")
			.expect()
			.statusCode(201)
			.when()
			.post("/projects");

		// post document: expect failure (Task -> Project)
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.body("{\"name\":\"Task2\",\"project\":{\"name\":\"Project2\"}}")
			.expect()
			.statusCode(422)
			.when()
			.post("/tasks");


	}

	@Test
	public void testSchemaAutocreateTargetToSource() {

		final String projectNodeId     = createSchemaNode("Project", new Pair("_name", "+String!"));
		final String taskNodeId        = createSchemaNode("Task",    new Pair("_name", "+String"));

		createSchemaRelationships(projectNodeId, taskNodeId, "TASK", "1", "*", "project", "tasks", Relation.TARGET_TO_SOURCE, Relation.SOURCE_TO_TARGET);

		// post document: expect success (Project -> Task)
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.body("{\"name\":\"Project1\",\"tasks\":[{\"name\":\"Task1\"}]}")
			.expect()
			.statusCode(422)
			.when()
			.post("/projects");

		// post document: expect failure (Task -> Project)
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.body("{\"name\":\"Task2\",\"project\":{\"name\":\"Project2\"}}")
			.expect()
			.statusCode(201)
			.when()
			.post("/tasks");


	}

	@Test
	public void testSchemaAutocreateAlways() {

		final String projectNodeId     = createSchemaNode("Project", new Pair("_name", "+String!"));
		final String taskNodeId        = createSchemaNode("Task",    new Pair("_name", "+String"));

		createSchemaRelationships(projectNodeId, taskNodeId, "TASK", "1", "*", "project", "tasks", Relation.ALWAYS, Relation.SOURCE_TO_TARGET);

		// post document: expect success (Project -> Task)
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.body("{\"name\":\"Project1\",\"tasks\":[{\"name\":\"Task1\"}]}")
			.expect()
			.statusCode(201)
			.when()
			.post("/projects");

		// post document: expect failure (Task -> Project)
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.body("{\"name\":\"Task2\",\"project\":{\"name\":\"Project2\"}}")
			.expect()
			.statusCode(201)
			.when()
			.post("/tasks");
	}

	@Test
	public void testSimpleDocumentWithSchema() {

		final String projectNodeId     = createSchemaNode("Project", new Pair("_name", "+String!"));
		final String taskNodeId        = createSchemaNode("Task",    new Pair("_name", "+String"));
		final String workerNodeId      = createSchemaNode("Worker",  new Pair("_name", "+String!"));

		// create relationships
		createSchemaRelationships(projectNodeId, taskNodeId, "TASK",    "1", "*", "project",    "tasks",    Relation.SOURCE_TO_TARGET, Relation.SOURCE_TO_TARGET);
		createSchemaRelationships(taskNodeId, taskNodeId,    "SUBTASK", "1", "*", "parentTask", "subtasks", Relation.SOURCE_TO_TARGET, Relation.SOURCE_TO_TARGET);
		createSchemaRelationships(workerNodeId, taskNodeId,  "WORKS",   "1", "*", "worker",     "tasks",    Relation.ALWAYS,           Relation.SOURCE_TO_TARGET);

		// create views
		RestAssured.given().contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.body("{ \"__test\": \"_name, _tasks\" }")
			.expect().statusCode(200).when().put("/schema_nodes/" + projectNodeId);

		RestAssured.given().contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.body("{ \"__test\": \"_name, _project, _subtasks, _parentTask, _worker\" }")
			.expect().statusCode(200).when().put("/schema_nodes/" + taskNodeId);

		RestAssured.given().contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.body("{ \"__test\": \"_name, _tasks\" }")
			.expect().statusCode(200).when().put("/schema_nodes/" + workerNodeId);

		// post document
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.body("{\"name\":\"Project1\",\"tasks\":[{\"name\":\"Task1\",\"worker\":{\"name\":\"Worker1\"},\"subtasks\":[{\"name\":\"Subtask1.1\",\"worker\":{\"name\":\"Worker1\"}},{\"name\":\"Subtask1.2\",\"worker\":{\"name\":\"Worker2\"}}]}]}")
			.expect()
			.statusCode(201)
			.when()
			.post("/projects");

		// check result
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
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
			.get("/projects/test?name=Project1");

		// post document
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.body("{\"name\":\"Project2\",\"tasks\":[{\"name\":\"Task2\",\"worker\":{\"name\":\"Worker2\"},\"subtasks\":[{\"name\":\"Subtask2.1\",\"worker\":{\"name\":\"Worker2\"}},{\"name\":\"Subtask2.2\",\"worker\":{\"name\":\"Worker3\"}}]}]}")
			.expect()
			.statusCode(201)
			.when()
			.post("/projects");

		// check result
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
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
			.get("/projects/test?name=Project2");

		// check workers
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
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
			.get("/workers/test?name=Worker2");

	}

	@Test
	public void testComplexDocumentWithSchema() {

		final String projectNodeId     = createSchemaNode("Project", new Pair("_name", "+String!"));
		final String taskNodeId        = createSchemaNode("Task",    new Pair("_name", "+String"));
		final String workerNodeId      = createSchemaNode("Worker",  new Pair("_name", "+String!"));
		final String companyNodeId     = createSchemaNode("Company", new Pair("_name", "+String!"));

		// create relationships
		createSchemaRelationships(projectNodeId, taskNodeId,   "TASK",     "1", "*", "project",    "tasks",    Relation.ALWAYS, Relation.SOURCE_TO_TARGET);
		createSchemaRelationships(taskNodeId, taskNodeId,      "SUBTASK",  "1", "*", "parentTask", "subtasks", Relation.ALWAYS, Relation.SOURCE_TO_TARGET);
		createSchemaRelationships(workerNodeId, taskNodeId,    "WORKS_ON", "1", "*", "worker",     "tasks",    Relation.ALWAYS, Relation.SOURCE_TO_TARGET);
		createSchemaRelationships(workerNodeId, companyNodeId, "WORKS_AT", "*", "1", "workers",    "company",  Relation.ALWAYS, Relation.SOURCE_TO_TARGET);

		// create views
		RestAssured.given().contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.body("{ \"__test\": \"_name, _tasks\" }")
			.expect().statusCode(200).when().put("/schema_nodes/" + projectNodeId);

		RestAssured.given().contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.body("{ \"__test\": \"_name, _subtasks, _worker\" }")
			.expect().statusCode(200).when().put("/schema_nodes/" + taskNodeId);

		RestAssured.given().contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.body("{ \"__test\": \"_name, _tasks, _company\" }")
			.expect().statusCode(200).when().put("/schema_nodes/" + workerNodeId);

		RestAssured.given().contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.body("{ \"__test\": \"_name, _workers\" }")
			.expect().statusCode(200).when().put("/schema_nodes/" + companyNodeId);

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

		System.out.println(jsonBody);

		// post document
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.body(jsonBody)
			.expect()
			.statusCode(201)
			.when()
			.post("/projects");



		// JSON output depth limits our ability to check the result here, so we need to check separately


		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
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
			.get("/projects/test?name=Project1");

		// check Task1
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
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
			.get("/tasks/test?name=Task1");

		// check Task2
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.body("result",			                               hasSize(1))
			.body("result_count",		                               equalTo(1))

			.body("result[0].name",                                        equalTo("Task2"))
			.body("result[0].worker.name",                                 equalTo("Worker2"))
			.body("result[0].worker.company.name",                         equalTo("Company1"))

			.when()
			.get("/tasks/test?name=Task2");


		// check Task3
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.body("result",			                               hasSize(1))
			.body("result_count",		                               equalTo(1))

			.body("result[0].name",                                        equalTo("Task3"))
			.body("result[0].worker.name",                                 equalTo("Worker3"))
			.body("result[0].worker.company.name",                         equalTo("Company2"))

			.when()
			.get("/tasks/test?name=Task3");


		// check Task4
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
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
			.get("/tasks/test?name=Task4");


		// check Task5
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
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
			.get("/tasks/test?name=Task5");


		// check Subtask5.1
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
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
			.get("/tasks/test?name=Subtask5.1");


		// check Subtask5.2
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
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
			.get("/tasks/test?name=Subtask5.2");


		// check companies
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.body("result",		hasSize(3))
			.body("result_count",	equalTo(3))

			.body("result[0].name", equalTo("Company1"))
			.body("result[1].name", equalTo("Company2"))
			.body("result[2].name", equalTo("Company3"))

			.when()
			.get("/companies/test");

		// check workers
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
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
			.get("/workers/test");

	}

	private String createSchemaNode(final String name, Pair... properties) {

		final StringBuilder buf = new StringBuilder();

		// append name
		buf.append("{ \"name\": \"");
		buf.append(name);
		buf.append("\"");

		for (final Pair pair : properties) {

			final boolean isString = pair.value instanceof String;

			buf.append(", \"");
			buf.append(pair.key);
			buf.append("\": ");

			if (isString) {
				buf.append("\"");
			}

			buf.append(pair.value);

			if (isString) {
				buf.append("\"");
			}
		}

		buf.append(" }");

		return createEntity("/schema_nodes", buf.toString());
	}

	// ----- private methods -----
	private String createSchemaRelationships(final String sourceId, final String targetId, final String relationshipType, final String sourceMultiplicity, final String targetMultiplicity, final String sourceJsonName, final String targetJsonName, final int autocreate, final int cascadingDelete) {

		return createEntity(
			"/schema_relationship_nodes",
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

	private static class Pair {

		public String key = null;
		public Object value = null;

		public Pair(final String key, final Object value) {
			this.key = key;
			this.value = value;
		}
	}
}
