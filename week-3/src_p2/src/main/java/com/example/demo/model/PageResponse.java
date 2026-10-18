package com.example.demo.model;

import java.util.List;

public class PageResponse<T> {

    private List<T> data;
    private int page;
    private int size;
    private int totalPages;
    private long totalElements;

    public PageResponse(
            List<T> data,
            int page,
            int size,
            int totalPages,
            long totalElements) {

        this.data = data;
        this.page = page;
        this.size = size;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
    }

    public List<T> getData() {
        return data;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public long getTotalElements() {
        return totalElements;
    }
}