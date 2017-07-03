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

package playground.agarwalamit.opdyts.plots;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.matsim.core.utils.charts.BarChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ExeRunner;
import playground.agarwalamit.utils.FileUtils;

/**
 * Created by amit on 02.07.17.
 */

public class StateVectorElementsSizePlotter {

    public static void main(String[] args) {
//        String inputFile = FileUtils.RUNS_SVN+"/opdyts/patna/output_allModes/calib_trails/_0/ITERS/it.1/1.stateVector_network modes.txt";
//        String outputFile = FileUtils.RUNS_SVN+"/opdyts/patna/output_allModes/calib_trails/_0/ITERS/it.1/1.stateVector_network modes.png";
//        String identifier = "networkModes";
//        new StateVectorElementsSizePlotter().run(inputFile,outputFile, identifier);


        // plot the size of the state vector elements
        String outDir = FileUtils.RUNS_SVN+"/opdyts/patna/output_allModes/calib_trails/_0/"+"/vectorElementSizePlots/";
        new File(outDir).mkdirs();

        int firstIt = 0;
        int lastIt = 50;
        int plotEveryItr = 5;

        for (int itr = firstIt+1; itr <=lastIt; itr++) {
            if ( (itr == firstIt+1 || itr%plotEveryItr ==0) && new File(FileUtils.RUNS_SVN+"/opdyts/patna/output_allModes/calib_trails/_0/ITERS/it."+itr+"/").exists() ) {
                StateVectorElementsSizePlotter.gnuHistogramPlot(
                        FileUtils.RUNS_SVN+"/opdyts/patna/output_allModes/calib_trails/_0/ITERS/it."+itr+"/"+itr+".stateVector_networkModes.txt",
                        outDir+"/"+itr+".stateVector_networkModes.eps", "networkModes");
                StateVectorElementsSizePlotter.gnuHistogramPlot(
                        FileUtils.RUNS_SVN+"/opdyts/patna/output_allModes/calib_trails/_0/ITERS/it."+itr+"/"+itr+".stateVector_teleportationModes.txt",
                        outDir+"/"+itr+".stateVector_teleportationModes.eps", "teleportationModes");
            }
        }
    }

    public void run (final String inputFile, final String outputFile, final String identifier) {
        List<Double> vectorElements = new ArrayList<>();
        storeData(inputFile, vectorElements);
        plotJfreeChart(outputFile, vectorElements, identifier);
    }

    private void plotJfreeChart(final String outputFile, final List<Double> vectorElements, final String identifier) {
        double ys [] = vectorElements.stream().mapToDouble(Double::doubleValue).toArray();

        BarChart barChart = new BarChart("vector state elements","value", "");
        barChart.addSeries(identifier, ys);
        barChart.saveAsPng(outputFile, 800, 600);
    }

    private void storeData (final String inputFile, final List<Double> vectorElements) {
        BufferedReader reader = IOUtils.getBufferedReader(inputFile);
        try {
            String line = reader.readLine();
            while (line!=null) {
                String parts[] = line.split("\t");
                vectorElements.add(Double.valueOf(parts[0]));
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
    }

    public static void gnuHistogramPlot(final String inputFile, final String outputFile, final String identifier) {
        String cmd = "gnuplot -c histogram.gnu ";
        cmd += " "+inputFile;
        cmd += " "+outputFile;
        cmd += " "+identifier;

        ExeRunner.run(cmd, FileUtils.GNU_SCRIPT_DIR+"/gnulog.log", 99999 ,FileUtils.GNU_SCRIPT_DIR);
    }
}
