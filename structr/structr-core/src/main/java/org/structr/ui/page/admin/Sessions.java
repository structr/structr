/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.ui.page.admin;

import java.util.List;
import java.util.logging.Logger;
import org.apache.click.Context;
import org.apache.click.control.Column;
import org.apache.click.control.Decorator;
import org.apache.click.control.Table;
import org.apache.click.dataprovider.DataProvider;
import org.apache.click.util.Bindable;
import org.structr.context.SessionMonitor;
import org.structr.context.SessionMonitor.Session;

/**
 * Sessions page
 * 
 * Displays sessions list
 * 
 * @author axel
 */
public class Sessions extends Admin {

    private static final Logger logger = Logger.getLogger(Sessions.class.getName());
    @Bindable
    protected Table sessionsTable = new Table("sessionsTable");
    @Override
    public String getTemplate() {
        return "/maintenance-template.htm";
    }

    public Sessions() {

        super();
//        maintenancePanel = new Panel("maintenancePanel", "/panel/maintenance-panel.htm");

        sessionsTable.addColumn(new Column("id"));
        sessionsTable.addColumn(new Column("uid"));
        sessionsTable.addColumn(new Column("state"));
        sessionsTable.addColumn(new Column("userName"));
        Column loginTimestampColumn = new Column("loginTimestamp", "Login");
        loginTimestampColumn.setFormat("{0,date,medium} {0,time,medium}");
        sessionsTable.addColumn(loginTimestampColumn);
        Column logoutTimestampColumn = new Column("logoutTimestamp", "Logout");
        logoutTimestampColumn.setFormat("{0,date,medium} {0,time,medium}");
        sessionsTable.addColumn(logoutTimestampColumn);
        sessionsTable.addColumn(new Column("lastActivityText"));


        Column inactiveSinceColumn = new Column("inactiveSince", "Inactive");
        inactiveSinceColumn.setDecorator(new Decorator() {

            @Override
            public String render(Object row, Context context) {
                long ms = ((Session) row).getInactiveSince();
                if (ms < 1000) {
                    return ms + " ms";
                } else if (ms < 60 * 1000) {
                    return ms / 1000 + " s";
                } else if (ms < 60 * 60 * 1000) {
                    long min = ms / (60 * 1000);
                    long sec = (ms - (min * 60 * 1000)) / 1000;
                    return min + " m " + sec + " s";
                } else if (ms < 24 * 60 * 60 * 1000) {
                    long hrs = ms / (60 * 60 * 1000);
                    long min = (ms - (hrs * 60 * 60 * 1000)) / (60 * 1000);
                    long sec = (ms - (hrs * 60 * 60 * 1000) - (min * 60 * 1000)) / 1000;
                    return hrs + " h " + min + " m " + sec + " s";
                } else {
                    return "more than a day";
                }
            }
        });
        sessionsTable.addColumn(inactiveSinceColumn);

        sessionsTable.setSortable(true);
        sessionsTable.setSortedColumn("inactiveSince");
        sessionsTable.setSortedAscending(true);
        sessionsTable.setPageSize(15);
        sessionsTable.setHoverRows(true);
        sessionsTable.setShowBanner(true);
        sessionsTable.setClass(TABLE_CLASS);

    }

    @Override
    public void onInit() {
        super.onInit();
    }

    @Override
    public void onRender() {

        // fill table with sessions
        sessionsTable.setDataProvider(new DataProvider() {

            @Override
            public List<Session> getData() {
                return (List<Session>) SessionMonitor.getSessions();
            }
        });

    }

}
