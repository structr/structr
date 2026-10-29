// Each grant covers the interval [minAmount, maxAmount], null means open-ended
let intervals = [
	[   0,   10],   // Grant0
	[  10,   20],   // Grant1: touches Grant0 at 10
	[  20,   30],   // Grant2: touches Grant1 at 20
	[   5,   25],   // Grant3: spans Grant0, Grant1 and Grant2
	[  12,   18],   // Grant4: completely inside Grant1
	[  15,   15],   // Grant5: zero-length
	[ -10,   -5],   // Grant6: negative values
	[  30, null],   // Grant7: no upper bound
	[null,    5],   // Grant8: no lower bound
	[null, null]    // Grant9: no bounds at all
];

// the lt/gt based predicates cannot handle null values, so they never find these
let openEnded = ['Grant7', 'Grant8', 'Grant9'];

let grants = [];

for (var i=0; i < intervals.length; i++) {
	grants.push($.create('ResearchGrant', {
		name: 'Grant' + i,
		minAmount: intervals[i][0],
		maxAmount: intervals[i][1]
	}));
}

let getSimplifiedSearchPredicates = (begin, end, includeStart, includeEnd) => {

	return $.predicate.and(

		// grant.minAmount < end   (or <= end)
		includeEnd ? $.predicate.lte('minAmount', end) : $.predicate.lt('minAmount', end),

		// grant.maxAmount > begin   (or >= begin)
		includeStart ? $.predicate.gte('maxAmount', begin) : $.predicate.gt('maxAmount', begin)
	);
};

let getSearchPredicatesWithOverlapsPredicate = (begin, end, flags) => {

	// an empty flags array calls the four-argument form, i.e. tests the defaults
	return $.predicate.overlaps('minAmount', 'maxAmount', begin, end, ...flags);
};

let names = (list) => list.map(g => g.name).sort().join(',');

let testOverlaps = (description, begin, end, flags, expected) => {

	let actual1 = names($.find('ResearchGrant', getSearchPredicatesWithOverlapsPredicate(begin, end, flags)));
	$.assert(expected.join(',') === actual1, 422, `Found '${actual1}', but when searching grants overlapping ${begin} - ${end} with flags [${flags}] (${description}), we should get '${expected.join(',')}'.`);

	// cross-check with the lt/gt predicates, only possible for a closed search interval
	if (begin !== null && end !== null) {

		let expected2 = expected.filter(n => !openEnded.includes(n)).join(',');
		let actual2   = names($.find('ResearchGrant', getSimplifiedSearchPredicates(begin, end, flags[0], flags[1])));
		$.assert(expected2 === actual2, 422, `Found '${actual2}', but when searching grants overlapping ${begin} - ${end} with lt/gt predicates and flags [${flags}] (${description}), we should get '${expected2}'.`);
	}
};

testOverlaps('very wide range',                     -100, 1000, [],            ['Grant0', 'Grant1', 'Grant2', 'Grant3', 'Grant4', 'Grant5', 'Grant6', 'Grant7', 'Grant8', 'Grant9']);
testOverlaps('narrow range inside some grants',       12,   14, [],            ['Grant1', 'Grant3', 'Grant4', 'Grant9']);
testOverlaps('explicit default flags',                12,   14, [false, false], ['Grant1', 'Grant3', 'Grant4', 'Grant9']);

// Grant6 ends at -5, Grant0 starts at 0
testOverlaps('touching grants are excluded',          -5,    0, [],            ['Grant8', 'Grant9']);
testOverlaps('includeStart: touching at start',       -5,    0, [true, false], ['Grant6', 'Grant8', 'Grant9']);
testOverlaps('includeEnd: touching at end',           -5,    0, [false, true], ['Grant0', 'Grant8', 'Grant9']);
testOverlaps('both: touching at both ends',           -5,    0, [true, true],  ['Grant0', 'Grant6', 'Grant8', 'Grant9']);

// identical to Grant2, Grant1 ends at 20 and Grant7 starts at 30
testOverlaps('identical interval',                    20,   30, [],            ['Grant2', 'Grant3', 'Grant9']);
testOverlaps('identical interval, inclusive',         20,   30, [true, true],  ['Grant1', 'Grant2', 'Grant3', 'Grant7', 'Grant9']);

