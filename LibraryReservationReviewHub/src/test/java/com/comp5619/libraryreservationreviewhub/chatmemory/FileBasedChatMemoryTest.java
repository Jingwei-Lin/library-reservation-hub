package com.comp5619.libraryreservationreviewhub.chatmemory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileBasedChatMemoryTest {

    /** Written by FileBasedChatMemory as it was before the id checks (commit d09cd3d), kept as a test resource. */
    private static final String LEGACY_ID = "0b5f8c3e-6d1a-4f2b-9c7e-3a8d2e1f4b6c";

    @TempDir
    Path dir;

    @Test
    void aConversationSavedBeforeTheChecksIsStillRead() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/chat-memory/" + LEGACY_ID + ".kryo")) {
            Files.copy(in, dir.resolve(LEGACY_ID + ".kryo"));
        }
        List<Message> messages = new FileBasedChatMemory(dir.toString()).get(LEGACY_ID);

        assertThat(messages).hasSize(2);
        assertThat(messages.get(0)).isInstanceOf(UserMessage.class);
        assertThat(messages.get(0).getText()).endsWith("[Message]: Recommend a fantasy book");
        assertThat(messages.get(1)).isInstanceOf(AssistantMessage.class);
        assertThat(messages.get(1).getText()).isEqualTo("1. **\"The Hobbit\"** by J.R.R. Tolkien");
    }

    @Test
    void aUuidConversationIsSavedAndReadBack() {
        FileBasedChatMemory memory = new FileBasedChatMemory(dir.toString());
        String id = UUID.randomUUID().toString();
        memory.add(id, List.of(new UserMessage("hello")));
        memory.add(id, List.of(new AssistantMessage("hi")));

        assertThat(memory.get(id)).extracting(Message::getText).containsExactly("hello", "hi");
        assertThat(dir.resolve(id + ".kryo")).exists();
    }

    @ParameterizedTest(name = "\"{0}\" is refused")
    @ValueSource(strings = {"../outside", "../../etc/x", "..\\outside", "sub/" + LEGACY_ID, "default", ""})
    void anIdThatIsNotAUuidCannotReadWriteOrDelete(String id) throws Exception {
        Path base = Files.createDirectory(dir.resolve("chat-memory"));
        Path outside = Files.writeString(dir.resolve("outside.kryo"), "not chat memory");
        FileBasedChatMemory memory = new FileBasedChatMemory(base.toString());

        assertThatThrownBy(() -> memory.get(id)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> memory.add(id, List.of(new UserMessage("x")))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> memory.clear(id)).isInstanceOf(IllegalArgumentException.class);

        assertThat(outside).hasContent("not chat memory");
        try (var files = Files.list(dir)) {
            assertThat(files).containsExactlyInAnyOrder(base, outside);
        }
    }
}
