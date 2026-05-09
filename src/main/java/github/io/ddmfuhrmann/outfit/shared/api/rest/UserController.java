package github.io.ddmfuhrmann.outfit.shared.api.rest;

import github.io.ddmfuhrmann.outfit.shared.application.dto.CreateUserRequest;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import github.io.ddmfuhrmann.outfit.shared.application.dto.UpdateUserRequest;
import github.io.ddmfuhrmann.outfit.shared.application.dto.UserResponse;
import github.io.ddmfuhrmann.outfit.shared.application.usecase.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/shared/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final ListUsersUseCase listUsers;
    private final CreateUserUseCase createUser;
    private final GetUserUseCase getUser;
    private final UpdateUserUseCase updateUser;
    private final DeactivateUserUseCase deactivateUser;

    public UserController(ListUsersUseCase listUsers, CreateUserUseCase createUser,
                          GetUserUseCase getUser, UpdateUserUseCase updateUser,
                          DeactivateUserUseCase deactivateUser) {
        this.listUsers = listUsers;
        this.createUser = createUser;
        this.getUser = getUser;
        this.updateUser = updateUser;
        this.deactivateUser = deactivateUser;
    }

    @GetMapping
    ResponseEntity<PageResponse<UserResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(listUsers.execute(PageRequest.of(page, size)));
    }

    @PostMapping
    ResponseEntity<UserResponse> create(@RequestBody @Valid CreateUserRequest request) {
        UserResponse created = createUser.execute(request);
        return ResponseEntity.created(URI.create("/shared/users/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    ResponseEntity<UserResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(getUser.execute(id));
    }

    @PutMapping("/{id}")
    ResponseEntity<UserResponse> update(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(updateUser.execute(id, request));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> deactivate(@PathVariable Long id) {
        deactivateUser.execute(id);
        return ResponseEntity.noContent().build();
    }
}
