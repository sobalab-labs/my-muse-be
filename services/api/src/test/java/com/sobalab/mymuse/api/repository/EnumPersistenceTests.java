package com.sobalab.mymuse.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import com.sobalab.mymuse.api.model.Artist;
import com.sobalab.mymuse.api.model.ArtistLink;
import com.sobalab.mymuse.api.model.BaseEntity;
import com.sobalab.mymuse.api.model.User;
import com.sobalab.mymuse.api.model.UserIdentity;
import com.sobalab.mymuse.api.model.Venue;
import com.sobalab.mymuse.api.model.enums.CountryCode;
import com.sobalab.mymuse.api.model.enums.LinkPlatform;
import com.sobalab.mymuse.api.model.enums.OAuthProvider;
import com.sobalab.mymuse.api.model.enums.SupportedLocale;
import com.sobalab.mymuse.api.model.enums.VenueType;

class EnumPersistenceTests extends RepositoryQueryTestSupport {
    static Stream<Arguments> enumMappings() {
        return Stream.of(
                cases(User.class, "users", "preferredLocale", "preferred_locale", SupportedLocale.values()),
                cases(UserIdentity.class, "user_identities", "provider", "provider", OAuthProvider.values()),
                cases(ArtistLink.class, "artist_link", "platform", "platform", LinkPlatform.values()),
                cases(Venue.class, "venues", "type", "type", VenueType.values()),
                cases(Venue.class, "venues", "nation", "nation", CountryCode.values()),
                cases(Artist.class, "artists", "nation", "nation", CountryCode.values()))
                .flatMap(stream -> stream);
    }

    private static Stream<Arguments> cases(Class<?> type, String table, String field, String column, Enum<?>[] values) {
        return Stream.of(values).map(value -> Arguments.of(type, table, field, column, value));
    }

    @ParameterizedTest
    @MethodSource("enumMappings")
    void storesDeclaredValueAndReloadsEnum(Class<? extends BaseEntity> type, String table,
            String fieldName, String column, Enum<?> value) throws ReflectiveOperationException {
        BaseEntity entity;
        if (type == User.class) {
            entity = User.builder().name("Enum test").build();
        } else if (type == UserIdentity.class) {
            entity = UserIdentity.builder().user(user("Identity owner"))
                    .providerSubject("subject").build();
        } else if (type == ArtistLink.class) {
            entity = BeanUtils.instantiateClass(ArtistLink.class);
            field(entity, "artist", artist("Link owner"));
            field(entity, "url", "https://example.com");
        } else {
            entity = BeanUtils.instantiateClass(type);
            field(entity, "name", "Enum test");
        }
        field(entity, fieldName, value);
        persist(entity);
        flushAndClear();

        String expected = (String) value.getClass().getMethod("getValue").invoke(value);
        assertThat(em.createNativeQuery("select " + column + "::text from " + table + " where id = :id", String.class)
                .setParameter("id", entity.getId()).getSingleResult()).isEqualTo(expected);
        assertThat(ReflectionTestUtils.getField(em.find(type, entity.getId()), fieldName)).isEqualTo(value);
        assertThat(em.createQuery("select e from " + type.getSimpleName() + " e where e." + fieldName + " = :value", type)
                .setParameter("value", value).getResultList()).extracting(BaseEntity::getId).contains(entity.getId());
    }
}
