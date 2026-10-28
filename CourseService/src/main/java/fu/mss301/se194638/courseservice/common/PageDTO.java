package fu.mss301.se194638.courseservice.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageDTO<T> {

    private int size;

    private int page;

    private int totalPages;

    private long totalElements;

    private boolean first;

    private boolean last;

    private List<T> content;
}
