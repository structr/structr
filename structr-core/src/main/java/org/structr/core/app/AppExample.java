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
