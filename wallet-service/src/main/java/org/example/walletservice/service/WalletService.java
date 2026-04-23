package org.example.walletservice.service;

import org.example.walletservice.entity.Statement;
import org.example.walletservice.entity.Wallet;

import java.util.List;

public interface WalletService {

    List<Wallet> getWallets();

    Wallet addWallet();

    Wallet addMoney(Long walletId, Double amount);

    Wallet payMoney(Long walletId, Double amount);

    Wallet getById(Long walletId);

    List<Statement> getStatements(Long walletId);

    void deleteById(Long walletId);
}
