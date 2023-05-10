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
(function($) {

    let selectionListContainerClass = 'select2-selection__rendered';
    let searchFieldClass            = 'select2-search';

    $.fn.select2Order = function() {

        let $selectElement    = $(this);
        let $select2Container = $selectElement.siblings('.select2-container');
        let selectedOptions   = [...this[0].selectedOptions];

        // map from rendered li elements to option elements
        return $($select2Container.find(`.${selectionListContainerClass} li[class!="${searchFieldClass}"]`).map(function() {
            let liTitle = $(this).attr('title');
            return selectedOptions.filter(el => ((el.value ?? el.textContent) === liTitle))[0];
        }));
    };

    $.fn.sortedValues = function() {

        let sortedOptions = this.select2Order();
        return sortedOptions.toArray().map(option => option.value);
    };

    $.fn.select2Sortable = function(callback) {

        let $selectElement    = $(this);
        let $select2Container = $selectElement.siblings('.select2-container');

        $select2Container.find(`.${selectionListContainerClass}`).sortable({
            placeholder: 'ui-state-highlight',
            items      : `li:not(.${searchFieldClass})`,
            update     : (typeof callback === "function") ? callback : () => {},
            tolerance  : 'pointer'
        });

        // Intercept form submit & order the select
        $selectElement.closest('form').on('submit', function() {
            let $sortedOptions = $selectElement.select2Order();
            $selectElement.children().remove();
            $selectElement.append($sortedOptions);
        });
    };
}(jQuery));