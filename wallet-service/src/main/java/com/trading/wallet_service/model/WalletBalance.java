package com.trading.wallet_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@Table(uniqueConstraints={
        @UniqueConstraint(columnNames = {"user_id", "asset"})
})
public class WalletBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(nullable = false)
    private String asset;
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal balance;
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal reserved;
}
