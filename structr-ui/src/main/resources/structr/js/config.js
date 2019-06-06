/*
 * Copyright (C) 2010-2019 Structr GmbH
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
function createNewEntry(e) {

	var currentTab = $('div.tab-content:visible');
	if (currentTab) {

		var name = window.prompt("Please enter a key for the new configuration entry.");
		if (name && name.length) {

			currentTab.append(
				'<div class="form-group">' +
				'<label>' + name + '</label>' +
				'<input type="text" name="' + name + '"/>' +
				'<input type="hidden" name="' + name + '._settings_group" value="' + $(currentTab).attr('id') + '" />' +
				'</div>'
				);
		}
	}
}

function resetToDefault(key) {

	var currentTab = $('#active_section').val();

	window.location.href = '/structr/config?reset=' + key + currentTab;

}

function resize() {
	$('.tab-content').css({
		height: $(window).height() - 160 - $('#configTabsMenu').height() + 'px'
	});
}

function appendInfoTextToElement (text, el, css) {

	var toggleElement = $('<span><i class="' + _Icons.getFullSpriteClass(_Icons.information_icon) + '"></span>');
	if (css) {
		toggleElement.css(css);
	}
	var helpElement = $('<span class="context-help-text">' + text + '</span>');

	toggleElement.on("mousemove", function(e) {
		helpElement.show();
		helpElement.css({
			left: e.clientX + 20,
			top: e.clientY + 10
		});
	});

	toggleElement.on("mouseout", function(e) {
		helpElement.hide();
	});

	return el.append(toggleElement).append(helpElement);
}

function getAnchorFromUrl(url) {
	if (url) {
		var pos = url.lastIndexOf('#');
		if (pos > 0) {
			return url.substring(pos + 1, url.length);
		}
	}
	return null;
}

$(function () {

	$('#new-entry-button').on('click', createNewEntry);

	$('#reload-config-button').on('click', function() {
		window.location.href = window.location.origin + window.location.pathname + "?reload" + $('#active_section').val();
	});

	$('#configTabs a').on('click', function() {
		$('#configTabs li').removeClass('active');
		$('.tab-content').hide();
		let el = $(this);
		el.parent().addClass('active');
		$('#active_section').val(el.attr('href'));
		$(el.attr('href')).show();
	});

	$(window).resize(function() {
		resize();
	});

	resize();

	$('label.has-comment').each(function(idx, label) {
		appendInfoTextToElement($(label).data("comment"), $(label), {
			margin: '0 4px'
		});
	});

	let anchor = getAnchorFromUrl(window.location.href) || 'general';
	$('a[href$=' + anchor + ']').click();

	$("button.toggle-option").on('click', function() {

		var button = $(this);
		var target = $('#' + button.data('target'));
		if (target) {

			var value = button.data('value');
			var list  = target.val();
			var parts = list.split(" ");

			// remove empty elements
			parts.forEach(function(p, i) {

				if (p.length < 2) {
					parts.splice(i, 1);
				}
			});
			var pos = parts.indexOf(value);
			if (pos >= 0) {

				parts.splice(pos, 1);
				button.removeClass('active');

			} else {

				parts.push(value);
				button.addClass('active');
			}

			target.val(parts.filter(function(e) { return e && e.length; }).join(' '));
		}
	});
});



/* config search */

let hitClass = 'search-matches';
let noHitClass = 'no-search-match';

let clearSearch = () => {

	document.querySelectorAll('.' + hitClass).forEach((node) => {
		node.classList.remove(hitClass);
	});

	document.querySelectorAll('.' + noHitClass).forEach((node) => {
		node.classList.remove(noHitClass);
	});

};

