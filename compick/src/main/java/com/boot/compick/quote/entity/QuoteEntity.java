package com.boot.compick.quote.entity;

import java.time.LocalDateTime;
import com.boot.compick.member.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "QUOTE")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class QuoteEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quote_id") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false) private Member member;
    @Column(name = "quote_name", nullable = false, length = 150) private String name;
    @Column(name = "quote_type", nullable = false, length = 20) private String type;
    @Column(name = "assembly_type", nullable = false, length = 20) private String assemblyType;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    public static QuoteEntity ai(Member member, String name) {
        QuoteEntity quote = new QuoteEntity();
        quote.member = member;
        quote.name = name;
        quote.type = "AI";
        quote.assemblyType = "SELF";
        quote.createdAt = LocalDateTime.now();
        quote.updatedAt = quote.createdAt;
        return quote;
    }
}
