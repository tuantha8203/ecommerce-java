package tuantha8203.common.api;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    HttpStatus status();
    String name();
}
