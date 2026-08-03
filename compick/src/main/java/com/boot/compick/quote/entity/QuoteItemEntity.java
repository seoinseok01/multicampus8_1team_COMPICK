package com.boot.compick.quote.entity;

import com.boot.compick.product.entity.ProductEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "QUOTE_ITEM", uniqueConstraints = @UniqueConstraint(columnNames = {"quote_id", "product_id"}))
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class QuoteItemEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quote_item_id") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_id", nullable = false) private QuoteEntity quote;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false) private ProductEntity product;
    @Column(name = "quantity", nullable = false) private int quantity;

    public static QuoteItemEntity create(QuoteEntity quote, ProductEntity product) {
        QuoteItemEntity item = new QuoteItemEntity();
        item.quote = quote;
        item.product = product;
        item.quantity = 1;
        return item;
    }
}