// zero-length search interval on top of zero-length Grant5
testOverlaps('zero-length search',                    15,   15, [],            ['Grant1', 'Grant3', 'Grant4', 'Grant9']);
testOverlaps('zero-length search, includeStart only', 15,   15, [true, false], ['Grant1', 'Grant3', 'Grant4', 'Grant9']);
testOverlaps('zero-length search, includeEnd only',   15,   15, [false, true], ['Grant1', 'Grant3', 'Grant4', 'Grant9']);
testOverlaps('zero-length search, inclusive',         15,   15, [true, true],  ['Grant1', 'Grant3', 'Grant4', 'Grant5', 'Grant9']);

// open-ended search intervals
testOverlaps('no upper bound',                        25, null, [],            ['Grant2', 'Grant7', 'Grant9']);
testOverlaps('no upper bound, includeStart',          25, null, [true, false], ['Grant2', 'Grant3', 'Grant7', 'Grant9']);
testOverlaps('no lower bound',                      null,    0, [],            ['Grant6', 'Grant8', 'Grant9']);
testOverlaps('no lower bound, includeEnd',          null,    0, [false, true], ['Grant0', 'Grant6', 'Grant8', 'Grant9']);
testOverlaps('no bounds at all',                    null, null, [],            ['Grant0', 'Grant1', 'Grant2', 'Grant3', 'Grant4', 'Grant5', 'Grant6', 'Grant7', 'Grant8', 'Grant9']);

// only the open-ended grants reach this far
testOverlaps('far above all grants',                1000, 2000, [],            ['Grant7', 'Grant9']);
testOverlaps('far below all grants',               -1000, -500, [],            ['Grant8', 'Grant9']);

// null flags fall back to the defaults
testOverlaps('null flags',                            -5,    0, [null, null],  ['Grant8', 'Grant9']);

{
	// string bounds are converted to the property type
	let expected = 'Grant1,Grant3,Grant4,Grant9';
	let actual   = names($.find('ResearchGrant', $.predicate.overlaps('minAmount', 'maxAmount', '12', '14')));
	$.assert(expected === actual, 422, `Found '${actual}', but when searching grants overlapping '12' - '14' given as strings, we should get '${expected}'.`);
}

{
	// combined with other predicates
	let expected = 'Grant1,Grant4,Grant9';
	let actual   = names($.find('ResearchGrant', $.predicate.and(
		$.predicate.overlaps('minAmount', 'maxAmount', 12, 14),
		$.predicate.not($.predicate.equals('name', 'Grant3'))
	)));
	$.assert(expected === actual, 422, `Found '${actual}', but when searching grants overlapping 12 - 14 excluding Grant3, we should get '${expected}'.`);
}

{
	// two disjoint search intervals
	let expected = 'Grant0,Grant3,Grant6,Grant7,Grant8,Grant9';
	let actual   = names($.find('ResearchGrant', $.predicate.or(
		$.predicate.overlaps('minAmount', 'maxAmount', -8, -6),
		$.predicate.overlaps('minAmount', 'maxAmount',  6,  8),
		$.predicate.overlaps('minAmount', 'maxAmount', 40, 50)
	)));
	$.assert(expected === actual, 422, `Found '${actual}', but when searching grants overlapping -8 - -6, 6 - 8 or 40 - 50, we should get '${expected}'.`);
}

{
	// complement
	let expected = 'Grant0,Grant2,Grant5,Grant6,Grant7,Grant8';
	let actual   = names($.find('ResearchGrant', $.predicate.not($.predicate.overlaps('minAmount', 'maxAmount', 12, 14))));
	$.assert(expected === actual, 422, `Found '${actual}', but when searching grants NOT overlapping 12 - 14, we should get '${expected}'.`);
}

{
	// missing keys are rejected
	let failed = false;

	try {
		$.predicate.overlaps(null, 'maxAmount', 12, 14);
	} catch (e) {
		failed = true;
	}

	$.assert(failed, 422, `Calling overlaps() without startKey should fail.`);
}

{
	// too few arguments are rejected
	let failed = false;

	try {
		$.predicate.overlaps('minAmount', 'maxAmount', 12);
	} catch (e) {
		failed = true;
	}

	$.assert(failed, 422, `Calling overlaps() with three arguments should fail.`);
}

"Success";
