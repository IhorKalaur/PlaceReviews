package ihor.kalaur.demo.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import ihor.kalaur.demo.dto.PlaceSearchRequestDto;
import ihor.kalaur.demo.dto.PlaceSearchResponseDto;
import ihor.kalaur.demo.exceptions.FetchDataFromGoogleApiException;
import ihor.kalaur.demo.service.GooglePlaceApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
@RequiredArgsConstructor
public class GooglePlaceApiServiceImpl implements GooglePlaceApiService {
    private static final String GOOGLE_PLACES_API_URL = "https://places.googleapis.com/v1/places:searchText";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String API_KEY_HEADER = "X-Goog-Api-Key";
    private static final String FIELD_MASK_HEADER = "X-Goog-FieldMask";
    private static final String FIELD_MASK_VALUE = "places.displayName.text,places.reviews";
    private static final String APPLICATION_JSON = "application/json";
    private static final String ERROR_MESSAGE = "Can't get data about restaurant: ";

    private final ObjectMapper objectMapper;

    @Value("${ihor.kalaur.demo.google.place.api.key}")
    private String apiKey;

    public PlaceSearchResponseDto fetchRestaurantReviews(PlaceSearchRequestDto searchRequest) {
        try {
            HttpRequest request = buildHttpRequest(searchRequest);
            HttpResponse<String> response = executeHttpRequest(request);

            System.out.println("Status Code: " + response.statusCode());
            System.out.println("Response Body: " + response.body());

            return objectMapper.readValue(response.body(), PlaceSearchResponseDto.class);
        } catch (Exception e) {
            throw new FetchDataFromGoogleApiException(ERROR_MESSAGE + searchRequest.textQuery(), e);
        }
    }

    private HttpRequest buildHttpRequest(PlaceSearchRequestDto searchRequest) throws Exception {
        String requestBody = objectMapper.writeValueAsString(searchRequest);

        return HttpRequest.newBuilder()
                .uri(URI.create(GOOGLE_PLACES_API_URL))
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                .header(API_KEY_HEADER, apiKey)
                .header(FIELD_MASK_HEADER, FIELD_MASK_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    private HttpResponse<String> executeHttpRequest(HttpRequest request) throws Exception {
        HttpClient httpClient = HttpClient.newHttpClient();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
