package com.haembina.projectreports.l10n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MessageTest {

    @Test
    void findsEveryMessageInEveryLanguageTable() {
        for (Language language : Language.values()) {
            Map<String, String> table = Message.table(language);

            assertThat(table).containsKeys(Arrays.stream(Message.values()).map(Message::key).toArray(String[]::new));
        }
    }

    @Test
    void refusesATableMissingAMessage() {
        Map<String, String> table = Map.of(Message.PAGE_TITLE.key(), "Reports");

        assertThatThrownBy(() -> Message.checked(Language.ENGLISH, table))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageStartingWith("haembinaprojectreports.properties has no haembinaprojectreports.api.controller, ")
            .hasMessageNotContaining("page.title");
    }

    @Test
    void readsTheTableAsUtf8() {
        InputStream table = new ByteArrayInputStream(
            "haembinaprojectreports.page.title=Berichte – Übersicht\n".getBytes(StandardCharsets.UTF_8));

        assertThat(Message.read(table, Language.ENGLISH))
            .containsExactly(Map.entry("haembinaprojectreports.page.title", "Berichte – Übersicht"));
    }

    @Test
    void namesATableTheJarDoesNotHold() {
        assertThatThrownBy(() -> Message.read(null, Language.ENGLISH))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("The plugin jar holds no /org/sonar/l10n/haembinaprojectreports.properties");
    }

    @Test
    void reportsATableThatCannotBeRead() {
        InputStream broken = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("disk gone");
            }

            @Override
            public int read(byte[] buffer, int offset, int length) throws IOException {
                throw new IOException("disk gone");
            }
        };

        assertThatThrownBy(() -> Message.read(broken, Language.ENGLISH))
            .isInstanceOf(UncheckedIOException.class)
            .hasRootCauseMessage("disk gone");
    }

    @Test
    void fillsEachNumberedPlaceholderOnceAndLeavesTheRest() {
        assertThat(Message.REPORT_PULL_REQUEST_TITLED.text("{1}", "Fix $1 \\ paths"))
            .isEqualTo("Pull request {1} – Fix $1 \\ paths");
        assertThat(Message.REPORT_PULL_REQUEST_TITLED.text(7)).isEqualTo("Pull request 7 – {1}");
        assertThat(Message.SETTINGS_BRANCH_URL_DESCRIPTION.text())
            .endsWith("as in {repository}/src/branch/{branch}.");
    }

    @Test
    void readsTheLanguageItIsAskedFor() {
        assertThat(Message.HEADING_ISSUES.in(Language.ENGLISH, 10000)).isEqualTo("Issues (10000)");
        assertThat(Message.PAGE_TITLE.key()).isEqualTo("haembinaprojectreports.page.title");
    }

    @Test
    void registersEachLanguageWithItsCodeDirectionAndLocale() {
        assertThat(Language.DEFAULT).isSameAs(Language.ENGLISH);
        assertThat(Language.ENGLISH.code()).isEqualTo("en");
        assertThat(Language.ENGLISH.direction()).isEqualTo("ltr");
        assertThat(Language.ENGLISH.locale()).isEqualTo(Locale.ENGLISH);
        assertThat(Language.ENGLISH.table()).isEqualTo("haembinaprojectreports.properties");
    }
}
