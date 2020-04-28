/**
 * Copyright (C) 2010-2020 Structr GmbH
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
import com.github.javaparser.ast.nodeTypes.NodeWithOptionalBlockStmt;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.service.LicenseManager;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.function.Functions;
import org.structr.core.function.XmlFunction;
import org.structr.core.property.PropertyMap;
import org.structr.javaparser.entity.AddJarsToIndexFunction;
import org.structr.javaparser.entity.ClassOrInterface;
import org.structr.javaparser.entity.JavaClass;
import org.structr.javaparser.entity.JavaInterface;
import org.structr.javaparser.entity.Method;
import org.structr.javaparser.entity.Module;
import org.structr.javaparser.entity.Package;
import org.structr.module.StructrModule;
import org.structr.schema.SourceFile;
import org.structr.schema.action.Actions;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 */
public class JavaParserModule implements StructrModule {

	protected static final Logger logger = LoggerFactory.getLogger(JavaParserModule.class.getName());

	private final StructrJavaTypeSolver structrTypeSolver;
	private JavaParserFacade            facade;
	private App                         app;

	private boolean ignoreTests = true;

	public JavaParserModule() {
		structrTypeSolver = StructrJavaTypeSolver.getInstance();
	}

	public JavaParserModule(final App app) {
		this.app = app;
		structrTypeSolver = StructrJavaTypeSolver.getInstance();
	}

	@Override
	public void onLoad(final LicenseManager licenseManager) {
	}

	@Override
	public void registerModuleFunctions(final LicenseManager licenseManager) {

		Functions.put(licenseManager, new IndexSourceTreeFunction());
		Functions.put(licenseManager, new ParseSourceTreeFunction());
		Functions.put(licenseManager, new AnalyzeSourceTreeFunction());
		Functions.put(licenseManager, new ParseJavaFunction());
		Functions.put(licenseManager, new AnalyzeJavaFunction());
		Functions.put(licenseManager, new AddJarsToIndexFunction());
	}

	/**
	 * Create an index containing all compilation units of Java files from
	 * the source tree under the given root folder.
	 *
	 * @param rootFolder
	 */
	public void indexSourceTree(final Folder rootFolder) {

		logger.info("Starting indexing of source tree " + rootFolder.getPath());

		final SecurityContext securityContext = rootFolder.getSecurityContext();
		app = StructrApp.getInstance(securityContext);

		structrTypeSolver.parseRoot(rootFolder);

		final CombinedTypeSolver typeSolver = new CombinedTypeSolver();
		typeSolver.add(new ReflectionTypeSolver());
		typeSolver.add(structrTypeSolver);

		facade = JavaParserFacade.get(typeSolver);

		logger.info("Done with indexing of source tree " + rootFolder.getPath());
	}

	/**
	 * Add compilation units of all jar files found in the given folder to the index.
	 *
	 * @param folderPath
	 */
	public void addJarsToIndex(final String folderPath) {

		logger.info("Starting adding jar files in " + folderPath);

		final CombinedTypeSolver typeSolver = new CombinedTypeSolver();
		typeSolver.add(new ReflectionTypeSolver());

		final AtomicLong count = new AtomicLong(0);

		try {
			Files.newDirectoryStream(Paths.get(folderPath), path -> path.toString().endsWith(".jar")).forEach((file) -> {
				try {
					typeSolver.add(new JarTypeSolver(new FileInputStream(file.toFile())));
					count.addAndGet(1L);

				} catch (IOException ex) {}
			});

		} catch (IOException ex) {}

		logger.info("Added " + count.toString() + " jar files to the type solver");

		typeSolver.add(structrTypeSolver);

		facade = JavaParserFacade.get(typeSolver);

		logger.info("Done with adding jar files in " + folderPath);
	}


	/**
	 * Analyze the source tree under the given root folder.
	 *
	 * @param rootFolder
	 */
	public void analyzeSourceTree(final Folder rootFolder) {

		logger.info("Starting analysis of source tree " + rootFolder.getPath());

		final SecurityContext securityContext = rootFolder.getSecurityContext();
		app = StructrApp.getInstance(securityContext);

		final CombinedTypeSolver typeSolver = new CombinedTypeSolver();
		typeSolver.add(new ReflectionTypeSolver());
		typeSolver.add(structrTypeSolver);
		facade = JavaParserFacade.get(typeSolver);

		analyzeFolder(rootFolder, 0, null);

		logger.info("Done with analysis of source tree " + rootFolder.getPath());
	}

