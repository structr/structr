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
document.addEventListener("DOMContentLoaded", () => {
	Structr.registerModule(_Documentation);
});

let _Documentation = {
	_content: {},
	_moduleName: 'docs',
	//urlHashKey: 'structrUrlHashKey_' + location.port,
	currentDoc: undefined,

	unload: () => {
		_Documentation.search.dispose();
		_Documentation.currentDoc = undefined;

		window.removeEventListener('hashchange', _Documentation.hashChangeHandler);
	},
	onload: () => {

		Structr.setMainContainerHTML(_Documentation.templates.main());
		_Documentation.init();

		const lastIndexOfHash = Structr.subModule?.lastIndexOf('#');
		let inPageHash;
		if (lastIndexOfHash > 0) {
			inPageHash = Structr.subModule?.substring(lastIndexOfHash + 1);
			Structr.subModule = Structr.subModule?.substring(0, lastIndexOfHash);
		}

        //_Documentation.loadDoc(Structr.subModule || 'Getting Started', inPageHash);
		_Documentation.loadDoc('concept00002', inPageHash);

		Structr.setFunctionBarHTML(_Documentation.templates.searchField());

		Structr.mainMenu.unblock(100);

		_Documentation.search.init();

		const checkIndexLinks = (clickedEl) => {
			document.querySelectorAll('.index a').forEach(el => {
				el.classList.remove('active');
			});
			clickedEl.classList.add('active');
			window.setTimeout(() => { window.scrollBy(0,-108); }, 10);
		};

		document.querySelectorAll('.index a').forEach(el => {
			el.addEventListener('click', e => {
				checkIndexLinks(e.target);
			});
		});
	},
	hashChangeHandler: e => {

		let hash = new URL(e.newURL).hash;

		if (hash.startsWith('#docs:')) {

			let rawPath = hash.substring(hash.indexOf(':') + 1);
			let deepHash = undefined;

			if (rawPath.indexOf('#') > -1) {
				deepHash = rawPath.substring(rawPath.indexOf('#') + 1);
				rawPath = rawPath.substring(0, rawPath.indexOf('#'));
			}

			_Documentation.loadDoc(rawPath, deepHash);

		} else {

			// browser should have navigated by itself - update hash so it survives page reload
			// _Documentation.updateHashWithoutAddingHistory('#docs:' + _Documentation.currentDoc + hash);
			_Documentation.loadDoc(_Documentation.currentDoc, hash.substring(1));
		}
	},
	updateHashWithoutAddingHistory: (newHash) => {
		history.replaceState(history.state, "", newHash);
	},
	keyDownHandler: e => {

		if (e.key === 'Escape') {
			_Documentation.search.hideSearch();
			_Documentation.search.clearSearchResults();
		}

        if ((e.code === 'KeyK' || e.keyCode === 75) && ((!_Helpers.isMac() && e.ctrlKey) || (_Helpers.isMac() && e.metaKey))) {
            e.preventDefault();
			_Documentation.search.showSearch();
        }
	},
	removeAllMarkdownChars: (text) => {
		return text.replace(/[*`![\]()_~#>\-+=|\\{}<>^]/g, ' ').replace(/ +/g, ' ');
	},
	init: () => {

        // build the table of contents
		const docsElement = document.getElementById('docs-main-navigation');
        const iframe = document.querySelector('iframe');

        if (iframe) {

            iframe.addEventListener('load', e => {
                //_Documentation.makeEditable();
            });

        } else {
            console.log('iframe not found');
        }

		fetch('/structr/docs/ontology?root=Structr&details=all&levels=1&format=toc&startLevel=1')
			.then(response => response.json())
			.then(json => {

                for (let entry of json.data) {

                    let menuItem = _Documentation.createElementFromHTML(_Documentation.templates.mainNavigationItem({
                        label: entry.name
                    }));

                    docsElement.appendChild(menuItem);

                    let sublist = document.createElement('ul');
                    menuItem.appendChild(sublist);

                    if (entry && entry.links && entry.links.length) {
                        for (let child of entry.links[0].targets) {

                            let subItem = _Documentation.createElementFromHTML(_Documentation.templates.mainNavigationSubItem({
                                parent: entry.id,
                                id: child.id,
                                label: child.name
                            }));

                            subItem.addEventListener('click', e => {
                                _Documentation.loadContext(child.id);
                            });

                            sublist.appendChild(subItem);
                        }
                    }
				}
			});

	},
	loadDoc: (id, hash, parents, searchHit) => {
        let iframe = document.querySelector('div#docs-area iframe');
        if (iframe) {
            let hash = '';
            if (parents && parents.length > 1) {
                hash = '#' + _Documentation.cleanStringForLink(parents[parents.length - 1].name);
            } else if (searchHit) {
                hash = '#' + _Documentation.cleanStringForLink(searchHit);
            }
            iframe.src = `/structr/docs/ontology?id=${id}&details=all&format=markdown${hash}`;
            _Documentation.loadContext(id);

            iframe.addEventListener('load', () => {
                iframe.contentDocument.querySelector('body').addEventListener('click', e => {
                    const el = e.target;
                    if (el.tagName === 'A') {
                        console.log(el.href);
                        fetch(el.href + '?details=name&levels=1&format=toc&startLevel=0')
                            .then(response => response.json())
                            .then(json => {
                                _Documentation.loadContext(json.data?.[0].id);
                            });

                    }

                });
            });

        }
	},
    loadContext: (id) => {

        document.querySelectorAll('a[target="main-documentation"]').forEach(el => el.classList.remove('active'));
        document.querySelector(`a[target="main-documentation"][href*="id=${id}"]`)?.classList.add('active');

        fetch(`/structr/docs/ontology?id=${id}&details=all&levels=1&format=toc&startLevel=1`)
            .then(response => response.json())
            .then(json => {

                let index = 1;
                let aside = document.querySelector('aside.index');

                aside.innerHTML = '<h2 class="index-heading">On this page</h2>';

                for (let entry of json.data) {

                    const cleanStringForLink = _Documentation.cleanStringForLink(entry.name);

                    aside.appendChild(_Documentation.createElementFromHTML(_Documentation.templates.indexItem({
                        id: id,
                        name: cleanStringForLink,
                        label: entry.name
                    })));

                    aside.querySelector(`a[href*="#${cleanStringForLink}"]`)?.addEventListener('click', e => {
                        aside.querySelectorAll('a[target="main-documentation"]').forEach(el => el.classList.remove('active'));
                        e.target.classList.add('active');
                    });
                }
            });
    },
    cleanStringForLink: (str) => str.replace('?', '').replaceAll(/[\W]+/g, '-').toLowerCase(),
	search: {
		searchOverlay: undefined,
		overlaySearchField: undefined,
		searchAnimationFrameKey: undefined,
		init: () => {

			_Documentation.search.searchOverlay      = _Helpers.createSingleDOMElementFromHTML(_Documentation.templates.searchOverlay());
			_Documentation.search.overlaySearchField = _Documentation.search.searchOverlay.querySelector('#overlay-search-field');
			_Documentation.search.searchResultsList  = _Documentation.search.searchOverlay.querySelector('#search-results ul');

			document.querySelector('body').insertAdjacentElement('beforeend', _Documentation.search.searchOverlay);

			document.querySelector('#search-field').addEventListener('click', e => {
				_Documentation.search.showSearch();
			});

			_Documentation.search.searchOverlay.addEventListener('click', e => {
				if (e.target?.id === 'search-overlay') {
					_Documentation.search.hideSearch();
					_Documentation.search.clearSearchResults();
				}
			});

			document.addEventListener('keydown', _Documentation.keyDownHandler);

			_Documentation.search.overlaySearchField.addEventListener('keyup', _Helpers.debounce(e => {
                _Documentation.search.doSearch();
			}, 500));

			_Documentation.search.overlaySearchField.addEventListener('search', e => {
				let query = e.target.value;
				if (query.trim() === '') {
					_Documentation.search.clearSearchResults();
				}
			});
		},
		dispose: () => {

			document.removeEventListener('keydown', _Documentation.keyDownHandler);

			_Helpers.fastRemoveElement(_Documentation.search.searchOverlay);

			delete _Documentation.search.searchOverlay;
			delete _Documentation.search.overlaySearchField;
			delete _Documentation.search.searchResultsList;
		},
		clearSearchResults: () => {
			_Documentation.search.searchResultsList.replaceChildren();
		},
        doSearch: () => {

            _Documentation.search.clearSearchResults();

            let searchText = _Documentation.search.overlaySearchField.value;
            if (searchText) {

                if (searchText.length < 3) {

                    let result = _Helpers.createSingleDOMElementFromHTML(`
                        <li class="search-result cursor-pointer">
                            <div class="group-aria-selected:text-sky-600">
                                <span>Please type at least 3 characters to search.</span>
                            </div>
                        </li>
                    `);

                    result.addEventListener('click', e => {
                        _Documentation.search.hideSearch();
                    });

                    _Documentation.search.searchResultsList.appendChild(result);

                } else {

                    fetch(`/structr/docs/ontology?search=${encodeURIComponent(searchText)}&details=all&levels=1&startLevel=0`)
                        .then(response => response.json())
                        .then(json => {

                            if (json.data) {

                                let num = json.data.length;
                                let count = 0;

                                if (num == 0) {

                                    let result = _Helpers.createSingleDOMElementFromHTML(`
                                                <li class="search-result">
                                                    <div class="text-gray-999 text-center">
                                                        <span>No results</span>
                                                    </div>
                                                </li>
                                            `);

                                    _Documentation.search.searchResultsList.appendChild(result);

                                } else {

                                    for (let entry of json.data) {

                                        if (count++ < 15) {

                                            let parents = _Documentation.search.extractParents(entry);
                                            let contextHint = _Documentation.search.formatContextHint(entry);
                                            let id = parents?.[0]?.id || entry.id;
                                            let type = entry.type;

                                            if (type === 'MarkdownFile' || type === 'MarkdownTopic') {
                                                type = 'Topic';
                                            }

                                            let result = _Helpers.createSingleDOMElementFromHTML(`
                                                <li class="search-result cursor-pointer">
                                                    <div class="group-aria-selected:text-sky-600">
                                                        <span>${entry.name}</span>
                                                    </div>
                                                    <div class="search-result-type">${type}</div>
                                                    <div class="search-result-description">${contextHint}<span class="sr-only">/</span></div>
                                                </li>
                                            `);

                                            result.addEventListener('click', e => {
                                                _Documentation.search.hideSearch();
                                                _Documentation.loadDoc(`${id}`, null, parents, entry.name);
                                            });

                                            _Documentation.search.searchResultsList.appendChild(result);

                                        } else {

                                            let result = _Helpers.createSingleDOMElementFromHTML(`
                                                <li class="search-result">
                                                    <div class="text-gray-999 text-center">
                                                        <span>Showing results 1 - 15 of ${num}</span>
                                                    </div>
                                                </li>
                                            `);

                                            _Documentation.search.searchResultsList.appendChild(result);

                                            break;
                                        }
                                    }
                                }
                            }
                        });
                }
            }
        },
        showSearch: () => {
            _Documentation.search.searchOverlay.classList.remove('hidden');
            _Documentation.search.overlaySearchField.value = '';
            _Documentation.search.overlaySearchField.focus();
        },
        hideSearch: () => {
            _Documentation.search.searchOverlay.classList.add('hidden');
        },
        extractParents: (entry) => {

            let parents = [];

            let parent = entry?.parents?.[0]?.targets?.[0];
            while (parent) {

                if (!parent.isToplevel) {
                    parents.push(parent);
                }

                parent = parent?.parents?.[0]?.targets?.[0];
            }

            parents = parents.reverse();

            return parents;
        },
        formatContextHint: (entry) => {

            if (entry?.shortDescription?.length) {
                return entry.shortDescription;
            }

            if (entry && entry.parents && entry.parents.length) {
                return entry.parents[0].targets.map(t => t.name).join(', ');
            }

            return '';
        }
    },
    createElementFromHTML: (html) => {
        let range = document.createRange();
        return range.createContextualFragment(html).firstElementChild;
    },
	templates: {
		main: config => `
			<link rel="stylesheet" type="text/css" media="screen" href="css/docs.css">
			<div id="docs-area">
				<nav>
					<ul id="docs-main-navigation">
					</ul>
				</nav>
				<iframe name="main-documentation"></iframe>
				<aside class="index"></aside>
			</div>`,
		searchField: config => `
			<div id="search-field">
				<svg aria-hidden="true" viewBox="0 0 20 20">
					<path fill="#555" d="M16.293 17.707a1 1 0 0 0 1.414-1.414l-1.414 1.414ZM9 14a5 5 0 0 1-5-5H2a7 7 0 0 0 7 7v-2ZM4 9a5 5 0 0 1 5-5V2a7 7 0 0 0-7 7h2Zm5-5a5 5 0 0 1 5 5h2a7 7 0 0 0-7-7v2Zm8.707 12.293-3.757-3.757-1.414 1.414 3.757 3.757 1.414-1.414ZM14 9a4.98 4.98 0 0 1-1.464 3.536l1.414 1.414A6.98 6.98 0 0 0 16 9h-2Zm-1.464 3.536A4.98 4.98 0 0 1 9 14v2a6.98 6.98 0 0 0 4.95-2.05l-1.414-1.414Z"></path>
				</svg>
				<input type="text" placeholder="Search docs">
			</div>
		`,
		mainNavigationItem: config => `<li class="docs-main-nav-item">${config.label}</li>`,
        mainNavigationSubItem: config => `<li><a target="main-documentation" href="/structr/docs/ontology?id=${config.id}&parent=${config.parent}&details=all">${config.label}</a></li>`,
		indexItem: config => `<h2><a target="main-documentation" href="/structr/docs/ontology?id=${config.id}&details=all#${config.name}">${config.label}</a></h2>`,
		searchOverlay: config => `
			<div id="search-overlay" class="hidden">
				<div id="search-overlay-dialog">
					<svg aria-hidden="true" viewbox="0 0 20 20">
						<path d="M16.293 17.707a1 1 0 0 0 1.414-1.414l-1.414 1.414ZM9 14a5 5 0 0 1-5-5H2a7 7 0 0 0 7 7v-2ZM4 9a5 5 0 0 1 5-5V2a7 7 0 0 0-7 7h2Zm5-5a5 5 0 0 1 5 5h2a7 7 0 0 0-7-7v2Zm8.707 12.293-3.757-3.757-1.414 1.414 3.757 3.757 1.414-1.414ZM14 9a4.98 4.98 0 0 1-1.464 3.536l1.414 1.414A6.98 6.98 0 0 0 16 9h-2Zm-1.464 3.536A4.98 4.98 0 0 1 9 14v2a6.98 6.98 0 0 0 4.95-2.05l-1.414-1.414Z"></path>
					</svg>
					<input spellcheck="false" id="overlay-search-field" autocomplete="off" type="search" maxlength="512" placeholder="Search docs" autofocus="autofocus" aria-autocomplete="both" aria-labelledby=":R2dja:-label" data-autofocus="true">
					<div id="search-results">
						<ul></ul>
					</div>
				</div>
			</div>
		`
	}
}
