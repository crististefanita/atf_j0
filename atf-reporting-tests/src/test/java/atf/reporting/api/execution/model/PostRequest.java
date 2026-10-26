package atf.reporting.api.execution.model;

public record PostRequest(String title, String body, int userId) {
    public PostRequest {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title must not be blank");
        if (body == null || body.isBlank()) throw new IllegalArgumentException("body must not be blank");
        if (userId <= 0) throw new IllegalArgumentException("userId must be positive");
    }
}
