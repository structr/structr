/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.javaparser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.type.Type;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.structr.api.service.LicenseManager;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.function.Functions;
import org.structr.module.StructrModule;
import org.structr.schema.action.Actions;

/**
 *
 */
public class JavaParserModule implements StructrModule {

	@Override
	public void onLoad(final LicenseManager licenseManager) {

//		final boolean basicEdition         = licenseManager == null || licenseManager.isEdition(LicenseManager.Basic);
//		final boolean smallBusinessEdition = licenseManager == null || licenseManager.isEdition(LicenseManager.SmallBusiness);
		final boolean enterpriseEdition    = licenseManager == null || licenseManager.isEdition(LicenseManager.Enterprise);
		
		// Enterprise only
		Functions.put(enterpriseEdition, LicenseManager.Enterprise, "parse_java",                      new ParseJavaFunction());
	}

	public JsonResult parse(final String javaCode) {
		return analyze(JavaParser.parse(javaCode));
	}
	
	public JsonResult parse(final InputStream javaCode) {
		return analyze(JavaParser.parse(javaCode));
	}

	private JsonResult analyze(final CompilationUnit cu) {
		
		JsonResult jsonResult = new JsonResult();

		NodeList<TypeDeclaration<?>> types = cu.getTypes();
		
		if (types.isEmpty()) {
			return jsonResult;
		}
		
		TypeDeclaration<?>            type = types.get(0);

		jsonResult.addName(type);
		jsonResult.addModifiers(type);

		final Optional<PackageDeclaration> pkg = cu.getPackageDeclaration();

		if (pkg.isPresent()) {
			jsonResult.addPackage(pkg.get());
		}

		final List<BodyDeclaration<?>> members = type.getMembers();

		List<JsonResult> membersList = new ArrayList<>();

		members.forEach((t) -> {

			JsonResult member = new JsonResult();

			if (t instanceof FieldDeclaration) {

				final FieldDeclaration fd = t.asFieldDeclaration();

				member.addName(fd.getVariable(0));
				member.addType(fd.getVariable(0).getType());
				member.addModifiers(fd);

			} else if (t instanceof CallableDeclaration) {

				final CallableDeclaration callable = t.asCallableDeclaration();

				if (t instanceof ConstructorDeclaration) {

					final ConstructorDeclaration cd = t.asConstructorDeclaration();

					member.addName(cd);
					member.isConstructor();
					member.addModifiers(cd);

				} else if (t instanceof MethodDeclaration) {

					final MethodDeclaration md = t.asMethodDeclaration();

					member.addName(md);
					member.isMethod();
					member.addReturnType(md.getType());
					member.addModifiers(md);

				}

				final NodeList<Parameter> parameters = callable.getParameters();

				List<JsonResult> parameterList = new ArrayList<>();

				parameters.forEach((p) -> {

					JsonResult param = new JsonResult();

					param.addName(p);
					param.addType(p.getType());
					param.addModifiers(p);

					parameterList.add(param);
				});

				member.addParameters(parameterList);

			}

			membersList.add(member);
		});

		jsonResult.addMembers(membersList);

		return jsonResult;		
	}
	
	@Override
	public String getName() {
		return "java-parser";
	}

	@Override
	public Set<String> getDependencies() {
		return new LinkedHashSet<>();
	}

	@Override
	public Set<String> getFeatures() {
		return null;
	}

	@Override
	public void insertImportStatements(final AbstractSchemaNode schemaNode, final StringBuilder buf) {
	}

	@Override
	public void insertSourceCode(final AbstractSchemaNode schemaNode, final StringBuilder buf) {
	}

	@Override
	public Set<String> getInterfacesForType(final AbstractSchemaNode schemaNode) {
		return null;
	}

	@Override
	public void insertSaveAction(final AbstractSchemaNode schemaNode, final StringBuilder buf, final Actions.Type type) {
	}
	
	
	public class JsonResult {

		private JsonObject obj = new JsonObject();

		private void addPackage(final PackageDeclaration pkg) {
			obj.add("package", new JsonPrimitive(getMemberName(pkg.getNameAsString())));
		}

		private void addName(final NodeWithSimpleName<?> namedNode) {
			obj.add("name", new JsonPrimitive(namedNode.getNameAsString()));
		}

		private void addType(final Type type) {
			obj.add("type", new JsonPrimitive(type.asString()));
		}

		private void addReturnType(final Type type) {
			obj.add("returnType", new JsonPrimitive(type.asString()));
		}

		private void isConstructor() {
			obj.add("type", new JsonPrimitive("constructor"));
		}
		
		private void isMethod() {
			obj.add("type", new JsonPrimitive("method"));
		}

		private void addModifiers(final NodeWithModifiers<?> type) {

			final JsonArray modifiers = new JsonArray();

			type.getModifiers().forEach((m) -> {
				modifiers.add(new JsonPrimitive(m.asString()));
			});		

			obj.add("modifiers", modifiers);
		}
		
		private void addMembers(final List<JsonResult> list) {
			final JsonArray members = new JsonArray();
			list.forEach((m) -> {
				members.add(m.get());
			});
			obj.add("members", members);
		}
		
		private void addParameters(final List<JsonResult> list) {
			final JsonArray parameters = new JsonArray();
			list.forEach((m) -> {
				parameters.add(m.get());
			});
			obj.add("parameters", parameters);
		}

		private void add(final JsonResult obj) {
			obj.add(obj);
		}
		
		public JsonObject get() {
			return obj;
		}
	}

	private String getMemberName(final String rawName) {
		final String[] parts = StringUtils.split(rawName, " ");
		return StringUtils.strip(StringUtils.remove(parts[parts.length-1], ";"));
	}	
}
