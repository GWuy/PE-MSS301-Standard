package fu.mss301.se194638.courseservice.dto;

import lombok.Getter;

@Getter
public enum CourseStatus {

    OPEN("OPEN"),
    CLOSED("CLOSED"),
    DRAFT("DRAFT");

    CourseStatus(String status) {
        this.status = status;
    }

    private final String status;
}
