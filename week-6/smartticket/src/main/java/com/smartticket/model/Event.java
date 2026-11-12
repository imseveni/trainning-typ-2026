package com.smartticket.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String venue;

    /**
     * Khớp với cột version đã tồn tại trong database và bật optimistic locking
     * khi nhiều admin cùng sửa một sự kiện. Hibernate tự khởi tạo version khi insert.
     */
    @Version
    @Column(nullable = false)
    private Integer version;
}
