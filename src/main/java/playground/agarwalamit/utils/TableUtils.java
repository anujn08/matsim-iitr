/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.agarwalamit.utils;

import java.util.Map;
import java.util.stream.Collectors;
import com.google.common.collect.Table;

/**
 * Created by amit on 16.11.17.
 */

public final class TableUtils {

    public static Map<String, Double> sumValues(Table<?, ?, Map<String, Double>> table) {
//        final Map<String, Double> outMap = new HashMap<>();
//        table.values().stream().forEach(e->
//                outMap.putAll(MapUtils.addMaps(outMap, e))
//        );
//        return outMap;
        return table.values()
             .stream()
             .flatMap(m -> m.entrySet().stream())
             .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Double::sum));

    }
}

