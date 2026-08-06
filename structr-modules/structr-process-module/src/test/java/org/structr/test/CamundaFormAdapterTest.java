/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test;

import org.structr.process.bpmn.interop.CamundaFormAdapter;
import org.structr.process.bpmn.interop.VendorFormField;
import org.structr.process.bpmn.interop.VendorTaskForm;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.testng.AssertJUnit.*;

/**
 * Pure-DOM unit test for {@link CamundaFormAdapter}: no graph, no services, so it runs without a
 * database. Verifies the Camunda-7 {@code camunda:formData/formField} dialect is translated into
 * the vendor-neutral form model (field key, type mapping, required/read-only), including user
 * tasks nested in a sub-process.
 */
public class CamundaFormAdapterTest {

	private static final String CAMUNDA_NS = "http://camunda.org/schema/1.0/bpmn";

	@Test
	public void testAppliesToByNamespace() {

		final CamundaFormAdapter adapter = new CamundaFormAdapter();

		assertTrue(adapter.appliesTo(Set.of(CAMUNDA_NS, "http://www.omg.org/spec/BPMN/20100524/MODEL")));
		assertFalse(adapter.appliesTo(Set.of("http://www.omg.org/spec/BPMN/20100524/MODEL")));
	}

	@Test
	public void testExtractsFormsIncludingSubprocessAndConstraints() throws Exception {

		final String xml =
			"<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:camunda=\"" + CAMUNDA_NS + "\">"
			+ "  <bpmn:process id=\"proc\">"
			+ "    <bpmn:userTask id=\"Task_A\" name=\"A\">"
			+ "      <bpmn:extensionElements>"
			+ "        <camunda:formData>"
			+ "          <camunda:formField id=\"title\" label=\"Title\" type=\"string\">"
			+ "            <camunda:validation><camunda:constraint name=\"required\"/></camunda:validation>"
			+ "          </camunda:formField>"
			+ "          <camunda:formField id=\"amount\" type=\"long\"/>"
			+ "          <camunda:formField id=\"approved\" type=\"boolean\">"
			+ "            <camunda:validation><camunda:constraint name=\"readonly\"/></camunda:validation>"
			+ "          </camunda:formField>"
			+ "        </camunda:formData>"
			+ "      </bpmn:extensionElements>"
			+ "    </bpmn:userTask>"
			+ "    <bpmn:userTask id=\"Task_NoForm\" name=\"No form\"/>"
			+ "    <bpmn:subProcess id=\"Sub\">"
			+ "      <bpmn:userTask id=\"Task_Nested\" name=\"Nested\">"
			+ "        <bpmn:extensionElements>"
			+ "          <camunda:formData>"
			+ "            <camunda:formField id=\"note\" type=\"string\"/>"
			+ "          </camunda:formData>"
			+ "        </bpmn:extensionElements>"
			+ "      </bpmn:userTask>"
			+ "    </bpmn:subProcess>"
			+ "  </bpmn:process>"
			+ "</bpmn:definitions>";

		final Element processEl = parseProcess(xml);
		final List<VendorTaskForm> forms = new CamundaFormAdapter().extractForms(processEl);

		// Task_A and Task_Nested carry forms; Task_NoForm does not.
		assertEquals("expected forms for the two form-bearing user tasks", 2, forms.size());

		final VendorTaskForm taskA = forms.stream().filter(f -> f.taskBpmnId().equals("Task_A")).findFirst().orElseThrow();
		assertEquals(3, taskA.fields().size());

		final VendorFormField title = taskA.fields().get(0);
		assertEquals("title", title.key());
		assertEquals("Title", title.label());
		assertEquals("String", title.structrType());
		assertTrue("title has a required constraint", title.required());
		assertFalse(title.readOnly());

		assertEquals("camunda long -> Structr Long", "Long", taskA.fields().get(1).structrType());

		final VendorFormField approved = taskA.fields().get(2);
		assertEquals("Boolean", approved.structrType());
		assertTrue("approved has a readonly constraint", approved.readOnly());

		final VendorTaskForm nested = forms.stream().filter(f -> f.taskBpmnId().equals("Task_Nested")).findFirst().orElseThrow();
		assertEquals(1, nested.fields().size());
		assertEquals("note", nested.fields().get(0).key());
	}

	@Test
	public void testNoCamundaFormsYieldsEmptyList() throws Exception {

		final String xml =
			"<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\">"
			+ "  <bpmn:process id=\"proc\"><bpmn:userTask id=\"T\" name=\"T\"/></bpmn:process>"
			+ "</bpmn:definitions>";

		assertTrue(new CamundaFormAdapter().extractForms(parseProcess(xml)).isEmpty());
	}

	private static Element parseProcess(final String xml) throws Exception {

		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		final Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

		return (Element) doc.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "process").item(0);
	}
}
