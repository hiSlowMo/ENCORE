package com.encore.service;

import com.encore.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CredentialPolicyTest {
    @Test
    void rejectsEveryEmbeddedIsoControlInDisplayNames() {
        // Keep these ranges explicit: they are the cross-language contract,
        // not a filter derived by executing the implementation under test.
        for (int code = 0; code <= 0x9f; code++) {
            if (code > 0x1f && code < 0x7f) {
                continue;
            }
            String value = "A" + (char) code + "B";
            assertThatThrownBy(() -> CredentialPolicy.normalizeDisplayName(value))
                    .as("embedded U+%04X", code)
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("昵称不能包含控制字符");
        }
    }

    @Test
    void acceptsNonControlNeighborsAndUnicodeDisplayNames() {
        for (int code : new int[]{0x20, 0x21, 0x7e, 0xa0, 0xa1, 0x4e2d, 0x2028, 0x2029, 0x1f600, 0x1f680}) {
            String value = "A" + new String(Character.toChars(code)) + "B";
            assertThat(CredentialPolicy.normalizeDisplayName(value)).as("U+%04X", code).isEqualTo(value);
        }
    }

    @Test
    void retainsDisplayNameLengthAndTrimmingRules() {
        for (String invalid : new String[]{"", "A", "A".repeat(33), "   "}) {
            assertThatThrownBy(() -> CredentialPolicy.normalizeDisplayName(invalid))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("昵称需为 2 到 32 个字符");
        }
        assertThat(CredentialPolicy.normalizeDisplayName("AB")).isEqualTo("AB");
        assertThat(CredentialPolicy.normalizeDisplayName("A".repeat(32))).isEqualTo("A".repeat(32));
        assertThat(CredentialPolicy.normalizeDisplayName("  Viewer  ")).isEqualTo("Viewer");
    }
}
