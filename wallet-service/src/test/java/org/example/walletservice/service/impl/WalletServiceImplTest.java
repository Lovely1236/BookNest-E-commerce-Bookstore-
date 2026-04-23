package org.example.walletservice.service.impl;

import org.example.walletservice.entity.Statement;
import org.example.walletservice.entity.Wallet;
import org.example.walletservice.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    @Test
    void addWallet_shouldCreateWalletWithZeroBalance() {
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Wallet saved = walletService.addWallet();

        assertNotNull(saved);
        assertEquals(0.0, saved.getCurrentBalance());
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void addMoney_shouldIncreaseBalanceAndAppendDepositStatement() {
        Wallet wallet = new Wallet();
        wallet.setWalletId(1L);
        wallet.setCurrentBalance(100.0);

        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Wallet updated = walletService.addMoney(1L, 50.0);

        assertEquals(150.0, updated.getCurrentBalance());
        assertEquals(1, updated.getStatements().size());
        Statement statement = updated.getStatements().get(0);
        assertEquals("DEPOSIT", statement.getTransactionType());
        assertEquals(50.0, statement.getAmount());
        assertEquals("Money Added", statement.getTransactionRemarks());
        assertSame(wallet, statement.getWallet());
        verify(walletRepository).save(wallet);
    }

    @Test
    void payMoney_shouldThrowWhenBalanceIsInsufficient() {
        Wallet wallet = new Wallet();
        wallet.setWalletId(1L);
        wallet.setCurrentBalance(20.0);

        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> walletService.payMoney(1L, 50.0));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void getById_shouldThrowWhenWalletDoesNotExist() {
        when(walletRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> walletService.getById(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void deleteById_shouldThrowWhenWalletDoesNotExist() {
        when(walletRepository.existsById(5L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> walletService.deleteById(5L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(walletRepository, never()).deleteById(5L);
    }
}
