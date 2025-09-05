${{
    //Structr.log('++++ begin retrieving tasks and projects ... ++++');

    // report configuration
    var request = Structr.get('request');

    // force german locale to get the correct calendar weeks
    Structr.setLocale('de');

    var from    = Structr.dateFormat(new Date(), 'yyyy-MM-01');
    var until   = Structr.dateFormat(new Date(), 'yyyy-MM-dd');
    var forDay  = Structr.dateFormat(new Date(), 'yyyy-MM-dd');
    var forWeek = Structr.dateFormat(new Date(), 'w/yyyy');
    var project = '';

    if (request.from    && request.from.length)    { from    = request.from;    }
    if (request.until   && request.until.length)   { until   = request.until;   }
    if (request.forDay  && request.forDay.length)  { forDay  = request.forDay;	}
    if (request.forWeek && request.forWeek.length) { forWeek = request.forWeek;	}

    var endDate = Structr.parseDate(until, 'yyyy-MM-dd');

    var to = Structr.dateFormat(endDate, 'yyyy-MM-dd');

    // add one day
    endDate.setDate(endDate.getDate() + 1);

    until   = Structr.dateFormat(endDate, 'yyyy-MM-dd');

    // this returns sunday
    var monday  = Structr.parseDate(forWeek, 'w/yyyy');
    var sunday  = Structr.parseDate(forWeek, 'w/yyyy');
    var weekday = Structr.parseDate(forWeek, 'w/yyyy');

    weekday.setDate(monday.getDate() + 1);
    monday.setDate(monday.getDate() + 1);
    sunday.setDate(sunday.getDate() + 7);

    Structr.store('from',  from);
    Structr.store('to',    to);
    Structr.store('until', until);
    Structr.store('forDay', forDay);
    Structr.store('forWeek', forWeek);
    Structr.store('monday', monday);
    Structr.store('sunday', sunday);
    Structr.store('weekday', weekday);

    // next and prev buttons
    var next = Structr.parseDate(forWeek, 'w/yyyy');
    var prev = Structr.parseDate(forWeek, 'w/yyyy');

    prev.setDate(prev.getDate() - 1);
    next.setDate(next.getDate() + 8);

    Structr.store('prev', Structr.dateFormat(prev, 'w/yyyy'));
    Structr.store('next', Structr.dateFormat(next, 'w/yyyy'));
}}