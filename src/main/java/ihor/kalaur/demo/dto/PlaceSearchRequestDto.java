package ihor.kalaur.demo.dto;

public record PlaceSearchRequestDto(
    String textQuery,
    Integer maxResultCount,
    LocationBias locationBias
) {
    public record LocationBias(Circle circle) {
    }

    public record Circle(Center center, Double radius) {
    }

    public record Center(Double latitude, Double longitude) {
    }
}
