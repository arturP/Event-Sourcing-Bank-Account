package io.artur.bankaccount.application.queries.readmodels;

import java.util.List;

public class PagedResult<T> {
    
    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean hasNext;
    private final boolean hasPrevious;
    
    public PagedResult(List<T> content, int page, int size, long totalElements) {
        this.content = content != null ? content : List.of();
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        this.hasNext = (page + 1) < totalPages;
        this.hasPrevious = page > 0;
    }
    
    public static <T> PagedResult<T> empty(int page, int size) {
        return new PagedResult<>(List.of(), page, size, 0);
    }
    
    public static <T> PagedResult<T> of(List<T> content, int page, int size, long totalElements) {
        return new PagedResult<>(content, page, size, totalElements);
    }
    
    public List<T> getContent() {
        return content;
    }
    
    public int getPage() {
        return page;
    }
    
    public int getSize() {
        return size;
    }
    
    public long getTotalElements() {
        return totalElements;
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public boolean hasNext() {
        return hasNext;
    }
    
    public boolean hasPrevious() {
        return hasPrevious;
    }
    
    public boolean isEmpty() {
        return content.isEmpty();
    }
    
    public int getNumberOfElements() {
        return content.size();
    }
    
    public boolean isFirst() {
        return page == 0;
    }
    
    public boolean isLast() {
        return !hasNext;
    }
    
    @Override
    public String toString() {
        return String.format("PagedResult{page=%d, size=%d, totalElements=%d, totalPages=%d, content=%d items}", 
                           page, size, totalElements, totalPages, content.size());
    }
}