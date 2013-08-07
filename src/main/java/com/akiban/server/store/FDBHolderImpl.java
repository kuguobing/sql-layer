/**
 * Copyright (C) 2009-2013 Akiban Technologies, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.akiban.server.store;

import com.akiban.server.service.Service;
import com.foundationdb.Database;
import com.foundationdb.FDB;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FDBHolderImpl implements FDBHolder, Service {
    private static final Logger LOG = LoggerFactory.getLogger(FDBHolderImpl.class.getName());

    private FDB fdb;
    private Database db;

    @Inject
    public FDBHolderImpl() {
    }


    //
    // Service
    //

    @Override
    public void start() {
        // Just one FDB for whole JVM and its dispose doesn't do anything.
        if(fdb == null) {
            fdb = FDB.selectAPIVersion(23);
            LOG.info("Started FDB with API Version 23");
        }
        db = fdb.open();
    }

    @Override
    public void stop() {
        if(db != null)
            db.dispose();
        db = null;
    }

    @Override
    public void crash() {
        stop();
    }


    //
    // FDBHolder
    //

    @Override
    public FDB getFDB() {
        return fdb;
    }

    @Override
    public Database getDatabase() {
        return db;
    }
}