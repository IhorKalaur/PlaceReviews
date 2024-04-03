package ihor.kalaur.demo;

import ihor.kalaur.demo.dto.PlaceSearchRequestDto;
import ihor.kalaur.demo.dto.PlaceSearchResponseDto;
import ihor.kalaur.demo.service.FileWriterService;
import ihor.kalaur.demo.service.GooglePlaceApiService;
import ihor.kalaur.demo.service.impl.CsvFileReaderService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataProcessor {

    private final CsvFileReaderService csvFileReaderService;

    private final GooglePlaceApiService googlePlaceApiService;

    private final FileWriterService<PlaceSearchResponseDto> fileWriterService;

    @PostConstruct
    public void process() {
        final Path pathFromFile = FileSystems.getDefault().getPath("src/main/resources/restaurant.csv");
        final Path pathToFile = FileSystems.getDefault().getPath("src/main/resources/reviews.csv");

        List<PlaceSearchRequestDto> placeSearchRequestDtos = csvFileReaderService.readFromFile(pathFromFile.toString());

        placeSearchRequestDtos.forEach(placeSearchRequest -> {
            PlaceSearchResponseDto placeSearchResponseDto = googlePlaceApiService.fetchRestaurantReviews(placeSearchRequest);
            fileWriterService.writeDataToFile(placeSearchResponseDto, pathToFile.toString());
        });
    }

}
