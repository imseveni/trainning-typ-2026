package com.smartticket.dto;

import java.io.Serializable;

public record EventResponse(
        Long eventId,
        String title,
        String description,
        String venue) implements Serializable {

    private static final long serialVersionUID = 1L;
}
