let predicates = [];
let equals     = $['find.equals'];
let contains   = $['find.contains'];

predicates.push(equals('aString', 'string01'));
predicates.push(contains('aString', '3'));

$.find('TestOne', $.predicate.or(predicates), $.predicate.sort('aString'));
