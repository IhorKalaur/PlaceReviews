package ihor.kalaur.demo.service.impl;

import ihor.kalaur.demo.dto.PlaceSearchResponseDto;
import ihor.kalaur.demo.exceptions.WriteDataToFileException;
import ihor.kalaur.demo.service.FileWriterService;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.apache.commons.text.StringEscapeUtils.escapeCsv;

@Service
public class CsvFileWriterService implements FileWriterService<PlaceSearchResponseDto> {

    private static final String HEADERS_FOR_OUTPUT_FILE = "Place Name,Review Name,Relative Publish Time Description,Rating,Text,Original Text,Author Display Name,Author URI,Author Photo URI,Publish Time\n";
    private static final String ERROR_MESSAGE_TEMPLATE = "Can't write reviews about %s to file";
    private static final String COMA_SPLITTER = ",";
    private static final String NEW_LINE = System.lineSeparator();
    private static final String EMPTY_TEXT = "";

    @Override
    public void writeDataToFile(PlaceSearchResponseDto data, String pathToFile) {
        File file = new File(pathToFile);

        try (FileWriter writer = new FileWriter(file, true)) {

            if (file.length() == 0) {
                writer.append(HEADERS_FOR_OUTPUT_FILE);
            }

            for (PlaceSearchResponseDto.Place place : data.places()) {
                if (place.reviews() != null) {
                    for (PlaceSearchResponseDto.Place.Review review : place.reviews()) {
                        PlaceSearchResponseDto.Place.Review.ReviewText text = review.text();
                        PlaceSearchResponseDto.Place.Review.ReviewText originalText = review.originalText();
                        PlaceSearchResponseDto.Place.Review.AuthorAttribution author = review.authorAttribution();

                        writer.append(escapeCsv(place.displayName().text())).append(COMA_SPLITTER);
                        writer.append(escapeCsv(review.name())).append(COMA_SPLITTER);
                        writer.append(escapeCsv(review.relativePublishTimeDescription())).append(COMA_SPLITTER);
                        writer.append(String.valueOf(review.rating())).append(COMA_SPLITTER);
                        writer.append(text != null && text.text() != null ? escapeCsv(text.text()) : EMPTY_TEXT)
                                .append(COMA_SPLITTER);
                        writer.append(originalText != null && originalText.text() != null ? escapeCsv(originalText.text()) : EMPTY_TEXT)
                                .append(COMA_SPLITTER);
                        writer.append(escapeCsv(author.displayName())).append(COMA_SPLITTER);
                        writer.append(escapeCsv(author.uri())).append(COMA_SPLITTER);
                        writer.append(escapeCsv(author.photoUri())).append(COMA_SPLITTER);
                        writer.append(escapeCsv(review.publishTime())).append(NEW_LINE);
                    }
                }
            }

            writer.flush();
        } catch (IOException e) {
            throw new WriteDataToFileException(
                    String.format(ERROR_MESSAGE_TEMPLATE, data.places().get(0).displayName().text()), e);
        }
    }
}
