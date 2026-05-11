package github.io.ddmfuhrmann.outfit.inventory.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Getter
@Entity
@Table(name = "stock_balance")
@EntityListeners(AuditingEntityListener.class)
public class StockBalance {

    @Id
    private Long productSkuId;

    @Version
    private Long version;

    private int currentBalance;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    protected StockBalance() {}

    public static StockBalance create(Long skuId) {
        var balance = new StockBalance();
        balance.productSkuId = skuId;
        balance.currentBalance = 0;
        return balance;
    }

    public void apply(int quantity) {
        this.currentBalance += quantity;
    }
}