	/**
	 * Parse the source tree under the given root folder.
	 *
	 * @param rootFolder
	 */
	public void parseSourceTree(final Folder rootFolder) {

		logger.info("Starting parsing of source tree " + rootFolder.getPath());

		final SecurityContext securityContext = rootFolder.getSecurityContext();
		app = StructrApp.getInstance(securityContext);

		final CombinedTypeSolver typeSolver = new CombinedTypeSolver();
		typeSolver.add(new ReflectionTypeSolver());
		typeSolver.add(structrTypeSolver);
		facade = JavaParserFacade.get(typeSolver);

		parseFolder(rootFolder, 0, null);

		logger.info("Done with parsing of source tree " + rootFolder.getPath());
	}

	public JsonResult parse(final String javaCode) {
		return toJson(JavaParser.parse(javaCode));
	}

	public JsonResult parse(final InputStream javaCode) {
		return toJson(JavaParser.parse(javaCode));
	}

	/**
	 * Get or create on object with given properties.
	 *
	 * Try to find an object of given type matching all of the given identifying properties.
	 * If none found, create one with all given properties.
	 *
	 * @param type
	 * @param identifyingProperties
	 * @param allProperties
	 * @return
	 */
	private GraphObject getOrCreate(final Class type, final PropertyMap identifyingProperties, final PropertyMap allProperties) {

		try {
			final GraphObject obj = app.nodeQuery(type).disableSorting().pageSize(1).and(identifyingProperties).getFirst();
			if (obj != null) {

				// return existing object
				return obj;
			}

			// create new object
			return app.create(type, allProperties);

		} catch (FrameworkException ex) {
			logger.error("Unable to create new graph object", type, allProperties, ex);
		}

		return null;
	}

	private Module createModule(final String moduleName, final Folder moduleFolder) {

		final PropertyMap identifyingProperties  = new PropertyMap();
		identifyingProperties.put(Module.name,   moduleName);

		final PropertyMap allProperties  = new PropertyMap();
		allProperties.putAll(identifyingProperties);
		allProperties.put(Module.folder, moduleFolder);

		return (Module) getOrCreate(Module.class, identifyingProperties, allProperties);
	}

	private void parseFolder(final Folder folder, final int depth, final Folder parentFolder) {

		logger.info("Parsing folder " + folder.getName() + ", depth " + depth);

		// Handle folder itself
		readPomAndPackageInfoFile(folder, parentFolder);

		// Handle all direct file children of this folder
		parseJavaFilesAndSolveTypes(folder);

		// Handle subfolders of this folder
		for (final Folder subfolder : folder.getFolders()) {
			parseFolder(subfolder, depth+1, folder);
		}
	}

	private void analyzeFolder(final Folder folder, final int depth, final Folder parentFolder) {

		logger.info("Analyzing folder " + folder.getName() + ", depth " + depth);

		// Handle folder itself
		//handleAnalyzeFolder(folder, parentFolder);

		// Handle all direct file children of this folder
		analyzeMethodsInJavaFiles(folder);

		// Handle subfolders of this folder
		folder.getFolders().forEach((final Folder subfolder) -> {
			analyzeFolder(subfolder, depth+1, folder);
		});
	}

	private void readPomAndPackageInfoFile(final Folder folder, final Folder parentFolder) {

		try {

			final File pomFile = app.nodeQuery(File.class).andName("pom.xml").and(StructrApp.key(File.class, "parent"), folder).getFirst();
			if (pomFile != null) {

				handlePomFile(pomFile, folder, parentFolder);
			}

			final File packageFile = app.nodeQuery(File.class).andName("package-info.java").and(StructrApp.key(Folder.class, "parent"), folder).getFirst();
			if (packageFile != null) {

				handlePackageFolder(folder, parentFolder);
			}

		} catch (FrameworkException ex) {
			logger.error("Error in node query", ex);
		}
	}

