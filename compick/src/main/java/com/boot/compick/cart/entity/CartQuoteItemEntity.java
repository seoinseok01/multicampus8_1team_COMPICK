package com.boot.compick.cart.entity;

import com.boot.compick.quote.entity.QuoteEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CART_QUOTE_ITEM", uniqueConstraints = @UniqueConstraint(columnNames = {"cart_id", "quote_id"}))
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class CartQuoteItemEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_quote_item_id") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false) private CartEntity cart;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_id", nullable = false) private QuoteEntity quote;
    @Column(name = "quantity", nullable = false) private int quantity;
    @Column(name = "is_selected", nullable = false, length = 1) private String selected;

    public static CartQuoteItemEntity create(CartEntity cart, QuoteEntity quote) {
        CartQuoteItemEntity item = new CartQuoteItemEntity();
        item.cart = cart;
        item.quote = quote;
        item.quantity = 1;
        item.selected = "N";
        return item;
    }

    public void select(boolean selected) {
        this.selected = selected ? "Y" : "N";
    }

    public boolean isSelected() {
        return "Y".equals(selected);
    }

    public void increaseQuantity(int quantity) { this.quantity += quantity; }
}
