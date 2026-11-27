package com.assurcy2000.ai;

import java.io.IOException;
import java.util.List;

public interface AiClient {

    record Turn(String role, String content) { }

    String complete(String systemPrompt, List<Turn> history, String question) throws IOException;

    class NotConfiguredException extends RuntimeException {
        public NotConfiguredException(String message) { super(message); }
    }
}