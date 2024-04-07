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
    private static final String API_KEY_HEADER = "X-Goog-Api-Key";
    private static final String RETURNED_TYPES_FIELD_MASK_HEADER = "X-Goog-FieldMask";
    private static final String FIELD_MASK_VALUE = "places.displayName.text,places.reviews";

    @Value("${ihor.kalaur.demo.google.place.api.key}")
    private String apiKey;
    private MapsPlaces mapsPlacesService;

    @PostConstruct
    public void init() {
        mapsPlacesService = new MapsPlaces.Builder(new NetHttpTransport(), new GsonFactory(), null)
                .setApplicationName("Your Application Name")
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
            log.info("Response Body: {}", response);
            return convertResponseToDto(response);
        } catch (Exception e) {
            log.error("Failed to fetch restaurant reviews", e);
            throw new FetchDataFromGoogleApiException("Can't get data about restaurant: " + searchRequestDto, e);
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
                Optional.ofNullable(apiPlace.getDisplayName()).map(name -> name.getText()).orElse(""));

        List<Review> reviews = Optional.ofNullable(apiPlace.getReviews()).orElse(Collections.emptyList()).stream()
                .map(this::convertApiReviewToDto)
                .collect(Collectors.toList());

        return new Place(displayName, reviews);
    }

    private Review convertApiReviewToDto(GoogleMapsPlacesV1Review apiReview) {
        ReviewText text = Optional.ofNullable(apiReview.getText())
                .map(t -> new ReviewText(t.getText(), t.getLanguageCode()))
                .orElse(new ReviewText("", ""));

        ReviewText originalText = Optional.ofNullable(apiReview.getOriginalText())
                .map(ot -> new Review.ReviewText(ot.getText(), ot.getLanguageCode()))
                .orElse(new Review.ReviewText("", ""));

        AuthorAttribution authorAttribution = new Review
                .AuthorAttribution(Optional.ofNullable(apiReview.getAuthorAttribution())
                                .map(GoogleMapsPlacesV1AuthorAttribution::getDisplayName)
                                .orElse(""), Optional.ofNullable(apiReview.getAuthorAttribution())
                .map(GoogleMapsPlacesV1AuthorAttribution::getUri).orElse(""),
                Optional.ofNullable(apiReview.getAuthorAttribution())
                        .map(GoogleMapsPlacesV1AuthorAttribution::getPhotoUri).orElse(""));

        return new Review(Optional.ofNullable(apiReview.getName()).orElse(""),
                Optional.ofNullable(
                        apiReview.getRelativePublishTimeDescription())
                        .orElse(""),
                Optional.ofNullable(apiReview.getRating()).map(Double::intValue).orElse(0),
                text, originalText, authorAttribution, Optional.ofNullable(apiReview.getPublishTime()).orElse(""));
    }
}
