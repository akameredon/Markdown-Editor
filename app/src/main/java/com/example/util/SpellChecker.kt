package com.example.util

import java.util.Locale

object SpellChecker {

    // Pre-built dictionary of common English words, markdown keywords, and technical terminology
    private val dictionary: Set<String> = setOf(
        "i", "you", "he", "she", "it", "we", "they", "me", "him", "her", "us", "them", "my", "your", "his", "its", "our", "their", "mine", "yours", "ours", "theirs",
        "a", "an", "the", "and", "but", "or", "yet", "so", "although", "because", "since", "unless", "while", "of", "at", "by", "for", "with", "about", "against",
        "between", "into", "through", "during", "before", "after", "above", "below", "to", "from", "up", "down", "in", "out", "on", "off", "over", "under", "again",
        "further", "then", "once", "am", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "having", "do", "does", "did", "doing", "can",
        "could", "shall", "should", "will", "would", "may", "might", "must", "about", "above", "accept", "according", "account", "across", "act", "action",
        "activities", "activity", "actually", "add", "address", "administration", "admit", "adult", "affect", "after", "again", "against", "age", "agency",
        "agent", "ago", "agree", "agreement", "ahead", "air", "all", "allow", "almost", "alone", "along", "already", "also", "although", "always", "among",
        "amount", "analysis", "animal", "another", "answer", "any", "anyone", "anything", "appear", "apple", "apply", "approach", "area", "argue", "arm",
        "around", "arrive", "art", "article", "artist", "as", "ask", "assume", "at", "attack", "attention", "author", "authority", "available", "avoid",
        "away", "baby", "back", "bad", "bag", "ball", "band", "bank", "bar", "base", "be", "beat", "beautiful", "because", "become", "bed", "before",
        "begin", "behavior", "behind", "believe", "benefit", "best", "better", "between", "beyond", "big", "bill", "billion", "bit", "black", "blood",
        "blue", "board", "body", "book", "born", "both", "box", "boy", "break", "bring", "brother", "budget", "build", "building", "business", "but",
        "buy", "by", "call", "camera", "campaign", "can", "candidate", "capital", "car", "card", "care", "career", "carry", "case", "catch", "cause",
        "cell", "center", "central", "century", "certain", "certainly", "chair", "challenge", "chance", "change", "character", "charge", "check",
        "chemical", "chest", "chicken", "child", "choice", "choose", "church", "citizen", "city", "civil", "claim", "class", "clear", "clearly",
        "close", "coach", "cold", "collection", "college", "color", "come", "commercial", "common", "community", "company", "compare", "computer",
        "concern", "condition", "conference", "congress", "consider", "consumer", "contain", "continue", "control", "copy", "corner", "cost",
        "could", "country", "couple", "course", "court", "cover", "create", "crime", "cultural", "culture", "cup", "current", "customer", "cut",
        "dark", "data", "daughter", "day", "dead", "deal", "death", "debate", "decade", "decide", "decision", "deep", "defense", "degree",
        "describe", "design", "despite", "detail", "determine", "develop", "development", "device", "difference", "different", "difficult",
        "dinner", "direction", "director", "discover", "discuss", "discussion", "disease", "do", "doctor", "dog", "domestic", "door", "double",
        "down", "draw", "dream", "dress", "drink", "drive", "drop", "drug", "during", "each", "early", "east", "easy", "eat", "economic",
        "economy", "edge", "education", "effect", "effort", "eight", "either", "election", "else", "employee", "end", "energy", "enjoy",
        "enough", "enter", "entire", "environment", "especially", "establish", "even", "evening", "event", "ever", "every", "everybody",
        "everyone", "everything", "evidence", "exactly", "example", "executive", "exist", "expectation", "experience", "expert", "explain",
        "eye", "face", "fact", "factor", "fail", "fall", "family", "far", "fast", "father", "fear", "federal", "feel", "feeling", "few",
        "field", "fight", "figure", "fill", "film", "final", "finally", "financial", "find", "fine", "finger", "finish", "fire", "firm",
        "first", "fish", "five", "floor", "fly", "focus", "follow", "food", "foot", "for", "force", "foreign", "forget", "form", "former",
        "forward", "four", "free", "friend", "from", "front", "full", "fund", "future", "game", "garden", "gas", "general", "generation",
        "gentle", "get", "girl", "give", "glass", "go", "goal", "good", "government", "great", "green", "ground", "group", "grow", "growth",
        "guest", "guide", "gun", "guy", "hair", "half", "hand", "hang", "happen", "happy", "hard", "have", "he", "head", "health", "hear",
        "heart", "heat", "heavy", "help", "her", "here", "herself", "high", "him", "himself", "his", "history", "hit", "hold", "home",
        "hope", "hospital", "hot", "hotel", "hour", "house", "how", "however", "huge", "human", "hundred", "husband", "idea", "identify",
        "if", "image", "imagine", "impact", "important", "improve", "in", "include", "including", "increase", "indeed", "indicate",
        "individual", "industry", "information", "inside", "instead", "interest", "interesting", "international", "interview", "into",
        "investment", "involve", "is", "issue", "it", "item", "its", "itself", "job", "join", "just", "keep", "key", "kid", "kill",
        "kind", "king", "kitchen", "know", "knowledge", "land", "language", "large", "last", "late", "later", "laugh", "law", "lawsuit",
        "lawyer", "lay", "lead", "leader", "learn", "least", "leave", "left", "leg", "legal", "less", "let", "letter", "level", "lie",
        "life", "light", "like", "likely", "line", "list", "listen", "little", "live", "local", "long", "look", "lose", "loss", "lot",
        "love", "low", "machine", "magazine", "main", "maintain", "major", "majority", "make", "male", "man", "manage", "management",
        "manager", "many", "map", "market", "marriage", "material", "matter", "may", "maybe", "me", "mean", "measure", "media",
        "medical", "meet", "meeting", "member", "memory", "mention", "message", "method", "middle", "might", "military", "million",
        "mind", "minute", "miss", "mission", "model", "modern", "moment", "money", "month", "more", "morning", "most", "mother",
        "mouth", "move", "movement", "movie", "mr", "mrs", "much", "music", "must", "my", "myself", "name", "nation", "national",
        "natural", "nature", "near", "nearly", "necessary", "need", "neighbor", "neighborhood", "neither", "nervous", "network",
        "never", "new", "news", "newspaper", "next", "nice", "night", "nine", "no", "nobody", "none", "nor", "north", "not", "note",
        "nothing", "notice", "now", "number", "occur", "of", "off", "offer", "office", "officer", "official", "often", "oh", "oil",
        "ok", "old", "on", "once", "one", "only", "onto", "open", "operation", "opportunity", "option", "or", "order", "organization",
        "other", "others", "our", "out", "outside", "over", "own", "owner", "pace", "pack", "page", "pain", "paint", "paper",
        "parent", "part", "participant", "particular", "particularly", "partner", "party", "pass", "past", "patient", "pattern",
        "pay", "peace", "people", "per", "perform", "performance", "perhaps", "period", "person", "personal", "phone", "physical",
        "pick", "picture", "piece", "place", "plan", "plant", "play", "player", "pm", "point", "police", "policy", "political",
        "politics", "poor", "popular", "population", "position", "positive", "possible", "power", "practice", "prepare", "president",
        "pressure", "pretty", "prevent", "price", "private", "probably", "problem", "process", "produce", "product", "production",
        "professional", "professor", "program", "project", "property", "protect", "prove", "provide", "public", "pull", "purpose",
        "push", "put", "quality", "question", "quickly", "quite", "race", "radio", "raise", "range", "rate", "rather", "reach",
        "read", "ready", "real", "reality", "realize", "really", "reason", "receive", "recent", "recently", "recognize", "record",
        "red", "reduce", "reflect", "region", "relate", "relationship", "religious", "remain", "remember", "remove", "report",
        "represent", "require", "research", "resource", "respond", "response", "responsibility", "rest", "result", "return",
        "reveal", "rich", "rid", "ride", "right", "ring", "rise", "risk", "road", "rock", "role", "room", "rule", "run",
        "safe", "same", "save", "say", "scene", "school", "science", "scientist", "score", "sea", "season", "seat", "second",
        "secret", "section", "security", "see", "seek", "seem", "sell", "send", "senior", "sense", "series", "serious", "serve",
        "service", "set", "seven", "several", "sex", "shake", "share", "she", "shoot", "short", "shot", "should", "shoulder",
        "show", "side", "sign", "significant", "silent", "silver", "similar", "simple", "simply", "since", "sing", "single",
        "sister", "site", "situation", "six", "size", "skill", "skin", "small", "smile", "so", "social", "society", "soft",
        "soldier", "some", "somebody", "someone", "something", "sometimes", "son", "song", "soon", "sort", "sound", "source",
        "south", "southern", "space", "speak", "special", "specific", "speech", "spend", "spirit", "split", "sport", "spring",
        "staff", "stage", "stand", "standard", "star", "start", "state", "statement", "station", "stay", "step", "still",
        "stock", "stop", "store", "story", "strategy", "street", "strong", "structure", "student", "study", "stuff", "style",
        "subject", "success", "successful", "such", "sudden", "suffer", "suggest", "summer", "support", "sure", "surface",
        "system", "table", "take", "talk", "task", "tax", "teach", "teacher", "team", "technology", "television", "tell",
        "ten", "term", "test", "text", "than", "thank", "that", "the", "their", "them", "themselves", "then", "theory",
        "there", "these", "they", "thing", "think", "third", "this", "those", "though", "thought", "thousand", "three",
        "through", "throughout", "throw", "thus", "ticket", "time", "to", "today", "together", "tonight", "too", "top",
        "total", "tough", "toward", "town", "toy", "track", "trade", "traditional", "training", "travel", "treat", "treatment",
        "tree", "trial", "trip", "trouble", "true", "truth", "try", "turn", "tv", "two", "type", "under", "understand",
        "unit", "universe", "university", "until", "up", "upon", "us", "use", "useful", "user", "usual", "usually",
        "value", "various", "very", "victim", "view", "violence", "visit", "voice", "vote", "wait", "walk", "wall",
        "want", "war", "warm", "warn", "wash", "watch", "water", "wave", "way", "we", "weapon", "wear", "week",
        "weight", "well", "west", "western", "what", "whatever", "when", "where", "whether", "which", "while",
        "white", "who", "whole", "whom", "whose", "why", "wide", "wife", "will", "win", "wind", "window",
        "wish", "with", "within", "without", "woman", "wonder", "word", "work", "worker", "world", "worry",
        "would", "write", "writer", "wrong", "yard", "yeah", "year", "yellow", "yes", "yesterday", "yet",
        "you", "young", "your", "yourself", "zone",

        // Markdown / Technical dictionary additions
        "markdown", "html", "css", "xml", "docx", "pdf", "kotlin", "android", "java", "jetpack", "compose", "viewmodel",
        "ksp", "room", "database", "dao", "gradle", "api", "json", "git", "github", "url", "http", "token", "sync",
        "simulated", "simulator", "workspace", "metadata", "device", "screenshot", "theme", "shortcuts", "editor",
        "template", "version", "backup", "restore", "snapshot", "bookmark", "notes", "calendar", "bullet", "checkable",
        "checkbox", "header", "italic", "bold", "sqlite", "syncing", "scaffold", "composable", "modifier", "snackbar"
    )

