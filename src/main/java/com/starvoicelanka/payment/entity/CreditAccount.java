package com.starvoicelanka.payment.entity;

import com.starvoicelanka.common.BaseEntity;
import com.starvoicelanka.user.entity.User;
import jakarta.persistence.*;

@Entity
@Table(name = "credit_accounts", indexes = {
        @Index(name = "idx_credit_account_user", columnList = "user_id", unique = true)
})
public class CreditAccount extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private int balance = 0;

    @Column(name = "lifetime_purchased", nullable = false)
    private int lifetimePurchased = 0;

    @Column(name = "lifetime_spent", nullable = false)
    private int lifetimeSpent = 0;

    public CreditAccount() {}

    public CreditAccount(User user) {
        this.user = user;
        this.balance = 0;
        this.lifetimePurchased = 0;
        this.lifetimeSpent = 0;
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public int getBalance() { return balance; }
    public void setBalance(int balance) { this.balance = balance; }

    public int getLifetimePurchased() { return lifetimePurchased; }
    public void setLifetimePurchased(int lifetimePurchased) { this.lifetimePurchased = lifetimePurchased; }

    public int getLifetimeSpent() { return lifetimeSpent; }
    public void setLifetimeSpent(int lifetimeSpent) { this.lifetimeSpent = lifetimeSpent; }
}
