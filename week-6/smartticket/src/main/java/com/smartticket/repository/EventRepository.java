package com.smartticket.repository;

import com.smartticket.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository
        extends JpaRepository<Event, Long> {
}
