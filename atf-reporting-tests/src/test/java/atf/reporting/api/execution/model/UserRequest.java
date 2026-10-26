package atf.reporting.api.execution.model;

public record UserRequest(String name, String email, String gender, String status) {
    public UserRequest {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("email must not be blank");
        gender = gender == null || gender.isBlank() ? "male" : gender;
        status = status == null || status.isBlank() ? "active" : status;
    }
}
