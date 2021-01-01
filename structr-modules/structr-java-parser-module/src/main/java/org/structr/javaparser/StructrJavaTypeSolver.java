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

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.javaparser.Navigator;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;

/**
 * Singleton implementation of a type solver based on java files stored in a Structr instance's virtual file system.
 * 
 * Walks down the virtual file system tree recursively and creates and caches
 * a list of compilation units (AST representations) of the Java files.
 * 
 * Caches also the resolved symbols.
 */
public class StructrJavaTypeSolver implements TypeSolver {

	protected static final Logger logger = LoggerFactory.getLogger(StructrJavaTypeSolver.class.getName());
	
	private Folder rootFolder;
	private TypeSolver parent;

	private List<CompilationUnit> index = new ArrayList<>();
	
	private Cache<String, Optional<CompilationUnit>> parsedFiles = CacheBuilder.newBuilder().softValues().build();
	private Cache<String, SymbolReference<ResolvedReferenceTypeDeclaration>> foundTypes = CacheBuilder.newBuilder().softValues().build();
	
	private long cuCount = 0;
	
	private static StructrJavaTypeSolver instance;
	
	public static StructrJavaTypeSolver getInstance() {
		
		if (StructrJavaTypeSolver.instance == null) {
			StructrJavaTypeSolver.instance = new StructrJavaTypeSolver();
		}
		
		return StructrJavaTypeSolver.instance;
	}
	
	public void parseRoot(final Folder folder) {
		this.rootFolder = folder;
		parse(rootFolder);
		
		logger.info("Parsed " + cuCount + " files.");
	}
	
	@Override
	public String toString() {
	    return "StructrJavaTypeSolver{" +
		    "rootFolderPath=" + rootFolder.getPath() +
		    ", parent=" + parent +
		    '}';
	}

	@Override
	public TypeSolver getParent() {
		return parent;
	}

	@Override
	public void setParent(TypeSolver parent) {
		this.parent = parent;
	}

	@Override
	public SymbolReference<ResolvedReferenceTypeDeclaration> tryToSolveType(final String name) {
		// TODO support enums
		// TODO support interfaces
		try {
			return foundTypes.get(name, () -> {
				SymbolReference<ResolvedReferenceTypeDeclaration> result = tryToSolveTypeUncached(name);
				if (result.isSolved()) {
					return SymbolReference.solved(result.getCorrespondingDeclaration());
				}
				return result;
			});
			
		} catch (ExecutionException e) {
		    throw new RuntimeException(e);
		}
	}

	private void parse(final Folder folder) {
		
		for (final File file: folder.getFiles()) {
			
			final String fileName = file.getName();
			
			if (StringUtils.isNotEmpty(fileName) && fileName.endsWith(".java") && !(fileName.endsWith("ackage-info.java"))) {
			
				final String fileContent = file.getFavoriteContent();
				
				try {
					
					final CompilationUnit cu = JavaParser.parse(fileContent);
					index.add(cu);
					cuCount++;

				} catch (final ParseProblemException ex) {
				
					logger.warn("Couldn't parse " + fileName, ex);
				}
			}
		}
		
		folder.getFolders().forEach((subfolder) -> {
			parse(subfolder);
		});
	}
	
	private SymbolReference<ResolvedReferenceTypeDeclaration> tryToSolveTypeUncached(final String name) {

		for (final CompilationUnit cu : index) {
			
			final Optional<com.github.javaparser.ast.body.TypeDeclaration<?>> astTypeDeclaration = Navigator.findType(cu, name);
			if (astTypeDeclaration.isPresent()) {
				return SymbolReference.solved(JavaParserFacade.get(this).getTypeDeclaration(astTypeDeclaration.get()));
			}
		}
			
		return SymbolReference.unsolved(ResolvedReferenceTypeDeclaration.class);
	}	
}
