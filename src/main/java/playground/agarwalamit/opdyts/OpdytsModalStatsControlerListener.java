/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.opdyts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.analysis.modalShare.FilteredModalShareEventHandler;
import playground.agarwalamit.analysis.tripDistance.LegModeBeelineDistanceDistributionHandler;
import playground.agarwalamit.opdyts.equil.EquilMixedTrafficObjectiveFunctionPenalty;

/**
 * Created by amit on 20/09/16.
 */

public class OpdytsModalStatsControlerListener implements StartupListener, IterationStartsListener, IterationEndsListener, ShutdownListener {

    @Inject
    private EventsManager events;

    private final FilteredModalShareEventHandler modalShareEventHandler = new FilteredModalShareEventHandler();
    private final DistanceDistribution referenceStudyDistri ;
    private LegModeBeelineDistanceDistributionHandler beelineDistanceDistributionHandler;

    private final SortedMap<String, SortedMap<Double, Integer>> initialMode2DistanceClass2LegCount = new TreeMap<>();

    private BufferedWriter writer;
    private final List<String> mode2consider;

    /*
     * after every 10/20/50 iterations, distance distribution will be written out.
     */
    private final int distriEveryItr = 50;

    public OpdytsModalStatsControlerListener(final List<String> modes2consider, final DistanceDistribution referenceStudyDistri) {
        this.mode2consider = modes2consider;
        this.referenceStudyDistri = referenceStudyDistri;
    }

    public OpdytsModalStatsControlerListener() {
        this(Arrays.asList(TransportMode.car, TransportMode.pt), null);
    }

    @Override
    public void notifyStartup(StartupEvent event) {
        String outFile = event.getServices().getConfig().controler().getOutputDirectory() + "/opdyts_modalStats.txt";
        writer = IOUtils.getBufferedWriter(outFile);
        try {
            StringBuilder stringBuilder = new StringBuilder("iterationNr" + "\t");
            mode2consider.stream().forEach(mode -> stringBuilder.append("legs_"+mode+ "\t"));
            mode2consider.stream().forEach(mode -> stringBuilder.append("asc_"+mode+ "\t"));
            mode2consider.stream().forEach(mode -> stringBuilder.append("util_trav_"+mode+ "\t"));
            mode2consider.stream().forEach(mode -> stringBuilder.append("util_dist_"+mode+ "\t"));
            mode2consider.stream().forEach(mode -> stringBuilder.append("money_dist_rate_"+mode+ "\t"));
            stringBuilder.append("objectiveFunctionValue"+"\t");
            stringBuilder.append("penaltyForObjectiveFunction"+"\t");
            stringBuilder.append("totalObjectiveFunctionValue");
            writer.write(stringBuilder.toString());
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("File not found.");
        }

        this.events.addHandler(modalShareEventHandler);

        // initializing it here so that scenario can be accessed.
        List<Double> dists = Arrays.stream(this.referenceStudyDistri.getDistClasses()).boxed().collect(Collectors.toList());
        this.beelineDistanceDistributionHandler = new LegModeBeelineDistanceDistributionHandler(dists, event.getServices().getScenario().getNetwork());
        this.events.addHandler(this.beelineDistanceDistributionHandler);
    }

