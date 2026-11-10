package com.sobalab.mymuse.api.security;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.sobalab.mymuse.api.controller.ArtistController;
import com.sobalab.mymuse.api.controller.ConcertController;
import com.sobalab.mymuse.api.controller.FeedController;
import com.sobalab.mymuse.api.controller.SearchController;
import com.sobalab.mymuse.api.controller.UserController;
import com.sobalab.mymuse.api.service.ArtistService;
import com.sobalab.mymuse.api.service.ConcertService;
import com.sobalab.mymuse.api.service.FeedService;
import com.sobalab.mymuse.api.service.SearchService;
import com.sobalab.mymuse.api.service.UserService;

@WebMvcTest(controllers = {
        SearchController.class,
        FeedController.class,
        ArtistController.class,
        ConcertController.class,
        UserController.class
})
@Import(SecurityConfig.class)
class SecurityConfigTests {

    private static final String ARTIST_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String CONCERT_ID = "123e4567-e89b-12d3-a456-426614174001";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppOidcUserService oidcUserService;

    @MockitoBean
    private AppOAuth2UserService oauth2UserService;

    @MockitoBean
    private SearchService searchService;

    @MockitoBean
    private FeedService feedService;

    @MockitoBean
    private ArtistService artistService;

    @MockitoBean
    private ConcertService concertService;

    @MockitoBean
    private UserService userService;

    @ParameterizedTest(name = "anonymous GET {0} is public")
    @MethodSource("publicGetRequests")
    @WithAnonymousUser
    void publicReadEndpointsAllowAnonymousRequests(String path, String parameter, String value) throws Exception {
        MockHttpServletRequestBuilder request = get(path);
        if (parameter != null) {
            request.param(parameter, value);
        }

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"));
    }

    static Stream<Arguments> publicGetRequests() {
        return Stream.of(
                Arguments.of("/search", "q", "public query"),
                Arguments.of("/feed/trending", null, null),
                Arguments.of("/artists/" + ARTIST_ID, null, null),
                Arguments.of("/concerts/" + CONCERT_ID, null, null));
    }

    @ParameterizedTest(name = "anonymous GET {0} receives 401")
    @MethodSource("privateGetRequests")
    @WithAnonymousUser
    void userEndpointsRejectAnonymousRequestsWithUnauthorized(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"));
        verifyNoInteractions(userService);
    }

    static Stream<Arguments> privateGetRequests() {
        return Stream.of(
                Arguments.of("/users/me"),
                Arguments.of("/users/me/feed"),
                Arguments.of("/users/me/followed-artists"),
                Arguments.of("/users/me/bookmarked-concerts"),
                Arguments.of("/users/me/notification-preferences"));
    }

    @ParameterizedTest(name = "anonymous POST {0} is not public")
    @MethodSource("publicReadPaths")
    @WithAnonymousUser
    void nonGetRequestsToPublicReadPathsStillRequireAuthentication(String path) throws Exception {
        mockMvc.perform(post(path)
                .with(csrf())
                .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"));
    }

    static Stream<Arguments> publicReadPaths() {
        return Stream.of(
                Arguments.of("/search"),
                Arguments.of("/feed/trending"),
                Arguments.of("/artists/" + ARTIST_ID),
                Arguments.of("/concerts/" + CONCERT_ID));
    }
}
