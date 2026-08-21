package com.oauth.server.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Standard API error response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiError {

    private int status;
    private String message;
    private LocalDateTime timestamp;
    private String path;

    /**
     * Creates a simple error response with just a status and message.
     */
    public static ApiError of(int status, String message) {
        return ApiError.builder()
                .status(status)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
