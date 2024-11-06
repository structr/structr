/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.knowledge.iso25964;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.*;
import org.structr.knowledge.iso25964.relationship.ThesaurusTermhasCustomTermAttributeCustomTermAttribute;
import org.structr.knowledge.iso25964.relationship.ThesaurusTermhasDefinitionDefinition;
import org.structr.knowledge.iso25964.relationship.ThesaurusTermhasEditorialNoteEditorialNote;
import org.structr.knowledge.iso25964.relationship.ThesaurusTermhasHistoryNoteHistoryNote;

import java.util.Date;

/**
 * Class as defined in ISO 25964 data model
 */

public class ThesaurusTerm extends AbstractNode {

	public enum Lang {
		aa, ab, ae, af, ak, am, an, ar, as, av, ay, az, ba, be, bg, bh, bi, bm, bn, bo, br, bs, ca, ce, ch, co, cr, cs, cu, cv, cy, da, de, dv, dz, ee, el, en, eo, es, et, eu, fa, ff, fi, fj, fo, fr, fy, ga, gd, gl, gn, gu, gv, ha, he, hi, ho, hr, ht, hu, hy, hz, ia, id, ie, ig, ii, ik, in, io, is, it, iu, iw, ja, ji, jv, ka, kg, ki, kj, kk, kl, km, kn, ko, kr, ks, ku, kv, kw, ky, la, lb, lg, li, ln, lo, lt, lu, lv, mg, mh, mi, mk, ml, mn, mo, mr, ms, mt, my, na, nb, nd, ne, ng, nl, nn, no, nr, nv, ny, oc, oj, om, or, os, pa, pi, pl, ps, pt, qu, rm, rn, ro, ru, rw, sa, sc, sd, se, sg, si, sk, sl, sm, sn, so, sq, sr, ss, st, su, sv, sw, ta, te, tg, th, ti, tk, tl, tn, to, tr, ts, tt, tw, ty, ug, uk, ur, uz, ve, vi, vo, wa, wo, xh, yi, yo, za, zh, zu
	}

	;

	public static final Property<Iterable<CustomTermAttribute>> customTermAttributesProperty = new EndNodes<>("customTermAttributes", ThesaurusTermhasCustomTermAttributeCustomTermAttribute.class);
	public static final Property<Iterable<HistoryNote>> historyNotesProperty = new EndNodes<>("historyNotes", ThesaurusTermhasHistoryNoteHistoryNote.class);
	public static final Property<Iterable<Definition>> definitionsProperty = new EndNodes<>("definitions", ThesaurusTermhasDefinitionDefinition.class);
	public static final Property<Iterable<EditorialNote>> editorialNotesProperty = new EndNodes<>("editorialNotes", ThesaurusTermhasEditorialNoteEditorialNote.class);

	public static final Property<String> lexicalValueProperty = new StringProperty("lexicalValue").indexed().notNull();
	public static final Property<String[]> identifierProperty = new ArrayProperty("identifier", String.class).indexed().notNull();
	public static final Property<Date> createdProperty = new DateProperty("created");
	public static final Property<Date> modifiedProperty = new DateProperty("modified");
	public static final Property<String> sourceProperty = new StringProperty("source");
	public static final Property<String> statusProperty = new StringProperty("status");
	public static final Property<String> langProperty = new EnumProperty("lang", Lang.class);

	public static final View uiView = new View(ThesaurusTerm.class, PropertyView.Ui,
		lexicalValueProperty, identifierProperty, createdProperty, modifiedProperty, sourceProperty, statusProperty, langProperty
	);

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidPropertyNotNull(this, ThesaurusTerm.identifierProperty, errorBuffer);
		valid &= ValidationHelper.isValidPropertyNotNull(this, ThesaurusTerm.lexicalValueProperty, errorBuffer);

		return valid;
	}
}
