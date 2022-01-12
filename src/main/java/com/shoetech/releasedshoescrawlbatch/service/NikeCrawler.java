package com.shoetech.releasedshoescrawlbatch.service;

import com.shoetech.releasedshoescrawlbatch.dto.Product;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NikeCrawler {

    private static final String NIKE_HOST = "https://www.nike.com";
    private static final String NIKE_PATH = "/kr/ko_kr/w/xg/xb/xc/new-releases?productCategoryType=FW";

    public Document crawlDocument(int page) throws IOException {
        Connection conn = Jsoup.connect(NIKE_HOST + NIKE_PATH + "&page=" + page + "&pageSize=40");

        return conn.get();
    }

    public List<Product> parseToProduct(Document html) {
        List<Product> products = new ArrayList<>();

        Elements elements = html.getElementsByClass("a-product");

        for (Element element : elements) {
            String name = element.getElementsByClass("product-display-name").text().replace("\"", "");
            String code = element.getElementsByTag("input")
                                 .attr("type", "hidden")
                                 .attr("name", "productmodel")
                                 .last()
                                 .attr("value");
            String price = element.getElementsByClass("product-display-price").text();
            String img = element.getElementsByClass("a-product-image-primary")
                                .select("img")
                                .attr("src");
            String productPath = element.getElementsByClass("a-product-image item-imgwrap action-hover").select("a").attr("href");
            String link = NIKE_HOST + productPath;

            Product product = Product.builder()
                                     .name(name)
                                     .code(code)
                                     .imageUrl(img)
                                     .price(price)
                                     .link(link)
                                     .build();

            products.add(product);
        }

        return products;
    }

    public int getPageSize() throws IOException {
        Document html = crawlDocument(0);

        double size = Integer.parseInt(html.getElementsByClass("text-color-primary-dark")
                                        .first()
                                        .getElementsByTag("span").text());
        return (int) Math.ceil(size / 40);
    }
}
