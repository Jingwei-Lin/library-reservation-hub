package com.comp5619.libraryreservationreviewhub.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.extern.slf4j.Slf4j;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 基于文件持久化的对话记忆
 * <p>
 * One file per conversation, {@code <conversationId>.kryo}. The id must be a UUID (the front end makes one per
 * page with {@code uuidv4()}), and the resolved file must stay inside the base directory, so a forged id such as
 * {@code ../../x} can't read or write files elsewhere. The file name and the Kryo format are unchanged, so
 * conversations saved before these checks are still read.
 * <p>
 * A conversation keeps only its last {@code maxMessages} messages (20 by default), in the file and in what the
 * model is sent, and the kept part always starts with a user message. Conversations nobody has touched for a while
 * are deleted by {@link #deleteIdleConversations}.
 */
@Slf4j
public class FileBasedChatMemory implements ChatMemory {

    /** How many messages a conversation keeps (step 5 plan D2) */
    public static final int DEFAULT_MAX_MESSAGES = 20;

    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private static final String SUFFIX = ".kryo";

    private final String BASE_DIR;
    private final int maxMessages;

    /** Kryo is not thread-safe, so each thread gets its own instance; the settings (and so the format) are unchanged. */
    private static final ThreadLocal<Kryo> KRYO = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);
        // 设置实例化策略
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
        return kryo;
    });

    /**
     * Reads and writes of one conversation are serialised, so add() (read, append, write) can't lose messages
     * and a read never sees a half-written file. Striped: a fixed set of locks, picked by the id's hash.
     */
    private static final Object[] LOCKS = new Object[64];

    static {
        for (int i = 0; i < LOCKS.length; i++) {
            LOCKS[i] = new Object();
        }
    }

    private static Object lockFor(String conversationId) {
        return LOCKS[Math.floorMod(conversationId.hashCode(), LOCKS.length)];
    }

    // 构造对象时，指定文件保存目录
    public FileBasedChatMemory(String dir) {
        this(dir, DEFAULT_MAX_MESSAGES);
    }

    public FileBasedChatMemory(String dir, int maxMessages) {
        if (maxMessages < 1) {
            throw new IllegalArgumentException("maxMessages must be at least 1");
        }
        this.BASE_DIR = dir;
        this.maxMessages = maxMessages;
        File baseDir = new File(dir);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
    }

    /**
     * Whether {@code conversationId} is a UUID, the only form of id this memory accepts.
     */
    public static boolean isValidConversationId(String conversationId) {
        return conversationId != null && UUID_PATTERN.matcher(conversationId).matches();
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        File file = getConversationFile(conversationId);
        synchronized (lockFor(conversationId)) {
            List<Message> conversationMessages = readConversation(file);
            conversationMessages.addAll(messages);
            saveConversation(file, window(conversationMessages));
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        File file = getConversationFile(conversationId);
        synchronized (lockFor(conversationId)) {
            return window(readConversation(file));
        }
    }

    @Override
    public void clear(String conversationId) {
        File file = getConversationFile(conversationId);
        synchronized (lockFor(conversationId)) {
            if (file.exists()) {
                file.delete();
            }
        }
    }

    /**
     * Deletes the conversations not written for longer than {@code maxIdle}. Each file is checked again under its
     * conversation's lock, so a conversation that was just written is kept. Returns how many were deleted.
     */
    public int deleteIdleConversations(Duration maxIdle) {
        File[] files = new File(BASE_DIR).listFiles((d, name) -> name.endsWith(SUFFIX));
        if (files == null) {
            return 0;
        }
        long cutoff = System.currentTimeMillis() - maxIdle.toMillis();
        int deleted = 0;
        for (File file : files) {
            String id = file.getName().substring(0, file.getName().length() - SUFFIX.length());
            if (!isValidConversationId(id)) {
                continue;   // not one of ours
            }
            synchronized (lockFor(id)) {
                if (file.lastModified() < cutoff && file.delete()) {
                    deleted++;
                }
            }
        }
        return deleted;
    }

    /**
     * The last {@code maxMessages} messages, starting at a user message: the model should never get an answer
     * without the question it answers.
     */
    private List<Message> window(List<Message> messages) {
        int from = Math.max(0, messages.size() - maxMessages);
        while (from < messages.size() && messages.get(from).getMessageType() != MessageType.USER) {
            from++;
        }
        return new ArrayList<>(messages.subList(from, messages.size()));
    }

    /** An unreadable file (for example cut short by a crash) starts the conversation afresh instead of failing it */
    private List<Message> readConversation(File file) {
        List<Message> messages = new ArrayList<>();
        if (file.exists()) {
            try (Input input = new Input(new FileInputStream(file))) {
                messages = KRYO.get().readObject(input, ArrayList.class);
            } catch (IOException | KryoException e) {
                log.warn("Chat memory file {} could not be read; the conversation starts afresh", file.getName(), e);
                messages = new ArrayList<>();
            }
        }
        return messages;
    }

    private void saveConversation(File file, List<Message> messages) {
        try (Output output = new Output(new FileOutputStream(file))) {
            KRYO.get().writeObject(output, messages);
        } catch (IOException | KryoException e) {
            log.warn("Chat memory file {} could not be written", file.getName(), e);
        }
    }

    private File getConversationFile(String conversationId) {
        if (!isValidConversationId(conversationId)) {
            throw new IllegalArgumentException("Invalid conversation id");
        }
        File file = new File(BASE_DIR, conversationId + SUFFIX);
        try {
            String baseDir = new File(BASE_DIR).getCanonicalPath() + File.separator;
            if (!file.getCanonicalPath().startsWith(baseDir)) {
                throw new IllegalArgumentException("Invalid conversation id");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return file;
    }
}