    @Override
    public void notifyIterationStarts(final IterationStartsEvent event) {
        this.beelineDistanceDistributionHandler.reset(event.getIteration());
        this.modalShareEventHandler.reset(event.getIteration());
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {

       final int iteration = event.getIteration();
       final Config config = event.getServices().getConfig();

        if (iteration == config.controler().getFirstIteration()) {
            this.initialMode2DistanceClass2LegCount.putAll(this.beelineDistanceDistributionHandler.getMode2DistanceClass2LegCounts());
        } else {
            // nothing to do
        }

        SortedMap<String, Integer> mode2Legs = modalShareEventHandler.getMode2numberOflegs();

        // evaluate objective function value
        ObjectiveFunctionEvaluator objectiveFunctionEvaluator = new ObjectiveFunctionEvaluator();
        Map<String, double[]> realCounts = this.referenceStudyDistri.getMode2DistanceBasedLegs();
        Map<String, double[]> simCounts = new TreeMap<>();

        // initialize simcounts array for each mode
        realCounts.entrySet().forEach(entry -> simCounts.put(entry.getKey(), new double[entry.getValue().length]));

        SortedMap<String, SortedMap<Double, Integer>> simCountsHandler = this.beelineDistanceDistributionHandler.getMode2DistanceClass2LegCounts();
        for (Map.Entry<String, SortedMap<Double, Integer>> e : simCountsHandler.entrySet()) {
            if (!realCounts.containsKey(e.getKey())) continue;
            double[] counts = new double[realCounts.get(e.getKey()).length];
            int index = 0;
            for (Integer count : simCountsHandler.get(e.getKey()).values()) {
                counts[index++] = count;
            }
            simCounts.put(e.getKey(), counts);
        }

        double objectiveFunctionValue = objectiveFunctionEvaluator.getObjectiveFunctionValue(realCounts,simCounts);
        double penalty = 0.;
        switch (this.referenceStudyDistri.getOpdytsScenario()) {
            case EQUIL:
            case PATNA_1Pct:
            case PATNA_10Pct:
                break;
            case EQUIL_MIXEDTRAFFIC:
                double ascBicycle = config.planCalcScore().getModes().get("bicycle").getConstant();
                double bicycleShare = objectiveFunctionEvaluator.getModeToShare().get("bicycle");
                penalty = EquilMixedTrafficObjectiveFunctionPenalty.getPenalty(bicycleShare, ascBicycle);
        }

        try {
            // write modalParams
            Map<String, PlanCalcScoreConfigGroup.ModeParams> mode2Params = config.planCalcScore().getModes();

            StringBuilder stringBuilder = new StringBuilder(iteration + "\t");
            mode2consider.stream().forEach(mode-> stringBuilder.append( mode2Legs.containsKey(mode) ? mode2Legs.get(mode) : String.valueOf(0) +"\t"));
            mode2consider.stream().forEach(mode -> stringBuilder.append(mode2Params.get(mode).getConstant()+"\t"));
            mode2consider.stream().forEach(mode -> stringBuilder.append(mode2Params.get(mode).getMarginalUtilityOfTraveling()+"\t"));
            mode2consider.stream().forEach(mode -> stringBuilder.append(mode2Params.get(mode).getMarginalUtilityOfDistance()+"\t"));
            mode2consider.stream().forEach(mode -> stringBuilder.append(mode2Params.get(mode).getMonetaryDistanceRate()+"\t"));
            stringBuilder.append(objectiveFunctionValue+"\t");
            stringBuilder.append(penalty+"\t");
            stringBuilder.append(String.valueOf(objectiveFunctionValue+penalty));

            writer.write(stringBuilder.toString());
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("File not found.");
        }

        if( iteration % distriEveryItr == 0 || iteration == config.controler().getLastIteration() ) {
            // dist-distribution file
            String distriDir = config.controler().getOutputDirectory() + "/distanceDistri/";
            new File(distriDir).mkdir();
            String outFile = distriDir + "/" + iteration + ".distanceDistri.txt";
            writeDistanceDistribution(outFile);
        }
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("File not found.");
        }
    }

    private void writeDistanceDistribution(String outputFile) {
        final BufferedWriter writer2 = IOUtils.getBufferedWriter(outputFile);
        try {
            writer2.write( "distBins" + "\t" );
            for(Double d : referenceStudyDistri.getDistClasses()) {
                writer2.write(d + "\t");
            }
            writer2.newLine();

            // from initial plans
            {
                writer2.write("===== begin writing distribution from initial plans ===== ");
                writer2.newLine();

                for (String mode : this.initialMode2DistanceClass2LegCount.keySet()) {
                    writer2.write(mode + "\t");
                    for (Double d : this.initialMode2DistanceClass2LegCount.get(mode).keySet()) {
                        writer2.write(this.initialMode2DistanceClass2LegCount.get(mode).get(d) + "\t");
                    }
                    writer2.newLine();
                }

                writer2.write("===== end writing distribution from initial plans ===== ");
                writer2.newLine();
            }

            // from objective function
            {
                writer2.write("===== begin writing distribution from objective function ===== ");
                writer2.newLine();

                Map<String, double []> mode2counts = this.referenceStudyDistri.getMode2DistanceBasedLegs();
                for (String mode : mode2counts.keySet()) {
                    writer2.write(mode + "\t");
                    for (Double d : mode2counts.get(mode)) {
                        writer2.write(d + "\t");
                    }
                    writer2.newLine();
                }

                writer2.write("===== end writing distribution from objective function ===== ");
                writer2.newLine();
            }

            // from simulation
            {
                writer2.write("===== begin writing distribution from simulation ===== ");
                writer2.newLine();

                SortedMap<String, SortedMap<Double, Integer>> mode2dist2counts = this.beelineDistanceDistributionHandler.getMode2DistanceClass2LegCounts();
                for (String mode : mode2dist2counts.keySet()) {
                    writer2.write(mode + "\t");
                    for (Double d : mode2dist2counts.get(mode).keySet()) {
                        writer2.write(mode2dist2counts.get(mode).get(d) + "\t");
                    }
                    writer2.newLine();
                }

                writer2.write("===== end writing distribution from simulation ===== ");
                writer2.newLine();
            }
            writer2.close();
        } catch (IOException e) {
            throw new RuntimeException("Data is not written. Reason "+ e);
        }
    }
}
