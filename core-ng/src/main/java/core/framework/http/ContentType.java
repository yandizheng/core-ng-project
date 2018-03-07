package core.framework.http;

import core.framework.util.ASCII;
import core.framework.util.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Optional;

/**
 * @author neo
 */
public final class ContentType {
    public static final ContentType TEXT_HTML = create("text/html", Charsets.UTF_8);
    public static final ContentType TEXT_CSS = create("text/css", Charsets.UTF_8);
    public static final ContentType TEXT_PLAIN = create("text/plain", Charsets.UTF_8);
    public static final ContentType TEXT_XML = create("text/xml", Charsets.UTF_8);
    public static final ContentType APPLICATION_JSON = create("application/json", Charsets.UTF_8);
    public static final ContentType APPLICATION_JAVASCRIPT = create("application/javascript", Charsets.UTF_8);
    public static final ContentType APPLICATION_OCTET_STREAM = create("application/octet-stream", null);
    public static final ContentType IMAGE_PNG = create("image/png", null);

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentType.class);

    // only cover common case, assume pattern is "media-type; charset=" or "multipart/form-data; boundary="
    public static ContentType parse(String contentType) {
        String mediaType = contentType;
        Charset charset = null;

        int firstSemicolon = contentType.indexOf(';');
        if (firstSemicolon > 0) {
            mediaType = contentType.substring(0, firstSemicolon);

            int charsetIndex = contentType.indexOf("charset=", firstSemicolon + 1);
            if (charsetIndex > 0) {
                charset = parseCharset(contentType.substring(charsetIndex + 8));
            }
        }

        return new ContentType(contentType, mediaType, charset);
    }

    private static Charset parseCharset(String charset) {
        try {
            return Charset.forName(charset);
        } catch (UnsupportedCharsetException e) {
            LOGGER.warn("ignore unsupported charset, charset={}", charset);
            return null;
        }
    }

    public static ContentType create(String mediaType, Charset charset) {
        String contentType = charset == null ? mediaType : mediaType + "; charset=" + ASCII.toLowerCase(charset.name());
        return new ContentType(contentType, mediaType, charset);
    }

    private final Charset charset;
    private final String contentType;
    private final String mediaType;

    private ContentType(String contentType, String mediaType, Charset charset) {
        this.contentType = contentType;
        this.mediaType = mediaType;
        this.charset = charset;
    }

    public String mediaType() {
        return mediaType;
    }

    public Optional<Charset> charset() {
        return Optional.ofNullable(charset);
    }

    @Override
    public String toString() {
        return contentType;
    }
}