	private void parseJavaFilesAndSolveTypes(final Folder folder) {

		if (ignoreTests && "test".equals(folder.getName())) {
			return;
		}

		for (final File file : folder.getFiles()) {

			if (file.getContentType().equals("text/x-java")) {

				final String javaFileName = file.getName();

				if (javaFileName.equals("package-info.java") || javaFileName.equals("testPackage-info.java")) {

				} else {

					final String javaContent = file.getFavoriteContent();

					ClassOrInterface clsOrIface = null;

					CompilationUnit cu = null;
					try {
						cu = JavaParser.parse(javaContent);

						for (final TypeDeclaration type : cu.findAll(TypeDeclaration.class)) {

							SymbolReference<? extends ResolvedValueDeclaration> decl = facade.solve(type.getName());

							if (type.isClassOrInterfaceDeclaration()) {

								org.structr.javaparser.entity.Package pkg = null;
								if (cu.getPackageDeclaration().isPresent()) {

									pkg = handlePackage(cu.getPackageDeclaration().get());
								}

								clsOrIface = handleClassOrInterface(type, pkg);

							}
						}

						for (final BodyDeclaration t : cu.findAll(BodyDeclaration.class)) {

//								if (t instanceof FieldDeclaration) {
//
//									final FieldDeclaration fd = t.asFieldDeclaration();
//
//									final String fieldName = fd.getVariable(0).getNameAsString();
//									logger.info("Field found: " + fieldName);
//
//									final SymbolReference<ResolvedReferenceTypeDeclaration> fieldRef = typeSolver.tryToSolveType(fieldName);
//									if (fieldRef.isSolved()) {
//
//										final ResolvedReferenceTypeDeclaration decl = fieldRef.getCorrespondingDeclaration();
//										if (decl.isField()) {
//
//											final ResolvedFieldDeclaration field = decl.asField();
//											if (field.isMethod()) {
//
//												logger.info("Solved method found: " + field.asMethod().getName());
//
//											} else if (field.isField()) {
//
//												logger.info("Solved field found: " + field.getName() + ", declared by", field.declaringType().getName());
//											}
//										}
//									}
//								}

							if (t instanceof CallableDeclaration) {

								final CallableDeclaration callable = t.asCallableDeclaration();

								if (t instanceof ConstructorDeclaration) {

//										final ConstructorDeclaration cd = t.asConstructorDeclaration();
//										logger.info("Constructor found: " + cd.getNameAsString());
//
//										final SymbolReference<ResolvedReferenceTypeDeclaration> constructorRef = typeSolver.tryToSolveType(cd.getNameAsString());
//										if (constructorRef.isSolved()) {
//
//											logger.info("Solved constructor: " + cd.getNameAsString());
//											//final ResolvedReferenceTypeDeclaration decl = constructorRef.getCorrespondingDeclaration();
//										}

								} else if (t instanceof MethodDeclaration) {

									final MethodDeclaration md = t.asMethodDeclaration();

									final String methodName = md.getNameAsString();

									logger.info("Method found: " + methodName);

									// Create methods and link to class
									final PropertyMap identifyingMethodProperties = new PropertyMap();
									identifyingMethodProperties.put(Method.name, methodName);

									final PropertyMap methodProperties       = new PropertyMap();
									methodProperties.putAll(identifyingMethodProperties);
									methodProperties.put(Method.classOrInterface, clsOrIface);
									methodProperties.put(Method.declaration, md.getDeclarationAsString());

									final Optional<BlockStmt> block = md.getBody();
									if (block.isPresent()) {
										methodProperties.put(Method.body, block.get().toString());
									}

									final String symbolName              = StringUtils.substringAfterLast(clsOrIface.getName(), ".") + "." + md.getNameAsString();
									//final String fullQualifiedSymbolName = cls.getProperty(JavaClass.packageProp).getName() + "." + symbolName;

									try {
										final SymbolReference<? extends ResolvedValueDeclaration> methodRef = facade.solve(md.getName());
										if (methodRef.isSolved()) {

											final ResolvedValueDeclaration decl = methodRef.getCorrespondingDeclaration();

											if (decl.isMethod()) {

												final String mName     = decl.asMethod().getName();
												final String signature = decl.asMethod().getSignature();

												logger.info("Solved method: " + methodRef.toString() + ", signature: " + signature);

												methodProperties.put(Method.resolved, true);
											}
										}
									} catch (final UnsolvedSymbolException ignore) {}

									getOrCreate(Method.class, identifyingMethodProperties, methodProperties);

									logger.info("Created (or found) method " + symbolName);

								}

	//							final NodeList<Parameter> parameters = callable.getParameters();
	//
	//							List<JsonResult> parameterList = new ArrayList<>();
	//
	//							parameters.forEach((p) -> {
	//
	//								JsonResult param = new JsonResult();
	//
	//								param.addName(p);
	//								param.addType(p.getType());
	//								param.addModifiers(p);
	//
	//								parameterList.add(param);
	//							});

							}
						}
					} catch (Throwable ignore) {}
				}
			}

		}
	}

