package org.example.walletservice.repository;

import org.example.walletservice.entity.Statement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatementsRepository extends JpaRepository<Statement, Long> {

    List<Statement> findByWalletWalletId(Long walletId);

    List<Statement> findByTransactionType(String transactionType);

    List<Statement> findByOrderId(Long orderId);
}
