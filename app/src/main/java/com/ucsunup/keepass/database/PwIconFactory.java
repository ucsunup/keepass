/*
 * Copyright 2010-2013 Brian Pellin.
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
package com.ucsunup.keepass.database;

import java.util.UUID;

import org.apache.commons.collections.map.AbstractReferenceMap;
import org.apache.commons.collections.map.ReferenceMap;

public class PwIconFactory {
    /**
     * customIconMap
     * Cache for icon drawable.
     * Keys: Integer, Values: PwIconStandard
     */
    private ReferenceMap cache = new ReferenceMap(AbstractReferenceMap.HARD, AbstractReferenceMap.WEAK);

    /**
     * standardIconMap
     * Cache for icon drawable.
     * Keys: UUID, Values: PwIconCustom
     */
    private ReferenceMap customCache = new ReferenceMap(AbstractReferenceMap.HARD, AbstractReferenceMap.WEAK);

    public PwIconStandard getIcon(int iconId) {
        PwIconStandard icon = (PwIconStandard) cache.get(iconId);

        if (icon == null) {
            if (iconId == 1) {
                icon = PwIconStandard.FIRST;
            } else {
                icon = new PwIconStandard(iconId);
            }
            cache.put(iconId, icon);
        }

        return icon;
    }

    public PwIconCustom getIcon(UUID iconUuid) {
        PwIconCustom icon = (PwIconCustom) customCache.get(iconUuid);

        if (icon == null) {
            icon = new PwIconCustom(iconUuid, null);
            customCache.put(iconUuid, icon);
        }

        return icon;
    }

    public PwIconCustom getIcon(UUID iconUuid, byte[] data) {
        PwIconCustom icon = (PwIconCustom) customCache.get(iconUuid);

        if (icon == null) {
            icon = new PwIconCustom(iconUuid, data);
            customCache.put(iconUuid, icon);
        } else {
            icon.imageData = data;
        }

        return icon;
    }

    public void setIconData(UUID iconUuid, byte[] data) {
        getIcon(iconUuid, data);
    }

    public void put(PwIconCustom icon) {
        customCache.put(icon.uuid, icon);
    }

}
