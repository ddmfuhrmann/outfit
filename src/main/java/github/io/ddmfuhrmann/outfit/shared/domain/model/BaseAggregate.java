package github.io.ddmfuhrmann.outfit.shared.domain.model;

import com.github.f4b6a3.tsid.TsidCreator;
import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Collection;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAggregate<T extends BaseAggregate<T>> extends AbstractAggregateRoot<T> {

    @Id
    private Long id;

    protected BaseAggregate() {
        this.id = TsidCreator.getTsid().toLong();
    }

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public Collection<Object> getRegisteredEvents() { return domainEvents(); }
    public void resetRegisteredEvents() { clearDomainEvents(); }
}
