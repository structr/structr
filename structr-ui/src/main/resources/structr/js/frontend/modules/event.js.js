'use strict';

export class Handler {

	constructor(frontendModule) {
		this.frontendModule = frontendModule;
	}

	handleReloadTarget(reloadTarget, element, parameters, status, options) {

		// remove prefix "event:"
		let event = reloadTarget.substring(6);

		element.dispatchEvent(new CustomEvent(event, { detail: { result: parameters, status: status } }));
	}
}
