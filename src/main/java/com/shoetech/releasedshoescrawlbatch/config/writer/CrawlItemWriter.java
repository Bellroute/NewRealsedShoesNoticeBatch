package com.shoetech.releasedshoescrawlbatch.config.writer;

import com.shoetech.releasedshoescrawlbatch.dto.Product;
import org.springframework.batch.item.ItemWriter;

import java.util.List;
import java.util.Map;

public class CrawlItemWriter implements ItemWriter {

    private Map<String, Product> storage;

    @Override
    public void write(List items) {
        for (Object item : items) {
            Product product = (Product) item;
            storage.put(product.getCode(), product);
        }
    }

    public void setStorage(Map<String, Product> temporaryStorage) {
        this.storage = temporaryStorage;
    }
}
