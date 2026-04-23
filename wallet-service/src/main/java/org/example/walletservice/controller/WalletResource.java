package org.example.walletservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.walletservice.entity.Statement;
import org.example.walletservice.entity.Wallet;
import org.example.walletservice.service.WalletService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletResource {

    private final WalletService walletService;

    @PostMapping("/create")
    public Wallet createWallet() {
        return walletService.addWallet();
    }

    @GetMapping
    public List<Wallet> getAll() {
        return walletService.getWallets();
    }

    @GetMapping("/{id}")
    public Wallet getById(@PathVariable Long id) {
        return walletService.getById(id);
    }

    @PostMapping("/addMoney/{id}")
    public Wallet addMoney(@PathVariable Long id, @RequestParam Double amount) {
        return walletService.addMoney(id, amount);
    }

    @PostMapping("/pay/{id}")
    public Wallet payMoney(@PathVariable Long id, @RequestParam Double amount) {
        return walletService.payMoney(id, amount);
    }

    @GetMapping("/statements/{id}")
    public List<Statement> getStatements(@PathVariable Long id) {
        return walletService.getStatements(id);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        walletService.deleteById(id);
        return "Deleted";
    }
}
