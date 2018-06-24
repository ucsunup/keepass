/*
 * Copyright 2011-2016 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.ucsunup.keepass.database.load;

import java.io.IOException;
import java.io.InputStream;

import com.ucsunup.keepass.UpdateStatus;
import com.ucsunup.keepass.database.PwDatabaseV3Debug;
import com.ucsunup.keepass.database.exception.InvalidDBException;

public class ImporterV3Debug extends ImporterV3 {

    @Override
    protected PwDatabaseV3Debug createDB() {
        return new PwDatabaseV3Debug();
    }

    @Override
    public PwDatabaseV3Debug openDatabase(InputStream inStream, String password,
                                          InputStream keyInputStream, UpdateStatus status) throws IOException,
            InvalidDBException {
        return (PwDatabaseV3Debug) super.openDatabase(inStream, password, keyInputStream, status);
    }
}
