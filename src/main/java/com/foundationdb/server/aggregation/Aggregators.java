/**
 * Copyright (C) 2009-2013 FoundationDB, LLC
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

package com.foundationdb.server.aggregation;

import com.foundationdb.qp.rowtype.RowType;

import java.util.ArrayList;
import java.util.List;

public final class Aggregators {

    public static List<AggregatorId> aggregatorIds(List<String> names, RowType rowType, int rowTypeOffset) {
        // TODO input validations
        List<AggregatorId> result = new ArrayList<>();
        for (String name : names) {
            result.add(new AggregatorId(name, rowType.typeAt(rowTypeOffset++)));
        }
        return result;
    }

    public static List<AggregatorFactory> factories(AggregatorRegistry registry, List<AggregatorId> aggregatorIds) {
        List<AggregatorFactory> result = new ArrayList<>();
        for (AggregatorId id : aggregatorIds) {
            result.add(registry.get(id.name(), id.type()));
        }
        return result;
    }

}