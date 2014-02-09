/**
 * Copyright (C) 2010-2014 Structr, c/o Morgner UG (haftungsbeschränkt) <structr@structr.org>
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.app;

import java.util.List;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Person;

/**
 *
 * @author Christian Morgner
 */
public class AppExample {

	public static void main(String[] args) {
		
		App app = StructrApp.getInstance();

		try {

			app.beginTx();

			Person chrisi = app.create(Person.class, "chrisi");
			Person axel   = app.create(Person.class, "axel");
			
			chrisi.setProperty(Person.name, "chrisi2");

			app.commitTx();

		} catch (FrameworkException fex) {

		} finally {

			app.finishTx();
		}


		try {

			List<Person> persons = app.get(Person.class);
			for (Person person : persons) {

				System.out.println(person.getName());
			}
			
		} catch (FrameworkException fex) {
		}			
	}
}