let doSearch = (q) => {

	clearSearch();

	// all tabs
	document.querySelectorAll('.tabs-menu li a').forEach((tabLink) => {

		let tab = document.querySelector(tabLink.getAttribute('href'));

		let hitInTab = false;

		// all form-groups in tab
		tab.querySelectorAll('.form-group').forEach((formGroup) => {

			let hitInFormGroup = false;

			// key
			formGroup.querySelectorAll('label').forEach((label) => {
				if (containsIgnoreCase(label.firstChild.textContent, q)) {
					hitInFormGroup = true;
					label.classList.add(hitClass);
				}
			});

			// input
			formGroup.querySelectorAll('[type=text][name]').forEach((input) => {
				if (input.value && containsIgnoreCase(input.value, q)) {
					hitInFormGroup = true;
					input.classList.add(hitClass);
				}
			});

			// select
			formGroup.querySelectorAll('select option').forEach((option) => {
				if (containsIgnoreCase(option.textContent, q)) {
					hitInFormGroup = true;
					option.closest('select').classList.add(hitClass);
				}
			});

			// button
			formGroup.querySelectorAll('button[data-value]').forEach((button) => {
				if (containsIgnoreCase(button.dataset.value, q)) {
					hitInFormGroup = true;
					button.classList.add(hitClass);
				}
			});

			// help text
			formGroup.querySelectorAll('label[data-comment]').forEach((label) => {
				if (containsIgnoreCase(label.dataset.comment, q)) {
					hitInFormGroup = true;
					label.querySelector('span').classList.add(hitClass);
				}
			});

			if (!hitInFormGroup) {
				formGroup.classList.add(noHitClass);
			}

			hitInTab = hitInTab || hitInFormGroup;
		});

		let servicesTable = tab.querySelector('#services-table');
		if (servicesTable) {
			servicesTable.querySelectorAll('td:first-of-type').forEach((td) => {
				if (containsIgnoreCase(td.textContent, q)) {
					hitInTab = true;
					td.classList.add(hitClass);
				}
			});
		}

		let liElement = tabLink.parentNode;

		if (hitInTab) {
			liElement.classList.add(hitClass)
		} else {
			liElement.classList.add(noHitClass);
			tab.classList.add(noHitClass);
		}
	});

	// hide everything without search hits
	document.querySelectorAll('.config-group').forEach((configGroup) => {
		let hitsInGroup = configGroup.querySelectorAll('.form-group:not(.' + noHitClass + ')').length;
		if (hitsInGroup === 0) {
			configGroup.classList.add(noHitClass);
		}
	});

	// if any tabs are left, activate the first (if the currently active one is hidden)
	let activeTabs = document.querySelectorAll('.tabs-menu li.active');
	if (activeTabs.length > 0 && activeTabs[0].classList.contains(noHitClass)) {
		let visibleTabLinks = document.querySelectorAll('.tabs-menu li.' + hitClass + ' a');
		if (visibleTabLinks.length > 0) {
			visibleTabLinks[0].click();
		} else {
			// nothing to show!
		}
	}
};

let containsIgnoreCase = (haystack, needle) => {
	return haystack.toLowerCase().includes(needle.toLowerCase());
};

let initSearch = () => {

	let isLogin = document.getElementById('login');

	if (!isLogin) {

		let header = document.getElementById('header');

		let searchContainer = document.createElement('div');
		searchContainer.id = 'search-container';

		let searchBox = document.createElement('input');
		searchBox.id = 'search-box';
		searchBox.placeholder = 'Search config...';

		searchContainer.appendChild(searchBox);
		header.appendChild(searchContainer);

		let searchTimeout;

		searchBox.addEventListener('keyup', (x) => {

			if (x.keyCode === 27) {

				clearSearch();
				searchBox.value = '';

			} else {

				window.clearTimeout(searchTimeout);

				searchTimeout = window.setTimeout(() => {
					let q = searchBox.value;

					if (q.length == 0) {
						clearSearch();
					} else if (q.length >= 2) {
						doSearch(searchBox.value);
					}
				}, 250);
			}
		});

		window.addEventListener("keydown",function (e) {
			// capture ctrl-f or meta-f (mac) to activate search
			if ((e.ctrlKey && e.keyCode === 70) || (e.metaKey && e.keyCode === 70)) {
				e.preventDefault();
				searchBox.focus();
			}
		});
	}
};

document.addEventListener('DOMContentLoaded', initSearch);