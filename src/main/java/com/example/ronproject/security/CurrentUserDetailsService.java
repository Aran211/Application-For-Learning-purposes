package com.example.ronproject.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.ronproject.user.CurrentUser;
import com.example.ronproject.user.UserAccountRepository;

@Service
public class CurrentUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public CurrentUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userAccountRepository.findByEmailAndDeletedFalse(username)
                .map(CurrentUser::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
