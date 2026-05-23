package com.trading.wallet_service.service;

import com.trading.wallet_service.exception.InsufficientAmount;
import com.trading.wallet_service.exception.WalletRecordNotFound;
import com.trading.wallet_service.model.WalletBalance;
import com.trading.wallet_service.repository.WalletRepository;
import com.trading.walletservice.grpc.ReserveFundsRequest;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class WalletService {
    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    private final WalletRepository walletRepository;

    @Transactional
    public void reserveFunds(ReserveFundsRequest request) {
        WalletBalance walletBalance = walletRepository.findByUserIdAndAsset(request.getUserId(), request.getAsset())
                .orElseThrow(() -> new WalletRecordNotFound(("Wallet not found")));
        BigDecimal requestedAmount = new BigDecimal(request.getAmount());
        if (walletBalance.getBalance().compareTo(requestedAmount) < 0) {
            throw  new InsufficientAmount("wanted: %s available: %s".formatted(requestedAmount, walletBalance.getBalance()));
        }
        walletBalance.setBalance(walletBalance.getBalance().subtract(requestedAmount));
        walletBalance.setReserved(walletBalance.getReserved().add(requestedAmount));
        walletRepository.save(walletBalance);
    }
}
