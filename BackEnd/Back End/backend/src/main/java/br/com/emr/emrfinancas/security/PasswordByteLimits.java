package br.com.emr.emrfinancas.security;

/** BCrypt's 72-byte limit, without allocating or truncating an UTF-8 representation. */
public final class PasswordByteLimits {
    private PasswordByteLimits() {}

    public static boolean isAcceptableForBcrypt(CharSequence raw) {
        if (raw == null || raw.length() == 0) {
            return false;
        }
        int bytes = 0;
        for (int i = 0; i < raw.length(); i++) {
            char character = raw.charAt(i);
            if (Character.isHighSurrogate(character)) {
                if (i + 1 >= raw.length() || !Character.isLowSurrogate(raw.charAt(i + 1))) {
                    return false;
                }
                i++;
                bytes += 4;
            } else if (Character.isLowSurrogate(character)) {
                return false;
            } else {
                bytes += character <= 0x7f ? 1 : character <= 0x7ff ? 2 : 3;
            }
            if (bytes > 72) {
                return false;
            }
        }
        return true;
    }
}
