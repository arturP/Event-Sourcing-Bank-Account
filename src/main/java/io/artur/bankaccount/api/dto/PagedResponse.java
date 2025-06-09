package io.artur.bankaccount.api.dto;

import io.artur.bankaccount.application.queries.readmodels.PagedResult;

import java.util.List;
import java.util.stream.Collectors;

public class PagedResponse<T> {
    
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
    private boolean first;
    private boolean last;
    private boolean empty;
    private int numberOfElements;
    
    public PagedResponse() {}
    
    public PagedResponse(List<T> content, int page, int size, long totalElements, int totalPages,
                        boolean hasNext, boolean hasPrevious, boolean first, boolean last, 
                        boolean empty, int numberOfElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
        this.first = first;
        this.last = last;
        this.empty = empty;
        this.numberOfElements = numberOfElements;
    }
    
    public static <T, R> PagedResponse<R> fromPagedResult(PagedResult<T> pagedResult, 
                                                         java.util.function.Function<T, R> mapper) {
        List<R> mappedContent = pagedResult.getContent().stream()
            .map(mapper)
            .collect(Collectors.toList());
        
        return new PagedResponse<>(
            mappedContent,
            pagedResult.getPage(),
            pagedResult.getSize(),
            pagedResult.getTotalElements(),
            pagedResult.getTotalPages(),
            pagedResult.hasNext(),
            pagedResult.hasPrevious(),
            pagedResult.isFirst(),
            pagedResult.isLast(),
            pagedResult.isEmpty(),
            pagedResult.getNumberOfElements()
        );
    }
    
    // Getters and setters
    public List<T> getContent() { return content; }
    public void setContent(List<T> content) { this.content = content; }
    
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    
    public boolean isHasNext() { return hasNext; }
    public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }
    
    public boolean isHasPrevious() { return hasPrevious; }
    public void setHasPrevious(boolean hasPrevious) { this.hasPrevious = hasPrevious; }
    
    public boolean isFirst() { return first; }
    public void setFirst(boolean first) { this.first = first; }
    
    public boolean isLast() { return last; }
    public void setLast(boolean last) { this.last = last; }
    
    public boolean isEmpty() { return empty; }
    public void setEmpty(boolean empty) { this.empty = empty; }
    
    public int getNumberOfElements() { return numberOfElements; }
    public void setNumberOfElements(int numberOfElements) { this.numberOfElements = numberOfElements; }
}