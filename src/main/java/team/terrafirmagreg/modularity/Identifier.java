package team.terrafirmagreg.modularity;

import java.util.Objects;
import java.util.regex.Pattern;

public record Identifier(String domain, String path) {
    private static final Pattern DOMAIN_CHARS = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH_CHARS = Pattern.compile("[a-z0-9_./-]+");

    public Identifier {
        Objects.requireNonNull(domain, "domain");
        Objects.requireNonNull(path, "path");
        if (!DOMAIN_CHARS.matcher(domain).matches()) {
            throw new IllegalArgumentException("Identifier domain '" + domain + "' must match [a-z0-9_.-]+");
        }
        if (!PATH_CHARS.matcher(path).matches()) {
            throw new IllegalArgumentException("Identifier path '" + path + "' must match [a-z0-9_./-]+");
        }
    }

    public static Identifier of(String domain, String path) {
        return new Identifier(domain, path);
    }

    public static Identifier parse(String text, String defaultDomain) {
        Objects.requireNonNull(text, "text");
        int sep = text.indexOf(':');
        if (sep < 0) {
            return new Identifier(defaultDomain, text);
        }
        return new Identifier(text.substring(0, sep), text.substring(sep + 1));
    }

    public Identifier append(String suffix) {
        return new Identifier(domain, path + suffix);
    }

    @Override
    public String toString() {
        return domain + ":" + path;
    }
}
