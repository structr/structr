/**
 * Based on https://github.com/mrhenry/jquery-chosen-sortable/blob/master/jquery-chosen-sortable.js
 * with a couple of add-ons:
 *
 * 1. chosenOrder() returned elements twice if a substring was contained in an element ("id" and "hidden" for example)
 * 2. provide a callback function for update event
 * 3. add a "sortedVals" function to just return the values of the selected elements
 * 4. use selectedOptions as source for options as the chosen-choices elements retain a removed element for a minimal amount of time which will result in errors
 */
/*
 * Author: Yves Van Broekhoven & Simon Menke
 * Created at: 2012-07-05
 *
 * Requirements:
 * - jQuery
 * - jQuery UI
 * - Chosen
 *
 * Version: 1.0.0
 */
(function($) {

	$.fn.chosenOrder = function() {
		var $this = this.filter('.chosen-sortable[multiple]').first(),
			$chosen = $this.siblings('.chosen-container');

		var $selectedOptions = $(this[0].selectedOptions).toArray();

		return $($chosen.find('.chosen-choices li[class!="search-field"]').map( function() {
			if (this) {
				var text = $(this).text();
				return $selectedOptions.filter(function(el) {
					return $(el).text() === text;
				})[0];
			}
		}));
	};

	$.fn.sortedVals = function() {
		var options = this.chosenOrder();

		return options.toArray().map(function(o) {
			return $(o).val();
		});
	};

	/*
	 * Extend jQuery
	 */
	$.fn.chosenSortable = function(cb){
		var $this = this.filter('.chosen-sortable[multiple]');

		$this.each(function(){
			var $select = $(this);
			var $chosen = $select.siblings('.chosen-container');

			// On mousedown of choice element,
			// we don't want to display the dropdown list
			$chosen.find('.chosen-choices').bind('mousedown', function(event){
				if ($(event.target).is('span')) {
					event.stopPropagation();
				}
			});

			// Initialize jQuery UI Sortable
			$chosen.find('.chosen-choices').sortable({
				'placeholder' : 'ui-state-highlight',
				'items'       : 'li:not(.search-field)',
				'update'      : (typeof cb === "function") ? cb : function(){},
				'tolerance'   : 'pointer'
			});

			// Intercept form submit & order the chosens
			$select.closest('form').on('submit', function(){
				var $options = $select.chosenOrder();
				$select.children().remove();
				$select.append($options);
			});
		});
	};
}(jQuery));