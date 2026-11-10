package com.sobalab.mymuse.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sobalab.mymuse.api.model.NotificationPreferences;
import com.sobalab.mymuse.api.model.User;
import com.sobalab.mymuse.api.model.UserIdentity;
import com.sobalab.mymuse.api.model.enums.OAuthProvider;
import com.sobalab.mymuse.api.repository.NotificationPreferencesRepository;
import com.sobalab.mymuse.api.repository.UserIdentityRepository;
import com.sobalab.mymuse.api.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final NotificationPreferencesRepository notificationPreferencesRepository;

    @Transactional
    public User processOAuth2User(OAuthProvider provider, String providerSubject, String email, boolean emailVerified,
            String name) {
        if (provider == null || providerSubject == null || providerSubject.isBlank()) {
            throw new IllegalArgumentException("OAuth provider and subject are required");
        }
        return userIdentityRepository.findByProviderAndProviderSubject(provider, providerSubject)
                .map(UserIdentity::getUser)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email != null && !email.isBlank() ? email.strip() : null)
                            .emailVerified(emailVerified && email != null && !email.isBlank())
                            .name(name != null && !name.isBlank() ? name.strip() : "User")
                            .build();
                    userRepository.save(newUser);

                    UserIdentity identity = UserIdentity.builder()
                            .user(newUser)
                            .provider(provider)
                            .providerSubject(providerSubject)
                            .build();
                    userIdentityRepository.save(identity);

                    NotificationPreferences sub = NotificationPreferences.builder()
                            .user(newUser)
                            .build();
                    notificationPreferencesRepository.save(sub);

                    return newUser;
                });
    }

}
