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
package org.structr.test.core.entity;

import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;

/**
 * A simple entity for the most basic tests.
 *
 * This class has a not-null constraint on the TestOne object, so when
 * the TestOne object is deleted, this object should be deleted as well.
 *
 *
 */
public class TestTwo extends AbstractNode {

	public static final Property<TestOne> testOne = new StartNode<>("testOne", OneTwoOneToOne.class);

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {

		if (super.isValid(errorBuffer)) {

			if (getTestOne() == null) {

				errorBuffer.add(new EmptyPropertyToken(TestTwo.class.getSimpleName(), testOne));

				return false;
			}

			return true;
		}

		return false;
	}

	private TestOne getTestOne() {
		return getProperty(testOne);
	}
}
