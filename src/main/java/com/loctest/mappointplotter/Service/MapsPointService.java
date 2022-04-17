package com.loctest.mappointplotter.Service;

import com.google.gson.Gson;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.*;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;


@Slf4j
@Service
public class MapsPointService {

    @Autowired
    GeoApiContext context;

    @Getter
    @Setter
    List<LatLng> coordinateList;

    public static final int INTER_POINT_DISTANCE = 50;

    public List<LatLng> getCoordinates(String origin, String destination) throws IOException, InterruptedException, ApiException {

        DirectionsApiRequest req  = DirectionsApi.getDirections(context, origin, destination);
        List<LatLng> coordinateList = onResult(req.await());

        return coordinateList;
    }



    public List<LatLng> onResult(DirectionsResult result) {

        //select least distance route
        DirectionsStep[] steps = getStepsFromShortestRoute(result.routes);
        return getCoordinatesOfPathFromSteps(steps);
    }

    public static DirectionsStep[] getStepsFromShortestRoute(DirectionsRoute[] routes)
    {

        DirectionsRoute minLengthRoute = routes[0];
        long minDistance = getDistanceOfRoute(routes[0]);

        for(DirectionsRoute route: routes){
            long distance = getDistanceOfRoute(route);
            if(distance < minDistance)
                minLengthRoute=route;
        }

        DirectionsLeg[] legs = minLengthRoute.legs;
        DirectionsStep[] steps = legs[0].steps;

        return steps;
    }


    public static long getDistanceOfRoute(DirectionsRoute route){
        long distance=0;
        for(DirectionsLeg leg: route.legs){
            distance+=leg.distance.inMeters;
        }
        return distance;
    }

    public static List<LatLng> getCoordinatesOfPathFromSteps(DirectionsStep []steps)
    {
        List<LatLng> coordinates = new ArrayList<>();

        for (DirectionsStep step:steps) {

            coordinates.add( step.startLocation);
            if(step.distance.inMeters > INTER_POINT_DISTANCE) {

                List<LatLng> polyLinePathCoordinates = step.polyline.decodePath();
                Map<LatLng, Distance> cumulativeDistance = new LinkedHashMap<>();
                populateCumulativeDistance(polyLinePathCoordinates, cumulativeDistance);

                //convert to Quartile (out of 100)
                for (LatLng key : cumulativeDistance.keySet()){
                    cumulativeDistance.get(key).inMeters =  (int)
                            ((cumulativeDistance.get(key).inMeters*100) / step.distance.inMeters);
                }
                int totalCoordinatesNeededInStep = (int) step.distance.inMeters/INTER_POINT_DISTANCE;
                float quartileIntervals =  ((float)100)/ ((float)totalCoordinatesNeededInStep);

                log.info("\ttotal distance = {}, totalCoordinatesNeededInStep={}, polylineLen={}",
                        step.distance.inMeters, totalCoordinatesNeededInStep, polyLinePathCoordinates.size());

                for(int i=1; i < totalCoordinatesNeededInStep; i++) {

                    int findLatLngForCD = (int) ( i*quartileIntervals);
                    LatLng CD=polyLinePathCoordinates.get(0);
                    for (LatLng key : cumulativeDistance.keySet()){
                        if( cumulativeDistance.get(key).inMeters <= findLatLngForCD )
                            CD = key;
                    }
                    log.debug("found LatLng : {} which is at quartile={}", CD.toString(), findLatLngForCD);
                    coordinates.add(CD);
                }

            }
        }

        DirectionsStep lastStep = steps[ steps.length - 1 ];
        coordinates.add(  new LatLng(lastStep.endLocation.lat, lastStep.endLocation.lng ));

        return coordinates;
    }

    private static void populateCumulativeDistance(List<LatLng> polyLinePathCoordinates, Map<LatLng, Distance> cumulativeDistance) {
        Distance d = new Distance();
        d.inMeters=0;
        cumulativeDistance.put( polyLinePathCoordinates.get(0), d );

        for(int i = 1; i< polyLinePathCoordinates.size(); i++){

            Distance dist = new Distance();
            LatLng oldPoint = polyLinePathCoordinates.get(i-1);
            LatLng newPoint = polyLinePathCoordinates.get(i);
            double distanceBetweenPoints = getDistanceBetweenCoordinates(newPoint.lat, oldPoint.lat,
                                                                         newPoint.lng, oldPoint.lng,
                                                                        0,0);
            dist.inMeters= (long) (cumulativeDistance.get(oldPoint).inMeters +  distanceBetweenPoints);
            cumulativeDistance.put(polyLinePathCoordinates.get(i), dist);
        }
    }


    public static double getDistanceBetweenCoordinates(double lat1, double lat2, double lon1,
                                                       double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }

}