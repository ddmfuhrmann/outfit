package github.io.ddmfuhrmann.outfit.shared.infrastructure.security;

import github.io.ddmfuhrmann.outfit.shared.domain.model.User;
import github.io.ddmfuhrmann.outfit.shared.domain.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        User user = userRepository.findById(login)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + login));
        return new UserPrincipal(user);
    }
}
