'use strict';

let debug = false;

import { Node } from "./model/Node.js";
import { StructrRest } from '../rest/StructrRest.js';

export class Persistence {

    constructor() {
        this._structrRest = new StructrRest();
    }


    async createNode(object) {
        return await this._persistObject(object);
    }

    async getNodesByClass(model) {

        let object;

        if (model === undefined || model === null) {
            object = new Node();
        } else {
            object = model;
        }

        let result = await this._structrRest.get(object.type);
        return this._extractRestResult(result, object);

    }

    async getNodesById(id, model) {

        let object;

        if (model === undefined || model === null) {
            object = new Node();
        } else {
            object = model;
        }

        let result = await this._structrRest.getById(object.type, id);
        return this._extractRestResult(result, object);
    }

    async getNodesByName(name, model) {

        let object;

        if (model === undefined || model === null) {
            object = new Node();
        } else {
            object = model;
        }

        let result = await this._structrRest.getByName(object.type, name);
        return this._extractRestResult(result, object);
    }


    async _persistObject(object) {

        let result = {};

        if (object.id !== undefined && object.id !== null) {
            // Object exists in db
            let data = object;
            result = await this._structrRest.put(object.type, object.id, Persistence._toSerializable(object));

        } else {
            // Object has to be created
            result = await this._structrRest.post(object.type, Persistence._toSerializable(object));
            object.id = result.result[0];
            return new Proxy(object, Node.getProxyHandler(this));
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
            containers.push(this._wrapObject(result.result, model))
        } else if (result.result_count > 1) {
            for(let i = 0; i < result.result.length; i++) {
                containers.push(this._wrapObject(result.result[i], model));
            }
        }
        return containers;
    }

    _wrapObject(object, model) {
        let curObj = new model.constructor();
        Object.keys(object).forEach(k => curObj[k] = object[k]);
        // Push new obj as proxy with handler for 2way data binding.
        return new Proxy(curObj, Node.getProxyHandler(this));
    }

}

if (debug === true) {
    window._flowPersistence = new Persistence();
}