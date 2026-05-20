package com.trading.wallet_service.repository;

import com.trading.wallet_service.model.WalletBalance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<WalletBalance, Long> {
}