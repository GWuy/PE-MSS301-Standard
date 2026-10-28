package fu.mss301.se194638.enrollmentservice.entity;

import lombok.Getter;

@Getter
public enum EnrollmentStatus {

    ACTIVE("ACTIVE"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED");

    private final String value;

    EnrollmentStatus(String value) {
        this.value = value;
    }
}
