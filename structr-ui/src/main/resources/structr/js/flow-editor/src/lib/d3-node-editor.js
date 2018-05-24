/*!
* d3-node-editor v0.6.6
* (c) 2017 Vitaliy Stoliarov
* Released under the MIT License.
*/
/**
 * Copyright (c) 2014-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

!(function(global) {
  "use strict";

  var Op = Object.prototype;
  var hasOwn = Op.hasOwnProperty;
  var undefined; // More compressible than void 0.
  var $Symbol = typeof Symbol === "function" ? Symbol : {};
  var iteratorSymbol = $Symbol.iterator || "@@iterator";
  var asyncIteratorSymbol = $Symbol.asyncIterator || "@@asyncIterator";
  var toStringTagSymbol = $Symbol.toStringTag || "@@toStringTag";

  var inModule = typeof module === "object";
  var runtime = global.regeneratorRuntime;
  if (runtime) {
    if (inModule) {
      // If regeneratorRuntime is defined globally and we're in a module,
      // make the exports object identical to regeneratorRuntime.
      module.exports = runtime;
    }
    // Don't bother evaluating the rest of this file if the runtime was
    // already defined globally.
    return;
  }

  // Define the runtime globally (as expected by generated code) as either
  // module.exports (if we're in a module) or a new, empty object.
  runtime = global.regeneratorRuntime = inModule ? module.exports : {};

  function wrap(innerFn, outerFn, self, tryLocsList) {
    // If outerFn provided and outerFn.prototype is a Generator, then outerFn.prototype instanceof Generator.
    var protoGenerator = outerFn && outerFn.prototype instanceof Generator ? outerFn : Generator;
    var generator = Object.create(protoGenerator.prototype);
    var context = new Context(tryLocsList || []);

    // The ._invoke method unifies the implementations of the .next,
    // .throw, and .return methods.
    generator._invoke = makeInvokeMethod(innerFn, self, context);

    return generator;
  }
  runtime.wrap = wrap;

  // Try/catch helper to minimize deoptimizations. Returns a completion
  // record like context.tryEntries[i].completion. This interface could
  // have been (and was previously) designed to take a closure to be
  // invoked without arguments, but in all the cases we care about we
  // already have an existing method we want to call, so there's no need
  // to create a new function object. We can even get away with assuming
  // the method takes exactly one argument, since that happens to be true
  // in every case, so we don't have to touch the arguments object. The
  // only additional allocation required is the completion record, which
  // has a stable shape and so hopefully should be cheap to allocate.
  function tryCatch(fn, obj, arg) {
    try {
      return { type: "normal", arg: fn.call(obj, arg) };
    } catch (err) {
      return { type: "throw", arg: err };
    }
  }

  var GenStateSuspendedStart = "suspendedStart";
  var GenStateSuspendedYield = "suspendedYield";
  var GenStateExecuting = "executing";
  var GenStateCompleted = "completed";

  // Returning this object from the innerFn has the same effect as
  // breaking out of the dispatch switch statement.
  var ContinueSentinel = {};

  // Dummy constructor functions that we use as the .constructor and
  // .constructor.prototype properties for functions that return Generator
  // objects. For full spec compliance, you may wish to configure your
  // minifier not to mangle the names of these two functions.
  function Generator() {}
  function GeneratorFunction() {}
  function GeneratorFunctionPrototype() {}

  // This is a polyfill for %IteratorPrototype% for environments that
  // don't natively support it.
  var IteratorPrototype = {};
  IteratorPrototype[iteratorSymbol] = function () {
    return this;
  };

  var getProto = Object.getPrototypeOf;
  var NativeIteratorPrototype = getProto && getProto(getProto(values([])));
  if (NativeIteratorPrototype &&
      NativeIteratorPrototype !== Op &&
      hasOwn.call(NativeIteratorPrototype, iteratorSymbol)) {
    // This environment has a native %IteratorPrototype%; use it instead
    // of the polyfill.
    IteratorPrototype = NativeIteratorPrototype;
  }

  var Gp = GeneratorFunctionPrototype.prototype =
    Generator.prototype = Object.create(IteratorPrototype);
  GeneratorFunction.prototype = Gp.constructor = GeneratorFunctionPrototype;
  GeneratorFunctionPrototype.constructor = GeneratorFunction;
  GeneratorFunctionPrototype[toStringTagSymbol] =
    GeneratorFunction.displayName = "GeneratorFunction";

  // Helper for defining the .next, .throw, and .return methods of the
  // Iterator interface in terms of a single ._invoke method.
  function defineIteratorMethods(prototype) {
    ["next", "throw", "return"].forEach(function(method) {
      prototype[method] = function(arg) {
        return this._invoke(method, arg);
      };
    });
  }

  runtime.isGeneratorFunction = function(genFun) {
    var ctor = typeof genFun === "function" && genFun.constructor;
    return ctor
      ? ctor === GeneratorFunction ||
        // For the native GeneratorFunction constructor, the best we can
        // do is to check its .name property.
        (ctor.displayName || ctor.name) === "GeneratorFunction"
      : false;
  };

  runtime.mark = function(genFun) {
    if (Object.setPrototypeOf) {
      Object.setPrototypeOf(genFun, GeneratorFunctionPrototype);
    } else {
      genFun.__proto__ = GeneratorFunctionPrototype;
      if (!(toStringTagSymbol in genFun)) {
        genFun[toStringTagSymbol] = "GeneratorFunction";
      }
    }
    genFun.prototype = Object.create(Gp);
    return genFun;
  };

  // Within the body of any async function, `await x` is transformed to
  // `yield regeneratorRuntime.awrap(x)`, so that the runtime can test
  // `hasOwn.call(value, "__await")` to determine if the yielded value is
  // meant to be awaited.
  runtime.awrap = function(arg) {
    return { __await: arg };
  };

  function AsyncIterator(generator) {
    function invoke(method, arg, resolve, reject) {
      var record = tryCatch(generator[method], generator, arg);
      if (record.type === "throw") {
        reject(record.arg);
      } else {
        var result = record.arg;
        var value = result.value;
        if (value &&
            typeof value === "object" &&
            hasOwn.call(value, "__await")) {
          return Promise.resolve(value.__await).then(function(value) {
            invoke("next", value, resolve, reject);
          }, function(err) {
            invoke("throw", err, resolve, reject);
          });
        }

        return Promise.resolve(value).then(function(unwrapped) {
          // When a yielded Promise is resolved, its final value becomes
          // the .value of the Promise<{value,done}> result for the
          // current iteration. If the Promise is rejected, however, the
          // result for this iteration will be rejected with the same
          // reason. Note that rejections of yielded Promises are not
          // thrown back into the generator function, as is the case
          // when an awaited Promise is rejected. This difference in
          // behavior between yield and await is important, because it
          // allows the consumer to decide what to do with the yielded
          // rejection (swallow it and continue, manually .throw it back
          // into the generator, abandon iteration, whatever). With
          // await, by contrast, there is no opportunity to examine the
          // rejection reason outside the generator function, so the
          // only option is to throw it from the await expression, and
          // let the generator function handle the exception.
          result.value = unwrapped;
          resolve(result);
        }, reject);
      }
    }

    var previousPromise;

    function enqueue(method, arg) {
      function callInvokeWithMethodAndArg() {
        return new Promise(function(resolve, reject) {
          invoke(method, arg, resolve, reject);
        });
      }

      return previousPromise =
        // If enqueue has been called before, then we want to wait until
        // all previous Promises have been resolved before calling invoke,
        // so that results are always delivered in the correct order. If
        // enqueue has not been called before, then it is important to
        // call invoke immediately, without waiting on a callback to fire,
        // so that the async generator function has the opportunity to do
        // any necessary setup in a predictable way. This predictability
        // is why the Promise constructor synchronously invokes its
        // executor callback, and why async functions synchronously
        // execute code before the first await. Since we implement simple
        // async functions in terms of async generators, it is especially
        // important to get this right, even though it requires care.
        previousPromise ? previousPromise.then(
          callInvokeWithMethodAndArg,
          // Avoid propagating failures to Promises returned by later
          // invocations of the iterator.
          callInvokeWithMethodAndArg
        ) : callInvokeWithMethodAndArg();
    }

    // Define the unified helper method that is used to implement .next,
    // .throw, and .return (see defineIteratorMethods).
    this._invoke = enqueue;
  }

  defineIteratorMethods(AsyncIterator.prototype);
  AsyncIterator.prototype[asyncIteratorSymbol] = function () {
    return this;
  };
  runtime.AsyncIterator = AsyncIterator;

  // Note that simple async functions are implemented on top of
  // AsyncIterator objects; they just return a Promise for the value of
  // the final result produced by the iterator.
  runtime.async = function(innerFn, outerFn, self, tryLocsList) {
    var iter = new AsyncIterator(
      wrap(innerFn, outerFn, self, tryLocsList)
    );

    return runtime.isGeneratorFunction(outerFn)
      ? iter // If outerFn is a generator, return the full iterator.
      : iter.next().then(function(result) {
          return result.done ? result.value : iter.next();
        });
  };

  function makeInvokeMethod(innerFn, self, context) {
    var state = GenStateSuspendedStart;

    return function invoke(method, arg) {
      if (state === GenStateExecuting) {
        throw new Error("Generator is already running");
      }

      if (state === GenStateCompleted) {
        if (method === "throw") {
          throw arg;
        }

        // Be forgiving, per 25.3.3.3.3 of the spec:
        // https://people.mozilla.org/~jorendorff/es6-draft.html#sec-generatorresume
        return doneResult();
      }

      context.method = method;
      context.arg = arg;

      while (true) {
        var delegate = context.delegate;
        if (delegate) {
          var delegateResult = maybeInvokeDelegate(delegate, context);
          if (delegateResult) {
            if (delegateResult === ContinueSentinel) continue;
            return delegateResult;
          }
        }

        if (context.method === "next") {
          // Setting context._sent for legacy support of Babel's
          // function.sent implementation.
          context.sent = context._sent = context.arg;

        } else if (context.method === "throw") {
          if (state === GenStateSuspendedStart) {
            state = GenStateCompleted;
            throw context.arg;
          }

          context.dispatchException(context.arg);

        } else if (context.method === "return") {
          context.abrupt("return", context.arg);
        }

        state = GenStateExecuting;

        var record = tryCatch(innerFn, self, context);
        if (record.type === "normal") {
          // If an exception is thrown from innerFn, we leave state ===
          // GenStateExecuting and loop back for another invocation.
          state = context.done
            ? GenStateCompleted
            : GenStateSuspendedYield;

          if (record.arg === ContinueSentinel) {
            continue;
          }

          return {
            value: record.arg,
            done: context.done
          };

        } else if (record.type === "throw") {
          state = GenStateCompleted;
          // Dispatch the exception by looping back around to the
          // context.dispatchException(context.arg) call above.
          context.method = "throw";
          context.arg = record.arg;
        }
      }
    };
  }

  // Call delegate.iterator[context.method](context.arg) and handle the
  // result, either by returning a { value, done } result from the
  // delegate iterator, or by modifying context.method and context.arg,
  // setting context.delegate to null, and returning the ContinueSentinel.
  function maybeInvokeDelegate(delegate, context) {
    var method = delegate.iterator[context.method];
    if (method === undefined) {
      // A .throw or .return when the delegate iterator has no .throw
      // method always terminates the yield* loop.
      context.delegate = null;

      if (context.method === "throw") {
        if (delegate.iterator.return) {
          // If the delegate iterator has a return method, give it a
          // chance to clean up.
          context.method = "return";
          context.arg = undefined;
          maybeInvokeDelegate(delegate, context);

          if (context.method === "throw") {
            // If maybeInvokeDelegate(context) changed context.method from
            // "return" to "throw", let that override the TypeError below.
            return ContinueSentinel;
          }
        }

        context.method = "throw";
        context.arg = new TypeError(
          "The iterator does not provide a 'throw' method");
      }

      return ContinueSentinel;
    }

    var record = tryCatch(method, delegate.iterator, context.arg);

    if (record.type === "throw") {
      context.method = "throw";
      context.arg = record.arg;
      context.delegate = null;
      return ContinueSentinel;
    }

    var info = record.arg;

    if (! info) {
      context.method = "throw";
      context.arg = new TypeError("iterator result is not an object");
      context.delegate = null;
      return ContinueSentinel;
    }

    if (info.done) {
      // Assign the result of the finished delegate to the temporary
      // variable specified by delegate.resultName (see delegateYield).
      context[delegate.resultName] = info.value;

      // Resume execution at the desired location (see delegateYield).
      context.next = delegate.nextLoc;

      // If context.method was "throw" but the delegate handled the
      // exception, let the outer generator proceed normally. If
      // context.method was "next", forget context.arg since it has been
      // "consumed" by the delegate iterator. If context.method was
      // "return", allow the original .return call to continue in the
      // outer generator.
      if (context.method !== "return") {
        context.method = "next";
        context.arg = undefined;
      }

    } else {
      // Re-yield the result returned by the delegate method.
      return info;
    }

    // The delegate iterator is finished, so forget it and continue with
    // the outer generator.
    context.delegate = null;
    return ContinueSentinel;
  }

  // Define Generator.prototype.{next,throw,return} in terms of the
  // unified ._invoke helper method.
  defineIteratorMethods(Gp);

  Gp[toStringTagSymbol] = "Generator";

  // A Generator should always return itself as the iterator object when the
  // @@iterator function is called on it. Some browsers' implementations of the
  // iterator prototype chain incorrectly implement this, causing the Generator
  // object to not be returned from this call. This ensures that doesn't happen.
  // See https://github.com/facebook/regenerator/issues/274 for more details.
  Gp[iteratorSymbol] = function() {
    return this;
  };

  Gp.toString = function() {
    return "[object Generator]";
  };

  function pushTryEntry(locs) {
    var entry = { tryLoc: locs[0] };

    if (1 in locs) {
      entry.catchLoc = locs[1];
    }

    if (2 in locs) {
      entry.finallyLoc = locs[2];
      entry.afterLoc = locs[3];
    }

    this.tryEntries.push(entry);
  }

  function resetTryEntry(entry) {
    var record = entry.completion || {};
    record.type = "normal";
    delete record.arg;
    entry.completion = record;
  }

  function Context(tryLocsList) {
    // The root entry object (effectively a try statement without a catch
    // or a finally block) gives us a place to store values thrown from
    // locations where there is no enclosing try statement.
    this.tryEntries = [{ tryLoc: "root" }];
    tryLocsList.forEach(pushTryEntry, this);
    this.reset(true);
  }

  runtime.keys = function(object) {
    var keys = [];
    for (var key in object) {
      keys.push(key);
    }
    keys.reverse();

    // Rather than returning an object with a next method, we keep
    // things simple and return the next function itself.
    return function next() {
      while (keys.length) {
        var key = keys.pop();
        if (key in object) {
          next.value = key;
          next.done = false;
          return next;
        }
      }

      // To avoid creating an additional object, we just hang the .value
      // and .done properties off the next function object itself. This
      // also ensures that the minifier will not anonymize the function.
      next.done = true;
      return next;
    };
  };

  function values(iterable) {
    if (iterable) {
      var iteratorMethod = iterable[iteratorSymbol];
      if (iteratorMethod) {
        return iteratorMethod.call(iterable);
      }

      if (typeof iterable.next === "function") {
        return iterable;
      }

      if (!isNaN(iterable.length)) {
        var i = -1, next = function next() {
          while (++i < iterable.length) {
            if (hasOwn.call(iterable, i)) {
              next.value = iterable[i];
              next.done = false;
              return next;
            }
          }

          next.value = undefined;
          next.done = true;

          return next;
        };

        return next.next = next;
      }
    }

    // Return an iterator with no values.
    return { next: doneResult };
  }
  runtime.values = values;

  function doneResult() {
    return { value: undefined, done: true };
  }

  Context.prototype = {
    constructor: Context,

    reset: function(skipTempReset) {
      this.prev = 0;
      this.next = 0;
      // Resetting context._sent for legacy support of Babel's
      // function.sent implementation.
      this.sent = this._sent = undefined;
      this.done = false;
      this.delegate = null;

      this.method = "next";
      this.arg = undefined;

      this.tryEntries.forEach(resetTryEntry);

      if (!skipTempReset) {
        for (var name in this) {
          // Not sure about the optimal order of these conditions:
          if (name.charAt(0) === "t" &&
              hasOwn.call(this, name) &&
              !isNaN(+name.slice(1))) {
            this[name] = undefined;
          }
        }
      }
    },

    stop: function() {
      this.done = true;

      var rootEntry = this.tryEntries[0];
      var rootRecord = rootEntry.completion;
      if (rootRecord.type === "throw") {
        throw rootRecord.arg;
      }

      return this.rval;
    },

    dispatchException: function(exception) {
      if (this.done) {
        throw exception;
      }

      var context = this;
      function handle(loc, caught) {
        record.type = "throw";
        record.arg = exception;
        context.next = loc;

        if (caught) {
          // If the dispatched exception was caught by a catch block,
          // then let that catch block handle the exception normally.
          context.method = "next";
          context.arg = undefined;
        }

        return !! caught;
      }

      for (var i = this.tryEntries.length - 1; i >= 0; --i) {
        var entry = this.tryEntries[i];
        var record = entry.completion;

        if (entry.tryLoc === "root") {
          // Exception thrown outside of any try block that could handle
          // it, so set the completion value of the entire function to
          // throw the exception.
          return handle("end");
        }

        if (entry.tryLoc <= this.prev) {
          var hasCatch = hasOwn.call(entry, "catchLoc");
          var hasFinally = hasOwn.call(entry, "finallyLoc");

          if (hasCatch && hasFinally) {
            if (this.prev < entry.catchLoc) {
              return handle(entry.catchLoc, true);
            } else if (this.prev < entry.finallyLoc) {
              return handle(entry.finallyLoc);
            }

          } else if (hasCatch) {
            if (this.prev < entry.catchLoc) {
              return handle(entry.catchLoc, true);
            }

          } else if (hasFinally) {
            if (this.prev < entry.finallyLoc) {
              return handle(entry.finallyLoc);
            }

          } else {
            throw new Error("try statement without catch or finally");
          }
        }
      }
    },

    abrupt: function(type, arg) {
      for (var i = this.tryEntries.length - 1; i >= 0; --i) {
        var entry = this.tryEntries[i];
        if (entry.tryLoc <= this.prev &&
            hasOwn.call(entry, "finallyLoc") &&
            this.prev < entry.finallyLoc) {
          var finallyEntry = entry;
          break;
        }
      }

      if (finallyEntry &&
          (type === "break" ||
           type === "continue") &&
          finallyEntry.tryLoc <= arg &&
          arg <= finallyEntry.finallyLoc) {
        // Ignore the finally entry if control is not jumping to a
        // location outside the try/catch block.
        finallyEntry = null;
      }

      var record = finallyEntry ? finallyEntry.completion : {};
      record.type = type;
      record.arg = arg;

      if (finallyEntry) {
        this.method = "next";
        this.next = finallyEntry.finallyLoc;
        return ContinueSentinel;
      }

      return this.complete(record);
    },

    complete: function(record, afterLoc) {
      if (record.type === "throw") {
        throw record.arg;
      }

      if (record.type === "break" ||
          record.type === "continue") {
        this.next = record.arg;
      } else if (record.type === "return") {
        this.rval = this.arg = record.arg;
        this.method = "return";
        this.next = "end";
      } else if (record.type === "normal" && afterLoc) {
        this.next = afterLoc;
      }

      return ContinueSentinel;
    },

    finish: function(finallyLoc) {
      for (var i = this.tryEntries.length - 1; i >= 0; --i) {
        var entry = this.tryEntries[i];
        if (entry.finallyLoc === finallyLoc) {
          this.complete(entry.completion, entry.afterLoc);
          resetTryEntry(entry);
          return ContinueSentinel;
        }
      }
    },

    "catch": function(tryLoc) {
      for (var i = this.tryEntries.length - 1; i >= 0; --i) {
        var entry = this.tryEntries[i];
        if (entry.tryLoc === tryLoc) {
          var record = entry.completion;
          if (record.type === "throw") {
            var thrown = record.arg;
            resetTryEntry(entry);
          }
          return thrown;
        }
      }

      // The context.catch method must only be called with a location
      // argument that corresponds to a known catch block.
      throw new Error("illegal catch attempt");
    },

    delegateYield: function(iterable, resultName, nextLoc) {
      this.delegate = {
        iterator: values(iterable),
        resultName: resultName,
        nextLoc: nextLoc
      };

      if (this.method === "next") {
        // Deliberately forget the last sent value so that we don't
        // accidentally pass it on to the delegate.
        this.arg = undefined;
      }

      return ContinueSentinel;
    }
  };
})(
  // In sloppy mode, unbound `this` refers to the global object, fallback to
  // Function constructor if we're in global strict mode. That is sadly a form
  // of indirect eval which violates Content Security Policy.
  (function() { return this })() || Function("return this")()
);

(function (global, factory) {
	typeof exports === 'object' && typeof module !== 'undefined' ? factory(exports) :
	typeof define === 'function' && define.amd ? define(['exports'], factory) :
	(factory((global.D3NE = {})));
}(this, (function (exports) { 'use strict';

var _typeof = typeof Symbol === "function" && typeof Symbol.iterator === "symbol" ? function (obj) {
  return typeof obj;
} : function (obj) {
  return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj;
};











var classCallCheck = function (instance, Constructor) {
  if (!(instance instanceof Constructor)) {
    throw new TypeError("Cannot call a class as a function");
  }
};

var createClass = function () {
  function defineProperties(target, props) {
    for (var i = 0; i < props.length; i++) {
      var descriptor = props[i];
      descriptor.enumerable = descriptor.enumerable || false;
      descriptor.configurable = true;
      if ("value" in descriptor) descriptor.writable = true;
      Object.defineProperty(target, descriptor.key, descriptor);
    }
  }

  return function (Constructor, protoProps, staticProps) {
    if (protoProps) defineProperties(Constructor.prototype, protoProps);
    if (staticProps) defineProperties(Constructor, staticProps);
    return Constructor;
  };
}();









var inherits = function (subClass, superClass) {
  if (typeof superClass !== "function" && superClass !== null) {
    throw new TypeError("Super expression must either be null or a function, not " + typeof superClass);
  }

  subClass.prototype = Object.create(superClass && superClass.prototype, {
    constructor: {
      value: subClass,
      enumerable: false,
      writable: true,
      configurable: true
    }
  });
  if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass;
};











var possibleConstructorReturn = function (self, call) {
  if (!self) {
    throw new ReferenceError("this hasn't been initialised - super() hasn't been called");
  }

  return call && (typeof call === "object" || typeof call === "function") ? call : self;
};



















var toConsumableArray = function (arr) {
  if (Array.isArray(arr)) {
    for (var i = 0, arr2 = Array(arr.length); i < arr.length; i++) arr2[i] = arr[i];

    return arr2;
  } else {
    return Array.from(arr);
  }
};

var Module = function () {
    function Module(data, titlesInput, titlesOutput) {
        classCallCheck(this, Module);

        var inputs = Module.extractNodes(data, titlesInput);
        var outputs = Module.extractNodes(data, titlesOutput);

        this.inputs = [];
        this.outputs = [];
        this.keys = {
            input: inputs.map(function (n) {
                return n.data.name;
            }),
            output: outputs.map(function (n) {
                return n.data.name;
            })
        };
    }

    createClass(Module, [{
        key: "read",
        value: function read(inputs) {
            var _this = this;

            this.keys.input.forEach(function (key, i) {
                _this.inputs[key] = inputs[i];
            });
        }
    }, {
        key: "write",
        value: function write(outputs) {
            var _this2 = this;

            this.keys.output.forEach(function (k, i) {
                outputs[i] = _this2.outputs[k];
            });
        }
    }], [{
        key: "extractNodes",
        value: function extractNodes(data, titles) {
            return Object.keys(data.nodes).filter(function (k) {
                return titles.includes(data.nodes[k].title);
            }).map(function (k) {
                return data.nodes[k];
            }).sort(function (n1, n2) {
                return n1.position[1] > n2.position[1];
            });
        }
    }]);
    return Module;
}();

var ModuleManager = function () {
    function ModuleManager(titlesInput, titlesOutput) {
        classCallCheck(this, ModuleManager);

        this.engine = null;
        this.titlesInput = titlesInput;
        this.titlesOutput = titlesOutput;
    }

    createClass(ModuleManager, [{
        key: "getInputs",
        value: function getInputs(data) {
            return Module.extractNodes(data, this.titlesInput).map(function (n) {
                return { title: n.title, name: n.data.name };
            });
        }
    }, {
        key: "getOutputs",
        value: function getOutputs(data) {
            return Module.extractNodes(data, this.titlesOutput).map(function (n) {
                return { title: n.title, name: n.data.name };
            });
        }
    }, {
        key: "workerModule",
        value: function workerModule(node, inputs, outputs) {
            var data, m, engine;
            return regeneratorRuntime.async(function workerModule$(_context) {
                while (1) {
                    switch (_context.prev = _context.next) {
                        case 0:
                            data = node.data.module.data;
                            m = new Module(data, this.titlesInput, this.titlesOutput);
                            engine = this.engine.clone();


                            m.read(inputs);
                            _context.next = 6;
                            return regeneratorRuntime.awrap(engine.process(data, null, m));

                        case 6:
                            m.write(outputs);

                        case 7:
                        case "end":
                            return _context.stop();
                    }
                }
            }, null, this);
        }
    }, {
        key: "workerInputs",
        value: function workerInputs(node, inputs, outputs, module) {
            if (module) outputs[0] = module.inputs[node.data.name][0];
        }
    }, {
        key: "workerOutputs",
        value: function workerOutputs(node, inputs, outputs, module) {
            if (module) module.outputs[node.data.name] = inputs[0][0];
        }
    }, {
        key: "setEngine",
        value: function setEngine(engine) {
            this.engine = engine;
        }
    }]);
    return ModuleManager;
}();

var Block = function () {
    function Block(Class) {
        classCallCheck(this, Block);

        if (this.constructor === Block) throw new TypeError('Cannot construct Block instances');

        this.id = Block.incrementId(Class);
        this.position = [0.0, 0.0];
        this.width = 0;
        this.height = 0;
        this.style = {};
    }

    createClass(Block, null, [{
        key: 'incrementId',
        value: function incrementId(Class) {
            if (!Class.latestId) Class.latestId = 1;else Class.latestId++;
            return Class.latestId;
        }
    }]);
    return Block;
}();

var Control = function () {
    function Control(html) {
        var handler = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : function () {};
        classCallCheck(this, Control);

        if (!(typeof html === 'string')) {
            throw new TypeError("Value of argument \"html\" violates contract.\n\nExpected:\nstring\n\nGot:\n" + _inspect$1(html));
        }

        this.html = html;
        this.parent = null;
        this.handler = handler;
    }

    createClass(Control, [{
        key: "getNode",
        value: function getNode() {
            if (this.parent === null) throw new Error("Control isn't added to Node/Input");

            return this.parent instanceof Node ? this.parent : this.parent.node;
        }
    }, {
        key: "getData",
        value: function getData(key) {
            return this.getNode().data[key];
        }
    }, {
        key: "putData",
        value: function putData(key, data) {
            this.getNode().data[key] = data;
        }
    }]);
    return Control;
}();

function _inspect$1(input, depth) {
    var maxDepth = 4;
    var maxKeys = 15;

    if (depth === undefined) {
        depth = 0;
    }

    depth += 1;

    if (input === null) {
        return 'null';
    } else if (input === undefined) {
        return 'void';
    } else if (typeof input === 'string' || typeof input === 'number' || typeof input === 'boolean') {
        return typeof input === "undefined" ? "undefined" : _typeof(input);
    } else if (Array.isArray(input)) {
        if (input.length > 0) {
            if (depth > maxDepth) return '[...]';

            var first = _inspect$1(input[0], depth);

            if (input.every(function (item) {
                return _inspect$1(item, depth) === first;
            })) {
                return first.trim() + '[]';
            } else {
                return '[' + input.slice(0, maxKeys).map(function (item) {
                    return _inspect$1(item, depth);
                }).join(', ') + (input.length >= maxKeys ? ', ...' : '') + ']';
            }
        } else {
            return 'Array';
        }
    } else {
        var keys = Object.keys(input);

        if (!keys.length) {
            if (input.constructor && input.constructor.name && input.constructor.name !== 'Object') {
                return input.constructor.name;
            } else {
                return 'Object';
            }
        }

        if (depth > maxDepth) return '{...}';
        var indent = '  '.repeat(depth - 1);
        var entries = keys.slice(0, maxKeys).map(function (key) {
            return (/^([A-Z_$][A-Z0-9_$]*)$/i.test(key) ? key : JSON.stringify(key)) + ': ' + _inspect$1(input[key], depth) + ';';
        }).join('\n  ' + indent);

        if (keys.length >= maxKeys) {
            entries += '\n  ' + indent + '...';
        }

        if (input.constructor && input.constructor.name && input.constructor.name !== 'Object') {
            return input.constructor.name + ' {\n  ' + indent + entries + '\n' + indent + '}';
        } else {
            return '{\n  ' + indent + entries + '\n' + indent + '}';
        }
    }
}

var Connection = function () {
    function Connection(output, input) {
        classCallCheck(this, Connection);

        this.output = output;
        this.input = input;
        this.style = {};

        this.input.addConnection(this);
    }

    createClass(Connection, [{
        key: "remove",
        value: function remove() {
            this.input.removeConnection(this);
            this.output.removeConnection(this);
        }
    }]);
    return Connection;
}();

var Socket = function () {
    function Socket(id, name, hint) {
        classCallCheck(this, Socket);

        if (!(typeof id === 'string')) {
            throw new TypeError("Value of argument \"id\" violates contract.\n\nExpected:\nstring\n\nGot:\n" + _inspect$3(id));
        }

        if (!(typeof name === 'string')) {
            throw new TypeError("Value of argument \"name\" violates contract.\n\nExpected:\nstring\n\nGot:\n" + _inspect$3(name));
        }

        if (!(typeof hint === 'string')) {
            throw new TypeError("Value of argument \"hint\" violates contract.\n\nExpected:\nstring\n\nGot:\n" + _inspect$3(hint));
        }

        this.id = id;
        this.name = name;
        this.hint = hint;
        this.compatible = [];
    }

    createClass(Socket, [{
        key: "combineWith",
        value: function combineWith(socket) {
            if (!(socket instanceof Socket)) {
                throw new TypeError("Value of argument \"socket\" violates contract.\n\nExpected:\nSocket\n\nGot:\n" + _inspect$3(socket));
            }

            this.compatible.push(socket);
        }
    }, {
        key: "compatibleWith",
        value: function compatibleWith(socket) {
            if (!(socket instanceof Socket)) {
                throw new TypeError("Value of argument \"socket\" violates contract.\n\nExpected:\nSocket\n\nGot:\n" + _inspect$3(socket));
            }

            return this === socket || this.compatible.includes(socket);
        }
    }]);
    return Socket;
}();

function _inspect$3(input, depth) {
    var maxDepth = 4;
    var maxKeys = 15;

    if (depth === undefined) {
        depth = 0;
    }

    depth += 1;

    if (input === null) {
        return 'null';
    } else if (input === undefined) {
        return 'void';
    } else if (typeof input === 'string' || typeof input === 'number' || typeof input === 'boolean') {
        return typeof input === "undefined" ? "undefined" : _typeof(input);
    } else if (Array.isArray(input)) {
        if (input.length > 0) {
            if (depth > maxDepth) return '[...]';

            var first = _inspect$3(input[0], depth);

            if (input.every(function (item) {
                return _inspect$3(item, depth) === first;
            })) {
                return first.trim() + '[]';
            } else {
                return '[' + input.slice(0, maxKeys).map(function (item) {
                    return _inspect$3(item, depth);
                }).join(', ') + (input.length >= maxKeys ? ', ...' : '') + ']';
            }
        } else {
            return 'Array';
        }
    } else {
        var keys = Object.keys(input);

        if (!keys.length) {
            if (input.constructor && input.constructor.name && input.constructor.name !== 'Object') {
                return input.constructor.name;
            } else {
                return 'Object';
            }
        }

        if (depth > maxDepth) return '{...}';
        var indent = '  '.repeat(depth - 1);
        var entries = keys.slice(0, maxKeys).map(function (key) {
            return (/^([A-Z_$][A-Z0-9_$]*)$/i.test(key) ? key : JSON.stringify(key)) + ': ' + _inspect$3(input[key], depth) + ';';
        }).join('\n  ' + indent);

        if (keys.length >= maxKeys) {
            entries += '\n  ' + indent + '...';
        }

        if (input.constructor && input.constructor.name && input.constructor.name !== 'Object') {
            return input.constructor.name + ' {\n  ' + indent + entries + '\n' + indent + '}';
        } else {
            return '{\n  ' + indent + entries + '\n' + indent + '}';
        }
    }
}

var IO = function () {
    function IO(title, socket, multiConns) {
        classCallCheck(this, IO);

        this.node = null;
        this.multipleConnections = multiConns;
        this.connections = [];

        this.title = title;
        this.socket = socket;
    }

    createClass(IO, [{
        key: 'removeConnection',
        value: function removeConnection(connection) {
            if (!(connection instanceof Connection)) {
                throw new TypeError('Value of argument "connection" violates contract.\n\nExpected:\nConnection\n\nGot:\n' + _inspect$4(connection));
            }

            this.connections.splice(this.connections.indexOf(connection), 1);
        }
    }]);
    return IO;
}();

function _inspect$4(input, depth) {
    var maxDepth = 4;
    var maxKeys = 15;

    if (depth === undefined) {
        depth = 0;
    }

    depth += 1;

    if (input === null) {
        return 'null';
    } else if (input === undefined) {
        return 'void';
    } else if (typeof input === 'string' || typeof input === 'number' || typeof input === 'boolean') {
        return typeof input === 'undefined' ? 'undefined' : _typeof(input);
    } else if (Array.isArray(input)) {
        if (input.length > 0) {
            if (depth > maxDepth) return '[...]';

            var first = _inspect$4(input[0], depth);

            if (input.every(function (item) {
                return _inspect$4(item, depth) === first;
            })) {
                return first.trim() + '[]';
            } else {
                return '[' + input.slice(0, maxKeys).map(function (item) {
                    return _inspect$4(item, depth);
                }).join(', ') + (input.length >= maxKeys ? ', ...' : '') + ']';
            }
        } else {
            return 'Array';
        }
    } else {
        var keys = Object.keys(input);

        if (!keys.length) {
            if (input.constructor && input.constructor.name && input.constructor.name !== 'Object') {
                return input.constructor.name;
            } else {
                return 'Object';
            }
        }

        if (depth > maxDepth) return '{...}';
        var indent = '  '.repeat(depth - 1);
        var entries = keys.slice(0, maxKeys).map(function (key) {
            return (/^([A-Z_$][A-Z0-9_$]*)$/i.test(key) ? key : JSON.stringify(key)) + ': ' + _inspect$4(input[key], depth) + ';';
        }).join('\n  ' + indent);

        if (keys.length >= maxKeys) {
            entries += '\n  ' + indent + '...';
        }

        if (input.constructor && input.constructor.name && input.constructor.name !== 'Object') {
            return input.constructor.name + ' {\n  ' + indent + entries + '\n' + indent + '}';
        } else {
            return '{\n  ' + indent + entries + '\n' + indent + '}';
        }
    }
}

var Input = function (_IO) {
    inherits(Input, _IO);

    function Input(title, socket) {
        var multiConns = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : false;
        classCallCheck(this, Input);

        if (!(typeof title === 'string')) {
            throw new TypeError('Value of argument "title" violates contract.\n\nExpected:\nstring\n\nGot:\n' + _inspect$2(title));
        }

        if (!(socket instanceof Socket)) {
            throw new TypeError('Value of argument "socket" violates contract.\n\nExpected:\nSocket\n\nGot:\n' + _inspect$2(socket));
        }

        if (!(typeof multiConns === 'boolean')) {
            throw new TypeError('Value of argument "multiConns" violates contract.\n\nExpected:\nboolean\n\nGot:\n' + _inspect$2(multiConns));
        }

        var _this = possibleConstructorReturn(this, (Input.__proto__ || Object.getPrototypeOf(Input)).call(this, title, socket, multiConns));

        _this.control = null;
        return _this;
    }

    createClass(Input, [{
        key: 'hasConnection',
        value: function hasConnection() {
            return this.connections.length > 0;
        }
    }, {
        key: 'addConnection',
        value: function addConnection(connection) {
            if (!(connection instanceof Connection)) {
                throw new TypeError('Value of argument "connection" violates contract.\n\nExpected:\nConnection\n\nGot:\n' + _inspect$2(connection));
            }

            if (!this.multipleConnections && this.hasConnection()) throw new Error('Multiple connections not allowed');
            this.connections.push(connection);
        }
    }, {
        key: 'addControl',
        value: function addControl(control) {
            if (!(control instanceof Control)) {
                throw new TypeError('Value of argument "control" violates contract.\n\nExpected:\nControl\n\nGot:\n' + _inspect$2(control));
            }

            this.control = control;
            control.parent = this;
        }
    }, {
        key: 'showControl',
        value: function showControl() {
            return !this.hasConnection() && this.control !== null;
        }
    }, {
        key: 'toJSON',
        value: function toJSON() {
            return {
                'connections': this.connections.map(function (c) {
                    return {
                        node: c.output.node.id,
                        output: c.output.node.outputs.indexOf(c.output)
                    };
                })
            };
        }
    }]);
    return Input;
}(IO);

function _inspect$2(input, depth) {
    var maxDepth = 4;
    var maxKeys = 15;

    if (depth === undefined) {
        depth = 0;
    }

    depth += 1;

    if (input === null) {
        return 'null';
    } else if (input === undefined) {
        return 'void';
    } else if (typeof input === 'string' || typeof input === 'number' || typeof input === 'boolean') {
        return typeof input === 'undefined' ? 'undefined' : _typeof(input);
    } else if (Array.isArray(input)) {
        if (input.length > 0) {
            if (depth > maxDepth) return '[...]';

            var first = _inspect$2(input[0], depth);

            if (input.every(function (item) {
                return _inspect$2(item, depth) === first;
            })) {
                return first.trim() + '[]';
            } else {
                return '[' + input.slice(0, maxKeys).map(function (item) {
                    return _inspect$2(item, depth);
                }).join(', ') + (input.length >= maxKeys ? ', ...' : '') + ']';
            }
        } else {
            return 'Array';
        }
    } else {
        var keys = Object.keys(input);

        if (!keys.length) {
            if (input.constructor && input.constructor.name && input.constructor.name !== 'Object') {
                return input.constructor.name;
            } else {
                return 'Object';
            }
        }

        if (depth > maxDepth) return '{...}';
        var indent = '  '.repeat(depth - 1);
        var entries = keys.slice(0, maxKeys).map(function (key) {
            return (/^([A-Z_$][A-Z0-9_$]*)$/i.test(key) ? key : JSON.stringify(key)) + ': ' + _inspect$2(input[key], depth) + ';';
        }).join('\n  ' + indent);

        if (keys.length >= maxKeys) {
            entries += '\n  ' + indent + '...';
        }

        if (input.constructor && input.constructor.name && input.constructor.name !== 'Object') {
            return input.constructor.name + ' {\n  ' + indent + entries + '\n' + indent + '}';
        } else {
            return '{\n  ' + indent + entries + '\n' + indent + '}';
        }
    }
}

var Output = function (_IO) {
    inherits(Output, _IO);

    function Output(title, socket) {
        var multiConns = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : true;
        classCallCheck(this, Output);

        if (!(typeof title === 'string')) {
            throw new TypeError('Value of argument "title" violates contract.\n\nExpected:\nstring\n\nGot:\n' + _inspect$5(title));
        }

        if (!(socket instanceof Socket)) {
            throw new TypeError('Value of argument "socket" violates contract.\n\nExpected:\nSocket\n\nGot:\n' + _inspect$5(socket));
        }

        if (!(typeof multiConns === 'boolean')) {
            throw new TypeError('Value of argument "multiConns" violates contract.\n\nExpected:\nboolean\n\nGot:\n' + _inspect$5(multiConns));
        }

        return possibleConstructorReturn(this, (Output.__proto__ || Object.getPrototypeOf(Output)).call(this, title, socket, multiConns));
    }

    createClass(Output, [{
        key: 'hasConnection',
        value: function hasConnection() {
            return this.connections.length > 0;
        }
    }, {
        key: 'connectTo',
        value: function connectTo(input) {
            if (!(input instanceof Input)) {
                throw new TypeError('Value of argument "input" violates contract.\n\nExpected:\nInput\n\nGot:\n' + _inspect$5(input));
            }

            if (!this.socket.compatibleWith(input.socket)) throw new Error('Sockets not compatible');
            if (!input.multipleConnections && input.hasConnection()) throw new Error('Input already has one connection');
            if (!this.multipleConnections && this.hasConnection()) throw new Error('Output already has one connection');

            var connection = new Connection(this, input);

            this.connections.push(connection);
            return connection;
        }
    }, {
        key: 'connectedTo',
        value: function connectedTo(input) {
            if (!(input instanceof Input)) {
                throw new TypeError('Value of argument "input" violates contract.\n\nExpected:\nInput\n\nGot:\n' + _inspect$5(input));
            }

            return this.connections.some(function (item) {
                return item.input === input;
            });
        }
    }, {
        key: 'toJSON',
        value: function toJSON() {
            return {
                'connections': this.connections.map(function (c) {
                    return {
                        node: c.input.node.id,
                        input: c.input.node.inputs.indexOf(c.input)
                    };
                })
            };
        }
    }]);
    return Output;
}(IO);

function _inspect$5(input, depth) {
    var maxDepth = 4;
    var maxKeys = 15;

    if (depth === undefined) {
        depth = 0;
    }

    depth += 1;

    if (input === null) {
        return 'null';
    } else if (input === undefined) {
        return 'void';
    } else if (typeof input === 'string' || typeof input === 'number' || typeof input === 'boolean') {
        return typeof input === 'undefined' ? 'undefined' : _typeof(input);
    } else if (Array.isArray(input)) {
        if (input.length > 0) {
            if (depth > maxDepth) return '[...]';

            var first = _inspect$5(input[0], depth);

            if (input.every(function (item) {
                return _inspect$5(item, depth) === first;
            })) {
                return first.trim() + '[]';
            } else {
                return '[' + input.slice(0, maxKeys).map(function (item) {
                    return _inspect$5(item, depth);
                }).join(', ') + (input.length >= maxKeys ? ', ...' : '') + ']';
            }
        } else {
            return 'Array';
        }
    } else {
        var keys = Object.keys(input);

        if (!keys.length) {
            if (input.constructor && input.constructor.name && input.constructor.name !== 'Object') {
                return input.constructor.name;
            } else {
                return 'Object';
            }
        }

        if (depth > maxDepth) return '{...}';
        var indent = '  '.repeat(depth - 1);
        var entries = keys.slice(0, maxKeys).map(function (key) {
            return (/^([A-Z_$][A-Z0-9_$]*)$/i.test(key) ? key : JSON.stringify(key)) + ': ' + _inspect$5(input[key], depth) + ';';
        }).join('\n  ' + indent);

        if (keys.length >= maxKeys) {
            entries += '\n  ' + indent + '...';
        }

        if (input.constructor && input.constructor.name && input.constructor.name !== 'Object') {
            return input.constructor.name + ' {\n  ' + indent + entries + '\n' + indent + '}';
        } else {
            return '{\n  ' + indent + entries + '\n' + indent + '}';
        }
    }
}

var Node = function (_Block) {
    inherits(Node, _Block);

    function Node(title) {
        classCallCheck(this, Node);

        if (!(typeof title === 'string')) {
            throw new TypeError('Value of argument "title" violates contract.\n\nExpected:\nstring\n\nGot:\n' + _inspect(title));
        }

        var _this = possibleConstructorReturn(this, (Node.__proto__ || Object.getPrototypeOf(Node)).call(this, Node));

        _this.group = null;
        _this.inputs = [];
        _this.outputs = [];
        _this.controls = [];
        _this.data = {};

        _this.title = title;
        var _ref = [180, 100];
        _this.width = _ref[0];
        _this.height = _ref[1];
        return _this;
    }

    createClass(Node, [{
        key: 'addControl',
        value: function addControl(control) {
            var index = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : null;

            if (!(control instanceof Control)) {
                throw new TypeError('Value of argument "control" violates contract.\n\nExpected:\nControl\n\nGot:\n' + _inspect(control));
            }

            if (!(index == null || typeof index === 'number' && !isNaN(index) && index >= 0 && index <= 255 && index === Math.floor(index))) {
                throw new TypeError('Value of argument "index" violates contract.\n\nExpected:\n?uint8\n\nGot:\n' + _inspect(index));
            }

            control.parent = this;

            if (index !== null) this.controls.splice(index, 0, control);else this.controls.push(control);

            return this;
        }
    }, {
        key: 'addInput',
        value: function addInput(input) {
            var index = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : null;

            if (!(input instanceof Input)) {
                throw new TypeError('Value of argument "input" violates contract.\n\nExpected:\nInput\n\nGot:\n' + _inspect(input));
            }

            if (!(index == null || typeof index === 'number' && !isNaN(index) && index >= 0 && index <= 255 && index === Math.floor(index))) {
                throw new TypeError('Value of argument "index" violates contract.\n\nExpected:\n?uint8\n\nGot:\n' + _inspect(index));
            }

            if (input.node !== null) throw new Error('Input has already been added to the node');

            input.node = this;

            if (index !== null) this.inputs.splice(index, 0, input);else this.inputs.push(input);

            return this;
        }
    }, {
        key: 'addOutput',
        value: function addOutput(output) {
            var index = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : null;

            if (!(output instanceof Output)) {
                throw new TypeError('Value of argument "output" violates contract.\n\nExpected:\nOutput\n\nGot:\n' + _inspect(output));
            }

            if (!(index == null || typeof index === 'number' && !isNaN(index) && index >= 0 && index <= 255 && index === Math.floor(index))) {
                throw new TypeError('Value of argument "index" violates contract.\n\nExpected:\n?uint8\n\nGot:\n' + _inspect(index));
            }

            if (output.node !== null) throw new Error('Output has already been added to the node');

            output.node = this;

            if (index !== null) this.outputs.splice(index, 0, output);else this.outputs.push(output);

            return this;
        }
    }, {
        key: 'getConnections',
        value: function getConnections(type) {
            var conns = [];

            if (type === 'input' || !type) this.inputs.map(function (input) {
                input.connections.forEach(function (c) {
                    conns.push(c);
                });
            });

            if (type === 'output' || !type) this.outputs.forEach(function (output) {
                output.connections.forEach(function (c) {
                    conns.push(c);
                });
            });
            return conns;
        }
    }, {
        key: 'inputsWithVisibleControl',
        value: function inputsWithVisibleControl() {
            return this.inputs.filter(function (input) {
                return input.showControl();
            });
        }
    }, {
        key: 'toJSON',
        value: function toJSON() {
            return {
                'id': this.id,
                'data': this.data,
                'group': this.group ? this.group.id : null,
                'inputs': this.inputs.map(function (input) {
                    return input.toJSON();
                }),
                'outputs': this.outputs.map(function (output) {
                    return output.toJSON();
                }),
                'position': this.position,
                'title': this.title
            };
        }
    }], [{
        key: 'fromJSON',
        value: function fromJSON(component, json) {
            var node;
            return regeneratorRuntime.async(function fromJSON$(_context) {
                while (1) {
                    switch (_context.prev = _context.next) {
                        case 0:
                            if (component instanceof Component) {
                                _context.next = 2;
                                break;
                            }

                            throw new TypeError('Value of argument "component" violates contract.\n\nExpected:\nComponent\n\nGot:\n' + _inspect(component));

                        case 2:
                            if (json instanceof Object) {
                                _context.next = 4;
                                break;
                            }

                            throw new TypeError('Value of argument "json" violates contract.\n\nExpected:\nObject\n\nGot:\n' + _inspect(json));

                        case 4:
                            node = component.newNode();


                            node.id = json.id;
                            node.data = json.data;
                            Node.latestId = Math.max(node.id, Node.latestId);
                            node.position = json.position;
                            node.title = json.title;

                            _context.next = 12;
                            return regeneratorRuntime.awrap(component.builder(node));

                        case 12:
                            return _context.abrupt('return', node);

                        case 13:
                        case 'end':
                            return _context.stop();
                    }
                }
            }, null, this);
        }
    }]);
    return Node;
}(Block);

function _inspect(input, depth) {
    var maxDepth = 4;
    var maxKeys = 15;

    if (depth === undefined) {
        depth = 0;
    }

    depth += 1;

    if (input === null) {
        return 'null';
    } else if (input === undefined) {
        return 'void';
    } else if (typeof input === 'string' || typeof input === 'number' || typeof input === 'boolean') {
        return typeof input === 'undefined' ? 'undefined' : _typeof(input);
    } else if (Array.isArray(input)) {
        if (input.length > 0) {
            if (depth > maxDepth) return '[...]';

            var first = _inspect(input[0], depth);

            if (input.every(function (item) {
                return _inspect(item, depth) === first;
            })) {
                return first.trim() + '[]';
            } else {
                return '[' + input.slice(0, maxKeys).map(function (item) {
                    return _inspect(item, depth);
                }).join(', ') + (input.length >= maxKeys ? ', ...' : '') + ']';
            }
        } else {
            return 'Array';
        }
    } else {
        var keys = Object.keys(input);

        if (!keys.length) {
            if (input.constructor && input.constructor.name && input.constructor.name !== 'Object') {
                return input.constructor.name;
            } else {
                return 'Object';
            }
        }

        if (depth > maxDepth) return '{...}';
        var indent = '  '.repeat(depth - 1);
        var entries = keys.slice(0, maxKeys).map(function (key) {
            return (/^([A-Z_$][A-Z0-9_$]*)$/i.test(key) ? key : JSON.stringify(key)) + ': ' + _inspect(input[key], depth) + ';';
        }).join('\n  ' + indent);

        if (keys.length >= maxKeys) {
            entries += '\n  ' + indent + '...';
        }

        if (input.constructor && input.constructor.name && input.constructor.name !== 'Object') {
            return input.constructor.name + ' {\n  ' + indent + entries + '\n' + indent + '}';
        } else {
            return '{\n  ' + indent + entries + '\n' + indent + '}';
        }
    }
}

function template (locals) {
  var pug_html = "";var pug_debug_filename, pug_debug_line;try {
    var pug_debug_sources = {};
    pug_html = pug_html + "<div class=\"title\">";
    pug_html = pug_html + "{{node.title}}</div>";
    pug_html = pug_html + "<!-- Outputs-->";
    pug_html = pug_html + "<div al-repeat=\"output in node.outputs\" style=\"text-align: right;\">";
    pug_html = pug_html + "<div class=\"output-title\">";
    pug_html = pug_html + "{{output.title}}</div>";
    pug_html = pug_html + "<div class=\"socket output {{output.socket.id.toLowerCase().replace(' ','-')}}\" al-pick-output title=\"{{output.socket.name}}\n{{output.socket.hint}}\"></div></div>";
    pug_html = pug_html + "<!-- Controls-->";
    pug_html = pug_html + "<div class=\"control\" al-repeat=\"control in node.controls\" style=\"text-align: center;\" :width=\"control.parent.width - 2 * control.margin\" :height=\"control.height\" al-control=\"control\"></div>";
    pug_html = pug_html + "<!-- Inputs-->";
    pug_html = pug_html + "<div al-repeat=\"input in node.inputs\" style=\"text-align: left;\">";
    pug_html = pug_html + "<div class=\"socket input {{input.socket.id.toLowerCase().replace(' ','-')}} {{input.multipleConnections?'multiple':''}}\" al-pick-input title=\"{{input.socket.name}}\n{{input.socket.hint}}\"></div>";
    pug_html = pug_html + "<div class=\"input-title\" al-if=\"!input.showControl()\">";
    pug_html = pug_html + "{{input.title}}</div>";
    pug_html = pug_html + "<div class=\"input-control\" al-if=\"input.showControl()\" al-control=\"input.control\"></div></div>";
  } catch (err) {
    pug.rethrow(err, pug_debug_filename, pug_debug_line, pug_debug_sources[pug_debug_filename]);
  }return pug_html;
}

var defaultTemplate = template();

var Component = function () {
    function Component(name, props) {
        classCallCheck(this, Component);

        this.name = name;
        this.template = props.template || defaultTemplate;
        this.builder = props.builder;
        this.worker = props.worker;
    }

    createClass(Component, [{
        key: 'newNode',
        value: function newNode() {
            return new Node(this.name);
        }
    }]);
    return Component;
}();

function Group(scope, el, expression, env) {
    var _this = this;

    var group = env.changeDetector.locals.group;

    group.el = el;
    env.watch('node.style', function () {
        Object.assign(el.style, group.style);
    }, { deep: true });

    d3.select(el).call(d3.drag().on('start', function () {
        if (!d3.event.sourceEvent.shiftKey) _this.editor.selectGroup(group, d3.event.sourceEvent.ctrlKey);
    }).on('drag', function () {
        if (_this.editor.readOnly) return;

        var k = _this.transform.k;
        var dx = d3.event.dx / k;
        var dy = d3.event.dy / k;

        _this.editor.selected.each(function (item) {
            item.position[0] += dx;
            item.position[1] += dy;
        });

        _this.editor.selected.eachGroup(function (item) {
            for (var i in item.nodes) {
                var node = item.nodes[i];

                if (_this.editor.selected.contains(node)) continue;

                node.position[0] += dx;
                node.position[1] += dy;
            }
        });

        _this.update();
    }).on('end', function () {
        _this.editor.eventListener.trigger('change');
    }));

    var items = {
        'Remove group': function RemoveGroup() {
            _this.editor.removeGroup(group);
        }
    };

    var onClick = function onClick(subitem) {
        subitem.call(_this);
        _this.contextMenu.hide();
    };

    d3.select(el).on('contextmenu', function () {
        if (_this.editor.readOnly) return;

        var x = d3.event.clientX;
        var y = d3.event.clientY;

        _this.editor.selectGroup(group);
        _this.contextMenu.show(x, y, items, false, onClick);
        d3.event.preventDefault();
    });
}

function GroupHandler(scope, el, arg, env) {
    var _this2 = this;

    var group = env.changeDetector.locals.group;
    var mousePrev = null;

    d3.select(el).call(d3.drag().on('start', function () {
        mousePrev = d3.mouse(_this2.container.node());
        _this2.editor.selectGroup(group);
    }).on('drag', function () {
        if (_this2.editor.readOnly) return;

        var zoom = d3.zoomTransform(_this2.container);
        var mouse = d3.mouse(_this2.container.node());
        var deltax = (mouse[0] - mousePrev[0]) / zoom.k;
        var deltay = (mouse[1] - mousePrev[1]) / zoom.k;
        var deltaw = Math.max(0, group.width - group.minWidth);
        var deltah = Math.max(0, group.height - group.minHeight);

        if (deltaw !== 0) mousePrev[0] = mouse[0];
        if (deltah !== 0) mousePrev[1] = mouse[1];

        if (arg.match('l')) {
            group.position[0] += Math.min(deltaw, deltax);
            group.setWidth(group.width - deltax);
        } else if (arg.match('r')) group.setWidth(group.width + deltax);

        if (arg.match('t')) {
            group.position[1] += Math.min(deltah, deltay);
            group.setHeight(group.height - deltay);
        } else if (arg.match('b')) group.setHeight(group.height + deltay);

        _this2.update();
    }).on('end', function () {
        _this2.editor.nodes.forEach(function (node) {
            if (group.isCoverNode(node)) group.addNode(node);else group.removeNode(node);
        });

        _this2.editor.eventListener.trigger('change');
        _this2.update();
    }));
}

function GroupTitle(scope, el, expression, env) {
    var group = env.changeDetector.locals.group;

    d3.select(el).on('click', function () {
        var title = prompt('Please enter title of the group', group.title);

        if (title !== null && title.length > 0) group.title = title;
        env.scan();
    });
}

function PickInput(scope, el, expression, env) {
    var _this = this;

    var input = env.changeDetector.locals.input;

    input.el = el;

    d3.select(el).on('mousedown', function () {
        if (_this.editor.readOnly) return;

        d3.event.preventDefault();
        if (_this.pickedOutput === null) {
            if (input.hasConnection()) {
                _this.pickedOutput = input.connections[0].output;
                _this.editor.removeConnection(input.connections[0]);
            }
            _this.update();
            return;
        }

        if (!input.multipleConnections && input.hasConnection()) _this.editor.removeConnection(input.connections[0]);

        if (!_this.pickedOutput.multipleConnections && _this.pickedOutput.hasConnection()) _this.editor.removeConnection(_this.pickedOutput.connections[0]);

        if (_this.pickedOutput.connectedTo(input)) {
            var connection = input.connections.find(function (c) {
                return c.output === _this.pickedOutput;
            });

            _this.editor.removeConnection(connection);
        }

        _this.editor.connect(_this.pickedOutput, input);

        _this.pickedOutput = null;
        _this.update();
    });
}

function PickOutput(scope, el, expression, env) {
    var _this2 = this;

    var output = env.changeDetector.locals.output;

    output.el = el;

    d3.select(el).on('mousedown', function () {
        if (_this2.editor.readOnly) return;

        _this2.pickedOutput = output;
    });
}

function Connection$1(scope, el, expression, env) {
    var path = env.changeDetector.locals.path;
    var connection = path.connection;

    if (!connection) return;

    connection.el = el;
    env.watch('path.connection.style', function () {
        Object.assign(el.style, connection.style);
    }, { deep: true });

    var input = path.connection.input;
    var output = path.connection.output;

    el.dataset.inputNode = input.node.id;
    el.dataset.inputIndex = input.node.inputs.indexOf(input);
    el.dataset.outputNode = output.node.id;
    el.dataset.outputIndex = output.node.outputs.indexOf(output);
    el.className.baseVal += ' output-' + output.socket.id.toLowerCase().replace(' ', '-');
    el.className.baseVal += ' input-' + input.socket.id.toLowerCase().replace(' ', '-');
}

function Control$1(scope, el, expression, env) {
    var locals = env.changeDetector.locals;
    var control = locals.input ? locals.input.control : locals.control;

    el.innerHTML = control.html;
    control.handler(el.children[0], control);
}

function Item(scope, el, expression, env) {
    var _this = this;

    var l = env.changeDetector.locals;
    var item = l.subitem || l.item;
    var haveSubitems = item.constructor === Object;

    d3.select(el).on('click', function () {
        if (!haveSubitems) _this.onClick(item);
        d3.event.stopPropagation();
    }).classed('have-subitems', haveSubitems);
}

var Utils = function () {
    function Utils() {
        classCallCheck(this, Utils);
    }

    createClass(Utils, null, [{
        key: 'nodesBBox',
        value: function nodesBBox(nodes) {
            var min = function min(arr) {
                return Math.min.apply(Math, toConsumableArray(arr));
            };
            var max = function max(arr) {
                return Math.max.apply(Math, toConsumableArray(arr));
            };

            var left = min(nodes.map(function (node) {
                return node.position[0];
            }));
            var top = min(nodes.map(function (node) {
                return node.position[1];
            }));
            var right = max(nodes.map(function (node) {
                return node.position[0] + node.width;
            }));
            var bottom = max(nodes.map(function (node) {
                return node.position[1] + node.height;
            }));

            return {
                left: left,
                right: right,
                top: top,
                bottom: bottom,
                width: Math.abs(left - right),
                height: Math.abs(top - bottom),
                getCenter: function getCenter() {
                    return [(left + right) / 2, (top + bottom) / 2];
                }
            };
        }
    }, {
        key: 'getConnectionPath',
        value: function getConnectionPath(a, b, produce) {
            var _produce = produce.apply(undefined, toConsumableArray(a).concat(toConsumableArray(b))),
                points = _produce.points,
                curve = _produce.curve;

            switch (curve) {
                case 'linear':
                    curve = d3.curveLinear;break;
                case 'step':
                    curve = d3.curveStep;break;
                case 'basis':
                    curve = d3.curveBasis;break;
                default:
                    curve = d3.curveBasis;break;
            }
            return this.pointsToPath(points, curve);
        }
    }, {
        key: 'pointsToPath',
        value: function pointsToPath(points, d3curve) {
            var curve = d3curve(d3.path());

            curve.lineStart();
            for (var i = 0; i < points.length; i++) {
                curve.point.apply(curve, toConsumableArray(points[i]));
            }curve.lineEnd();

            return curve._context.toString();
        }
    }, {
        key: 'getOutputPosition',
        value: function getOutputPosition(output) {
            var node = output.node;
            var el = output.el;

            return [node.position[0] + el.offsetLeft + el.offsetWidth / 2, node.position[1] + el.offsetTop + el.offsetHeight / 2];
        }
    }, {
        key: 'getInputPosition',
        value: function getInputPosition(input) {
            var node = input.node;
            var el = input.el;

            return [node.position[0] + el.offsetLeft + el.offsetWidth / 2, node.position[1] + el.offsetTop + el.offsetHeight / 2];
        }
    }, {
        key: 'isValidData',
        value: function isValidData(data) {
            return typeof data.id === 'string' && this.isValidId(data.id) && data.nodes instanceof Object && !(data.nodes instanceof Array) && (!data.groups || data.groups instanceof Object);
        }
    }, {
        key: 'isValidId',
        value: function isValidId(id) {
            return (/^[\w-]{3,}@[0-9]+\.[0-9]+\.[0-9]+$/.test(id)
            );
        }
    }, {
        key: 'validate',
        value: function validate(id, data) {
            var msg = '';
            var id1 = id.split('@');
            var id2 = data.id.split('@');

            if (!this.isValidData(data)) msg += 'Data is not suitable. ';
            if (id !== data.id) msg += 'IDs not equal. ';
            if (id1[0] !== id2[0]) msg += 'Names don\'t match. ';
            if (id1[1] !== id2[1]) msg += 'Versions don\'t match';

            return { success: msg === '', msg: msg };
        }
    }]);
    return Utils;
}();

var Group$1 = function (_Block) {
    inherits(Group, _Block);

    function Group(title, params) {
        classCallCheck(this, Group);

        if (!(typeof title === 'string')) {
            throw new TypeError('Value of argument "title" violates contract.\n\nExpected:\nstring\n\nGot:\n' + _inspect$7(title));
        }

        if (!(params instanceof Object)) {
            throw new TypeError('Value of argument "params" violates contract.\n\nExpected:\nObject\n\nGot:\n' + _inspect$7(params));
        }

        var _this = possibleConstructorReturn(this, (Group.__proto__ || Object.getPrototypeOf(Group)).call(this, Group));

        _this.title = title;

        _this.nodes = [];
        _this.setMinSizes(300, 250);

        if (params.nodes) _this.coverNodes(params.nodes);else {
            _this.position = params.position;
            _this.width = params.width;
            _this.height = params.height;
        }
        return _this;
    }

    createClass(Group, [{
        key: 'setMinSizes',
        value: function setMinSizes(width, height) {
            this.minWidth = width;
            this.minHeight = height;
        }
    }, {
        key: 'setWidth',
        value: function setWidth(w) {
            if (!(typeof w === 'number')) {
                throw new TypeError('Value of argument "w" violates contract.\n\nExpected:\nnumber\n\nGot:\n' + _inspect$7(w));
            }

            return this.width = Math.max(this.minWidth, w);
        }
    }, {
        key: 'setHeight',
        value: function setHeight(h) {
            if (!(typeof h === 'number')) {
                throw new TypeError('Value of argument "h" violates contract.\n\nExpected:\nnumber\n\nGot:\n' + _inspect$7(h));
            }

            return this.height = Math.max(this.minHeight, h);
        }
    }, {
        key: 'isCoverNode',
        value: function isCoverNode(node) {
            if (!(node instanceof Node)) {
                throw new TypeError('Value of argument "node" violates contract.\n\nExpected:\nNode\n\nGot:\n' + _inspect$7(node));
            }

            var gp = this.position;
            var np = node.position;

            return np[0] > gp[0] && np[1] > gp[1] && np[0] + node.width < gp[0] + this.width && np[1] + node.height < gp[1] + this.height;
        }
    }, {
        key: 'coverNodes',
        value: function coverNodes(nodes) {
            if (!(Array.isArray(nodes) && nodes.every(function (item) {
                return item instanceof Node;
            }))) {
                throw new TypeError('Value of argument "nodes" violates contract.\n\nExpected:\nNode[]\n\nGot:\n' + _inspect$7(nodes));
            }

            var self = this;
            var margin = 30;
            var bbox = Utils.nodesBBox(nodes);

            nodes.forEach(function (node) {
                if (node.group !== null) node.group.removeNode(node.group);
                self.addNode(node);
            });
            this.position = [bbox.left - margin, bbox.top - 2 * margin];
            this.setWidth(bbox.right - bbox.left + 2 * margin);
            this.setHeight(bbox.bottom - bbox.top + 3 * margin);
        }
    }, {
        key: 'containNode',
        value: function containNode(node) {
            if (!(node instanceof Node)) {
                throw new TypeError('Value of argument "node" violates contract.\n\nExpected:\nNode\n\nGot:\n' + _inspect$7(node));
            }

            return this.nodes.indexOf(node) !== -1;
        }
    }, {
        key: 'addNode',
        value: function addNode(node) {
            if (!(node instanceof Node)) {
                throw new TypeError('Value of argument "node" violates contract.\n\nExpected:\nNode\n\nGot:\n' + _inspect$7(node));
            }

            if (this.containNode(node)) return false;
            if (node.group !== null) node.group.removeNode(node);
            node.group = this;
            this.nodes.push(node);
            return true;
        }
    }, {
        key: 'removeNode',
        value: function removeNode(node) {
            if (!(node instanceof Node)) {
                throw new TypeError('Value of argument "node" violates contract.\n\nExpected:\nNode\n\nGot:\n' + _inspect$7(node));
            }

            if (!this.containNode(node)) return;
            this.nodes.splice(this.nodes.indexOf(node), 1);
            node.group = null;
        }
    }, {
        key: 'remove',
        value: function remove() {
            this.nodes.forEach(function (node) {
                node.group = null;
            });
        }
    }, {
        key: 'toJSON',
        value: function toJSON() {
            return {
                'id': this.id,
                'title': this.title,
                'nodes': this.nodes.map(function (a) {
                    return a.id;
                }),
                'minWidth': this.minWidth,
                'minHeight': this.minHeight,
                'position': this.position,
                'width': this.width,
                'height': this.height
            };
        }
    }], [{
        key: 'fromJSON',
        value: function fromJSON(json) {
            if (!(json instanceof Object)) {
                throw new TypeError('Value of argument "json" violates contract.\n\nExpected:\nObject\n\nGot:\n' + _inspect$7(json));
            }

            var group = new Group(json.title, {
                position: json.position,
                width: json.width,
                height: json.height
            });

            group.id = json.id;
            Group.latestId = Math.max(group.id, Group.latestId);
            group.minWidth = json.minWidth;
            group.minHeight = json.minHeight;
            return group;
        }
    }]);
    return Group;
}(Block);

function _inspect$7(input, depth) {
    var maxDepth = 4;
    var maxKeys = 15;

    if (depth === undefined) {
        depth = 0;
    }

    depth += 1;

    if (input === null) {
        return 'null';
    } else if (input === undefined) {
        return 'void';
    } else if (typeof input === 'string' || typeof input === 'number' || typeof input === 'boolean') {
        return typeof input === 'undefined' ? 'undefined' : _typeof(input);
    } else if (Array.isArray(input)) {
        if (input.length > 0) {
            if (depth > maxDepth) return '[...]';

            var first = _inspect$7(input[0], depth);

            if (input.every(function (item) {
                return _inspect$7(item, depth) === first;
            })) {
                return first.trim() + '[]';
            } else {
                return '[' + input.slice(0, maxKeys).map(function (item) {
                    return _inspect$7(item, depth);
                }).join(', ') + (input.length >= maxKeys ? ', ...' : '') + ']';
            }
        } else {
            return 'Array';
        }
    } else {
        var keys = Object.keys(input);

        if (!keys.length) {
            if (input.constructor && input.constructor.name && input.constructor.name !== 'Object') {
                return input.constructor.name;
            } else {
                return 'Object';
            }
        }

        if (depth > maxDepth) return '{...}';
        var indent = '  '.repeat(depth - 1);
        var entries = keys.slice(0, maxKeys).map(function (key) {
            return (/^([A-Z_$][A-Z0-9_$]*)$/i.test(key) ? key : JSON.stringify(key)) + ': ' + _inspect$7(input[key], depth) + ';';
        }).join('\n  ' + indent);

        if (keys.length >= maxKeys) {
            entries += '\n  ' + indent + '...';
        }

        if (input.constructor && input.constructor.name && input.constructor.name !== 'Object') {
            return input.constructor.name + ' {\n  ' + indent + entries + '\n' + indent + '}';
        } else {
            return '{\n  ' + indent + entries + '\n' + indent + '}';
        }
    }
}

function Node$1(scope, el, expression, env) {
    var _this = this;

    var node = env.changeDetector.locals.node;

    node.el = el;
    env.watch('node.style', function () {
        Object.assign(el.style, node.style);
    }, { deep: true });

    d3.select(el).call(d3.drag().on('start', function () {
        d3.select(el).raise();
        if (!d3.event.sourceEvent.shiftKey) _this.editor.selectNode(node, d3.event.sourceEvent.ctrlKey);
    }).on('drag', function () {
        if (_this.editor.readOnly) return;

        var k = _this.transform.k;
        var dx = d3.event.dx / k;
        var dy = d3.event.dy / k;

        _this.editor.selected.each(function (item) {
            item.position[0] += dx;
            item.position[1] += dy;
        });

        _this.editor.selected.eachGroup(function (item) {
            for (var i in item.nodes) {
                var _node = item.nodes[i];

                if (_this.editor.selected.contains(_node)) continue;

                _node.position[0] += dx;
                _node.position[1] += dy;
            }
        });

        _this.update();
    }).on('end', function () {
        _this.editor.groups.forEach(function (group) {
            _this.editor.selected.eachNode(function (_node) {
                var contain = group.containNode(_node);
                var cover = group.isCoverNode(_node);

                if (contain && !cover) group.removeNode(_node);else if (!contain && cover) group.addNode(_node);
            });
        });

        _this.editor.eventListener.trigger('change');
        _this.update();
    }));

    window.addEventListener('load', function () {
        node.width = el.offsetWidth;
        node.height = el.offsetHeight;
    });

    var items = {
        'Remove node': function RemoveNode() {
            _this.editor.removeNode(node);
        },
        'Add to group': function AddToGroup() {
            var group = new Group$1('Group', { nodes: [node] });

            _this.editor.addGroup(group);
        }
    };

    var onClick = function onClick(subitem) {
        subitem.call(_this);
        _this.contextMenu.hide();
    };

    d3.select(el).on('contextmenu', function () {
        if (_this.editor.readOnly) return;

        var x = d3.event.clientX;
        var y = d3.event.clientY;

        _this.editor.selectNode(node);
        _this.contextMenu.show(x, y, items, false, onClick);
        d3.event.preventDefault();
    });
}

function declareViewDirectives(view, alight) {

    alight.directives.al.node = Node$1.bind(view);

    alight.directives.al.group = Group.bind(view);
    alight.directives.al.groupHandler = GroupHandler.bind(view);
    alight.directives.al.groupTitle = GroupTitle.bind(view);

    alight.directives.al.pickInput = PickInput.bind(view);
    alight.directives.al.pickOutput = PickOutput.bind(view);

    alight.directives.al.control = Control$1.bind(view);

    alight.directives.al.connection = Connection$1.bind(view);
}

function declareMenuDirectives(menu, alight) {

    alight.directives.al.item = Item.bind(menu);
}

function template$1 (locals) {
  var pug_html = "";var pug_debug_filename, pug_debug_line;try {
    var pug_debug_sources = {};
    pug_html = pug_html + "<div class=\"context-menu\" :style.left=\"contextMenu.x+&quot;px&quot;\" :style.top=\"contextMenu.y+&quot;px&quot;\" @mouseleave=\"contextMenu.hide()\" al-if=\"contextMenu.visible\">";
    pug_html = pug_html + "<div class=\"search item\" al-if=\"contextMenu.searchBar\">";
    pug_html = pug_html + "<input al-value=\"filter\"></div>";
    pug_html = pug_html + "<div class=\"item\" al-repeat=\"(name,item) in contextMenu.searchItems(filter)\" al-item>";
    pug_html = pug_html + "{{name}}";
    pug_html = pug_html + "<div class=\"subitems\" al-if=\"contextMenu.haveSubitems(item)\">";
    pug_html = pug_html + "<div class=\"item\" al-repeat=\"(name,subitem) in item\" al-item>";
    pug_html = pug_html + "{{name}}</div></div></div></div>";
  } catch (err) {
    pug.rethrow(err, pug_debug_filename, pug_debug_line, pug_debug_sources[pug_debug_filename]);
  }return pug_html;
}

var ContextMenu = function () {
    function ContextMenu(items) {
        var searchBar = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : true;
        classCallCheck(this, ContextMenu);

        if (!(items instanceof Object)) {
            throw new TypeError('Value of argument "items" violates contract.\n\nExpected:\nObject\n\nGot:\n' + _inspect$6(items));
        }

        if (!(typeof searchBar === 'boolean')) {
            throw new TypeError('Value of argument "searchBar" violates contract.\n\nExpected:\nboolean\n\nGot:\n' + _inspect$6(searchBar));
        }

        this.visible = false;
        this.x = 0;
        this.y = 0;
        this.default = {
            items: items,
            searchBar: searchBar,
            onClick: function onClick() {
                throw new TypeError('onClick should be overrided');
            }
        };

        this.bindTemplate(template$1());
    }

    createClass(ContextMenu, [{
        key: 'bindTemplate',
        value: function bindTemplate(t) {
            this.dom = d3.select('body').append('div');
            this.dom.node().setAttribute('tabindex', 1);
            this.dom.html(t);

            declareMenuDirectives(this, alight);
            this.$cd = alight(this.dom.node(), { contextMenu: this });
        }
    }, {
        key: 'searchItems',
        value: function searchItems(filter) {
            var _this = this;

            if (!(filter == null || typeof filter === 'string')) {
                throw new TypeError('Value of argument "filter" violates contract.\n\nExpected:\n?string\n\nGot:\n' + _inspect$6(filter));
            }

            var regex = new RegExp(filter, 'i');
            var items = {};

            Object.keys(this.items).forEach(function (key) {
                var item = _this.items[key];

                if (item.constructor === Object) {
                    var subitems = Object.keys(item).filter(function (subitem) {
                        return regex.test(subitem);
                    });

                    if (subitems.length > 0) {
                        items[key] = {};
                        subitems.forEach(function (sumitem) {
                            items[key][sumitem] = item[sumitem];
                        });
                    }
                } else if (regex.test(key)) items[key] = item;
            });

            return items;
        }
    }, {
        key: 'haveSubitems',
        value: function haveSubitems(item) {
            return item.constructor === Object;
        }
    }, {
        key: 'isVisible',
        value: function isVisible() {
            return this.visible;
        }
    }, {
        key: 'show',
        value: function show(x, y) {
            var items = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : null;
            var searchBar = arguments.length > 3 && arguments[3] !== undefined ? arguments[3] : null;
            var onClick = arguments.length > 4 && arguments[4] !== undefined ? arguments[4] : null;

            if (!(typeof x === 'number')) {
                throw new TypeError('Value of argument "x" violates contract.\n\nExpected:\nnumber\n\nGot:\n' + _inspect$6(x));
            }

            if (!(typeof y === 'number')) {
                throw new TypeError('Value of argument "y" violates contract.\n\nExpected:\nnumber\n\nGot:\n' + _inspect$6(y));
            }

            if (!(items == null || items instanceof Object)) {
                throw new TypeError('Value of argument "items" violates contract.\n\nExpected:\n?Object\n\nGot:\n' + _inspect$6(items));
            }

            if (!(searchBar == null || typeof searchBar === 'boolean')) {
                throw new TypeError('Value of argument "searchBar" violates contract.\n\nExpected:\n?boolean\n\nGot:\n' + _inspect$6(searchBar));
            }

            this.visible = true;
            this.items = items || this.default.items;
            this.searchBar = searchBar || this.default.searchBar;
            this.onClick = onClick || this.default.onClick;
            this.x = x;
            this.y = y;
            this.$cd.scan();
        }
    }, {
        key: 'hide',
        value: function hide() {
            this.visible = false;
            this.$cd.scan();
        }
    }]);
    return ContextMenu;
}();

function _inspect$6(input, depth) {
    var maxDepth = 4;
    var maxKeys = 15;

    if (depth === undefined) {
        depth = 0;
    }

    depth += 1;

    if (input === null) {
        return 'null';
    } else if (input === undefined) {
        return 'void';
    } else if (typeof input === 'string' || typeof input === 'number' || typeof input === 'boolean') {
        return typeof input === 'undefined' ? 'undefined' : _typeof(input);
    } else if (Array.isArray(input)) {
        if (input.length > 0) {
            if (depth > maxDepth) return '[...]';

            var first = _inspect$6(input[0], depth);

            if (input.every(function (item) {
                return _inspect$6(item, depth) === first;
            })) {
                return first.trim() + '[]';
            } else {
                return '[' + input.slice(0, maxKeys).map(function (item) {
                    return _inspect$6(item, depth);
                }).join(', ') + (input.length >= maxKeys ? ', ...' : '') + ']';
            }
        } else {
            return 'Array';
        }
    } else {
        var keys = Object.keys(input);

        if (!keys.length) {
            if (input.constructor && input.constructor.name && input.constructor.name !== 'Object') {
                return input.constructor.name;
            } else {
                return 'Object';
            }
        }

        if (depth > maxDepth) return '{...}';
        var indent = '  '.repeat(depth - 1);
        var entries = keys.slice(0, maxKeys).map(function (key) {
            return (/^([A-Z_$][A-Z0-9_$]*)$/i.test(key) ? key : JSON.stringify(key)) + ': ' + _inspect$6(input[key], depth) + ';';
        }).join('\n  ' + indent);

        if (keys.length >= maxKeys) {
            entries += '\n  ' + indent + '...';
        }

        if (input.constructor && input.constructor.name && input.constructor.name !== 'Object') {
            return input.constructor.name + ' {\n  ' + indent + entries + '\n' + indent + '}';
        } else {
            return '{\n  ' + indent + entries + '\n' + indent + '}';
        }
    }
}

var p = function p(v) {
    return parseInt(v);
};
var eqArr = function eqArr(ar1, ar2) {
    return ar1.every(function (v, i) {
        return v === ar2[i];
    });
};
var eqObj = function eqObj(o1, o2) {
    return JSON.stringify(o1) === JSON.stringify(o2);
};
var eqCon = function eqCon(c1, c2, target) {
    return c1.node === c2.node && c1[target] === c2[target];
};
var diffCons = function diffCons(cons1, cons2) {

    var removed = cons1.filter(function (c1) {
        return !cons2.some(function (c2) {
            return eqCon(c1, c2, 'input');
        });
    });
    var added = cons2.filter(function (c2) {
        return !cons1.some(function (c1) {
            return eqCon(c1, c2, 'input');
        });
    });

    return { removed: removed, added: added };
};

var Diff = function () {
    function Diff(data1, data2) {
        classCallCheck(this, Diff);

        this.a = data1;
        this.b = data2;
    }

    createClass(Diff, [{
        key: 'compare',
        value: function compare() {
            var a = this.a;
            var b = this.b;

            var k1 = Object.keys(a.nodes);
            var k2 = Object.keys(b.nodes);

            var removed = k1.filter(function (k) {
                return !k2.includes(k);
            }).map(p);
            var added = k2.filter(function (k) {
                return !k1.includes(k);
            }).map(p);
            var stayed = k1.filter(function (k) {
                return k2.includes(k);
            }).map(p);

            var moved = stayed.filter(function (id) {
                var p1 = a.nodes[id].position;
                var p2 = b.nodes[id].position;

                return !eqArr(p1, p2);
            });

            var datachanged = stayed.filter(function (id) {
                var d1 = a.nodes[id].data;
                var d2 = b.nodes[id].data;

                return !eqObj(d1, d2);
            });

            var connects = stayed.reduce(function (arr, id) {
                var o1 = a.nodes[id].outputs;
                var o2 = b.nodes[id].outputs;

                var output = o1.map(function (out, i) {
                    return Object.assign({ output: i }, diffCons(out.connections, o2[i].connections));
                }).filter(function (diff) {
                    return diff.added.length !== 0 || diff.removed.length !== 0;
                });

                return [].concat(toConsumableArray(arr), toConsumableArray(output.map(function (o) {
                    return o.node = id, o;
                })));
            }, []);

            return { removed: removed, added: added, moved: moved, datachanged: datachanged, connects: connects };
        }
    }]);
    return Diff;
}();

var State = { AVALIABLE: 0, PROCESSED: 1, ABORT: 2 };

var Engine = function () {
    function Engine(id, components) {
        classCallCheck(this, Engine);

        if (!(typeof id === 'string')) {
            throw new TypeError('Value of argument "id" violates contract.\n\nExpected:\nstring\n\nGot:\n' + _inspect$8(id));
        }

        if (!(Array.isArray(components) && components.every(function (item) {
            return item instanceof Component;
        }))) {
            throw new TypeError('Value of argument "components" violates contract.\n\nExpected:\nComponent[]\n\nGot:\n' + _inspect$8(components));
        }

        if (!Utils.isValidId(id)) throw new Error('ID should be valid to name@0.1.0 format');

        this.id = id;
        this.components = components;
        this.args = [];
        this.data = null;
        this.state = State.AVALIABLE;
        this.onAbort = function () {};
    }

    createClass(Engine, [{
        key: 'clone',
        value: function clone() {
            return new Engine(this.id, this.components);
        }
    }, {
        key: 'processStart',
        value: function processStart() {
            if (this.state === State.AVALIABLE) {
                this.state = State.PROCESSED;
                return true;
            }

            if (this.state === State.ABORT) {
                return false;
            }

            console.warn('The process is busy and has not been restarted. Use abort() to force it to complete');
            return false;
        }
    }, {
        key: 'processDone',
        value: function processDone() {
            var success = this.state !== State.ABORT;

            this.state = State.AVALIABLE;

            if (!success) {
                this.onAbort();
                this.onAbort = function () {};
            }

            return success;
        }
    }, {
        key: 'abort',
        value: function abort() {
            var _this = this;

            return regeneratorRuntime.async(function abort$(_context) {
                while (1) {
                    switch (_context.prev = _context.next) {
                        case 0:
                            return _context.abrupt('return', new Promise(function (ret) {
                                if (_this.state === State.PROCESSED) {
                                    _this.state = State.ABORT;
                                    _this.onAbort = ret;
                                } else if (_this.state === State.ABORT) {
                                    _this.onAbort();
                                    _this.onAbort = ret;
                                } else ret();
                            }));

                        case 1:
                        case 'end':
                            return _context.stop();
                    }
                }
            }, null, this);
        }
    }, {
        key: 'lock',
        value: function lock(node) {
            return regeneratorRuntime.async(function lock$(_context2) {
                while (1) {
                    switch (_context2.prev = _context2.next) {
                        case 0:
                            return _context2.abrupt('return', new Promise(function (res) {
                                node.unlockPool = node.unlockPool || [];
                                if (node.busy && !node.outputData) node.unlockPool.push(res);else res();

                                node.busy = true;
                            }));

                        case 1:
                        case 'end':
                            return _context2.stop();
                    }
                }
            }, null, this);
        }
    }, {
        key: 'unlock',
        value: function unlock(node) {
            node.unlockPool.forEach(function (a) {
                return a();
            });
            node.unlockPool = [];
            node.busy = false;
        }
    }, {
        key: 'extractInputData',
        value: function extractInputData(node) {
            var _this2 = this;

            return regeneratorRuntime.async(function extractInputData$(_context5) {
                while (1) {
                    switch (_context5.prev = _context5.next) {
                        case 0:
                            _context5.next = 2;
                            return regeneratorRuntime.awrap(Promise.all(node.inputs.map(function _callee2(input) {
                                var conns, connData;
                                return regeneratorRuntime.async(function _callee2$(_context4) {
                                    while (1) {
                                        switch (_context4.prev = _context4.next) {
                                            case 0:
                                                conns = input.connections;
                                                _context4.next = 3;
                                                return regeneratorRuntime.awrap(Promise.all(conns.map(function _callee(c) {
                                                    var outputs;
                                                    return regeneratorRuntime.async(function _callee$(_context3) {
                                                        while (1) {
                                                            switch (_context3.prev = _context3.next) {
                                                                case 0:
                                                                    _context3.next = 2;
                                                                    return regeneratorRuntime.awrap(_this2.processNode(_this2.data.nodes[c.node]));

                                                                case 2:
                                                                    outputs = _context3.sent;

                                                                    if (outputs) {
                                                                        _context3.next = 7;
                                                                        break;
                                                                    }

                                                                    _this2.abort();
                                                                    _context3.next = 8;
                                                                    break;

                                                                case 7:
                                                                    return _context3.abrupt('return', outputs[c.output]);

                                                                case 8:
                                                                case 'end':
                                                                    return _context3.stop();
                                                            }
                                                        }
                                                    }, null, _this2);
                                                })));

                                            case 3:
                                                connData = _context4.sent;
                                                return _context4.abrupt('return', connData);

                                            case 5:
                                            case 'end':
                                                return _context4.stop();
                                        }
                                    }
                                }, null, _this2);
                            })));

                        case 2:
                            return _context5.abrupt('return', _context5.sent);

                        case 3:
                        case 'end':
                            return _context5.stop();
                    }
                }
            }, null, this);
        }
    }, {
        key: 'processNode',
        value: function processNode(node) {
            var inputData, key, component;
            return regeneratorRuntime.async(function processNode$(_context6) {
                while (1) {
                    switch (_context6.prev = _context6.next) {
                        case 0:
                            if (!(this.state === State.ABORT || !node)) {
                                _context6.next = 2;
                                break;
                            }

                            return _context6.abrupt('return', null);

                        case 2:
                            _context6.next = 4;
                            return regeneratorRuntime.awrap(this.lock(node));

                        case 4:
                            if (node.outputData) {
                                _context6.next = 22;
                                break;
                            }

                            _context6.next = 7;
                            return regeneratorRuntime.awrap(this.extractInputData(node));

                        case 7:
                            inputData = _context6.sent;


                            node.outputData = node.outputs.map(function () {
                                return null;
                            });

                            key = node.title;
                            component = this.components.find(function (c) {
                                return c.name === key;
                            });
                            _context6.prev = 11;
                            _context6.next = 14;
                            return regeneratorRuntime.awrap(component.worker.apply(component, [node, inputData, node.outputData].concat(toConsumableArray(this.args))));

                        case 14:
                            _context6.next = 20;
                            break;

                        case 16:
                            _context6.prev = 16;
                            _context6.t0 = _context6['catch'](11);

                            this.abort();
                            console.warn(_context6.t0);

                        case 20:
                            if (!(node.outputData.length !== node.outputs.length)) {
                                _context6.next = 22;
                                break;
                            }

                            throw new Error('Output data does not correspond to number of outputs');

                        case 22:

                            this.unlock(node);
                            return _context6.abrupt('return', node.outputData);

                        case 24:
                        case 'end':
                            return _context6.stop();
                    }
                }
            }, null, this, [[11, 16]]);
        }
    }, {
        key: 'forwardProcess',
        value: function forwardProcess(node) {
            var _this3 = this;

            return regeneratorRuntime.async(function forwardProcess$(_context9) {
                while (1) {
                    switch (_context9.prev = _context9.next) {
                        case 0:
                            if (!(this.state === State.ABORT)) {
                                _context9.next = 2;
                                break;
                            }

                            return _context9.abrupt('return', null);

                        case 2:
                            _context9.next = 4;
                            return regeneratorRuntime.awrap(Promise.all(node.outputs.map(function _callee4(output) {
                                return regeneratorRuntime.async(function _callee4$(_context8) {
                                    while (1) {
                                        switch (_context8.prev = _context8.next) {
                                            case 0:
                                                _context8.next = 2;
                                                return regeneratorRuntime.awrap(Promise.all(output.connections.map(function _callee3(c) {
                                                    return regeneratorRuntime.async(function _callee3$(_context7) {
                                                        while (1) {
                                                            switch (_context7.prev = _context7.next) {
                                                                case 0:
                                                                    _context7.next = 2;
                                                                    return regeneratorRuntime.awrap(_this3.processNode(_this3.data.nodes[c.node]));

                                                                case 2:
                                                                    _context7.next = 4;
                                                                    return regeneratorRuntime.awrap(_this3.forwardProcess(_this3.data.nodes[c.node]));

                                                                case 4:
                                                                case 'end':
                                                                    return _context7.stop();
                                                            }
                                                        }
                                                    }, null, _this3);
                                                })));

                                            case 2:
                                                return _context8.abrupt('return', _context8.sent);

                                            case 3:
                                            case 'end':
                                                return _context8.stop();
                                        }
                                    }
                                }, null, _this3);
                            })));

                        case 4:
                            return _context9.abrupt('return', _context9.sent);

                        case 5:
                        case 'end':
                            return _context9.stop();
                    }
                }
            }, null, this);
        }
    }, {
        key: 'copy',
        value: function copy(data) {
            data = Object.assign({}, data);
            data.nodes = Object.assign({}, data.nodes);

            Object.keys(data.nodes).forEach(function (key) {
                data.nodes[key] = Object.assign({}, data.nodes[key]);
            });
            return data;
        }
    }, {
        key: 'process',
        value: function process(data) {
            var startId = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : null;

            for (var _len = arguments.length, args = Array(_len > 2 ? _len - 2 : 0), _key = 2; _key < _len; _key++) {
                args[_key - 2] = arguments[_key];
            }

            var checking, startNode, i, node;
            return regeneratorRuntime.async(function process$(_context10) {
                while (1) {
                    switch (_context10.prev = _context10.next) {
                        case 0:
                            if (data instanceof Object) {
                                _context10.next = 2;
                                break;
                            }

                            throw new TypeError('Value of argument "data" violates contract.\n\nExpected:\nObject\n\nGot:\n' + _inspect$8(data));

                        case 2:
                            if (startId == null || typeof startId === 'number') {
                                _context10.next = 4;
                                break;
                            }

                            throw new TypeError('Value of argument "startId" violates contract.\n\nExpected:\n?number\n\nGot:\n' + _inspect$8(startId));

                        case 4:
                            if (this.processStart()) {
                                _context10.next = 6;
                                break;
                            }

                            return _context10.abrupt('return');

                        case 6:
                            checking = Utils.validate(this.id, data);

                            if (checking.success) {
                                _context10.next = 9;
                                break;
                            }

                            throw new Error(checking.msg);

                        case 9:

                            this.data = this.copy(data);
                            this.args = args;

                            if (!startId) {
                                _context10.next = 19;
                                break;
                            }

                            startNode = this.data.nodes[startId];

                            if (startNode) {
                                _context10.next = 15;
                                break;
                            }

                            throw new Error('Node with such id not found');

                        case 15:
                            _context10.next = 17;
                            return regeneratorRuntime.awrap(this.processNode(startNode));

                        case 17:
                            _context10.next = 19;
                            return regeneratorRuntime.awrap(this.forwardProcess(startNode));

                        case 19:
                            _context10.t0 = regeneratorRuntime.keys(this.data.nodes);

                        case 20:
                            if ((_context10.t1 = _context10.t0()).done) {
                                _context10.next = 30;
                                break;
                            }

                            i = _context10.t1.value;

                            if (!(typeof this.data.nodes[i].outputData === 'undefined')) {
                                _context10.next = 28;
                                break;
                            }

                            node = this.data.nodes[i];
                            _context10.next = 26;
                            return regeneratorRuntime.awrap(this.processNode(node));

                        case 26:
                            _context10.next = 28;
                            return regeneratorRuntime.awrap(this.forwardProcess(node));

                        case 28:
                            _context10.next = 20;
                            break;

                        case 30:
                            return _context10.abrupt('return', this.processDone() ? 'success' : 'aborted');

                        case 31:
                        case 'end':
                            return _context10.stop();
                    }
                }
            }, null, this);
        }
    }]);
    return Engine;
}();

function _inspect$8(input, depth) {
    var maxDepth = 4;
    var maxKeys = 15;

    if (depth === undefined) {
        depth = 0;
    }

    depth += 1;

    if (input === null) {
        return 'null';
    } else if (input === undefined) {
        return 'void';
    } else if (typeof input === 'string' || typeof input === 'number' || typeof input === 'boolean') {
        return typeof input === 'undefined' ? 'undefined' : _typeof(input);
    } else if (Array.isArray(input)) {
        if (input.length > 0) {
            if (depth > maxDepth) return '[...]';

            var first = _inspect$8(input[0], depth);

            if (input.every(function (item) {
                return _inspect$8(item, depth) === first;
            })) {
                return first.trim() + '[]';
            } else {
                return '[' + input.slice(0, maxKeys).map(function (item) {
                    return _inspect$8(item, depth);
                }).join(', ') + (input.length >= maxKeys ? ', ...' : '') + ']';
            }
        } else {
            return 'Array';
        }
    } else {
        var keys = Object.keys(input);

        if (!keys.length) {
            if (input.constructor && input.constructor.name && input.constructor.name !== 'Object') {
                return input.constructor.name;
            } else {
                return 'Object';
            }
        }

        if (depth > maxDepth) return '{...}';
        var indent = '  '.repeat(depth - 1);
        var entries = keys.slice(0, maxKeys).map(function (key) {
            return (/^([A-Z_$][A-Z0-9_$]*)$/i.test(key) ? key : JSON.stringify(key)) + ': ' + _inspect$8(input[key], depth) + ';';
        }).join('\n  ' + indent);

        if (keys.length >= maxKeys) {
            entries += '\n  ' + indent + '...';
        }

        if (input.constructor && input.constructor.name && input.constructor.name !== 'Object') {
            return input.constructor.name + ' {\n  ' + indent + entries + '\n' + indent + '}';
        } else {
            return '{\n  ' + indent + entries + '\n' + indent + '}';
        }
    }
}

function template$2 (locals) {
  var pug_html = "";var pug_debug_filename, pug_debug_line;try {
    var pug_debug_sources = {};
    pug_html = pug_html + "<!-- Groups-->";
    pug_html = pug_html + "<div class=\"group\" al-repeat=\"group in editor.groups\" :style.transform=\"&quot;translate(&quot;+group.position[0]+&quot;px,&quot;+group.position[1]+&quot;px)&quot;\" :style.width=\"group.width+&quot;px&quot;\" :style.height=\"group.height+&quot;px&quot;\" al-group :class.selected=\"editor.selected.contains(group)\">";
    pug_html = pug_html + "<div class=\"group-title\" al-group-title>";
    pug_html = pug_html + "{{group.title}}</div>";
    pug_html = pug_html + "<div class=\"group-handler right bottom\" al-group-handler=\"rb\"></div>";
    pug_html = pug_html + "<div class=\"group-handler left top\" al-group-handler=\"lt\"></div>";
    pug_html = pug_html + "<div class=\"group-handler left bottom\" al-group-handler=\"lb\"></div>";
    pug_html = pug_html + "<div class=\"group-handler right top\" al-group-handler=\"rt\"></div></div>";
    pug_html = pug_html + "<!-- Connections-->";
    pug_html = pug_html + "<svg class=\"connections\">";
    pug_html = pug_html + "<path class=\"connection\" al-repeat=\"path in editor.paths\" :d=\"path.d\" al-connection :class.selected=\"path.selected\"></path></svg>";
    pug_html = pug_html + "<!-- Nodes-->";
    pug_html = pug_html + "<div class=\"node {{editor.selected.contains(node)?'selected':''}} {{node.title.toLowerCase().replace(' ','-')}}\" al-repeat=\"node in editor.nodes\" :style.transform=\"&quot;translate(&quot;+node.position[0]+&quot;px,&quot;+node.position[1]+&quot;px)&quot;\" al-node :data-id=\"node.id\">";
    pug_html = pug_html + "<div al-html=\"editor.view.getTemplate(node)\"></div></div>";
  } catch (err) {
    pug.rethrow(err, pug_debug_filename, pug_debug_line, pug_debug_sources[pug_debug_filename]);
  }return pug_html;
}

var zoomMargin = 0.9;

var EditorView = function () {
    function EditorView(editor, container, menu) {
        var _this = this;

        classCallCheck(this, EditorView);

        if (!(editor instanceof NodeEditor)) {
            throw new TypeError('Value of argument "editor" violates contract.\n\nExpected:\nNodeEditor\n\nGot:\n' + _inspect$10(editor));
        }

        if (!(container instanceof HTMLElement)) {
            throw new TypeError('Value of argument "container" violates contract.\n\nExpected:\nHTMLElement\n\nGot:\n' + _inspect$10(container));
        }

        if (!(menu instanceof ContextMenu)) {
            throw new TypeError('Value of argument "menu" violates contract.\n\nExpected:\nContextMenu\n\nGot:\n' + _inspect$10(menu));
        }

        this.editor = editor;
        this.pickedOutput = null;
        this.container = d3.select(container).attr('tabindex', 1);
        this.mouse = [0, 0];
        this.transform = d3.zoomIdentity;

        this.contextMenu = menu;

        this.container.on('click', function () {
            if (_this.container.node() === d3.event.target) _this.areaClick();
        });

        this.view = this.container.append('div').style('transform-origin', '0 0').style('width', 1).style('height', 1);

        this.zoom = d3.zoom().on('zoom', function () {
            _this.transform = d3.event.transform;
            _this.update();
            _this.editor.eventListener.trigger('transform', _this.transform);
        });

        this.container.call(this.zoom);

        this.setScaleExtent(0.1, 1);
        var size = Math.pow(2, 12);

        this.setTranslateExtent(-size, -size, size, size);

        d3.select(window).on('mousemove.d3ne' + editor._id, function () {

            var k = _this.transform.k;
            var position = d3.mouse(_this.view.node());

            _this.mouse = [position[0] / k, position[1] / k];
            _this.update();
        }).on('keydown.d3ne' + editor._id, function (e) {
            if (_this.container.node() === document.activeElement) editor.keyDown(e);
        }).on('resize.d3ne' + editor._id, this.resize.bind(this));

        this.view.html(template$2());

        var al = alight.makeInstance();

        declareViewDirectives(this, al);
        this.$cd = al(this.view.node(), { editor: editor });
    }

    createClass(EditorView, [{
        key: 'getTemplate',
        value: function getTemplate(node) {
            var component = this.editor.components.find(function (c) {
                return c.name === node.title;
            });

            return component.template;
        }
    }, {
        key: 'resize',
        value: function resize() {
            var width = this.container.node().parentElement.clientWidth;
            var height = this.container.node().parentElement.clientHeight;

            this.container.style('width', width + 'px').style('height', height + 'px');

            this.update();
        }
    }, {
        key: 'connectionProducer',
        value: function connectionProducer(x1, y1, x2, y2) {
            var offsetX = 0.3 * Math.abs(x1 - x2);
            var offsetY = 0.1 * (y2 - y1);

            var p1 = [x1, y1];
            var p2 = [x1 + offsetX, y1 + offsetY];
            var p3 = [x2 - offsetX, y2 - offsetY];
            var p4 = [x2, y2];

            return {
                points: [p1, p2, p3, p4],
                curve: 'basis'
            };
        }
    }, {
        key: 'updateConnections',
        value: function updateConnections() {
            var _this2 = this;

            var pathData = [];

            this.editor.nodes.forEach(function (node) {
                node.getConnections('output').forEach(function (con) {
                    var output = con.output;
                    var input = con.input;

                    if (input.el) {
                        pathData.push({
                            connection: con,
                            d: Utils.getConnectionPath(Utils.getOutputPosition(output), Utils.getInputPosition(input), _this2.connectionProducer)
                        });
                    }
                });
            });

            if (this.pickedOutput !== null && this.pickedOutput.el) {
                var output = this.pickedOutput;
                var input = this.mouse;

                pathData.push({
                    selected: true,
                    d: Utils.getConnectionPath(Utils.getOutputPosition(output), input, this.connectionProducer)
                });
            }

            this.editor.paths = pathData;
        }
    }, {
        key: 'update',
        value: function update() {
            var t = this.transform;

            this.view.style('transform', 'translate(' + t.x + 'px, ' + t.y + 'px) scale(' + t.k + ')');
            this.updateConnections();
            this.$cd.scan();
        }
    }, {
        key: 'assignContextMenuHandler',
        value: function assignContextMenuHandler() {
            var _this3 = this;

            this.contextMenu.default.onClick = function _callee(item) {
                var node;
                return regeneratorRuntime.async(function _callee$(_context) {
                    while (1) {
                        switch (_context.prev = _context.next) {
                            case 0:
                                if (!(item instanceof Component)) {
                                    _context.next = 8;
                                    break;
                                }

                                node = item.newNode();
                                _context.next = 4;
                                return regeneratorRuntime.awrap(item.builder(node));

                            case 4:
                                _this3.editor.addNode(node, true);
                                _this3.editor.selectNode(node);
                                _context.next = 9;
                                break;

                            case 8:
                                item();

                            case 9:
                                _this3.contextMenu.hide();

                            case 10:
                            case 'end':
                                return _context.stop();
                        }
                    }
                }, null, _this3);
            };
        }
    }, {
        key: 'areaClick',
        value: function areaClick() {
            if (this.editor.readOnly) return;

            if (this.pickedOutput !== null && !d3.event.ctrlKey) this.pickedOutput = null;else if (this.contextMenu.visible) this.contextMenu.hide();else {
                this.assignContextMenuHandler();
                this.contextMenu.show(d3.event.pageX, d3.event.pageY);
            }
            this.update();
        }
    }, {
        key: 'zoomAt',
        value: function zoomAt(nodes) {
            if (!(Array.isArray(nodes) && nodes.every(function (item) {
                return item instanceof Node;
            }))) {
                throw new TypeError('Value of argument "nodes" violates contract.\n\nExpected:\nNode[]\n\nGot:\n' + _inspect$10(nodes));
            }

            if (nodes.length === 0) return;

            var w = this.container.node().clientWidth;
            var h = this.container.node().clientHeight;
            var bbox = Utils.nodesBBox(nodes);
            var kw = w / bbox.width;
            var kh = h / bbox.height;
            var k = Math.min(kh, kw, 1);

            var center = bbox.getCenter();
            var win = [w / 2, h / 2];

            k *= zoomMargin;

            this.translate(win[0] - center[0] * k, win[1] - center[1] * k);
            this.scale(k);
        }
    }, {
        key: 'translate',
        value: function translate(x, y) {
            this.transform.x = x;
            this.transform.y = y;
            this.update();
        }
    }, {
        key: 'scale',
        value: function scale(_scale) {
            this.transform.k = _scale;
            this.update();
        }
    }, {
        key: 'setScaleExtent',
        value: function setScaleExtent(scaleMin, scaleMax) {
            if (!(typeof scaleMin === 'number')) {
                throw new TypeError('Value of argument "scaleMin" violates contract.\n\nExpected:\nnumber\n\nGot:\n' + _inspect$10(scaleMin));
            }

            if (!(typeof scaleMax === 'number')) {
                throw new TypeError('Value of argument "scaleMax" violates contract.\n\nExpected:\nnumber\n\nGot:\n' + _inspect$10(scaleMax));
            }

            this.zoom.scaleExtent([scaleMin, scaleMax]);
        }
    }, {
        key: 'setTranslateExtent',
        value: function setTranslateExtent(left, top, right, bottom) {
            if (!(typeof left === 'number')) {
                throw new TypeError('Value of argument "left" violates contract.\n\nExpected:\nnumber\n\nGot:\n' + _inspect$10(left));
            }

            if (!(typeof top === 'number')) {
                throw new TypeError('Value of argument "top" violates contract.\n\nExpected:\nnumber\n\nGot:\n' + _inspect$10(top));
            }

            if (!(typeof right === 'number')) {
                throw new TypeError('Value of argument "right" violates contract.\n\nExpected:\nnumber\n\nGot:\n' + _inspect$10(right));
            }

            if (!(typeof bottom === 'number')) {
                throw new TypeError('Value of argument "bottom" violates contract.\n\nExpected:\nnumber\n\nGot:\n' + _inspect$10(bottom));
            }

            this.zoom.translateExtent([[left, top], [right, bottom]]);
        }
    }]);
    return EditorView;
}();

function _inspect$10(input, depth) {
    var maxDepth = 4;
    var maxKeys = 15;

    if (depth === undefined) {
        depth = 0;
    }

    depth += 1;

    if (input === null) {
        return 'null';
    } else if (input === undefined) {
        return 'void';
    } else if (typeof input === 'string' || typeof input === 'number' || typeof input === 'boolean') {
        return typeof input === 'undefined' ? 'undefined' : _typeof(input);
    } else if (Array.isArray(input)) {
        if (input.length > 0) {
            if (depth > maxDepth) return '[...]';

            var first = _inspect$10(input[0], depth);

            if (input.every(function (item) {
                return _inspect$10(item, depth) === first;
            })) {
                return first.trim() + '[]';
            } else {
                return '[' + input.slice(0, maxKeys).map(function (item) {
                    return _inspect$10(item, depth);
                }).join(', ') + (input.length >= maxKeys ? ', ...' : '') + ']';
            }
        } else {
            return 'Array';
        }
    } else {
        var keys = Object.keys(input);

        if (!keys.length) {
            if (input.constructor && input.constructor.name && input.constructor.name !== 'Object') {
                return input.constructor.name;
            } else {
                return 'Object';
            }
        }

        if (depth > maxDepth) return '{...}';
        var indent = '  '.repeat(depth - 1);
        var entries = keys.slice(0, maxKeys).map(function (key) {
            return (/^([A-Z_$][A-Z0-9_$]*)$/i.test(key) ? key : JSON.stringify(key)) + ': ' + _inspect$10(input[key], depth) + ';';
        }).join('\n  ' + indent);

        if (keys.length >= maxKeys) {
            entries += '\n  ' + indent + '...';
        }

        if (input.constructor && input.constructor.name && input.constructor.name !== 'Object') {
            return input.constructor.name + ' {\n  ' + indent + entries + '\n' + indent + '}';
        } else {
            return '{\n  ' + indent + entries + '\n' + indent + '}';
        }
    }
}

var EventListener = function () {
    function EventListener() {
        classCallCheck(this, EventListener);

        this.events = {
            nodecreate: [],
            groupcreate: [],
            connectioncreate: [],
            noderemove: [],
            groupremove: [],
            connectionremove: [],
            nodeselect: [],
            groupselect: [],
            error: [],
            change: [],
            transform: []
        };
        this.persistent = true;
    }

    createClass(EventListener, [{
        key: 'on',
        value: function on(names, handler) {
            var _this = this;

            if (!(typeof names === 'string')) {
                throw new TypeError('Value of argument "names" violates contract.\n\nExpected:\nstring\n\nGot:\n' + _inspect$11(names));
            }

            if (!(typeof handler === 'function')) {
                throw new TypeError('Value of argument "handler" violates contract.\n\nExpected:\n() => {}\n\nGot:\n' + _inspect$11(handler));
            }

            names.split(' ').forEach(function (name) {
                if (!_this.events[name]) throw new Error('The event ' + name + ' does not exist');
                _this.events[name].push(handler);
            });

            return this;
        }
    }, {
        key: 'trigger',
        value: function trigger(name, param) {
            var _this2 = this;

            if (!(typeof name === 'string')) {
                throw new TypeError('Value of argument "name" violates contract.\n\nExpected:\nstring\n\nGot:\n' + _inspect$11(name));
            }

            if (!(name in this.events)) throw new Error('The event ' + name + ' cannot be triggered');

            return this.events[name].reduce(function (r, e) {
                return e(param, _this2.persistent) !== false && r;
            }, true); // return false if at least one event is false        
        }
    }]);
    return EventListener;
}();

function _inspect$11(input, depth) {
    var maxDepth = 4;
    var maxKeys = 15;

    if (depth === undefined) {
        depth = 0;
    }

    depth += 1;

    if (input === null) {
        return 'null';
    } else if (input === undefined) {
        return 'void';
    } else if (typeof input === 'string' || typeof input === 'number' || typeof input === 'boolean') {
        return typeof input === 'undefined' ? 'undefined' : _typeof(input);
    } else if (Array.isArray(input)) {
        if (input.length > 0) {
            if (depth > maxDepth) return '[...]';

            var first = _inspect$11(input[0], depth);

            if (input.every(function (item) {
                return _inspect$11(item, depth) === first;
            })) {
                return first.trim() + '[]';
            } else {
                return '[' + input.slice(0, maxKeys).map(function (item) {
                    return _inspect$11(item, depth);
                }).join(', ') + (input.length >= maxKeys ? ', ...' : '') + ']';
            }
        } else {
            return 'Array';
        }
    } else {
        var keys = Object.keys(input);

        if (!keys.length) {
            if (input.constructor && input.constructor.name && input.constructor.name !== 'Object') {
                return input.constructor.name;
            } else {
                return 'Object';
            }
        }

        if (depth > maxDepth) return '{...}';
        var indent = '  '.repeat(depth - 1);
        var entries = keys.slice(0, maxKeys).map(function (key) {
            return (/^([A-Z_$][A-Z0-9_$]*)$/i.test(key) ? key : JSON.stringify(key)) + ': ' + _inspect$11(input[key], depth) + ';';
        }).join('\n  ' + indent);

        if (keys.length >= maxKeys) {
            entries += '\n  ' + indent + '...';
        }

        if (input.constructor && input.constructor.name && input.constructor.name !== 'Object') {
            return input.constructor.name + ' {\n  ' + indent + entries + '\n' + indent + '}';
        } else {
            return '{\n  ' + indent + entries + '\n' + indent + '}';
        }
    }
}

var Command = function Command(exec, undo, args) {
    classCallCheck(this, Command);

    this.exec = function () {
        exec.apply(undefined, toConsumableArray(args));
    };
    this.undo = function () {
        undo.apply(undefined, toConsumableArray(args));
    };
    this.a = args[0];
};

var History = function () {
    function History(editor) {
        classCallCheck(this, History);

        this.editor = editor;
        this.list = [];
        this._locked = false;
        this.position = -1;
    }

    createClass(History, [{
        key: "add",
        value: function add(exec, undo, args) {
            if (this._locked) return;

            this.position++;
            this.list.splice(this.position);
            this.list.push(new Command(exec, undo, args));
        }
    }, {
        key: "undo",
        value: function undo() {
            if (this.position < 0) return;

            this._locked = true;
            this.list[this.position--].undo();
            this._locked = false;
        }
    }, {
        key: "redo",
        value: function redo() {
            if (this.position + 1 >= this.list.length) return;

            this._locked = true;
            this.list[++this.position].exec();
            this._locked = false;
        }
    }]);
    return History;
}();

var Selected = function () {
    function Selected() {
        classCallCheck(this, Selected);

        this.list = [];
    }

    createClass(Selected, [{
        key: 'add',
        value: function add(item) {
            var accumulate = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : false;

            if (!(item instanceof Node || item instanceof Group$1)) {
                throw new TypeError('Value of argument "item" violates contract.\n\nExpected:\nNode | Group\n\nGot:\n' + _inspect$12(item));
            }

            if (accumulate) {
                if (this.contains(item)) this.remove(item);else this.list.push(item);
            } else this.list = [item];
        }
    }, {
        key: 'clear',
        value: function clear() {
            var _this = this;

            this.each(function (item) {
                _this.remove(item);
            });
        }
    }, {
        key: 'remove',
        value: function remove(item) {
            this.list.splice(this.list.indexOf(item), 1);
        }
    }, {
        key: 'contains',
        value: function contains(item) {
            return this.list.indexOf(item) !== -1;
        }
    }, {
        key: 'each',
        value: function each(callback) {
            this.list.forEach(callback);
        }
    }, {
        key: 'eachNode',
        value: function eachNode(callback) {
            this.list.filter(function (item) {
                return item instanceof Node;
            }).forEach(callback);
        }
    }, {
        key: 'eachGroup',
        value: function eachGroup(callback) {
            this.list.filter(function (item) {
                return item instanceof Group$1;
            }).forEach(callback);
        }
    }, {
        key: 'getNodes',
        value: function getNodes() {
            return this.list.filter(function (item) {
                return item instanceof Node;
            });
        }
    }, {
        key: 'getGroups',
        value: function getGroups() {
            return this.list.filter(function (item) {
                return item instanceof Group$1;
            });
        }
    }]);
    return Selected;
}();

function _inspect$12(input, depth) {
    var maxDepth = 4;
    var maxKeys = 15;

    if (depth === undefined) {
        depth = 0;
    }

    depth += 1;

    if (input === null) {
        return 'null';
    } else if (input === undefined) {
        return 'void';
    } else if (typeof input === 'string' || typeof input === 'number' || typeof input === 'boolean') {
        return typeof input === 'undefined' ? 'undefined' : _typeof(input);
    } else if (Array.isArray(input)) {
        if (input.length > 0) {
            if (depth > maxDepth) return '[...]';

            var first = _inspect$12(input[0], depth);

            if (input.every(function (item) {
                return _inspect$12(item, depth) === first;
            })) {
                return first.trim() + '[]';
            } else {
                return '[' + input.slice(0, maxKeys).map(function (item) {
                    return _inspect$12(item, depth);
                }).join(', ') + (input.length >= maxKeys ? ', ...' : '') + ']';
            }
        } else {
            return 'Array';
        }
    } else {
        var keys = Object.keys(input);

        if (!keys.length) {
            if (input.constructor && input.constructor.name && input.constructor.name !== 'Object') {
                return input.constructor.name;
            } else {
                return 'Object';
            }
        }

        if (depth > maxDepth) return '{...}';
        var indent = '  '.repeat(depth - 1);
        var entries = keys.slice(0, maxKeys).map(function (key) {
            return (/^([A-Z_$][A-Z0-9_$]*)$/i.test(key) ? key : JSON.stringify(key)) + ': ' + _inspect$12(input[key], depth) + ';';
        }).join('\n  ' + indent);

        if (keys.length >= maxKeys) {
            entries += '\n  ' + indent + '...';
        }

        if (input.constructor && input.constructor.name && input.constructor.name !== 'Object') {
            return input.constructor.name + ' {\n  ' + indent + entries + '\n' + indent + '}';
        } else {
            return '{\n  ' + indent + entries + '\n' + indent + '}';
        }
    }
}

var NodeEditor = function () {
    function NodeEditor(id, container, components, menu) {
        classCallCheck(this, NodeEditor);

        if (!(typeof id === 'string')) {
            throw new TypeError('Value of argument "id" violates contract.\n\nExpected:\nstring\n\nGot:\n' + _inspect$9(id));
        }

        if (!(container instanceof HTMLElement)) {
            throw new TypeError('Value of argument "container" violates contract.\n\nExpected:\nHTMLElement\n\nGot:\n' + _inspect$9(container));
        }

        if (!(Array.isArray(components) && components.every(function (item) {
            return item instanceof Component;
        }))) {
            throw new TypeError('Value of argument "components" violates contract.\n\nExpected:\nComponent[]\n\nGot:\n' + _inspect$9(components));
        }

        if (!(menu instanceof ContextMenu)) {
            throw new TypeError('Value of argument "menu" violates contract.\n\nExpected:\nContextMenu\n\nGot:\n' + _inspect$9(menu));
        }

        if (!Utils.isValidId(id)) throw new Error('ID should be valid to name@0.1.0 format');

        this.id = id;
        this._id = Math.random().toString(36).substr(2, 9);
        this.components = components;
        this.view = new EditorView(this, container, menu);
        this.eventListener = new EventListener();
        this.selected = new Selected();
        this.history = new History(this);
        this.nodes = [];
        this.groups = [];
        this.readOnly = false;

        this.view.resize();
    }

    createClass(NodeEditor, [{
        key: 'addNode',
        value: function addNode(node) {
            var mousePlaced = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : false;

            if (!(node instanceof Node)) {
                throw new TypeError('Value of argument "node" violates contract.\n\nExpected:\nNode\n\nGot:\n' + _inspect$9(node));
            }

            if (this.eventListener.trigger('nodecreate', node)) {
                if (mousePlaced) node.position = this.view.mouse;
                this.nodes.push(node);
                this.eventListener.trigger('change');

                this.history.add(this.addNode.bind(this), this.removeNode.bind(this), [node]);
            }
        }
    }, {
        key: 'addGroup',
        value: function addGroup(group) {
            if (!(group instanceof Group$1)) {
                throw new TypeError('Value of argument "group" violates contract.\n\nExpected:\nGroup\n\nGot:\n' + _inspect$9(group));
            }

            if (this.eventListener.trigger('groupcreate', group)) {
                this.groups.push(group);
                this.eventListener.trigger('change');
            }

            this.view.update();
        }
    }, {
        key: 'removeNode',
        value: function removeNode(node) {
            var _this = this;

            if (!(node instanceof Node)) {
                throw new TypeError('Value of argument "node" violates contract.\n\nExpected:\nNode\n\nGot:\n' + _inspect$9(node));
            }

            var index = this.nodes.indexOf(node);

            if (this.eventListener.trigger('noderemove', node)) {
                node.getConnections().forEach(function (c) {
                    return _this.removeConnection(c);
                });

                this.nodes.splice(index, 1);
                this.eventListener.trigger('change');

                this.history.add(this.removeNode.bind(this), this.addNode.bind(this), [node]);
            }

            this.view.update();
        }
    }, {
        key: 'removeGroup',
        value: function removeGroup(group) {
            if (!(group instanceof Group$1)) {
                throw new TypeError('Value of argument "group" violates contract.\n\nExpected:\nGroup\n\nGot:\n' + _inspect$9(group));
            }

            if (this.eventListener.trigger('groupremove', group)) {
                group.remove();
                this.groups.splice(this.groups.indexOf(group), 1);
                this.eventListener.trigger('change');
            }

            this.view.update();
        }
    }, {
        key: 'connect',
        value: function connect(output, input) {
            if (!(output instanceof Output || output instanceof Connection)) {
                throw new TypeError('Value of argument "output" violates contract.\n\nExpected:\nOutput | Connection\n\nGot:\n' + _inspect$9(output));
            }

            if (!(input == null || input instanceof Input)) {
                throw new TypeError('Value of argument "input" violates contract.\n\nExpected:\n?Input\n\nGot:\n' + _inspect$9(input));
            }

            if (output instanceof Connection) {
                input = output.input;
                output = output.output;
            }

            if (this.eventListener.trigger('connectioncreate', { output: output, input: input })) {
                try {
                    var connection = output.connectTo(input);

                    this.eventListener.trigger('change');
                    this.history.add(this.connect.bind(this), this.removeConnection.bind(this), [connection]);
                } catch (e) {
                    console.warn(e);
                    this.eventListener.trigger('error', e);
                }
            }
            this.view.update();
        }
    }, {
        key: 'removeConnection',
        value: function removeConnection(connection) {
            if (!(connection instanceof Connection)) {
                throw new TypeError('Value of argument "connection" violates contract.\n\nExpected:\nConnection\n\nGot:\n' + _inspect$9(connection));
            }

            if (this.eventListener.trigger('connectionremove', connection)) {
                connection.remove();
                this.eventListener.trigger('change');

                this.history.add(this.removeConnection.bind(this), this.connect.bind(this), [connection]);
            }
            this.view.update();
        }
    }, {
        key: 'selectNode',
        value: function selectNode(node) {
            var accumulate = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : false;

            if (!(node instanceof Node)) {
                throw new TypeError('Value of argument "node" violates contract.\n\nExpected:\nNode\n\nGot:\n' + _inspect$9(node));
            }

            if (!(typeof accumulate === 'boolean')) {
                throw new TypeError('Value of argument "accumulate" violates contract.\n\nExpected:\nboolean\n\nGot:\n' + _inspect$9(accumulate));
            }

            if (this.nodes.indexOf(node) === -1) throw new Error('Node not exist in list');

            if (this.eventListener.trigger('nodeselect', node)) this.selected.add(node, accumulate);

            this.view.update();
        }
    }, {
        key: 'selectGroup',
        value: function selectGroup(group) {
            var accumulate = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : false;

            if (!(group instanceof Group$1)) {
                throw new TypeError('Value of argument "group" violates contract.\n\nExpected:\nGroup\n\nGot:\n' + _inspect$9(group));
            }

            if (!(typeof accumulate === 'boolean')) {
                throw new TypeError('Value of argument "accumulate" violates contract.\n\nExpected:\nboolean\n\nGot:\n' + _inspect$9(accumulate));
            }

            if (this.groups.indexOf(group) === -1) throw new Error('Group not exist in list');

            if (this.eventListener.trigger('groupselect', group)) this.selected.add(group, accumulate);

            this.view.update();
        }
    }, {
        key: 'keyDown',
        value: function keyDown() {
            if (this.readOnly) return;

            switch (d3.event.keyCode) {
                case 46:
                    this.selected.eachNode(this.removeNode.bind(this));
                    this.selected.eachGroup(this.removeGroup.bind(this));
                    this.view.update();
                    break;
                case 71:
                    var nodes = this.selected.getNodes();

                    if (nodes.length > 0) this.addGroup(new Group$1('Group', { nodes: nodes }));

                    break;
                case 90:
                    if (d3.event.ctrlKey && d3.event.shiftKey) this.history.redo();else if (d3.event.ctrlKey) this.history.undo();

                    break;
            }
        }
    }, {
        key: 'clear',
        value: function clear() {
            this.nodes.splice(0, this.nodes.length);
            this.groups.splice(0, this.groups.length);
        }
    }, {
        key: 'toJSON',
        value: function toJSON() {
            var nodes = {};
            var groups = {};

            this.nodes.forEach(function (node) {
                return nodes[node.id] = node.toJSON();
            });
            this.groups.forEach(function (group) {
                return groups[group.id] = group.toJSON();
            });

            return {
                'id': this.id,
                'nodes': nodes,
                'groups': groups
            };
        }
    }, {
        key: 'fromJSON',
        value: function fromJSON(json) {
            var _this2 = this;

            var checking, nodes;
            return regeneratorRuntime.async(function fromJSON$(_context2) {
                while (1) {
                    switch (_context2.prev = _context2.next) {
                        case 0:
                            if (json instanceof Object) {
                                _context2.next = 2;
                                break;
                            }

                            throw new TypeError('Value of argument "json" violates contract.\n\nExpected:\nObject\n\nGot:\n' + _inspect$9(json));

                        case 2:
                            checking = Utils.validate(this.id, json);

                            if (checking.success) {
                                _context2.next = 7;
                                break;
                            }

                            this.eventListener.trigger('error', checking.msg);
                            console.warn(checking.msg);
                            return _context2.abrupt('return', false);

                        case 7:

                            this.eventListener.persistent = false;

                            this.clear();
                            nodes = {};
                            _context2.prev = 10;
                            _context2.next = 13;
                            return regeneratorRuntime.awrap(Promise.all(Object.keys(json.nodes).map(function _callee(id) {
                                var node, component;
                                return regeneratorRuntime.async(function _callee$(_context) {
                                    while (1) {
                                        switch (_context.prev = _context.next) {
                                            case 0:
                                                node = json.nodes[id];
                                                component = _this2.components.find(function (c) {
                                                    return c.name === node.title;
                                                });

                                                if (component) {
                                                    _context.next = 4;
                                                    break;
                                                }

                                                throw 'Component ' + node.title + ' was not found';

                                            case 4:
                                                _context.next = 6;
                                                return regeneratorRuntime.awrap(Node.fromJSON(component, node));

                                            case 6:
                                                nodes[id] = _context.sent;

                                                _this2.addNode(nodes[id]);

                                            case 8:
                                            case 'end':
                                                return _context.stop();
                                        }
                                    }
                                }, null, _this2);
                            })));

                        case 13:

                            Object.keys(json.nodes).forEach(function (id) {
                                var jsonNode = json.nodes[id];
                                var node = nodes[id];

                                jsonNode.outputs.forEach(function (outputJson, i) {
                                    outputJson.connections.forEach(function (jsonConnection) {
                                        var nodeId = jsonConnection.node;
                                        var inputIndex = jsonConnection.input;
                                        var targetInput = nodes[nodeId].inputs[inputIndex];

                                        _this2.connect(node.outputs[i], targetInput);
                                    });
                                });
                            });

                            if (_typeof(json.groups) === 'object') Object.keys(json.groups).forEach(function (id) {
                                var group = Group$1.fromJSON(json.groups[id]);

                                json.groups[id].nodes.forEach(function (nodeId) {
                                    var node = nodes[nodeId];

                                    group.addNode(node);
                                });
                                _this2.addGroup(group);
                            });
                            _context2.next = 22;
                            break;

                        case 17:
                            _context2.prev = 17;
                            _context2.t0 = _context2['catch'](10);

                            console.warn(_context2.t0);
                            this.eventListener.trigger('error', _context2.t0);
                            return _context2.abrupt('return', false);

                        case 22:
                            this.view.update();
                            this.eventListener.persistent = true;
                            return _context2.abrupt('return', true);

                        case 25:
                        case 'end':
                            return _context2.stop();
                    }
                }
            }, null, this, [[10, 17]]);
        }
    }]);
    return NodeEditor;
}();

function _inspect$9(input, depth) {
    var maxDepth = 4;
    var maxKeys = 15;

    if (depth === undefined) {
        depth = 0;
    }

    depth += 1;

    if (input === null) {
        return 'null';
    } else if (input === undefined) {
        return 'void';
    } else if (typeof input === 'string' || typeof input === 'number' || typeof input === 'boolean') {
        return typeof input === 'undefined' ? 'undefined' : _typeof(input);
    } else if (Array.isArray(input)) {
        if (input.length > 0) {
            if (depth > maxDepth) return '[...]';

            var first = _inspect$9(input[0], depth);

            if (input.every(function (item) {
                return _inspect$9(item, depth) === first;
            })) {
                return first.trim() + '[]';
            } else {
                return '[' + input.slice(0, maxKeys).map(function (item) {
                    return _inspect$9(item, depth);
                }).join(', ') + (input.length >= maxKeys ? ', ...' : '') + ']';
            }
        } else {
            return 'Array';
        }
    } else {
        var keys = Object.keys(input);

        if (!keys.length) {
            if (input.constructor && input.constructor.name && input.constructor.name !== 'Object') {
                return input.constructor.name;
            } else {
                return 'Object';
            }
        }

        if (depth > maxDepth) return '{...}';
        var indent = '  '.repeat(depth - 1);
        var entries = keys.slice(0, maxKeys).map(function (key) {
            return (/^([A-Z_$][A-Z0-9_$]*)$/i.test(key) ? key : JSON.stringify(key)) + ': ' + _inspect$9(input[key], depth) + ';';
        }).join('\n  ' + indent);

        if (keys.length >= maxKeys) {
            entries += '\n  ' + indent + '...';
        }

        if (input.constructor && input.constructor.name && input.constructor.name !== 'Object') {
            return input.constructor.name + ' {\n  ' + indent + entries + '\n' + indent + '}';
        } else {
            return '{\n  ' + indent + entries + '\n' + indent + '}';
        }
    }
}

var Task = function () {
    function Task(inputs, action) {
        var _this = this;

        classCallCheck(this, Task);

        this.inputs = inputs;
        this.action = action;
        this.next = [];
        this.outputData = null;
        this.closed = [];

        this.getOptions().forEach(function (input) {
            input.forEach(function (con) {
                con.task.next.push({ index: con.index, task: _this });
            });
        });
    }

    createClass(Task, [{
        key: "getOptions",
        value: function getOptions() {
            return this.inputs.filter(function (input) {
                return input[0] && input[0].task;
            });
        }
    }, {
        key: "getOutputs",
        value: function getOutputs() {
            return this.inputs.filter(function (input) {
                return input[0] && input[0].get;
            });
        }
    }, {
        key: "reset",
        value: function reset() {
            this.outputData = null;
        }
    }, {
        key: "run",
        value: function run(data) {
            var _this2 = this;

            var inputs = this.getOutputs().map(function (input) {
                return input.map(function (con) {
                    if (con) {
                        con.run();
                        return con.get();
                    }
                });
            });

            if (!this.outputData) {
                this.outputData = this.action(inputs, data);

                this.next.filter(function (f) {
                    return !_this2.closed.includes(f.index);
                }).forEach(function (f) {
                    return f.task.run();
                });
            }
        }
    }, {
        key: "option",
        value: function option(index) {
            var task = this;

            return { task: task, index: index };
        }
    }, {
        key: "output",
        value: function output(index) {
            var task = this;

            return {
                run: task.run.bind(task),
                get: function get$$1() {
                    return task.outputData[index];
                }
            };
        }
    }]);
    return Task;
}();

exports.Component = Component;
exports.ContextMenu = ContextMenu;
exports.Control = Control;
exports.Diff = Diff;
exports.NodeEditor = NodeEditor;
exports.Engine = Engine;
exports.Group = Group$1;
exports.Input = Input;
exports.Node = Node;
exports.Output = Output;
exports.Socket = Socket;
exports.Task = Task;
exports.Module = Module;
exports.ModuleManager = ModuleManager;

Object.defineProperty(exports, '__esModule', { value: true });

})));
//# sourceMappingURL=d3-node-editor.js.map
