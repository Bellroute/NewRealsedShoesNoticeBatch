package com.shoetech.releasedshoescrawlbatch.config.reader;

import org.springframework.batch.item.ItemReader;

import java.util.Map;

public class SlackItemReader<T> implements ItemReader<T> {

    private Map<String, T> items;

    public SlackItemReader(Map<String, T> items) {
        this.items = items;
    }

    @Override
    public T read() {
        if (!items.isEmpty()) {
            return items.remove(items.keySet().iterator().next());
        }
        return null; // null을 리턴하면 chunk 반복을 끝낸다는 의미
    }
}