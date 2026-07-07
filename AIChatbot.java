import java.io.*;
import java.util.*;

/**
 * CodeAlpha Java Programming Internship - Task 3
 * Artificial Intelligence Chatbot (Console)
 *
 * A rule-based chatbot with lightweight NLP: input is tokenized and
 * cleaned of stop-words, then matched against a knowledge base of
 * FAQ entries using keyword-overlap scoring (a simple bag-of-words
 * similarity measure) rather than plain exact-string matching.
 *
 * The bot can also "learn": if it doesn't understand a question, it
 * asks the user to teach it the correct answer, and persists that new
 * Q&A pair to knowledge_base.txt so it remembers it in future sessions.
 */
public class AIChatbot {

    private static final String KNOWLEDGE_FILE = "knowledge_base.txt";
    private static final String CHAT_LOG_FILE = "chat_log.txt";

    private final KnowledgeBase knowledgeBase = new KnowledgeBase();
    private final NlpProcessor nlp = new NlpProcessor();
    private final Scanner scanner = new Scanner(System.in);
    private final List<String> transcript = new ArrayList<>();

    public static void main(String[] args) {
        AIChatbot bot = new AIChatbot();
        bot.knowledgeBase.loadDefaults();
        bot.knowledgeBase.loadFromFile(KNOWLEDGE_FILE);
        bot.run();
        bot.knowledgeBase.saveToFile(KNOWLEDGE_FILE);
        bot.appendTranscript();
        System.out.println("\nChatBot: Goodbye! (conversation logged to " + CHAT_LOG_FILE + ")");
    }

