package com.estatetrader.apigw.core.contracts;

/**
 * 表示一个cookie
 */
public interface GatewayCookie {
    /**
     * Returns the name of this {@link GatewayCookie}.
     *
     * @return The name of this {@link GatewayCookie}
     */
    String name();

    /**
     * Returns the value of this {@link GatewayCookie}.
     *
     * @return The value of this {@link GatewayCookie}
     */
    String value();

    /**
     * Sets the value of this {@link GatewayCookie}.
     *
     * @param value The value to set
     */
    void setValue(String value);

    /**
     * Returns true if the raw value of this {@link GatewayCookie},
     * was wrapped with double quotes in original Set-Cookie header.
     *
     * @return If the value of this {@link GatewayCookie} is to be wrapped
     */
    boolean wrap();

    /**
     * Sets true if the value of this {@link GatewayCookie}
     * is to be wrapped with double quotes.
     *
     * @param wrap true if wrap
     */
    void setWrap(boolean wrap);

    /**
     * Returns the domain of this {@link GatewayCookie}.
     *
     * @return The domain of this {@link GatewayCookie}
     */
    String domain();

    /**
     * Sets the domain of this {@link GatewayCookie}.
     *
     * @param domain The domain to use
     */
    void setDomain(String domain);

    /**
     * Returns the path of this {@link GatewayCookie}.
     *
     * @return The {@link GatewayCookie}'s path
     */
    String path();

    /**
     * Sets the path of this {@link GatewayCookie}.
     *
     * @param path The path to use for this {@link GatewayCookie}
     */
    void setPath(String path);

    /**
     * Returns the maximum age of this {@link GatewayCookie} in seconds
     *
     * @return The maximum age of this {@link GatewayCookie}
     */
    long maxAge();

    /**
     * Sets the maximum age of this {@link GatewayCookie} in seconds.
     * If an age of {@code 0} is specified, this {@link GatewayCookie} will be
     * automatically removed by browser because it will expire immediately.
     * browser is closed.
     *
     * @param maxAge The maximum age of this {@link GatewayCookie} in seconds
     */
    void setMaxAge(long maxAge);

    /**
     * Checks to see if this {@link GatewayCookie} is secure
     *
     * @return True if this {@link GatewayCookie} is secure, otherwise false
     */
    boolean isSecure();

    /**
     * Sets the security getStatus of this {@link GatewayCookie}
     *
     * @param secure True if this {@link GatewayCookie} is to be secure, otherwise false
     */
    void setSecure(boolean secure);

    /**
     * Checks to see if this {@link GatewayCookie} can only be accessed via HTTP.
     * If this returns true, the {@link GatewayCookie} cannot be accessed through
     * client side script - But only if the browser supports it.
     * For more information, please look <a href="http://www.owasp.org/index.php/HTTPOnly">here</a>
     *
     * @return True if this {@link GatewayCookie} is HTTP-only or false if it isn't
     */
    boolean isHttpOnly();

    /**
     * Determines if this {@link GatewayCookie} is HTTP only.
     * If set to true, this {@link GatewayCookie} cannot be accessed by a client
     * side script. However, this works only if the browser supports it.
     * For for information, please look
     * <a href="http://www.owasp.org/index.php/HTTPOnly">here</a>.
     *
     * @param httpOnly True if the {@link GatewayCookie} is HTTP only, otherwise false.
     */
    void setHttpOnly(boolean httpOnly);
}
