'use strict';

let debug = false;

import {Node} from "./model/Node.js";
import {StructrRest} from '../rest/StructrRest.js';

export class Persistence {

    constructor(basePath) {
        this._structrRest = new StructrRest(basePath);
    }


    async createNode(object) {
        let result = await this._persistObject(object);
        return result[0];
    }

    async deleteNode(object) {
        if(object.id !== undefined && object.type !== undefined) {
            return await this._structrRest.delete(object.type, object.id);
        }
        return undefined;
    }

    async getNodesByClass(model, view) {

        let object;

        if (model === undefined || model === null) {
            object = new Node();
        } else {
            object = model;
        }

        let result = await this._structrRest.get(object.type, view);
        return this._extractRestResult(result, object);

    }

    async getNodesById(id, model, view) {

        let object;

        if (model === undefined || model === null) {
            object = new Node();
        } else {
            object = model;
        }

        let result = await this._structrRest.getById(object.type, id, view);
        return this._extractRestResult(result, object);
    }

    async getNodesByName(name, model, view) {

        let object;

        if (model === undefined || model === null) {
            object = new Node();
        } else {
            object = model;
        }

        let result = await this._structrRest.getByName(object.type, name, view);
        return this._extractRestResult(result, object);
    }


    async _persistObject(object) {

        let result = {};

        if (object.id !== undefined && object.id !== null && object.type !== undefined && object.type !== null) {
            // Object exists in db
            await this._structrRest.put(object.type, object.id, Persistence._toSerializable(object));
			result = await this._structrRest.getById(object.type, object.id);

        } else if (object.type !== undefined && object.type !== null) {
            // Object has to be created
            result = await this._structrRest.post(object.type, Persistence._toSerializable(object));
            result = await this._structrRest.getById(object.type, result.result[0]);
        } else {
            console.log('_persistObject called with invalid parameters!');
        }

        return this._extractRestResult(result, object);
    }

    static _toSerializable(object) {
        let result = {};

        Object.keys(object).forEach(k => {
            if (k !== 'id' && k !== 'type') {
                result[k] = object[k];
            }
        });

        return result;
    }

    _extractRestResult(result, model) {
        let containers = [];

        if(result.result_count === 1) {
            if(Array.isArray(result.result)) {
                if  (result.result.length > 0) {
					containers.push(this._wrapObject(result.result[0], model));
				} else {
					return containers;
                }
            } else {
                containers.push(this._wrapObject(result.result, model));
            }
        } else if (result.result_count > 1) {
            for(let i = 0; i < result.result.length; i++) {
                containers.push(this._wrapObject(result.result[i], model));
            }
        }
        return containers;
    }

    _wrapObject(object, model) {
        if (object !== null && object !== undefined && model !== null && model !== undefined) {
            let curObj = new model.constructor();
            Object.keys(object).forEach(k => curObj[k] = object[k]);
            // Push new obj as proxy with handler for 2way data binding.
            return new Proxy(curObj, Node.getProxyHandler(this));
        } else {
            return null;
        }
    }

}

if (debug === true) {
    window._flowPersistence = new Persistence();
}
