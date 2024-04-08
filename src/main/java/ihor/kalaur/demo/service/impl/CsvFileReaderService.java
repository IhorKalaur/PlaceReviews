package ihor.kalaur.demo.service.impl;

import ihor.kalaur.demo.dto.PlaceSearchRequestDto;
import ihor.kalaur.demo.exceptions.ReadDataFromFileException;
import ihor.kalaur.demo.service.FileReaderService;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

@Service
public class CsvFileReaderService implements FileReaderService<PlaceSearchRequestDto> {
    private static final Double RADIUS_OF_CIRCLE = 0.0;
    private static final Integer MAX_RESULT_COUNT = 1;
    private static final Integer NUMBER_OF_PARAMETERS_FOR_COORDINATE_SYSTEM = 2;
    private static final Integer INDEX_OF_LATITUDE = 0;
    private static final Integer INDEX_OF_LONGITUDE = 1;
    private static final String READ_DATA_FROM_FILE_EXCEPTION_MESSAGE = "Can't read data from file: %s";
    private static final String ILLEGAL_NUMBER_OF_PARAMETERS_FOR_COORDINATE_SYSTEM_EXCEPTION_MESSAGE
            = "Coordinates string must contain exactly one comma separating latitude and longitude.";
    private static final String INVALID_FORMAT_FOR_LATITUDE_OR_LONGITUDE_EXCEPTION_MESSAGE
            = "Invalid format for latitude or longitude";

    private static final String COORDINATES_SPLIT_REGEX = ",";
    private static final String NAME_HEADER = "Name";
    private static final String COORDINATES_HEADER = "Coordinates";

    @Override
    public List<PlaceSearchRequestDto> readFromFile(String pathToFile) {
        List<PlaceSearchRequestDto> restaurants = new ArrayList<>();
        try (
                Reader reader = new FileReader(pathToFile);
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
            throw new ReadDataFromFileException(String.format(READ_DATA_FROM_FILE_EXCEPTION_MESSAGE, pathToFile), e);
        }
        return restaurants;
    }

    private PlaceSearchRequestDto createPlaceSearchRequest(String textQuery, String coordinates) {
        String[] latLong = coordinates.split(COORDINATES_SPLIT_REGEX);
        if (latLong.length != NUMBER_OF_PARAMETERS_FOR_COORDINATE_SYSTEM) {
            throw new IllegalArgumentException(ILLEGAL_NUMBER_OF_PARAMETERS_FOR_COORDINATE_SYSTEM_EXCEPTION_MESSAGE);
        }

        try {
            double latitude = Double.parseDouble(latLong[INDEX_OF_LATITUDE].trim());
            double longitude = Double.parseDouble(latLong[INDEX_OF_LONGITUDE].trim());

            PlaceSearchRequestDto.Center center = new PlaceSearchRequestDto.Center(latitude, longitude);
            PlaceSearchRequestDto.Circle circle = new PlaceSearchRequestDto.Circle(center, RADIUS_OF_CIRCLE);
            PlaceSearchRequestDto.LocationBias locationBias = new PlaceSearchRequestDto.LocationBias(circle);

            return new PlaceSearchRequestDto(textQuery, MAX_RESULT_COUNT, locationBias);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(INVALID_FORMAT_FOR_LATITUDE_OR_LONGITUDE_EXCEPTION_MESSAGE);
        }
    }
}

