package ihor.kalaur.demo.service.impl;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.places.v1.MapsPlaces;
import com.google.api.services.places.v1.model.GoogleMapsPlacesV1AuthorAttribution;
import com.google.api.services.places.v1.model.GoogleMapsPlacesV1Circle;
import com.google.api.services.places.v1.model.GoogleMapsPlacesV1Place;
import com.google.api.services.places.v1.model.GoogleMapsPlacesV1Review;
import com.google.api.services.places.v1.model.GoogleMapsPlacesV1SearchTextRequest;
import com.google.api.services.places.v1.model.GoogleMapsPlacesV1SearchTextRequestLocationBias;
import com.google.api.services.places.v1.model.GoogleMapsPlacesV1SearchTextResponse;
import com.google.api.services.places.v1.model.GoogleTypeLatLng;
import com.google.api.services.places.v1.model.GoogleTypeLocalizedText;
import ihor.kalaur.demo.dto.PlaceSearchRequestDto;
import ihor.kalaur.demo.dto.PlaceSearchResponseDto;
import ihor.kalaur.demo.dto.PlaceSearchResponseDto.Place;
import ihor.kalaur.demo.dto.PlaceSearchResponseDto.Place.DisplayName;
import ihor.kalaur.demo.dto.PlaceSearchResponseDto.Place.Review;
import ihor.kalaur.demo.dto.PlaceSearchResponseDto.Place.Review.AuthorAttribution;
import ihor.kalaur.demo.dto.PlaceSearchResponseDto.Place.Review.ReviewText;
import ihor.kalaur.demo.exceptions.FetchDataFromGoogleApiException;
import ihor.kalaur.demo.service.GooglePlaceApiService;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GooglePlaceApiServiceImpl implements GooglePlaceApiService {
    private static final String DEFAULT_EMPTY_VALUE = "";
    private static final Integer DEFAULT_RATING = 0;
    private static final String RESPONSE_BODY_LOG = "Response Body: {}";
    private static final String RESPONSE_ERROR_LOG = "Failed to fetch restaurant reviews";
    private static final String FETCH_DATA_FROM_GOOGLE_API_EXCEPTION_MESSAGE = "Can't get data about restaurant: ";
    private static final String APPLICATION_NAME = "GetPlacesReviewsApp";
    private static final String API_KEY_HEADER = "X-Goog-Api-Key";
    private static final String RETURNED_TYPES_FIELD_MASK_HEADER = "X-Goog-FieldMask";
    private static final String FIELD_MASK_VALUE = "places.displayName.text,places.reviews";

    @Value("${ihor.kalaur.demo.google.place.api.key}")
    private String apiKey;
    private MapsPlaces mapsPlacesService;

    @PostConstruct
    public void init() {
        mapsPlacesService = new MapsPlaces.Builder(new NetHttpTransport(), new GsonFactory(), null)
                .setApplicationName(APPLICATION_NAME)
                .setGoogleClientRequestInitializer(request -> request.getRequestHeaders()
                        .set(API_KEY_HEADER, apiKey)
                        .set(RETURNED_TYPES_FIELD_MASK_HEADER, FIELD_MASK_VALUE))
                .build();
    }

    @Override
    public PlaceSearchResponseDto fetchRestaurantReviews(PlaceSearchRequestDto searchRequestDto) {
        try {
            GoogleMapsPlacesV1SearchTextRequest requestModel = convertToGoogleSearchRequest(searchRequestDto);
            GoogleMapsPlacesV1SearchTextResponse response = mapsPlacesService.places().searchText(requestModel).execute();
            log.info(RESPONSE_BODY_LOG, response);
            return convertResponseToDto(response);
        } catch (Exception e) {
            log.error(RESPONSE_ERROR_LOG, e);
            throw new FetchDataFromGoogleApiException(FETCH_DATA_FROM_GOOGLE_API_EXCEPTION_MESSAGE + searchRequestDto, e);
        }
    }

    public GoogleMapsPlacesV1SearchTextRequest convertToGoogleSearchRequest(PlaceSearchRequestDto requestDto) {
        GoogleMapsPlacesV1SearchTextRequest googleRequest = new GoogleMapsPlacesV1SearchTextRequest();
        googleRequest.setTextQuery(requestDto.textQuery());

        if (requestDto.locationBias() != null && requestDto.locationBias().circle() != null) {
            GoogleMapsPlacesV1SearchTextRequestLocationBias locationBias = createLocationBias(requestDto);
            googleRequest.setLocationBias(locationBias);
        }

        googleRequest.setMaxResultCount(requestDto.maxResultCount());

        return googleRequest;
    }

    private GoogleMapsPlacesV1SearchTextRequestLocationBias createLocationBias(PlaceSearchRequestDto requestDto) {
        GoogleMapsPlacesV1Circle circle = new GoogleMapsPlacesV1Circle();
        GoogleTypeLatLng center = new GoogleTypeLatLng();

        center.setLatitude(requestDto.locationBias().circle().center().latitude());
        center.setLongitude(requestDto.locationBias().circle().center().longitude());

        circle.setCenter(center);
        circle.setRadius(requestDto.locationBias().circle().radius());

        GoogleMapsPlacesV1SearchTextRequestLocationBias locationBias = new GoogleMapsPlacesV1SearchTextRequestLocationBias();
        locationBias.setCircle(circle);
        return locationBias;
    }

    private PlaceSearchResponseDto convertResponseToDto(GoogleMapsPlacesV1SearchTextResponse apiResponse) {
        List<Place> places = apiResponse.getPlaces().stream()
                .map(this::convertApiPlaceToDto)
                .collect(Collectors.toList());
        return new PlaceSearchResponseDto(places);
    }

    private Place convertApiPlaceToDto(
            GoogleMapsPlacesV1Place apiPlace) {
        DisplayName displayName = new DisplayName(
                Optional.ofNullable(apiPlace.getDisplayName()).map(GoogleTypeLocalizedText::getText).orElse(DEFAULT_EMPTY_VALUE));

        List<Review> reviews = Optional.ofNullable(apiPlace.getReviews()).orElse(Collections.emptyList()).stream()
                .map(this::convertApiReviewToDto)
                .collect(Collectors.toList());

        return new Place(displayName, reviews);
    }

    private Review convertApiReviewToDto(GoogleMapsPlacesV1Review apiReview) {
        ReviewText text =
                Optional.ofNullable(apiReview.getText())
                        .map(t -> new ReviewText(t.getText(), t.getLanguageCode()))
                        .orElse(new ReviewText(DEFAULT_EMPTY_VALUE, DEFAULT_EMPTY_VALUE));

        ReviewText originalText =
                Optional.ofNullable(apiReview.getOriginalText())
                        .map(ot -> new ReviewText(ot.getText(), ot.getLanguageCode()))
                        .orElse(new ReviewText(DEFAULT_EMPTY_VALUE, DEFAULT_EMPTY_VALUE));

        AuthorAttribution authorAttribution = new AuthorAttribution(
                Optional.ofNullable(apiReview.getAuthorAttribution())
                                .map(GoogleMapsPlacesV1AuthorAttribution::getDisplayName)
                                .orElse(DEFAULT_EMPTY_VALUE),
                Optional.ofNullable(apiReview.getAuthorAttribution())
                        .map(GoogleMapsPlacesV1AuthorAttribution::getUri).orElse(DEFAULT_EMPTY_VALUE),
                Optional.ofNullable(apiReview.getAuthorAttribution())
                        .map(GoogleMapsPlacesV1AuthorAttribution::getPhotoUri).orElse(DEFAULT_EMPTY_VALUE));

        return new Review(
                Optional.ofNullable(apiReview.getName()).orElse(DEFAULT_EMPTY_VALUE),
                Optional.ofNullable(
                        apiReview.getRelativePublishTimeDescription())
                        .orElse(DEFAULT_EMPTY_VALUE),
                Optional.ofNullable(apiReview.getRating()).map(Double::intValue).orElse(DEFAULT_RATING),
                text,
                originalText,
                authorAttribution,
                Optional.ofNullable(apiReview.getPublishTime()).orElse(DEFAULT_EMPTY_VALUE));
    }
}
