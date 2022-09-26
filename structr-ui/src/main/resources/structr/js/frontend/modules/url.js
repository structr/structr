'use strict';

export class Handler {

	constructor(frontendModule) {
		this.frontendModule = frontendModule;
	}

	handleReloadTarget(reloadTarget, element, parameters, status, options) {

		// remove prefix "url:"
		let selector = reloadTarget.substring(4);

		// Add $ to {} to make string interpolation work
		let url = reloadTarget.substring(4).replaceAll('{', '${');

		// define replacement function
		let replace = new Function('result', 'return `' + url + '`;');

		// interpolate url string with data from result
		let value = replace(parameters);

		// go to URL
		window.location.href = value;
	}
}
