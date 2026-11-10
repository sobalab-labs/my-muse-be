package com.sobalab.mymuse.api.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import com.sobalab.mymuse.api.model.NotificationPreferences;
import com.sobalab.mymuse.api.model.User;
import com.sobalab.mymuse.api.model.UserIdentity;
import com.sobalab.mymuse.api.model.enums.OAuthProvider;
import com.sobalab.mymuse.api.repository.NotificationPreferencesRepository;
import com.sobalab.mymuse.api.repository.UserIdentityRepository;
import com.sobalab.mymuse.api.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {
    @Mock UserRepository userRepository;
    @Mock UserIdentityRepository userIdentityRepository;
    @Mock NotificationPreferencesRepository notificationPreferencesRepository;
    @InjectMocks AuthService authService;

    @ParameterizedTest
    @EnumSource(OAuthProvider.class)
    void existingIdentityDoesNotOverwriteProfileOrCreateSubscriptions(OAuthProvider provider) {
        User user = User.builder().email("original@example.com").name("Original").build();
        when(userIdentityRepository.findByProviderAndProviderSubject(provider, "subject"))
                .thenReturn(Optional.of(UserIdentity.builder().user(user).build()));
        assertThat(authService.processOAuth2User(provider, "subject", "changed@example.com", true, "Changed"))
                .isSameAs(user);
        assertThat(user.getEmail()).isEqualTo("original@example.com");
        verifyNoInteractions(userRepository, notificationPreferencesRepository);
        verify(userIdentityRepository).findByProviderAndProviderSubject(provider, "subject");
        verifyNoMoreInteractions(userIdentityRepository);
    }

    @ParameterizedTest
    @EnumSource(OAuthProvider.class)
    void newIdentityCreatesOneAccountAndDisabledPreferences(OAuthProvider provider) {
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        User result = authService.processOAuth2User(provider, "subject", "person@example.com", true, "Person");
        var identity = ArgumentCaptor.forClass(UserIdentity.class);
        var preferences = ArgumentCaptor.forClass(NotificationPreferences.class);
        var order = inOrder(userRepository, userIdentityRepository, notificationPreferencesRepository);
        order.verify(userIdentityRepository).findByProviderAndProviderSubject(provider, "subject");
        order.verify(userRepository).save(result);
        order.verify(userIdentityRepository).save(identity.capture());
        order.verify(notificationPreferencesRepository).save(preferences.capture());
        assertThat(identity.getValue().getUser()).isSameAs(result);
        assertThat(identity.getValue().getProvider()).isEqualTo(provider);
        assertThat(identity.getValue().getProviderSubject()).isEqualTo("subject");
        assertThat(result.getEmail()).isEqualTo("person@example.com");
        assertThat(result.isEmailVerified()).isTrue();
        assertThat(result.isOnboardingCompleted()).isFalse();
        assertThat(preferences.getValue().getUser()).isSameAs(result);
        assertThat(preferences.getValue().isChannelEmailEnabled()).isFalse();
        assertThat(preferences.getValue().isChannelKakaoEnabled()).isFalse();
        assertThat(preferences.getValue().isGeneralWeeklyUpdates()).isFalse();
        assertThat(preferences.getValue().isArtistNewConcertEnabled()).isFalse();
        assertThat(preferences.getValue().isBookmarkedEventTicketOpenEnabled()).isFalse();
    }

    @Test
    void absentOptionalClaimsCannotProduceVerifiedEmailOrBlankName() {
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        User result = authService.processOAuth2User(OAuthProvider.KAKAO, "subject", "  ", true, "  ");
        assertThat(result.getEmail()).isNull();
        assertThat(result.isEmailVerified()).isFalse();
        assertThat(result.getName()).isEqualTo("User");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void invalidSubjectNeverReachesDatabase(String subject) {
        assertThatThrownBy(() -> authService.processOAuth2User(OAuthProvider.GOOGLE, subject, null, false, null))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(userRepository, userIdentityRepository, notificationPreferencesRepository);
    }

    @Test
    void nullProviderNeverReachesDatabase() {
        assertThatThrownBy(() -> authService.processOAuth2User(null, "subject", null, false, null))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(userRepository, userIdentityRepository, notificationPreferencesRepository);
    }

    @Test
    void duplicateIdentityFailurePropagatesForTransactionRollbackWithoutCreatingPreferences() {
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var failure = new DataIntegrityViolationException("duplicate provider/subject");
        when(userIdentityRepository.save(any())).thenThrow(failure);
        assertThatThrownBy(() -> authService.processOAuth2User(OAuthProvider.GOOGLE, "subject", null, false, null))
                .isSameAs(failure);
        verifyNoInteractions(notificationPreferencesRepository);
        // Mockito cannot prove rollback; a database integration test is needed for that.
    }

    @Test
    void preferenceFailurePropagatesRatherThanReturningPartiallyCreatedAccount() {
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var failure = new DataIntegrityViolationException("preferences");
        when(notificationPreferencesRepository.save(any())).thenThrow(failure);
        assertThatThrownBy(() -> authService.processOAuth2User(OAuthProvider.KAKAO, "subject", null, false, null))
                .isSameAs(failure);
    }

    @Test
    void userSaveFailureStopsDependentWrites() {
        var failure = new DataIntegrityViolationException("user");
        when(userRepository.save(any())).thenThrow(failure);
        assertThatThrownBy(() -> authService.processOAuth2User(OAuthProvider.GOOGLE, "subject", null, false, null))
                .isSameAs(failure);
        verify(userIdentityRepository).findByProviderAndProviderSubject(OAuthProvider.GOOGLE, "subject");
        verifyNoMoreInteractions(userIdentityRepository);
        verifyNoInteractions(notificationPreferencesRepository);
    }

}