    private void run() {
        printBanner();
        say("Hi! I'm CodeAlphaBot, your FAQ assistant. Ask me anything, or type 'help' for topics. Type 'bye' to exit.");
        boolean running = true;
        while (running) {
            System.out.print("\nYou: ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;
            transcript.add("You: " + input);

            String lower = input.toLowerCase();
            if (lower.equals("bye") || lower.equals("exit") || lower.equals("quit")) {
                say("It was nice chatting with you. Have a great day!");
                running = false;
            } else if (lower.equals("help") || lower.equals("topics")) {
                showTopics();
            } else {
                respond(input);
            }
        }
    }

    private void printBanner() {
        System.out.println("=========================================");
        System.out.println("     AI CHATBOT (FAQ ASSISTANT)          ");
        System.out.println("            CodeAlpha                     ");
        System.out.println("=========================================");
    }

    private void showTopics() {
        say("I can help with topics like: " + String.join(", ", knowledgeBase.getTopics()) + ".");
    }

    private void respond(String userInput) {
        List<String> tokens = nlp.extractKeywords(userInput);
        MatchResult match = knowledgeBase.findBestMatch(tokens);

        if (match != null && match.score >= KnowledgeBase.CONFIDENCE_THRESHOLD) {
            say(match.entry.getResponse());
        } else {
            handleUnknown(userInput);
        }
    }

    private void handleUnknown(String userInput) {
        say("I'm not sure I understand that yet. Could you rephrase, or teach me the answer?");
        System.out.print("Type an answer to teach me (or press Enter to skip): ");
        String taughtAnswer = scanner.nextLine().trim();
        transcript.add("You (teach): " + taughtAnswer);
        if (!taughtAnswer.isEmpty()) {
            List<String> keywords = nlp.extractKeywords(userInput);
            knowledgeBase.addEntry(keywords, taughtAnswer);
            say("Thanks! I'll remember that for next time.");
        } else {
            say("No problem, let's move on.");
        }
    }

    private void say(String message) {
        System.out.println("ChatBot: " + message);
        transcript.add("ChatBot: " + message);
    }

    private void appendTranscript() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(CHAT_LOG_FILE, true))) {
            writer.println("----- Session: " + new Date() + " -----");
            for (String line : transcript) writer.println(line);
            writer.println();
        } catch (IOException e) {
            System.out.println("Error saving chat log: " + e.getMessage());
        }
    }

    // ===================== NLP Layer =====================

    /** Very small NLP helper: tokenization, stop-word removal, keyword extraction. */
    static class NlpProcessor {
        private static final Set<String> STOP_WORDS = Set.of(
                "a", "an", "the", "is", "are", "am", "was", "were", "be", "been",
                "do", "does", "did", "i", "you", "he", "she", "it", "we", "they",
                "to", "of", "in", "on", "at", "for", "and", "or", "but", "with",
                "what", "when", "where", "how", "why", "who", "can", "could",
                "would", "should", "will", "shall", "my", "me", "your", "please",
                "tell", "about", "this", "that"
        );

        List<String> tokenize(String text) {
            String cleaned = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
            List<String> tokens = new ArrayList<>();
            for (String word : cleaned.split("\\s+")) {
                if (!word.isBlank()) tokens.add(word);
            }
            return tokens;
        }

        /** Tokenize and strip stop-words to get the "meaningful" keywords. */
        List<String> extractKeywords(String text) {
            List<String> keywords = new ArrayList<>();
            for (String token : tokenize(text)) {
                if (!STOP_WORDS.contains(token)) keywords.add(token);
            }
            return keywords.isEmpty() ? tokenize(text) : keywords;
        }
    }

    // ===================== Knowledge Base =====================

    static class MatchResult {
        final FaqEntry entry;
        final double score;
        MatchResult(FaqEntry entry, double score) { this.entry = entry; this.score = score; }
    }

    /** A single FAQ entry: a set of trigger keywords mapped to a response. */
    static class FaqEntry {
        private final String topic;
        private final Set<String> keywords;
        private final String response;

        FaqEntry(String topic, Set<String> keywords, String response) {
            this.topic = topic;
            this.keywords = keywords;
            this.response = response;
        }

        String getTopic() { return topic; }
        Set<String> getKeywords() { return keywords; }
        String getResponse() { return response; }

        /**
         * Containment-style overlap score: the fraction of overlapping keywords
         * relative to the SMALLER of the two keyword sets. This works well for
         * short user queries matched against multi-keyword FAQ entries (unlike
         * plain Jaccard similarity, which unfairly penalizes short queries).
         */
        double scoreAgainst(List<String> inputTokens) {
            if (keywords.isEmpty() || inputTokens.isEmpty()) return 0;
            Set<String> uniqueInput = new LinkedHashSet<>(inputTokens);
            long overlap = uniqueInput.stream().filter(keywords::contains).count();
            int smaller = Math.min(keywords.size(), uniqueInput.size());
            return (double) overlap / smaller;
        }

        String toFileLine() {
            return topic + "|" + String.join(",", keywords) + "|" + response.replace("|", "/");
        }

        static FaqEntry fromFileLine(String line) {
            String[] parts = line.split("\\|", 3);
            if (parts.length < 3) return null;
            Set<String> kws = new LinkedHashSet<>(Arrays.asList(parts[1].split(",")));
            return new FaqEntry(parts[0], kws, parts[2]);
        }
    }

    static class KnowledgeBase {
        static final double CONFIDENCE_THRESHOLD = 0.5;
        private final List<FaqEntry> entries = new ArrayList<>();

        void loadDefaults() {
            add("greeting", Set.of("hello", "hi", "hey", "greetings"),
                    "Hello there! How can I help you today?");
            add("bot_identity", Set.of("name", "who", "you"),
                    "I'm CodeAlphaBot, a rule-based FAQ chatbot built in Java for the CodeAlpha internship.");
            add("internship_duration", Set.of("duration", "long", "internship", "weeks", "period"),
                    "CodeAlpha internships are typically flexible-duration, project-based programs — check your offer letter for exact dates.");
            add("certificate", Set.of("certificate", "certification", "completion", "verify", "qr"),
                    "Yes! CodeAlpha provides a QR-verified Completion Certificate once you finish the minimum required tasks.");
            add("tasks_required", Set.of("tasks", "how", "many", "complete", "minimum", "required"),
                    "You need to complete a minimum of 2 or 3 tasks from your domain's task list to be eligible for the certificate.");
            add("submission", Set.of("submit", "submission", "form", "upload", "github"),
                    "Upload your source code to a GitHub repo named CodeAlpha_ProjectName, then submit it via the form shared in your WhatsApp group.");
            add("linkedin_post", Set.of("linkedin", "post", "video", "share", "tag"),
                    "Post a short video explaining your project on LinkedIn, include your GitHub repo link, and tag @CodeAlpha.");
            add("contact", Set.of("contact", "email", "whatsapp", "support", "help", "reach"),
                    "You can reach CodeAlpha at services@codealpha.tech or via WhatsApp at +91 9336576683.");
            add("thanks", Set.of("thanks", "thank", "appreciate"),
                    "You're welcome! Happy to help.");
            add("mood", Set.of("how", "feeling", "doing"),
                    "I'm just a program, but I'm running smoothly! How can I assist you?");
            add("java_help", Set.of("java", "programming", "language", "learn"),
                    "Java is a versatile, object-oriented language — great for backend systems, Android apps, and enterprise software. What would you like to know?");
        }

        void add(String topic, Set<String> keywords, String response) {
            entries.add(new FaqEntry(topic, keywords, response));
        }

        void addEntry(List<String> keywords, String response) {
            String topic = "custom_" + (entries.size() + 1);
            entries.add(new FaqEntry(topic, new LinkedHashSet<>(keywords), response));
        }

        List<String> getTopics() {
            List<String> topics = new ArrayList<>();
            for (FaqEntry e : entries) {
                if (!e.getTopic().startsWith("custom_")) topics.add(e.getTopic());
            }
            return topics;
        }

        MatchResult findBestMatch(List<String> inputTokens) {
            FaqEntry best = null;
            double bestScore = 0;
            for (FaqEntry entry : entries) {
                double score = entry.scoreAgainst(inputTokens);
                if (score > bestScore) {
                    bestScore = score;
                    best = entry;
                }
            }
            return best == null ? null : new MatchResult(best, bestScore);
        }

        void saveToFile(String path) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(path))) {
                for (FaqEntry entry : entries) {
                    if (entry.getTopic().startsWith("custom_")) {
                        writer.println(entry.toFileLine());
                    }
                }
            } catch (IOException e) {
                System.out.println("Error saving knowledge base: " + e.getMessage());
            }
        }

        void loadFromFile(String path) {
            File file = new File(path);
            if (!file.exists()) return;
            int loaded = 0;
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    FaqEntry entry = FaqEntry.fromFileLine(line);
                    if (entry != null) {
                        entries.add(entry);
                        loaded++;
                    }
                }
            } catch (IOException e) {
                System.out.println("Error loading knowledge base: " + e.getMessage());
            }
            if (loaded > 0) System.out.println("(Loaded " + loaded + " previously learned answer(s).)");
        }
    }
}
