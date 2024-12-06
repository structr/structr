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
document.addEventListener('DOMContentLoaded', () => {

	let collapsibleListClass  = 'collapsibleList';
	let collapsedClass        = 'collapsibleListClosed';
	let expandedClass         = 'collapsibleListOpen';

	let autoCollapseKey          = 'autoCollapse';
	let autoCollapseThresholdKey = 'autoCollapseThreshold';

	let resultList = document.querySelector(`#right > ul > li > ul > li > ul.${collapsibleListClass}`);
	let topBar     = document.querySelector('#left');

	let shouldAutoCollapse = () => {
		return JSON.parse(localStorage.getItem(autoCollapseKey) ?? true);
	};
	let getAutoCollapseThreshold = () => {
		return parseInt(localStorage.getItem(autoCollapseThresholdKey) ?? 5);
	};
	let expandAll = () => {
		for (let el of resultList.querySelectorAll(`.${collapsedClass}`)) {
			el.classList.remove(collapsedClass);
			el.classList.add(expandedClass);
		}
	};
	let collapseAll = () => {
		for (let el of resultList.querySelectorAll(`.${expandedClass}`)) {
			el.classList.remove(expandedClass);
			el.classList.add(collapsedClass);
		}
	};
	let toggleElement = (el) => {
		el.classList.toggle(expandedClass);
		el.classList.toggle(collapsedClass);
	};
	let canToggle = (el) => {
		return el.classList.contains(expandedClass) || el.classList.contains(collapsedClass);
	};
	let isObject = (el) => {
		return (el.previousElementSibling.textContent === '{');
	};

	let initialize = () => {

		topBar.insertAdjacentHTML('beforeEnd', `
			<div class="flex items-center" style="float: right;">
				<label class="flex items-center mr-4">
					<input name="auto-collapse" class="mr-2" type="checkbox" ${shouldAutoCollapse() ? 'checked' : ''}> Auto-collapse objects on load (for more than 5 objects)
				</label>
				<button class="expand">+</button>
				<button class="collapse">-</button>
			</div>
		`);

		let autoCollapseCheckbox = topBar.querySelector('input[name=auto-collapse]');

		autoCollapseCheckbox.addEventListener('change', () => {
			localStorage.setItem(autoCollapseKey, autoCollapseCheckbox.checked);
		});

		// first set all possible lists to "open" status
		for (let ul of resultList.querySelectorAll(`ul.${collapsibleListClass}`)) {

			ul.closest('li')?.classList.add(expandedClass);
		}

		for (let expandedElement of resultList.querySelectorAll(`.${expandedClass}`)) {

			// prepare count
			for (let child of expandedElement.children) {

				if (child.classList.contains(collapsibleListClass)) {

					if (isObject(child)) {
						child.dataset.count = '...';
					} else {
						child.dataset.count = '' + child.children.length;
					}
				}
			}

			if (expandedElement.querySelector('ul')) {

				expandedElement.addEventListener('click', (e) => {
					e.stopPropagation();

					let closestLi = e.target.closest('li');

					if (canToggle(closestLi)) {

						toggleElement(closestLi);
					}
				});
			}
		}

		// do not auto-collapse if root element is object
		if (!isObject(resultList)) {

			let totalNumberOfResults = document.querySelectorAll(`#right > ul > li > ul > li > ul.${collapsibleListClass} > li`).length;

			if (shouldAutoCollapse() && totalNumberOfResults > getAutoCollapseThreshold()) {
				collapseAll();
			}
		}
	};

	if (resultList) {
		initialize();
	}

	document.querySelector('button.expand')?.addEventListener('click', expandAll);
	document.querySelector('button.collapse')?.addEventListener('click', collapseAll);
});