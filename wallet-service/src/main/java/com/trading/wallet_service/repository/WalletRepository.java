package com.trading.wallet_service.repository;

import com.trading.wallet_service.model.WalletBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<WalletBalance, Long> {
    Optional<WalletBalance> findByUserIdAndAsset(Long userId, String asset);
}