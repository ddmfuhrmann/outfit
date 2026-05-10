package github.io.ddmfuhrmann.outfit.shared.domain.model;

import github.io.ddmfuhrmann.outfit.shared.domain.event.UserCreated;
import github.io.ddmfuhrmann.outfit.shared.domain.event.UserDeactivated;
import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Getter
@Entity
@Table(name = "app_user")
@EntityListeners(AuditingEntityListener.class)
public class User extends AbstractAggregateRoot<User> {

    @Id
    @Column(nullable = false, length = 100)
    private String login;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    @Column(nullable = false)
    private boolean active = true;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    protected User() {}

    public static User create(String login, String passwordHash, String name, UserRole role) {
        if (login == null || login.isBlank()) throw new IllegalArgumentException("login is required");
        if (passwordHash == null || passwordHash.isBlank()) throw new IllegalArgumentException("passwordHash is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (role == null) throw new IllegalArgumentException("role is required");
        var user = new User();
        user.login = login;
        user.passwordHash = passwordHash;
        user.name = name;
        user.role = role;
        user.active = true;
        user.registerEvent(new UserCreated(user.login, user.name, user.role));
        return user;
    }

    public void updateProfile(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        this.name = name;
    }

    public void changeRole(UserRole role) {
        if (role == null) throw new IllegalArgumentException("role is required");
        this.role = role;
    }

    public void deactivate() {
        if (!this.active) throw new IllegalStateException("User is already inactive");
        this.active = false;
        registerEvent(new UserDeactivated(this.login));
    }
}
