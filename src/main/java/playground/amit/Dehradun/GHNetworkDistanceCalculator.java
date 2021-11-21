package playground.amit.Dehradun;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import playground.amit.Dehradun.metro2021scenario.MetroStopsQuadTree;

import java.io.IOException;
import java.net.BindException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

/**
 * @author Amit, created on 20-09-2021
 */

public class GHNetworkDistanceCalculator {

    private final MetroStopsQuadTree metroStopsQuadTree;
    private static final double walk_speed = 5.;
    private static final double walk_beeline_distance_factor = 1.1;

    public GHNetworkDistanceCalculator(){
        metroStopsQuadTree= new MetroStopsQuadTree();
    }

    public static void main(String[] args) {
        System.out.println( GHNetworkDistanceCalculator.getDistanceInKmTimeInHr(new Coord(28.555764, 77.09652), new Coord(28.57,77.32), "car", "fastest"));
    }

    public Tuple<Double, Double> getTripDistanceInKmTimeInHrFromAvgSpeeds(Coord origin, Coord destination, String travelMode){
        //this is coming from a Routing Engine like Graphhopper
        double dist = 0;
        if ( travelMode.equals("bus") || travelMode.equals("IPT") ) {
            dist = GHNetworkDistanceCalculator.getDistanceInKmTimeInHr(origin, destination, travelMode, null).getFirst();
            return new Tuple<>(dist, dist/DehradunUtils.getSpeedKPHFromReport(travelMode));
        } else if (travelMode.equals("metro")) {
            return getMetroDistTime(origin, destination);
        } else {
            dist = GHNetworkDistanceCalculator.getDistanceInKmTimeInHr(origin, destination, travelMode, "fastest").getFirst();
            return new Tuple<>(dist, dist/DehradunUtils.getSpeedKPHFromReport(travelMode));
        }
    }

    /**
     *
     * The metro travel time would be from avg speeds only.
     */
    public Tuple<Double, Double> getTripDistanceInKmTimeInHrFromGHRouter(Coord origin, Coord destination, String travelMode){
        //this is coming from a Routing Engine like Graphhopper
        if ( travelMode.equals("bus") || travelMode.equals("IPT") ) {
            return GHNetworkDistanceCalculator.getDistanceInKmTimeInHr(origin, destination, travelMode, null);
        } else if (travelMode.equals("metro")) {
            return getMetroDistTime(origin, destination);
        } else {
            return GHNetworkDistanceCalculator.getDistanceInKmTimeInHr(origin, destination, travelMode, "fastest");
        }
    }

    private Tuple<Double, Double> getMetroDistTime(Coord origin, Coord destination) {
        Node [] nearestMetroStops_origin = this.metroStopsQuadTree.getNearestNodeAndNodeInOppositeDirection(origin);
        Node [] nearestMetroStops_destination = this.metroStopsQuadTree.getNearestNodeAndNodeInOppositeDirection(destination);
        nearestMetroStops_destination = MetroStopsQuadTree.arrangeMetroStopsAsPerOriginLines(nearestMetroStops_origin, nearestMetroStops_destination);

        double shortestDist = Double.POSITIVE_INFINITY;
        Node nearest_origin = null;
        Node nearest_destination = null;
        for (int i = 0; i<nearestMetroStops_origin.length; i++) {
            double dist =  GHNetworkDistanceCalculator.getDistanceInKmTimeInHr(nearestMetroStops_origin[i].getCoord(), nearestMetroStops_destination[i].getCoord(),"metro",null).getFirst();
            if (dist < shortestDist) {
                nearest_origin = nearestMetroStops_origin[i];
                nearest_destination = nearestMetroStops_destination[i];
                shortestDist = dist;
            }
        }

        // distance is metro distance but
        double accessDistance = getWalkTravelDistance(origin, nearest_origin.getCoord());
        double egressDistance = getWalkTravelDistance(nearest_destination.getCoord(), destination);
        return new Tuple<>(shortestDist+accessDistance+egressDistance,
                shortestDist/DehradunUtils.getSpeedKPHFromReport("metro")+(accessDistance+egressDistance)/walk_speed);
    }

    private double getWalkTravelDistance(Coord origin, Coord destination){
        return walk_beeline_distance_factor * NetworkUtils.getEuclideanDistance(
                DehradunUtils.transformation.transform(origin),
                DehradunUtils.transformation.transform(destination))/(1000. );
    }

    public static Tuple<Double, Double> getDistanceInKmTimeInHr(Coord origin, Coord destination, String mode, String routeType) {
        if (origin == null ) throw new RuntimeException("Origin is null. Aborting...");

        if (destination == null ) throw new RuntimeException("Destination is null. Aborting...");


        if (origin==destination) {
//            Logger.getLogger(GHNetworkDistanceCalculator.class).warn("Identical origin and destination.");
            return new Tuple<>(0.0, 0.0);
        }

        if (mode == null ) {
            Logger.getLogger(GHNetworkDistanceCalculator.class).warn("Transport mode is null. Setting to "+"car");
            mode = "car";
        } else if (mode.equals("motorbike")) mode = "motorcycle";
        else if (mode.equals("metro")) mode = "metro";
        else if(mode.equals("IPT")) mode ="ipt";

        String base_url = "http://localhost:9098/routing?";
        String json_suffix = "&mediaType=json";
        String url_string = base_url+"StartLoc="+origin.getX()+"%2C"+origin.getY()+
                "&EndLoc="+destination.getX()+"%2C"+destination.getY();

        if (routeType!=null) url_string = url_string+"&RouteType="+routeType+"&Vehicle="+mode+json_suffix;
        else url_string = url_string+"&Vehicle="+mode+json_suffix;

//        System.out.println("URL is "+url_string);
        try {
            return getDoubleDoubleTuple(url_string);
        } catch (BindException e) {
            System.out.println("Caught "+e+"; re-running " + url_string);
            try {
                Thread.sleep(40000);
                return getDoubleDoubleTuple(url_string);
            } catch (IOException | ParseException | InterruptedException ex) {
                throw new RuntimeException("URL is not connected. Reason "+e);
            }
        } catch (IOException | ParseException e) {
            System.out.println("Origin: "+origin);
            System.out.println("Destination: "+destination);
            System.out.println(url_string);
            throw new RuntimeException("URL is not connected. Reason "+e);
        }
    }

    private static Tuple<Double, Double> getDoubleDoubleTuple(String url_string) throws IOException, ParseException {
        URL url = new URL(url_string);
        URLConnection request = url.openConnection();
        request.connect();

        Scanner sc = new Scanner(url.openStream());
        StringBuilder inline = new StringBuilder();
        while(sc.hasNext())
        {
            inline.append(sc.nextLine());
        }
        sc.close();

        JSONParser parse = new JSONParser();
        JSONObject json =  (JSONObject)parse.parse(inline.toString());
        JSONObject summary = (JSONObject) json.get("summary");
        return new Tuple<>((Double) summary.get("distance")/1000., (Double) summary.get("time")/3600.);
    }
}
