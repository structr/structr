/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.web.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

//~--- classes ----------------------------------------------------------------

/**
 * A ThreadLocal implementation that provides a reusable Matcher for better performance.
 * The matcher obtained from the <code>get</code> method if this class must be initialized
 * with the input string like this:
 *
 * <pre>
 * Matcher matcher = threadLocalMatcher.get();
 * matcher.reset(input);
 * </pre>
 *
 * @author Christian Morgner
 */
public class ThreadLocalMatcher extends ThreadLocal<Matcher> {

	private Pattern pattern = null;

	//~--- constructors ---------------------------------------------------

	public ThreadLocalMatcher(String pattern) {

		this.pattern = Pattern.compile(pattern);

	}

	//~--- methods --------------------------------------------------------

	@Override
	protected Matcher initialValue() {

		return pattern.matcher("");

	}

}
