package com.shoetech.releasedshoescrawlbatch.config;

import com.shoetech.releasedshoescrawlbatch.config.reader.CrawlItemReader;
import com.shoetech.releasedshoescrawlbatch.config.reader.SlackItemReader;
import com.shoetech.releasedshoescrawlbatch.config.writer.CrawlItemWriter;
import com.shoetech.releasedshoescrawlbatch.dto.Product;
import com.shoetech.releasedshoescrawlbatch.service.CrawlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.item.support.builder.CompositeItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@Slf4j
public class BatchConfiguration {

    private final static int CHUNK_SIZE = 100;
    private final static String SLACK_URL = "https://hooks.slack.com/services/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";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final CrawlService crawlService;
    private final EntityManagerFactory entityManagerFactory;
    private final DataSource dataSource;
    private final Map<String, Product> temporaryStorage;

    public BatchConfiguration(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory, CrawlService crawlService, EntityManagerFactory entityManagerFactory, DataSource dataSource) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.crawlService = crawlService;
        this.entityManagerFactory = entityManagerFactory;
        this.dataSource = dataSource;
        this.temporaryStorage = new ConcurrentHashMap<>();
    }

    @Bean
    public Job releasedShoesCrawlJob() throws Exception {
        return this.jobBuilderFactory.get("releasedShoesCrawlJob")
                                     .incrementer(new RunIdIncrementer())
                                     .start(this.releasedShoesCrawlStep())
                                     .next(this.updateDatabaseStep())
                                     .next(this.newReleasedItemStep())
                                     .build();
    }

    @Bean
    public Step releasedShoesCrawlStep() throws Exception {
        return this.stepBuilderFactory.get("releasedShoesCrawlStep")
                .<Product, Product>chunk(CHUNK_SIZE)
                .reader(new CrawlItemReader<>(crawlService.crawlProducts()))
                .processor(crawlProcessor())
                .writer(temporaryStorageWriter())
                .build();
    }

    private ItemProcessor<Product, Product> crawlProcessor() {
        return item -> {
            if (item.getName().contains("조던") || item.getName().contains("덩크")) {
                log.info("crawlProcessor: {}", item.toString());
                return item;
            }

            return null;
        };
    }

    private ItemWriter<Product> temporaryStorageWriter() {
        CrawlItemWriter itemWriter = new CrawlItemWriter();
        itemWriter.setStorage(temporaryStorage);

        return itemWriter;
    }

    @Bean
    public Step updateDatabaseStep() {
        return this.stepBuilderFactory.get("updateDatabaseStep")
                .<Product, Product>chunk(CHUNK_SIZE)
                .reader(storedItemReader())
                .processor(storedItemProcessor())
                .writer(deprecatedItemDeleteWriter())
                .build();
    }

    private JpaPagingItemReader<Product> storedItemReader() {
        return new JpaPagingItemReaderBuilder<Product>()
                .name("productReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(10)
                .queryString("SELECT p FROM Product p")
                .build();
    }

    private ItemProcessor<Product, Product> storedItemProcessor() {
        return item -> {
            if (!temporaryStorage.containsKey(item.getCode())) {
                log.info("storedItemProcessor: {}", item.toString());

                return item;
            }

            temporaryStorage.remove(item.getCode());
            return null;
        };
    }

    private ItemWriter<Product> deprecatedItemDeleteWriter() {
        JdbcBatchItemWriter<Product> itemWriter = new JdbcBatchItemWriterBuilder<Product>()
                .dataSource(dataSource)
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql("DELETE FROM product WHERE code = :code")
                .build();

        itemWriter.afterPropertiesSet();
        return itemWriter;
    }

    @Bean
    public Step newReleasedItemStep() throws Exception {
        return this.stepBuilderFactory.get("newReleasedItemStep")
                .<Product, Product>chunk(CHUNK_SIZE)
                .reader(new SlackItemReader<>(temporaryStorage))
                .writer(newReleasedItemWriter())
                .build();
    }

    private ItemWriter<Product> newReleasedItemWriter() throws Exception {
        CompositeItemWriter<Product> itemWriter = new CompositeItemWriterBuilder<Product>()
                .delegates(savedNewReleasedItemWriter(), sendToSlackWriter())
                .build();

        itemWriter.afterPropertiesSet();
        return itemWriter;
    }

    private ItemWriter<? super Product> sendToSlackWriter() {
        return new JpaItemWriterBuilder<Product>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

    private ItemWriter<? super Product> savedNewReleasedItemWriter() {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

        return items -> items.forEach(product -> {
            log.info("newReleasedItemWriter: {}", product.toString());
            HttpEntity<String> name = new HttpEntity<>(convertProductToSlackMessageJson(product), headers);
            restTemplate.postForEntity(SLACK_URL, name, String.class);
        });
    }

    private String convertProductToSlackMessageJson(Product product) {
        // TODO product to json 하드코딩 리팩토링 필요
        String json = "{\n" +
                "    \"text\": \"신상 떴다요!!!\",\n" +
                "    \"attachments\": [\n" +
                "        {\n" +
                "            \"text\": \"상품명 : {product.name} ({product.code}) \\n 발매가 : {product.price} \\n\\n 구매 링크 : {product.link}\",\n" +
                "\t\t\t\"image_url\" : \"{product.imageUrl}\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";

        return json.replace("{product.name}", product.getName())
                   .replace("{product.code}", product.getCode())
                   .replace("{product.price}", product.getPrice())
                   .replace("{product.link}", product.getLink())
                   .replace("{product.imageUrl}", product.getImageUrl());
    }
}
