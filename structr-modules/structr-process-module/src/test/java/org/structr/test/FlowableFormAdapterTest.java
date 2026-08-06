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

import org.structr.process.bpmn.interop.FlowableFormAdapter;
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
 * Pure-DOM unit test for {@link FlowableFormAdapter}: Flowable {@code flowable:formProperty} (and
 * the legacy Activiti namespace) translate into the vendor-neutral form model, honoring the
 * {@code required} and {@code writable="false"} (read-only) flags.
 */
public class FlowableFormAdapterTest {

	private static final String FLOWABLE_NS = "http://flowable.org/bpmn";
	private static final String ACTIVITI_NS = "http://activiti.org/bpmn";

	@Test
	public void testAppliesToEitherNamespace() {

		final FlowableFormAdapter adapter = new FlowableFormAdapter();
		assertTrue(adapter.appliesTo(Set.of(FLOWABLE_NS)));
		assertTrue(adapter.appliesTo(Set.of(ACTIVITI_NS)));
		assertFalse(adapter.appliesTo(Set.of("http://camunda.org/schema/1.0/bpmn")));
	}

	@Test
	public void testExtractsFlowableFormProperties() throws Exception {

		final String xml =
			"<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:flowable=\"" + FLOWABLE_NS + "\">"
			+ "  <bpmn:process id=\"proc\">"
			+ "    <bpmn:userTask id=\"Task_A\">"
			+ "      <bpmn:extensionElements>"
			+ "        <flowable:formProperty id=\"title\"  name=\"Title\"  type=\"string\"  required=\"true\"/>"
			+ "        <flowable:formProperty id=\"amount\" name=\"Amount\" type=\"long\"/>"
			+ "        <flowable:formProperty id=\"ref\"    name=\"Ref\"    type=\"string\" writable=\"false\"/>"
			+ "      </bpmn:extensionElements>"
			+ "    </bpmn:userTask>"
			+ "  </bpmn:process>"
			+ "</bpmn:definitions>";

		final List<VendorTaskForm> forms = new FlowableFormAdapter().extractForms(parseProcess(xml));
		assertEquals(1, forms.size());

		final List<VendorFormField> fields = forms.get(0).fields();
		assertEquals(3, fields.size());

		assertEquals("title", fields.get(0).key());
		assertEquals("String", fields.get(0).structrType());
		assertTrue(fields.get(0).required());
		assertFalse(fields.get(0).readOnly());

		assertEquals("Long", fields.get(1).structrType());

		assertEquals("ref", fields.get(2).key());
		assertTrue("writable=false means read-only", fields.get(2).readOnly());
	}

	@Test
	public void testActivitiNamespaceAlsoWorks() throws Exception {

		final String xml =
			"<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:activiti=\"" + ACTIVITI_NS + "\">"
			+ "  <bpmn:process id=\"proc\">"
			+ "    <bpmn:userTask id=\"Task_A\">"
			+ "      <bpmn:extensionElements>"
			+ "        <activiti:formProperty id=\"note\" type=\"string\"/>"
			+ "      </bpmn:extensionElements>"
			+ "    </bpmn:userTask>"
			+ "  </bpmn:process>"
			+ "</bpmn:definitions>";

		final List<VendorTaskForm> forms = new FlowableFormAdapter().extractForms(parseProcess(xml));
		assertEquals(1, forms.size());
		assertEquals("note", forms.get(0).fields().get(0).key());
	}

	private static Element parseProcess(final String xml) throws Exception {

		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		final Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		return (Element) doc.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "process").item(0);
	}
}
