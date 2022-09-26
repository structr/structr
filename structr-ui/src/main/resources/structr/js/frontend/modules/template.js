'use strict';

export class Handler {

	constructor(frontendModule) {
		this.frontendModule = frontendModule;
	}

	handleReloadTarget(reloadTarget, element, parameters, status, options) {

		// remove prefix "template:"
		let selector = reloadTarget.substring(9);

		this.frontendModule.instantiateTemplate(selector, parameters, status, options);
	}
}
