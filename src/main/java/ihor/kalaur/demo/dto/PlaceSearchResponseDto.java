package ihor.kalaur.demo.dto;

import java.util.List;
import java.util.Optional;

public record PlaceSearchResponseDto(List<Place> places) {

    public record Place(DisplayName displayName, List<Review> reviews) {

        public record DisplayName(String text) {
        }

        public record Review(
            String name,
            String relativePublishTimeDescription,
            int rating,
            ReviewText text,
            ReviewText originalText,
            AuthorAttribution authorAttribution,
            String publishTime
        ) {

            public record ReviewText(String text, String languageCode) {
            }

            public record AuthorAttribution(
                String displayName,
                String uri,
                String photoUri
            ) {
            }
        }
    }
}