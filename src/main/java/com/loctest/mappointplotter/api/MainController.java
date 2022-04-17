package com.loctest.mappointplotter.api;

import com.google.gson.Gson;
import com.google.maps.errors.ApiException;
import com.google.maps.model.LatLng;
import com.loctest.mappointplotter.Service.MapsPointService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@Slf4j
public class MainController {

    @Autowired
    MapsPointService mps;


    //Accepts origin: lat,lon and destination: lat,lng as input
    //Example: http://localhost:8080/coordinates?origin=19.113075179311267,72.9057133812882&destination=19.13105946401141,72.9285652039916
    //Plotted Example: https://www.mapcustomizer.com/map/linesandcurves
    @GetMapping(value = "coordinates")
    public String getCoordinates(@RequestParam String origin, @RequestParam String destination) throws IOException, InterruptedException, ApiException {
        log.info("RECEIVED: request for getting coordinates between origin:{} destination:{}", origin, destination);

        List<LatLng> coordinatesList = mps.getCoordinates(origin, destination);

//      Can be commented to not print to console - intended for easy copying of coordinates onto the mapcustomizer tool
        printCoordinatesToConsole(coordinatesList);

        return new Gson().toJson(coordinatesList);
    }

    @ExceptionHandler
    public ResponseEntity<String> responseHandler()
    {
        ResponseEntity<String> responseEntity = new ResponseEntity<String>("Error Occurred.",
                HttpStatus.INTERNAL_SERVER_ERROR);
        return  responseEntity;
    }


    void printCoordinatesToConsole(List<LatLng> coordinatesList){
        String z="";
        for(int i=0; i<coordinatesList.size();i++){
            z=z+coordinatesList.get(i).toString()+"\n";
        }
        System.out.println(z);
    }

}
