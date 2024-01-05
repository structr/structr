/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.schema.compiler;

import org.structr.module.JarConfigurationProvider;

import javax.tools.*;
import javax.tools.JavaFileObject.Kind;
import java.io.IOException;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public class ClassFileManager extends ForwardingJavaFileManager {

	/**
	 * Instance of JavaClassObject that will store the compiled byte code of
	 * our class
	 */
	public final Map<String, JavaClassObject> objects = new LinkedHashMap<>();

	/**
	 * Will initialize the manager with the specified standard java file
	 * manager
	 *
	 * @param standardManager
	 */
	public ClassFileManager(final StandardJavaFileManager standardManager) {
		super(standardManager);
	}

	/**
	 * Will be used by us to get the class loader for our compiled class. It
	 * creates an anonymous class extending the SecureClassLoader which uses
	 * the byte code created by the compiler and stored in the
	 * JavaClassObject, and returns the Class for it
	 * @param location
	 * @return class loader
	 */
	@Override
	public ClassLoader getClassLoader(final Location location) {

		return new SecureClassLoader() {

			@Override
			protected Class<?> findClass(String name) throws ClassNotFoundException {

				final JavaClassObject obj = objects.get(name);
				if (obj != null) {

					byte[] b = obj.getBytes();
					return super.defineClass(name, obj.getBytes(), 0, b.length);
				}

				throw new ClassNotFoundException(name);
			}
		};
	}

	/**
	 * Gives the compiler an instance of the JavaClassObject so that the
	 * compiler can write the byte code into it.
	 * @param location
	 * @param className
	 * @param kind
	 * @param sibling
	 * @return file object
	 * @throws java.io.IOException
	 */
	@Override
	public JavaFileObject getJavaFileForOutput(final Location location, final String className, final Kind kind, final FileObject sibling) throws IOException {

		JavaClassObject obj = new JavaClassObject(className, kind);

		objects.put(className, obj);

		return obj;
	}

	@Override
	public Iterable<JavaFileObject> list(Location location, String packageName, Set set, boolean recurse) throws IOException {
		if (location == StandardLocation.CLASS_PATH
				&& (packageName.equals(JarConfigurationProvider.DYNAMIC_TYPES_PACKAGE))) {
			return new ArrayList<>(objects.values());
		}
		return super.list(location, packageName, set, recurse);
	}

	@Override
	public String inferBinaryName(Location location, JavaFileObject file) {
		if (file instanceof JavaClassObject) {
			return ((JavaClassObject)file).getClassName();
		}
		return super.inferBinaryName(location, file);
	}
}