	public void analyzeMethodsInJavaFile(final String code, String clsName) {

		try {
			final CompilationUnit cu = JavaParser.parse(code);

			final Map<String, Object> params = new HashMap<>();

			if (facade == null) {
				final CombinedTypeSolver typeSolver = new CombinedTypeSolver();
				typeSolver.add(new ReflectionTypeSolver());
				typeSolver.add(structrTypeSolver);
				facade = JavaParserFacade.get(typeSolver);
			}

			if (clsName == null) {
				try {
					clsName = cu.getType(0).getNameAsString();
				} catch (final Exception ignore) {}
			}


			params.put("clsName", clsName);
			params.put("facade",  facade);
			params.put("app",     app);

			final MethodVisitorAdapter adapter = new MethodVisitorAdapter();
			adapter.visit(cu, params);
		} catch (Throwable ignore) {}
	}

	public void analyzeMethodsInJavaFile(final String code) {
		analyzeMethodsInJavaFile(code, null);
	}

	private void analyzeMethodsInJavaFiles(final Folder folder) {

		if (ignoreTests && "test".equals(folder.getName())) {
			return;
		}

		for (final File file : folder.getFiles()) {

			if (file.getContentType().equals("text/x-java")) {

				final String javaFileName = file.getName();

				if (!(javaFileName.equals("package-info.java") || javaFileName.equals("testPackage-info.java"))) {

					final String clsName     = StringUtils.substringBeforeLast(javaFileName, ".java");
					final String javaContent = file.getFavoriteContent();

					analyzeMethodsInJavaFile(javaContent, clsName);
				}
			}
		}
	}

