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

package playground.agarwalamit.opdyts.patna.networkModesOnly;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import playground.agarwalamit.utils.FileUtils;

/**
 * Created by amit on 06.06.17.
 */


public class PatnaASCAnalyzer {

    private final double carASC = 0.;
    private final double [] ascTrials = {-3.0, -2.5, -2.0, -1.5, -1.0, -0.5, 0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0};

    public static void main(String[] args) {

        String configFile ;
        String outDir ;

        if(args.length>0) {
            configFile = args[0];
            outDir = args[1];
        } else {
            configFile = FileUtils.RUNS_SVN+"/opdyts/patna/input_networkModes/"+"/config_networkModesOnly.xml";
            outDir = FileUtils.RUNS_SVN+"/opdyts/patna/output_networkModes/ascAnalysis/";
        }

        new PatnaASCAnalyzer().run(configFile, outDir);
    }

    public void run (String configFile, String outDir) {
        Config config = ConfigUtils.loadConfig(configFile);
        config.controler().setLastIteration(300);
        for (double ascBike : ascTrials) {
            for (double ascMotorbike : ascTrials) {
                config.controler().setOutputDirectory(outDir+"/bikeASC"+ascBike+"_motorbikeASC"+ascMotorbike+"/");
                config.planCalcScore().getModes().get("bike").setConstant(ascBike);
                config.planCalcScore().getModes().get("motorbike").setConstant(ascMotorbike);
                new PatnaNetworkModesPlansRelaxor().run(config);
            }
        }
    }
}
