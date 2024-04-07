package ihor.kalaur.demo.service;

import ihor.kalaur.demo.dto.PlaceSearchRequestDto;
import ihor.kalaur.demo.dto.PlaceSearchResponseDto;

public interface GooglePlaceApiService {
    PlaceSearchResponseDto fetchRestaurantReviews(PlaceSearchRequestDto placeSearchRequestDto);
}
