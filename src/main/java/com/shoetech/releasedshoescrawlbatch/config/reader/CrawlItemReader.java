package com.shoetech.releasedshoescrawlbatch.config.reader;

import org.springframework.batch.item.ItemReader;

import java.util.ArrayList;
import java.util.List;

public class CrawlItemReader<T> implements ItemReader<T> {

    private final List<T> items;

    public CrawlItemReader(List<T> items) {
        this.items = new ArrayList<>(items);
    }

    @Override
    public T read() {
        if (!items.isEmpty()) {
            return items.remove(0);
        }
        return null; // null을 리턴하면 chunk 반복을 끝낸다는 의미
    }
}
