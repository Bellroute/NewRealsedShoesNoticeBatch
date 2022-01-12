package com.shoetech.releasedshoescrawlbatch.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Builder
@Getter
@NoArgsConstructor
public class Product {

    @Id
    private String code;

    @Column
    private String name;

    @Column
    private String price;

    @Column
    private String imageUrl;

    @Column
    private String link;

    public Product(String code, String name, String price, String imageUrl, String link) {
        this.code = code;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.link = link;
    }

    @Override
    public String toString() {
        return "Product{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", price='" + price + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", link='" + link + '\'' +
                '}';
    }
}