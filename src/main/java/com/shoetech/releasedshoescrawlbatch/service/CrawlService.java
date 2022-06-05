package com.shoetech.releasedshoescrawlbatch.service;

import com.shoetech.releasedshoescrawlbatch.dto.Product;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

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

        CountDownLatch countdownLatch = new CountDownLatch(pageSize);
        for (int page = 1; page <= pageSize; page++) {
            Thread thread = makeThread(page , countdownLatch);
            thread.start();
        }

        countdownLatch.await();
        return products;
    }

    private Thread makeThread(int page, CountDownLatch countdownLatch) {
        return new Thread(() -> {
            Document html = null;
            try {
                html = nikeCrawler.crawlDocument(page);
            } catch (IOException e) {
                e.printStackTrace();
            }
            addProducts(nikeCrawler.parseToProduct(html));
            countdownLatch.countDown();
        });
    }

    private synchronized void addProducts(List<Product> products) {
        this.products.addAll(products);
    }
}
