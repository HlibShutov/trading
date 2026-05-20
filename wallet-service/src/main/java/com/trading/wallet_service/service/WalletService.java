package com.trading.wallet_service.service;

import com.trading.walletservice.grpc.ReserveFundsRequest;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class WalletService {
    @Transactional
    public void reserveFunds(ReserveFundsRequest request) {
        System.out.println(String.format("Reserve funds request received for %d", request.getUserId()));
    }
}
