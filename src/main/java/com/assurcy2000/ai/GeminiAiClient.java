package com.assurcy2000.ai;

import com.google.genai.Client;
import com.google.genai.gaos.models.interactions.CreateModelInteraction;
import com.google.genai.gaos.models.interactions.GenerationConfig;
import com.google.genai.gaos.models.interactions.Interaction;
import com.google.genai.gaos.models.interactions.InteractionsInput;
import com.google.genai.gaos.models.interactions.Model;
import com.google.genai.gaos.models.interactions.ModelOutputStep;
import com.google.genai.gaos.models.interactions.TextContent;
import com.google.genai.gaos.models.interactions.ThinkingLevel;
import com.google.genai.gaos.models.operations.CreateInteractionRequestBody;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;


public class GeminiAiClient implements AiClient {

    private static final String DEFAULT_MODEL = "gemini-3.1-flash-lite";
    private static final Properties CONFIG = loadConfig();

    @Override
    public String complete(String systemPrompt, List<Turn> history, String question) throws IOException {
        String apiKey = setting("GEMINI_API_KEY", "gemini.api.key");
        if (apiKey == null) {
            throw new NotConfiguredException("Clé Gemini absente : définir GEMINI_API_KEY (variable d'environnement) ou gemini.api.key dans db.properties.");
        }
        String model = setting("GEMINI_MODEL", "gemini.model");

        try (Client client = Client.builder().apiKey(apiKey).build()) {
            CreateModelInteraction params = CreateModelInteraction.builder()
                    .model(Model.of(model != null ? model : DEFAULT_MODEL))
                    .systemInstruction(systemPrompt)
                    .input(InteractionsInput.of(transcript(history, question)))
                    .generationConfig(GenerationConfig.builder()
                            .thinkingLevel(ThinkingLevel.of("low"))
                            .build())
                    .build();

            Interaction interaction =
                    client.interactions.create(CreateInteractionRequestBody.of(params)).interaction().get();
            String text = extractText(interaction);
            if (text.isBlank()) {
                throw new IOException("Réponse sans texte (steps="
                        + interaction.steps().map(List::size).orElse(0) + ").");
            }
            return text;
        } catch (Exception e) {
            throw new IOException("Appel à Gemini échoué : " + e.getMessage(), e);
        }
    }

    private static String extractText(Interaction interaction) {
        StringBuilder sb = new StringBuilder();
        for (var step : interaction.steps().orElse(List.of())) {
            if (step instanceof ModelOutputStep out) {
                for (var content : out.content().orElse(List.of())) {
                    if (content instanceof TextContent text) sb.append(text.text().orElse(""));
                }
            }
        }
        return sb.length() > 0 ? sb.toString() : interaction.outputText().orElse("");
    }

    private static String transcript(List<Turn> history, String question) {
        StringBuilder sb = new StringBuilder();
        if (!history.isEmpty()) {
            sb.append("Historique de la conversation (du plus ancien au plus récent) :\n");
            for (Turn turn : history) {
                sb.append("user".equals(turn.role()) ? "Client : " : "Assistant : ")
                        .append(turn.content()).append('\n');
            }
            sb.append('\n');
        }
        return sb.append("Nouvelle question du client : ").append(question).toString();
    }

    private static String setting(String envVar, String propertyKey) {
        for (String name : new String[] {envVar, envVar.toLowerCase()}) {
            String env = System.getenv(name);
            if (env != null && !env.isBlank()) return env.trim();
        }
        String value = CONFIG.getProperty(propertyKey);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Properties loadConfig() {
        Properties p = new Properties();
        for (String file : new String[] {"db.properties", "config.properties"}) {
            try (InputStream in = GeminiAiClient.class.getClassLoader().getResourceAsStream(file)) {
                if (in != null) p.load(in);
            } catch (IOException e) {
            }
        }
        return p;
    }
}