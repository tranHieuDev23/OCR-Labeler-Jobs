package ocrlabeler.controllers;

import java.nio.file.Paths;

import io.github.cdimascio.dotenv.Dotenv;

public class Utils {
    private Utils() {
    }

    public static final Dotenv DOTENV;
    public static final String UPLOAD_DIRECTORY;

    static {
        DOTENV = Dotenv.configure().directory("./.env").ignoreIfMalformed().ignoreIfMissing().load();
        UPLOAD_DIRECTORY = DOTENV.get("UPLOADED_DIRECTORY");
    }

    public static String joinPath(String firstPart, String... others) {
        return Paths.get(firstPart, others).normalize().toAbsolutePath().toString();
    }
}
