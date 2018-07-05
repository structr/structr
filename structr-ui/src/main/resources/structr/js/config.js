/*
 * Copyright (C) 2010-2018 Structr GmbH
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

	if (currentTab && currentTab.length) {

		window.location.href = '/structr/config?reset=' + key + "#" + currentTab;

	} else {

		window.location.href = '/structr/config?reset=' + key;
	}
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

	$('#configTabs a').on('click', function() {
		$('#configTabs li').removeClass('active');
		$('.tab-content').hide();
		let el = $(this);
		el.parent().addClass('active');
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
	$('a[href=#' + anchor + ']').click();

});

