package com.zxl.chatbase.opencode.node;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum OpencodeNodeCommand {
    HEALTH("opencode.health"),
    SESSION_LIST("opencode.session.list"),
    SESSION_GET("opencode.session.get"),
    SESSION_CREATE("opencode.session.create"),
    PROMPT_SUBMIT("opencode.prompt.submit"),
    MESSAGE_LIST("opencode.message.list"),
    QUESTION_LIST("opencode.question.list"),
    QUESTION_REPLY("opencode.question.reply");

    private final String value;

    OpencodeNodeCommand(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static Set<String> valuesSet() {
        return Arrays.stream(values()).map(OpencodeNodeCommand::value).collect(Collectors.toUnmodifiableSet());
    }

    public static boolean isAllowed(String value) {
        return valuesSet().contains(value);
    }
}
