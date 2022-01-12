package com.shoetech.releasedshoescrawlbatch.service;

import com.shoetech.releasedshoescrawlbatch.dto.Product;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class CrawlService {

    private NikeCrawler nikeCrawler;
    private List<Product> products = new ArrayList<>();

    public CrawlService() {
        this.nikeCrawler = new NikeCrawler();
    }

    public List<Product> crawlProducts() throws InterruptedException, IOException {
        int pageSize = nikeCrawler.getPageSize();
        log.info("page size : {}", pageSize);

        for (int page = 1; page <= pageSize; page++) {
            Thread thread = makeThread(page);
            thread.start();
        }

        Thread.sleep(10000);
        log.info("products size : {}", products.size());
        return products;
    }

    private Thread makeThread(int page) {
        Thread thread = new Thread(() -> {
            Document html = null;
            try {
                html = nikeCrawler.crawlDocument(page);
            } catch (IOException e) {
                e.printStackTrace();
            }
            products.addAll(nikeCrawler.parseToProduct(html));
        });

        return thread;
    }
}
