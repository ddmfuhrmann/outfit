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
        return ResponseEntity.created(URI.create("/shared/users/" + created.login())).body(created);
    }

    @GetMapping("/{login}")
    ResponseEntity<UserResponse> get(@PathVariable String login) {
        return ResponseEntity.ok(getUser.execute(login));
    }

    @PutMapping("/{login}")
    ResponseEntity<UserResponse> update(@PathVariable String login, @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(updateUser.execute(login, request));
    }

    @DeleteMapping("/{login}")
    ResponseEntity<Void> deactivate(@PathVariable String login) {
        deactivateUser.execute(login);
        return ResponseEntity.noContent().build();
    }
}
