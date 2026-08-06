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

import org.structr.process.bpmn.interop.VendorFormField;
import org.structr.process.bpmn.interop.VendorTaskForm;
import org.structr.process.bpmn.interop.ZeebeFormAdapter;
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
 * Pure-DOM/JSON unit test for {@link ZeebeFormAdapter}: a user task's {@code zeebe:formDefinition}
 * is resolved to the embedded {@code zeebe:userTaskForm} form-js schema, and its input components
 * become fields (layout components without a key are skipped; required/readonly are honored).
 */
public class ZeebeFormAdapterTest {

	private static final String ZEEBE_NS = "http://camunda.org/schema/zeebe/1.0";

	@Test
	public void testAppliesToByNamespace() {

		final ZeebeFormAdapter adapter = new ZeebeFormAdapter();
		assertTrue(adapter.appliesTo(Set.of(ZEEBE_NS)));
		assertFalse(adapter.appliesTo(Set.of("http://www.omg.org/spec/BPMN/20100524/MODEL")));
	}

	@Test
	public void testResolvesFormDefinitionAndParsesFormJs() throws Exception {

		final String formJson =
			"{ \"components\": ["
			+ "  { \"type\": \"textfield\", \"key\": \"title\", \"label\": \"Title\", \"validate\": { \"required\": true } },"
			+ "  { \"type\": \"number\",    \"key\": \"amount\", \"label\": \"Amount\" },"
			+ "  { \"type\": \"checkbox\",  \"key\": \"approved\", \"readonly\": true },"
			+ "  { \"type\": \"text\",      \"text\": \"just a label, no key\" }"
			+ "] }";

		final String xml =
			"<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:zeebe=\"" + ZEEBE_NS + "\">"
			+ "  <bpmn:process id=\"proc\">"
			+ "    <bpmn:extensionElements>"
			+ "      <zeebe:userTaskForm id=\"form_1\">" + xmlEscape(formJson) + "</zeebe:userTaskForm>"
			+ "    </bpmn:extensionElements>"
			+ "    <bpmn:userTask id=\"Task_A\">"
			+ "      <bpmn:extensionElements>"
			+ "        <zeebe:formDefinition formKey=\"camunda-forms:bpmn:form_1\"/>"
			+ "      </bpmn:extensionElements>"
			+ "    </bpmn:userTask>"
			+ "  </bpmn:process>"
			+ "</bpmn:definitions>";

		final List<VendorTaskForm> forms = new ZeebeFormAdapter().extractForms(parseProcess(xml));
		assertEquals(1, forms.size());

		final List<VendorFormField> fields = forms.get(0).fields();
		// the layout "text" component has no key and is skipped
		assertEquals(3, fields.size());

		assertEquals("title", fields.get(0).key());
		assertEquals("String", fields.get(0).structrType());
		assertTrue(fields.get(0).required());

		assertEquals("amount", fields.get(1).key());
		assertEquals("form-js number -> Double", "Double", fields.get(1).structrType());

		assertEquals("approved", fields.get(2).key());
		assertEquals("Boolean", fields.get(2).structrType());
		assertTrue("readonly component is read-only", fields.get(2).readOnly());
	}

	@Test
	public void testUnknownFormReferenceYieldsNoFields() throws Exception {

		final String xml =
			"<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:zeebe=\"" + ZEEBE_NS + "\">"
			+ "  <bpmn:process id=\"proc\">"
			+ "    <bpmn:extensionElements>"
			+ "      <zeebe:userTaskForm id=\"form_1\">{ \"components\": [ { \"type\": \"textfield\", \"key\": \"x\" } ] }</zeebe:userTaskForm>"
			+ "    </bpmn:extensionElements>"
			+ "    <bpmn:userTask id=\"Task_A\">"
			+ "      <bpmn:extensionElements>"
			+ "        <zeebe:formDefinition formKey=\"camunda-forms:bpmn:does_not_exist\"/>"
			+ "      </bpmn:extensionElements>"
			+ "    </bpmn:userTask>"
			+ "  </bpmn:process>"
			+ "</bpmn:definitions>";

		assertTrue(new ZeebeFormAdapter().extractForms(parseProcess(xml)).isEmpty());
	}

	private static String xmlEscape(final String s) {

		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static Element parseProcess(final String xml) throws Exception {

		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		final Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

		return (Element) doc.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "process").item(0);
	}
}
