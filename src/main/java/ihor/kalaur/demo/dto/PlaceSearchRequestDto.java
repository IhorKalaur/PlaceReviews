package ihor.kalaur.demo.dto;

public record PlaceSearchRequestDto(
    String textQuery,
    LocationBias locationBias
) {
    public record LocationBias(Circle circle) {
    }

    public record Circle(Center center, Integer radius) {
    }

    public record Center(Double latitude, Double longitude) {
    }
}