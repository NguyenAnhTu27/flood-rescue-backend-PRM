package com.floodrescue.config.security;

import com.floodrescue.module.user.entity.UserEntity;
import com.floodrescue.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Ở đây username chính là userId (subject trong JWT)
        Long userId;
        try {
            userId = Long.parseLong(username);
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("Invalid userId in token subject: " + username);
        }

        return buildUserDetailsById(userId);
    }

    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        return buildUserDetailsById(userId);
    }

    private UserDetails buildUserDetailsById(Long userId) {
        // Fetch role eagerly via JOIN FETCH to avoid LazyInitializationException in security filter
        UserEntity user = userRepo.findByIdWithRole(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));

        String roleCode = user.getRole().getCode(); // "CITIZEN", "DISPATCHER"...
        List<GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + roleCode));

        boolean enabled = user.getStatus() == 1;

        return new org.springframework.security.core.userdetails.User(
                String.valueOf(user.getId()),     // username = userId (để controller parse Long được)
                user.getPasswordHash(),           // password hash (BCrypt)
                enabled,                          // enabled
                true,                             // accountNonExpired
                true,                             // credentialsNonExpired
                true,                             // accountNonLocked
                authorities
        );
    }
}