    /**
     * Checks if a word is spelled correctly, against standard and custom dictionaries.
     */
    fun isCorrect(word: String, customDictionary: Set<String>): Boolean {
        val clean = word.trim().lowercase(Locale.getDefault())
        if (clean.isEmpty()) return true
        
        // Handle numbers/numerical strings
        if (clean.all { it.isDigit() || it == '.' || it == ',' || it == '%' || it == '$' }) return true
        
        return dictionary.contains(clean) || customDictionary.contains(clean)
    }

    /**
     * Sanitizes raw text into individual words while retaining their index offsets.
     */
    fun extractWords(text: String): List<SpellingWord> {
        val wordsList = mutableListOf<SpellingWord>()
        val regex = Regex("[a-zA-Z']+")
        val matches = regex.findAll(text)
        
        for (match in matches) {
            val word = match.value
            // Strip trailing apostrophes (e.g. "users'" -> "users")
            val cleanWord = if (word.endsWith("'") && word.length > 1) {
                word.substring(0, word.length - 1)
            } else {
                word
            }
            wordsList.add(
                SpellingWord(
                    raw = cleanWord,
                    startIndex = match.range.first,
                    endIndex = match.range.first + cleanWord.length
                )
            )
        }
        return wordsList
    }

    /**
     * Generates top-3 replacement recommendations using Levenshtein distance.
     */
    fun getSuggestions(word: String, customDictionary: Set<String>, maxSuggestions: Int = 3): List<String> {
        val clean = word.trim().lowercase(Locale.getDefault())
        if (clean.isEmpty()) return emptyList()

        val fullDict = dictionary + customDictionary
        
        return fullDict.asSequence()
            .map { dictWord -> dictWord to levenshteinDistance(clean, dictWord) }
            .filter { (_, score) -> score <= 3 } // Only reasonably close words
            .sortedBy { (_, score) -> score }
            .map { (word, _) -> word }
            .take(maxSuggestions)
            .toList()
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) {
            dp[i][0] = i
        }
        for (j in 0..s2.length) {
            dp[0][j] = j
        }

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,        // Deletion
                    dp[i][j - 1] + 1,        // Insertion
                    dp[i - 1][j - 1] + cost   // Substitution
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}

data class SpellingWord(
    val raw: String,
    val startIndex: Int,
    val endIndex: Int
)

data class SpellingError(
    val word: SpellingWord,
    val suggestions: List<String>,
    val contextSentence: String
)
