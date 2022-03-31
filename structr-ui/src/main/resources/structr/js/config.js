/*
 * Copyright (C) 2010-2021 Structr GmbH
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

	let currentTab = $('div.tab-content:visible');
	if (currentTab) {

		let name = window.prompt("Please enter a key for the new configuration entry.");
		if (name && name.length) {

			currentTab.append(`
				<div class="form-group">
					<label class="bold basis-full sm:basis-auto sm:min-w-128">${name}</label>
					<input type="text" name="${name}">
					<input type="hidden" name="${name}._settings_group" value="${$(currentTab).attr('id')}">
				</div>
			`);
		}
	}
}

function appendInfoTextToElement (text, el, css) {

	let toggleElement = $(`<span>${_Icons.getSvgIcon('info-icon', 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-blue', 'ml-2']))}</span>`);
	if (css) {
		toggleElement.css(css);
	}
	let helpElement = $('<span class="context-help-text">' + text + '</span>');

	toggleElement.on("mousemove", (e) => {
		helpElement.show();
		helpElement.css({
			left: e.clientX + 20,
			top: e.clientY + 10
		});
	});

	toggleElement.on("mouseout", (e) => {
		helpElement.hide();
	});

	return el.append(toggleElement).append(helpElement);
}

/* config search */
let _Search = {
	hitClass: 'search-matches',
	noHitClass: 'no-search-match',
	lsSearchStringKey: 'structrConfigSearchKey',
	containsIgnoreCase: (haystack, needle) => {
    	return haystack.toLowerCase().includes(needle.toLowerCase());
    },
	init: () => {

    	let isLogin   = document.getElementById('login');
    	let isWelcome = document.getElementById('welcome');

    	if (!isLogin && !isWelcome) {

    		let header = document.getElementById('header');

    		let searchContainer = document.createElement('div');
    		searchContainer.id = 'search-container';

    		let searchBox = document.createElement('input');
    		searchBox.id = 'search-box';
    		searchBox.placeholder = 'Search config...';

    		searchContainer.appendChild(searchBox);
    		header.appendChild(searchContainer);

    		let searchTimeout;

    		let lastSearch = window.localStorage.getItem(_Search.lsSearchStringKey);
    		if (lastSearch) {
    		    searchBox.value = lastSearch;
    		    _Search.doSearch(lastSearch);
    		}

    		searchBox.addEventListener('keyup', (x) => {

    			if (x.keyCode === 27) {

    				_Search.clearSearch();
    				searchBox.value = '';
                    window.localStorage.removeItem(_Search.lsSearchStringKey);

    			} else {

    				window.clearTimeout(searchTimeout);

    				searchTimeout = window.setTimeout(() => {
    					let q = searchBox.value;

    					if (q.length === 0) {
    						_Search.clearSearch();
                            window.localStorage.removeItem(_Search.lsSearchStringKey);
    					} else if (q.length >= 2) {
    						_Search.doSearch(searchBox.value);
    						window.localStorage.setItem(_Search.lsSearchStringKey, searchBox.value);
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
    },
	clearSearch: () => {
    	document.querySelectorAll('.' + _Search.hitClass).forEach((node) => {
    		node.classList.remove(_Search.hitClass);
    	});

    	document.querySelectorAll('.' + _Search.noHitClass).forEach((node) => {
    		node.classList.remove(_Search.noHitClass);
    	});
    },
	doSearch: (q) => {

		_Search.clearSearch();

		// all tabs
		document.querySelectorAll('.tabs-menu li a').forEach((tabLink) => {

			let tab = document.querySelector(tabLink.getAttribute('href'));

			let hitInTab = false;

			if (tab.id === 'databases') {

				tab.querySelectorAll('.config-group').forEach((configGroup) => {

					let hitInConfigGroup = false;

					configGroup.querySelectorAll('label').forEach((label) => {
						if (_Search.containsIgnoreCase(label.firstChild.textContent, q)) {
							hitInConfigGroup = true;
							label.classList.add(_Search.hitClass);
						}
					});

					configGroup.querySelectorAll('[type=text]').forEach((input) => {
						if (input.value && _Search.containsIgnoreCase(input.value, q)) {
							hitInConfigGroup = true;
							input.classList.add(_Search.hitClass);
						}
					});

					hitInTab = hitInTab || hitInConfigGroup;
				});

			} else {

				// all form-groups in tab
				tab.querySelectorAll('.form-group').forEach((formGroup) => {

					let hitInFormGroup = false;

					// key
					formGroup.querySelectorAll('label').forEach((label) => {
						if (_Search.containsIgnoreCase(label.firstChild.textContent, q)) {
							hitInFormGroup = true;
							label.classList.add(_Search.hitClass);
						}
					});

					// input
					formGroup.querySelectorAll('[type=text][name]').forEach((input) => {
						if (input.value && _Search.containsIgnoreCase(input.value, q)) {
							hitInFormGroup = true;
							input.classList.add(_Search.hitClass);
						}
					});

					// textarea
					formGroup.querySelectorAll('textarea').forEach((textarea) => {
						if (textarea.value && _Search.containsIgnoreCase(textarea.value, q)) {
							hitInFormGroup = true;
							textarea.classList.add(_Search.hitClass);
						}
					});

					// select
					formGroup.querySelectorAll('select option').forEach((option) => {
						if (_Search.containsIgnoreCase(option.textContent, q)) {
							hitInFormGroup = true;
							option.closest('select').classList.add(_Search.hitClass);
						}
					});

					// button
					formGroup.querySelectorAll('button[data-value]').forEach((button) => {
						if (_Search.containsIgnoreCase(button.dataset.value, q)) {
							hitInFormGroup = true;
							button.classList.add(_Search.hitClass);
						}
					});

					// help text
					formGroup.querySelectorAll('label[data-comment]').forEach((label) => {
						if (_Search.containsIgnoreCase(label.dataset.comment, q)) {
							hitInFormGroup = true;
							label.querySelector('span').classList.add(_Search.hitClass);
						}
					});

					if (!hitInFormGroup) {
						formGroup.classList.add(_Search.noHitClass);
					}

					hitInTab = hitInTab || hitInFormGroup;
				});
			}

			let servicesTable = tab.querySelector('#services-table');
			if (servicesTable) {
				servicesTable.querySelectorAll('td:first-of-type').forEach((td) => {
					if (_Search.containsIgnoreCase(td.textContent, q)) {
						hitInTab = true;
						td.classList.add(_Search.hitClass);
					}
				});
			}

			let liElement = tabLink.parentNode;

			if (hitInTab) {
				liElement.classList.add(_Search.hitClass);
			} else {
				liElement.classList.add(_Search.noHitClass);
				tab.classList.add(_Search.noHitClass);
			}
		});

		// hide everything without search hits
		document.querySelectorAll('.config-group').forEach((configGroup) => {
			let hitsInGroup = configGroup.querySelectorAll('.' + _Search.hitClass).length;
			if (hitsInGroup === 0) {
				configGroup.classList.add(_Search.noHitClass);
			}
		});

		// if any tabs are left, activate the first (if the currently active one is hidden)
		let activeTabs = document.querySelectorAll('.tabs-menu li.active');
		if (activeTabs.length > 0 && activeTabs[0].classList.contains(_Search.noHitClass)) {
			let visibleTabLinks = document.querySelectorAll('.tabs-menu li.' + _Search.hitClass + ' a');
			if (visibleTabLinks.length > 0) {
				visibleTabLinks[0].click();

				// in case a password field got auto-focused by the browser
				document.getElementById('search-box').focus();
			} else {
				// nothing to show!
			}
		}
	}
};

const Structr = {
    getPrefixedRootUrl: (rootUrl = '/structr/rest') => {
        let prefix = [];
        const pathEntries = window.location.pathname.split('/')?.filter( pathEntry => pathEntry !== '') ?? [];
        let entry = pathEntries.shift();

        while (entry !== 'structr' && entry !== undefined) {
           prefix.push(entry);
           entry = pathEntries.shift();
        }

		return `${ (prefix.length ? '/' : '') + prefix.join('/') }${rootUrl}`;
    }
}

document.addEventListener('DOMContentLoaded', () => {

	$('#new-entry-button').on('click', createNewEntry);

	for (let resetButton of document.querySelectorAll('.reset-key')) {
		resetButton.addEventListener('click', () => {

			let currentTab = $('#active_section').val();
			let key        = resetButton.dataset['key'];

			window.location.href = Structr.getPrefixedRootUrl('/structr/config') + '?reset=' + key + currentTab;
		});
	}

	$('#reload-config-button').on('click', function() {
		window.location.href = Structr.getPrefixedRootUrl('/structr/config') + "?reload" + $('#active_section').val();
	});

	$('#configTabs a').on('click', function() {
		$('#configTabs li').removeClass('active');
		$('.tab-content').hide();
		let el = $(this);
		el.parent().addClass('active');
		$('#active_section').val(el.attr('href'));
		$(el.attr('href')).show();
	});

	let resizeFunction = () => {
		$('.tab-content').css({
			height: $(window).height() - $('#header').height() - $('#configTabs .tabs-menu').height() - 124
		});
	};

	$(window).resize(function() {
		resizeFunction();
	});

	resizeFunction();

	$('label.has-comment').each(function(idx, label) {
		appendInfoTextToElement($(label).data("comment"), $(label));
	});

	let anchor = (new URL(window.location.href)).hash.substring(1) || 'general';
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

	$('.new-connection.collapsed').on('click', function() {
		$(this).removeClass('collapsed');
	});

	_Search.init();
});

function collectData(name) {

	if (!name) {
		name = 'structr-new-connection';
	}

	let nameInput    = $('input#name-' + name);
	let driverSelect = $('select#driver-' + name);
	let urlInput     = $('input#url-' + name);
	let dbNameInput  = $('input#database-' + name);
	let userInput    = $('input#username-' + name);
	let pwdInput     = $('input#password-' + name);
	let nowCheckbox  = $('input#connect-checkbox');

	nameInput.parent().removeClass();
	driverSelect.parent().removeClass();
	urlInput.parent().removeClass();
	userInput.parent().removeClass();
	pwdInput.parent().removeClass();

	let data = {
		name:     nameInput.val(),
		driver:   driverSelect.val(),
		url:      urlInput.val(),
		database: dbNameInput.val(),
		username: userInput.val(),
		password: pwdInput.val(),
		now:      nowCheckbox && nowCheckbox.is(':checked'),
		active_section: '#databases'
	};

	return data;
}

function addConnection(button) {

	let name = 'structr-new-connection';
	let data = collectData();

	button.dataset.text = button.innerHTML;
	button.disabled     = true;

	if (data.now) {
		button.innerHTML = 'Connecting..';
	}

	let status = $('div#status-' + name);
	status.addClass('hidden');
	status.empty();

	if (data.now) {
		_Config.showNonBlockUILoadingMessage('Connection is being established', 'Please wait...');
	}

	$.ajax({
		type: 'post',
		url: Structr.getPrefixedRootUrl('/structr/config/add'),
		data: data,
		statusCode: {
			200: reload,
			302: reload,
			422: response => handleErrorResponse(name, response, button),
			503: response => handleErrorResponse(name, response, button)
		}
	});
}

function deleteConnection(name) {

	$.ajax({
		type: 'post',
		url: Structr.getPrefixedRootUrl('/structr/config/') + name + '/delete',
		data: {
			'active_section': '#databases'
		},
		statusCode: {
			200: reload,
			302: reload,
			422: response => handleErrorResponse(name, response),
			503: response => handleErrorResponse(name, response)
		}
	});
}

function setNeo4jDefaults() {
	$('#driver-structr-new-connection').val('org.structr.bolt.BoltDatabaseService');
	$('#name-structr-new-connection').val('neo4j-localhost-7687');
	$('#url-structr-new-connection').val('bolt://localhost:7687');
	$('#database-structr-new-connection').val('neo4j');
	$('#username-structr-new-connection').val('neo4j');
	$('#password-structr-new-connection').val('neo4j');
}

function saveConnection(name) {

	let data = collectData(name);

	if (data.now) {
		_Config.showNonBlockUILoadingMessage('Connection is being established', 'Please wait...');
	}

	$.ajax({
		type: 'post',
		url: Structr.getPrefixedRootUrl('/structr/config/') + name + '/use',
		data: data,
		statusCode: {
			200: reload,
			302: reload,
			422: response => handleErrorResponse(name, response),
			503: response => handleErrorResponse(name, response)
		}
	});
}

function reload(response) {

	_Config.hideNonBlockUILoadingMessage();

	window.location.href = Structr.getPrefixedRootUrl('/structr/config#databases');
	window.location.reload(true);
}

function connect(button, name) {

	button.disabled = true;
	button.dataset.text = button.innerHTML;
	button.innerHTML = 'Connecting..';

	let status = $('div#status-' + name);
	status.addClass('hidden');
	status.empty();

	_Config.showNonBlockUILoadingMessage('Connection is being established', 'Please wait...');

	$.ajax({
		type: 'post',
		url: Structr.getPrefixedRootUrl('/structr/config/') + name + '/connect',
		data: collectData(name),
		statusCode: {
			200: reload,
			302: reload,
			422: response => handleErrorResponse(name, response, button),
			503: response => handleErrorResponse(name, response, button)
		}
	});
}

function disconnect(button, name) {

	button.disabled = true;
	button.dataset.text = button.innerHTML;
	button.innerHTML = 'Disconnecting..';

	let status = $('div#status-' + name);
	status.addClass('hidden');
	status.empty();

	_Config.showNonBlockUILoadingMessage('Database is being disconnected', 'Please wait...');

	$.ajax({
		type: 'post',
		url: Structr.getPrefixedRootUrl('/structr/config/') + name + '/disconnect',
		data: collectData(name),
		statusCode: {
			200: reload,
			302: reload,
			422: response => handleErrorResponse(name, response, button),
			503: response => handleErrorResponse(name, response, button)
		}
	});
}

function handleErrorResponse(name, response, button) {

	_Config.hideNonBlockUILoadingMessage();

	let json = response.responseJSON;

	if (!name) {
		name = 'structr-new-connection';
	}

	if (button) {
		button.disabled = false;
		button.innerHTML = button.dataset.text;
	}

	switch (response.status) {

		case 422:
			if (json.errors && json.errors.length) {

				json.errors.forEach(t => {
					if (t.property !== undefined && t.token !== undefined) {
						$('input#' + t.property + '-' + name).parent().addClass(t.token);
					}
				});

			} else {

				let status = $('div#status-' + name);
				status.empty();
				status.append(json.message);
				status.removeClass('hidden');
			}
			break;

		case 503:
			let status = $('div#status-' + name);
			status.empty();
			status.append(json.message);
			status.removeClass('hidden');
			break;
	}
}



_Config = {
	nonBlockUIBlockerId: 'non-block-ui-blocker',
	nonBlockUIBlockerContentId: 'non-block-ui-blocker-content',
	showNonBlockUILoadingMessage: function(title, text) {

		let messageTitle = title || 'Executing Task';
		let messageText  = text || 'Please wait until the operation has finished...';

		let pageBlockerDiv = $(`<div id="${_Config.nonBlockUIBlockerId}"></div>`);
		let messageDiv     = $(`<div id="${_Config.nonBlockUIBlockerContentId}"></div>`);
		messageDiv.html(`<div class="flex items-center justify-center">${_Icons.getSvgIcon('waiting-spinner', 24, 24, 'mr-2')}<b>${messageTitle}</b></div><br>${messageText}`);


		$('body').append(pageBlockerDiv);
		$('body').append(messageDiv);
	},
	hideNonBlockUILoadingMessage: function() {
		$('#' + _Config.nonBlockUIBlockerId).remove();
		$('#' + _Config.nonBlockUIBlockerContentId).remove();
	}
};