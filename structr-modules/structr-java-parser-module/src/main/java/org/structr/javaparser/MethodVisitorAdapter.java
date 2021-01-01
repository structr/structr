/*
 * Copyright (C) 2010-2021 Structr GmbH
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

import com.drew.lang.Iterables;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.structr.core.app.App;
import static org.structr.javaparser.JavaParserModule.logger;
import org.structr.javaparser.entity.JavaClass;
import org.structr.javaparser.entity.Method;

public class MethodVisitorAdapter extends VoidVisitorAdapter<Object> {

	@Override
	public void visit(final MethodCallExpr methodCall, final Object arg) {

		final Map<String, Object> params = (HashMap) arg;

		final String          clsName = (String)           params.get("clsName");
		final JavaParserFacade facade = (JavaParserFacade) params.get("facade");
		final App                 app = (App)              params.get("app");

		logger.info("###### " + clsName + ": " + methodCall.getName());

		try {
			////// !!!!!!!!!!! Methoden-Aufruf kann in den meisten Fällen nicht aufgelöst werden!!
			final SymbolReference<ResolvedMethodDeclaration> ref = facade.solve(methodCall);

			if (ref.isSolved()) {

				final String qualifiedSignature = ref.getCorrespondingDeclaration().getQualifiedSignature();
				//final String scopeString = scope.toString();
				final String parentNodeAsString = methodCall.getParentNode().toString();
				//logger.info("Resolved to " + qualifiedSignature + ", scope: " + scopeString + ", parent node: " + parentNodeAsString);
				logger.info("Resolved to " + qualifiedSignature + ", parent node: " + parentNodeAsString);

				final String calledMethodQualifiedName      = StringUtils.replacePattern(qualifiedSignature, "\\(.*\\)", "");
				final String calledMethodQualifiedClassName = StringUtils.substringBeforeLast(calledMethodQualifiedName, ".");
				final String calledMethodName               = StringUtils.substringAfterLast(calledMethodQualifiedName, ".");

				Method calledMethod = null;

				final JavaClass calledMethodClass = (JavaClass) app.nodeQuery(JavaClass.class).and(JavaClass.name, calledMethodQualifiedClassName).getFirst();
				if (calledMethodClass != null) {

					logger.info("└ Found called class in graph: " + calledMethodClass.getName());
					calledMethod = (Method) app.nodeQuery(Method.class).and(Method.name, calledMethodName).and(Method.classOrInterface, calledMethodClass).getFirst();

					if (calledMethod != null) {
						logger.info("└ Found called method in graph: " + calledMethod.getProperty(Method.declaration));

						final Optional<MethodDeclaration> callingMethod = methodCall.getAncestorOfType(MethodDeclaration.class);
						if (callingMethod.isPresent()) {

							final String callingMethodDeclaration = callingMethod.get().getDeclarationAsString();

							logger.info("└ Calling method: " + callingMethodDeclaration);

							final String callingMethodName = callingMethod.get().getNameAsString();
							final Optional<TypeDeclaration> typeDecl = callingMethod.get().getAncestorOfType(TypeDeclaration.class);

							if (typeDecl.isPresent()) {
								final String callingMethodClassName = typeDecl.get().getNameAsString();

								// Find compilation unit
								final Optional<CompilationUnit> localCU = typeDecl.get().getAncestorOfType(CompilationUnit.class);
								if (localCU.isPresent()) {

									// Does it have a package declaration?
									final Optional<PackageDeclaration> packageDecl = localCU.get().getPackageDeclaration();
									if (packageDecl.isPresent()) {

										// Assemble qualified class name
										final String packageName = packageDecl.get().getNameAsString();
										final String fqcn = packageName + "." + callingMethodClassName;

										// Find class in graph
										final JavaClass callingClass = (JavaClass) app.nodeQuery(JavaClass.class).and(JavaClass.name, fqcn).getFirst();
										if (callingClass != null) {

											final Method method = (Method) app.nodeQuery(Method.class).and(Method.name, callingMethodName).and(Method.classOrInterface, callingClass).getFirst();
											if (method != null) {

												logger.info("Found calling method in graph: " + method.getName());

												final List<Method> methodsCalled = Iterables.toList(method.getProperty(Method.methodsCalled));
												methodsCalled.add(calledMethod);

												method.setProperty(Method.methodsCalled, methodsCalled);

												logger.info("Added " + calledMethod.getName() + " to list of methods called in " + method.getName());
											}

										}
									}
								}

							}
						}


					}

				}

			}

		} catch (final Throwable t) {
			logger.info("Unable to resolve " + clsName, t);
		}
	}
}
