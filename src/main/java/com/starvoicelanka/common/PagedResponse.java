package com.starvoicelanka.common;

import java.util.List;

public class PagedResponse<T> {
    private boolean success = true;
    private List<T> data;
    private Meta meta;

    public PagedResponse() {}

    public PagedResponse(List<T> data, int page, int limit, long total) {
        this.data = data;
        int pages = limit > 0 ? (int) Math.ceil((double) total / limit) : 1;
        this.meta = new Meta(page, limit, total, Math.max(1, pages));
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public List<T> getData() { return data; }
    public void setData(List<T> data) { this.data = data; }

    public Meta getMeta() { return meta; }
    public void setMeta(Meta meta) { this.meta = meta; }

    public static class Meta {
        private int page;
        private int limit;
        private long total;
        private int pages;

        public Meta() {}

        public Meta(int page, int limit, long total, int pages) {
            this.page = page;
            this.limit = limit;
            this.total = total;
            this.pages = pages;
        }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }

        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }

        public int getPages() { return pages; }
        public void setPages(int pages) { this.pages = pages; }
    }

    public static class PaginationMeta extends Meta {
        public PaginationMeta() { super(); }
        public PaginationMeta(int page, int limit, long total, int pages) {
            super(page, limit, total, pages);
        }
    }
}