	private void handlePomFile(final File file, final Folder folder, final Folder parentFolder) {

		final XPath xpath = XPathFactory.newInstance().newXPath();
		QName returnType  = XPathConstants.STRING;

		final String content = file.getFavoriteContent();
		final String projectName;

		try {
			projectName = (String) xpath.evaluate("/project/name", parseXml(content), returnType);

			final Module newMod = createModule(projectName, folder);

			logger.info("Created module '" + projectName + "' in folder " + folder.getPath());

			// Check if we are child of a parent module
			// Find the closest ancestor folder which has a module
			Module  mod = null;

			Folder possibleModuleParentFolder = parentFolder;

			// Continue until root folder or a module was found
			while (possibleModuleParentFolder != null && mod == null) {

				try {
					mod = app.nodeQuery(Module.class).and(Module.folder, possibleModuleParentFolder).getFirst();

				} catch (FrameworkException ignore) {}

				if (mod != null) {

					newMod.setProperty(Module.parent, mod);
					break;
				}

				// Continue while loop
				possibleModuleParentFolder = possibleModuleParentFolder.getParent();
			}

		} catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException | FrameworkException ex) {
			logger.warn("Exception exception occured", ex);
		}
	}

	private ClassOrInterface handleClassOrInterface(final TypeDeclaration type, final org.structr.javaparser.entity.Package pkg) {

		final String name = (pkg != null ? pkg.getName() + "." : "") + type.getNameAsString();

		logger.info("Parsing class or interface " + name);

		final PropertyMap identifyingProperties = new PropertyMap();

		identifyingProperties.put(ClassOrInterface.name, name);
		if (pkg != null) {
			identifyingProperties.put(ClassOrInterface.packageProp, pkg);
		}

		ClassOrInterface clsOrIface = null;

		boolean isInterface = type.asClassOrInterfaceDeclaration().isInterface();

		if (isInterface) {

			// Create Java interface
			clsOrIface = (JavaInterface) getOrCreate(JavaInterface.class, identifyingProperties, identifyingProperties);

			logger.info("Created (or found) interface " + clsOrIface.getName());
		} else {

			// Create Java class
			clsOrIface = (JavaClass) getOrCreate(JavaClass.class, identifyingProperties, identifyingProperties);

			logger.info("Created (or found) class " + clsOrIface.getName());
		}

		return clsOrIface;
	}

	private org.structr.javaparser.entity.Package handlePackage(final PackageDeclaration pkg) {

		final PropertyMap packageIdentifyingProperties = new PropertyMap();

		packageIdentifyingProperties.put(org.structr.javaparser.entity.Package.name, pkg.getNameAsString());
		org.structr.javaparser.entity.Package clsPackage = (org.structr.javaparser.entity.Package) getOrCreate(Package.class, packageIdentifyingProperties, packageIdentifyingProperties);

		if (clsPackage != null) {

			try {
				// Find corresponding folder
				final Folder packageFolder = app.nodeQuery(Folder.class).and(StructrApp.key(Folder.class, "path"), StringUtils.replaceAll(clsPackage.getName(), ".", "/")).getFirst();

				if (packageFolder != null) {

					clsPackage.setProperty(Package.folder, packageFolder);
				}

			} catch (final FrameworkException ex) {};
		}

		return clsPackage;
	}

	private void handlePackageFolder(final Folder folder, final Folder parentFolder) {

		// Folder contains a package-info.java so it must be a package
		String[] parts = folder.getPath().split("src/main/java/");

		// We look for the part after "src/main/java"

		if (parts.length > 1) {

			final PropertyMap identifyingProperties = new PropertyMap();
			final PropertyMap allProperties         = new PropertyMap();

			// Convert path to package path
			String path = StringUtils.replaceAll(parts[1], "/", ".");

			identifyingProperties.put(Package.name, path);
			allProperties.putAll(identifyingProperties);
			allProperties.put(Package.folder, folder);

			// Check if we are contained in a module:
			// Find the closest ancestor folder which has a module
			Module  mod = null;
			Package pkg = null;

			Folder possibleModuleParentFolder = parentFolder;

			// Continue until root folder or a module was found
			while (possibleModuleParentFolder != null && mod == null) {

				try {
					mod = app.nodeQuery(Module.class).and(Module.folder, possibleModuleParentFolder).getFirst();
					pkg = app.nodeQuery(Package.class).and(Module.folder, possibleModuleParentFolder).getFirst();

				} catch (FrameworkException ignore) {}

				if (pkg != null) {

					// Parent folder contains a package
					allProperties.put(Package.parent, pkg);

				} else if (mod != null) {

					// Parent folder contains a module
					allProperties.put(Package.module, mod);
					break;
				}

				// Continue while loop
				possibleModuleParentFolder = possibleModuleParentFolder.getParent();

			}

			getOrCreate(Package.class, identifyingProperties, allProperties);

			logger.info("Created or found package '" + path + "' in folder " + folder.getPath());
		}
	}


	private JsonResult toJson(final CompilationUnit cu) {

		final JsonResult              jsonResult = new JsonResult();
		final NodeList<TypeDeclaration<?>> types = cu.getTypes();

		if (types.isEmpty()) {
			return jsonResult;
		}

		final TypeDeclaration<?>            type = types.get(0);

		jsonResult.addName(type);
		jsonResult.addModifiers(type);

		final Optional<PackageDeclaration> pkg = cu.getPackageDeclaration();

		if (pkg.isPresent()) {
			jsonResult.addPackage(pkg.get());
		}

		final List<BodyDeclaration<?>> members = type.getMembers();
		final List<JsonResult>     membersList = new ArrayList<>();

		members.forEach((t) -> {

			final JsonResult member = new JsonResult();

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

					member.addBody(md);
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
	public void insertImportStatements(final AbstractSchemaNode schemaNode, final SourceFile buf) {
	}

	@Override
	public void insertSourceCode(final AbstractSchemaNode schemaNode, final SourceFile buf) {
	}

	@Override
	public Set<String> getInterfacesForType(final AbstractSchemaNode schemaNode) {
		return null;
	}

	@Override
	public void insertSaveAction(final AbstractSchemaNode schemaNode, final SourceFile buf, final Actions.Type type) {
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

		private void addBody(final NodeWithOptionalBlockStmt node) {
			Optional<BlockStmt> block = node.getBody();
			if (block.isPresent()) {
				obj.add("body", new JsonPrimitive(block.get().toString()));
			}
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

	private Document parseXml(final String xml) throws ParserConfigurationException, SAXException, IOException {

		final DocumentBuilder builder = XmlFunction.getDocumentBuilder();

		if (builder != null) {

			final StringReader reader = new StringReader(xml);
			final InputSource src = new InputSource(reader);

			return builder.parse(src);
		}

		return null;
	}
}