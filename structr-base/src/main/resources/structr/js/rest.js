/*
 * Copyright (C) 2010-2023 Structr GmbH
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

    let allElements = document.querySelectorAll('ul.collapsibleList > li');

    let expandAll = () => {
        for (let el of allElements) {
            el.classList.remove('collapsibleListClosed');
            el.classList.add('collapsibleListOpen');
        }
    };

    let collapseAll = () => {
        for (let el of allElements) {
            el.classList.remove('collapsibleListOpen');
            el.classList.add('collapsibleListClosed');
        }
    };

    let initialize = () => {

        let totalNumberOfRootElements = document.querySelectorAll('#right > ul > li > ul > li > ul.collapsibleList > li').length;
        for (let el of allElements) {

            if (el.querySelector('ul')) {

                if (totalNumberOfRootElements > 5) {
                    el.classList.add('collapsibleListClosed');
                } else {
                    el.classList.add('collapsibleListOpen');
                }

                el.addEventListener('click', (e) => {
                    e.stopPropagation();

                    if (e.target == el || e.target.parentNode === el) {
                        el.classList.toggle('collapsibleListClosed');
                        el.classList.toggle('collapsibleListOpen');
                    }
                });
            }
        }
    };

    initialize();

    document.querySelector('button.expand')?.addEventListener('click', expandAll);
    document.querySelector('button.collapse')?.addEventListener('click', collapseAll);
});