package org.example.walletservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.walletservice.entity.Statement;
import org.example.walletservice.entity.Wallet;
import org.example.walletservice.repository.WalletRepository;
import org.example.walletservice.service.WalletService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;

    @Override
    public List<Wallet> getWallets() {
        return walletRepository.findAll();
    }

    @Override
    public Wallet addWallet() {
        Wallet wallet = new Wallet();
        wallet.setCurrentBalance(0.0);
        return walletRepository.save(wallet);
    }

    @Override
    public Wallet addMoney(Long walletId, Double amount) {
        Wallet wallet = getExistingWallet(walletId);
        wallet.setCurrentBalance(wallet.getCurrentBalance() + amount);

        Statement statement = new Statement();
        statement.setTransactionType("DEPOSIT");
        statement.setAmount(amount);
        statement.setDateTime(LocalDateTime.now());
        statement.setTransactionRemarks("Money Added");
        statement.setWallet(wallet);

        wallet.getStatements().add(statement);
        return walletRepository.save(wallet);
    }

    @Override
    public Wallet payMoney(Long walletId, Double amount) {
        Wallet wallet = getExistingWallet(walletId);

        if (wallet.getCurrentBalance() < amount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient Balance");
        }

        wallet.setCurrentBalance(wallet.getCurrentBalance() - amount);

        Statement statement = new Statement();
        statement.setTransactionType("WITHDRAW");
        statement.setAmount(amount);
        statement.setDateTime(LocalDateTime.now());
        statement.setTransactionRemarks("Payment Done");
        statement.setWallet(wallet);

        wallet.getStatements().add(statement);
        return walletRepository.save(wallet);
    }

    @Override
    public Wallet getById(Long walletId) {
        return getExistingWallet(walletId);
    }

    @Override
    public List<Statement> getStatements(Long walletId) {
        return getExistingWallet(walletId).getStatements();
    }

    @Override
    public void deleteById(Long walletId) {
        if (!walletRepository.existsById(walletId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found with id " + walletId);
        }
        walletRepository.deleteById(walletId);
    }

    private Wallet getExistingWallet(Long walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Wallet not found with id " + walletId));
    }
}
