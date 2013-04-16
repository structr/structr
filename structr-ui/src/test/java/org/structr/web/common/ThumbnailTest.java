/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.web.common;

import org.structr.common.error.FrameworkException;
import org.structr.core.property.PropertyMap;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.StructrTransaction;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.web.common.ImageHelper.Thumbnail;
import org.structr.web.entity.Image;
import org.structr.web.entity.TestImage;

//~--- classes ----------------------------------------------------------------

/**
 * Test creation of thumbnails
 *
 * All tests are executed in superuser context
 *
 * @author Axel Morgner
 */
public class ThumbnailTest extends StructrTest {

	private static String base64Image =
		"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAWAAAABUCAYAAAC8/e1DAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAA2ZpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDUuMy1jMDExIDY2LjE0NTY2MSwgMjAxMi8wMi8wNi0xNDo1NjoyNyAgICAgICAgIj4gPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4gPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIgeG1sbnM6eG1wTU09Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bWxuczp4bXA9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC8iIHhtcE1NOk9yaWdpbmFsRG9jdW1lbnRJRD0ieG1wLmRpZDo2RjYyNjlFMUNFMTNFMjExQTQ2N0ZGMDI2MEZEQ0Q3NSIgeG1wTU06RG9jdW1lbnRJRD0ieG1wLmRpZDo2MDcwOEExQzEzRDMxMUUyQTMyQzlEQjBGNTBBQUUwMSIgeG1wTU06SW5zdGFuY2VJRD0ieG1wLmlpZDo2MDcwOEExQjEzRDMxMUUyQTMyQzlEQjBGNTBBQUUwMSIgeG1wOkNyZWF0b3JUb29sPSJBZG9iZSBQaG90b3Nob3AgQ1M2IChXaW5kb3dzKSI+IDx4bXBNTTpEZXJpdmVkRnJvbSBzdFJlZjppbnN0YW5jZUlEPSJ4bXAuaWlkOkYzODhBQzYwRDIxM0UyMTFBNDY3RkYwMjYwRkRDRDc1IiBzdFJlZjpkb2N1bWVudElEPSJ4bXAuZGlkOjZGNjI2OUUxQ0UxM0UyMTFBNDY3RkYwMjYwRkRDRDc1Ii8+IDwvcmRmOkRlc2NyaXB0aW9uPiA8L3JkZjpSREY+IDwveDp4bXBtZXRhPiA8P3hwYWNrZXQgZW5kPSJyIj8+xNxK9AAAAt9JREFUeNrs3TFOKlEUgGGuIWhGSwmVLoMlWFi8ddiZuA41sTMuw9LlYGWBEqOE4DC8c4mNhSUzhvt9yQ2813GCf46DOmk8Hu9dXl7+q6rqPqU06gGwNev1+mU+n1/c3d099uPfB+Eh/nMYx3QAtmuUmxuPTznAh+IL0Kphbm8O8GC1WhkHQLsGOcCpaRqjAGhXygHu2YAB2ifAAF0GuK5rkwCwAQPYgAEQYIAdDbBLEAA2YAABBqCFALsEASDAAGUF2CUIABswQFkB9tfQAGzAAGUF2DVggI4CvFgsTALABgxQUIDdkBOgfXtGACDAAAIMgAADCDAAAgwgwAAIMIAAAyDAAH/a5leRU0omAWADBrABA2ADBtjNAK/Dm1EAtCOaO8sPOcDL19fXm6Zp3o0FYLuitbNo7nVubxqPx1U8OY1zEucojgvC3egbAYV81516Py9/5jtC5DsD19/Pd3r5jfMR5znOJH/R5xvCTeJM4wwEuNM3JpQg/RKmpoDXnl/nMs5nbm//+Pi4d3V1dVZV1X1KaeS9AbDFAq/XL/P5/OL29vYxb8AH+/v7D03TDI0GYOtGubnx+JQDfBhFHroxJ0Br8sJ7mAM8iO3XOADaNcgBTgIM0Lq0+dGn1WplFAAt2wTYBgzQUYC/vr5MAqCLALsEASDAAGUF2CUIgI4C7EM4gI4CXNe1SQAIMEBBAfYhHIAAA5QVYJcgAGzAAGUF2N8CBugowC5BAAgwQFkBXiwWJgFgAwYoKMA+hANo354RAAgwgAADIMAAAgyAAAMIMAACDCDAAAgwwJ+2+VXklJJJANiAAWzAANiAAXYzwOvwZhQA7YjmzvJDDvByOp3eNE3zbiwA2xWtnUVzr3N70/n5eRVPTuOcxDmK44JwN/pGQCHfdafez8uf+Y4Qqzj19/OdXn7jfMR5jjPJX/T5hnCTONM4AwHu9I0JJUi/hKkp4LXn17mM85nb+1+AAQDuVAgNv/BqVwAAAABJRU5ErkJggg==";
	private static final Logger logger = Logger.getLogger(ThumbnailTest.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void test00DbAvailable() {

		super.test00DbAvailable();

	}

	public void test01CreateThumbnail() {

		try {

			final PropertyMap props = new PropertyMap();

			props.put(AbstractNode.type, TestImage.class.getSimpleName());

			TestImage img = transactionCommand.execute(new StructrTransaction<TestImage>() {

				@Override
				public TestImage execute() throws FrameworkException {

					try {

						return (TestImage) ImageHelper.createImageBase64(securityContext, base64Image, TestImage.class);

					} catch (IOException ex) {

						logger.log(Level.SEVERE, null, ex);

					}

					return null;

				}

			});

			assertNotNull(img);
			assertTrue(img instanceof TestImage);

			Image tn = img.getProperty(TestImage.thumbnail);

			assertNotNull(tn);
			assertEquals(new Integer(200), tn.getWidth());
			assertEquals(new Integer(48), tn.getHeight());  // cropToFit = false
			assertEquals("image/" + Thumbnail.FORMAT, tn.getContentType());

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

}
