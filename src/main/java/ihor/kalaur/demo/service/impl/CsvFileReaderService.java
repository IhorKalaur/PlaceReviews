package ihor.kalaur.demo.service.impl;

import ihor.kalaur.demo.dto.PlaceSearchRequestDto;
import ihor.kalaur.demo.exceptions.ReadDataFromFileException;
import ihor.kalaur.demo.service.FileReaderService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvFileReaderService implements FileReaderService<PlaceSearchRequestDto> {
    private static final int RADIUS_OF_CIRCLE = 0;
    private static final String ERROR_MESSAGE_TEMPLATE = "Can't read data from file: %s";
    private static final String COORDINATES_SPLIT_REGEX = ",";
    private static final String NAME_HEADER = "Name";
    private static final String COORDINATES_HEADER = "Coordinates";

    @Override
    public List<PlaceSearchRequestDto> readFromFile(String pathToFile) {
        List<PlaceSearchRequestDto> restaurants = new ArrayList<>();
        try (Reader reader = new FileReader(pathToFile);
             CSVParser parser = CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                String name = record.get(NAME_HEADER);
                String coordinates = record.get(COORDINATES_HEADER);
                PlaceSearchRequestDto restaurant = createPlaceSearchRequest(name, coordinates);
                restaurants.add(restaurant);
            }
        } catch (Exception e) {
            throw new ReadDataFromFileException(String.format(ERROR_MESSAGE_TEMPLATE, pathToFile), e);
        }
        return restaurants;
    }

    private PlaceSearchRequestDto createPlaceSearchRequest(String textQuery, String coordinates) {
        String[] latLong = coordinates.split(COORDINATES_SPLIT_REGEX);
        if (latLong.length != 2) {
            throw new IllegalArgumentException("Coordinates string must contain exactly one comma separating latitude and longitude.");
        }

        try {
            double latitude = Double.parseDouble(latLong[0].trim());
            double longitude = Double.parseDouble(latLong[1].trim());

            PlaceSearchRequestDto.Center center = new PlaceSearchRequestDto.Center(latitude, longitude);
            PlaceSearchRequestDto.Circle circle = new PlaceSearchRequestDto.Circle(center, RADIUS_OF_CIRCLE);
            PlaceSearchRequestDto.LocationBias locationBias = new PlaceSearchRequestDto.LocationBias(circle);

            return new PlaceSearchRequestDto(textQuery, locationBias);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid format for latitude or longitude.");
        }
    }
}

