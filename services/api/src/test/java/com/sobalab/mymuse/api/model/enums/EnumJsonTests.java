package com.sobalab.mymuse.api.model.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sobalab.mymuse.api.dto.UserDto;
import com.sobalab.mymuse.api.model.User;

import tools.jackson.databind.json.JsonMapper;

class EnumJsonTests {
    private final JsonMapper mapper = JsonMapper.builder().build();

    static Stream<Enum<?>> values() {
        return Stream.of(CountryCode.values(), LinkPlatform.values(), OAuthProvider.values(),
                SupportedLocale.values(), VenueType.values()).flatMap(Stream::of);
    }

    @ParameterizedTest
    @MethodSource("values")
    void usesDeclaredValueAndRejectsUnknownValues(Enum<?> value) throws ReflectiveOperationException {
        String expected = (String) value.getClass().getMethod("getValue").invoke(value);
        String json = mapper.writeValueAsString(value);
        assertThat(json).isEqualTo("\"" + expected + "\"");
        assertThat(mapper.readValue(json, value.getDeclaringClass())).isEqualTo(value);
        assertThat(mapper.readValue("null", value.getDeclaringClass())).isNull();
        assertThatThrownBy(() -> mapper.readValue("\"unsupported\"", value.getDeclaringClass()))
                .isInstanceOf(tools.jackson.databind.exc.InvalidFormatException.class);
    }

    @Test
    void userDtoUsesLanguageCode() {
        User user = User.builder().name("Test").build();
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", java.util.UUID.randomUUID());
        assertThat(UserDto.from(user).preferredLocale()).isEqualTo("ko");
    }
}
