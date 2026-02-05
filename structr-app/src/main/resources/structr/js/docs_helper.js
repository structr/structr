/*
 * Copyright (C) 2010-2026 Structr GmbH
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
document.addEventListener('DOMContentLoaded', e => {

	let openDetailsLinks = document.querySelectorAll('a.open-details');

	for (let link of openDetailsLinks) {

		link.addEventListener('click', showBuiltinFunctionDetails);
	}
});

async function showBuiltinFunctionDetails(e) {

	let link = e.target;
	let conceptId = link.dataset.conceptId;

	let tr      = link.closest('tr');
	let tdCount = tr.querySelectorAll('td').length;

	if (tr.nextElementSibling?.dataset.detailsFor === conceptId) {
		tr.nextElementSibling.classList.toggle('hidden');
		return;
	}

	let res = await fetch('/structr/docs/ontology?format=markdown&id=' + conceptId);

	if (res.ok) {

		let nextTr = createDOMElementsFromHTML(`<tr data-details-for="${conceptId}"><td colspan="${tdCount}"></td></tr>`)[0];
		let nextTd = nextTr.firstChild;
		tr.insertAdjacentElement('afterend', nextTr);

		let html            = await res.text();
		let detailsDocument = new DOMParser().parseFromString(html, "text/html");
		let body            = detailsDocument.querySelector('body');

		for (let child of body.children) {
			nextTd.appendChild(child);
		}
	}
}

function createDOMElementsFromHTML (html) {
	// use template element so we can create arbitrary HTML which is not parsed but not rendered (otherwise tr/td and some other elements would not work)
	let dummy = document.createElement('template');
	dummy.insertAdjacentHTML('beforeend', html);

	return dummy.children;
}