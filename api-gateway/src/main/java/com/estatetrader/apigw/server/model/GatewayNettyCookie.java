package com.estatetrader.apigw.server.model;

import com.estatetrader.apigw.core.contracts.GatewayCookie;
import io.netty.handler.codec.http.cookie.Cookie;

public class GatewayNettyCookie implements Cookie, GatewayCookie {

    private final Cookie cookie;

    public GatewayNettyCookie(Cookie cookie) {
        this.cookie = cookie;
    }

    /**
     * Returns the name of this {@link GatewayCookie}.
     *
     * @return The name of this {@link GatewayCookie}
     */
    @Override
    public String name() {
        return cookie.name();
    }

    /**
     * Returns the value of this {@link GatewayCookie}.
     *
     * @return The value of this {@link GatewayCookie}
     */
    @Override
    public String value() {
        return cookie.value();
    }

    /**
     * Sets the value of this {@link GatewayCookie}.
     *
     * @param value The value to set
     */
    @Override
    public void setValue(String value) {
        cookie.setValue(value);
    }

    /**
     * Returns true if the raw value of this {@link GatewayCookie},
     * was wrapped with double quotes in original Set-Cookie header.
     *
     * @return If the value of this {@link GatewayCookie} is to be wrapped
     */
    @Override
    public boolean wrap() {
        return cookie.wrap();
    }

    /**
     * Sets true if the value of this {@link GatewayCookie}
     * is to be wrapped with double quotes.
     *
     * @param wrap true if wrap
     */
    @Override
    public void setWrap(boolean wrap) {
        cookie.setWrap(wrap);
    }

    /**
     * Returns the domain of this {@link GatewayCookie}.
     *
     * @return The domain of this {@link GatewayCookie}
     */
    @Override
    public String domain() {
        return cookie.domain();
    }

    /**
     * Sets the domain of this {@link GatewayCookie}.
     *
     * @param domain The domain to use
     */
    @Override
    public void setDomain(String domain) {
        cookie.setDomain(domain);
    }

    /**
     * Returns the path of this {@link GatewayCookie}.
     *
     * @return The {@link GatewayCookie}'s path
     */
    @Override
    public String path() {
        return cookie.path();
    }

    /**
     * Sets the path of this {@link GatewayCookie}.
     *
     * @param path The path to use for this {@link GatewayCookie}
     */
    @Override
    public void setPath(String path) {
        cookie.setPath(path);
    }

    /**
     * Returns the maximum age of this {@link GatewayCookie} in seconds
     *
     * @return The maximum age of this {@link GatewayCookie}
     */
    @Override
    public long maxAge() {
        return cookie.maxAge();
    }

    /**
     * Sets the maximum age of this {@link GatewayCookie} in seconds.
     * If an age of {@code 0} is specified, this {@link GatewayCookie} will be
     * automatically removed by browser because it will expire immediately.
     * browser is closed.
     *
     * @param maxAge The maximum age of this {@link GatewayCookie} in seconds
     */
    @Override
    public void setMaxAge(long maxAge) {
        cookie.setMaxAge(maxAge);
    }

    /**
     * Checks to see if this {@link GatewayCookie} is secure
     *
     * @return True if this {@link GatewayCookie} is secure, otherwise false
     */
    @Override
    public boolean isSecure() {
        return cookie.isSecure();
    }

    /**
     * Sets the security getStatus of this {@link GatewayCookie}
     *
     * @param secure True if this {@link GatewayCookie} is to be secure, otherwise false
     */
    @Override
    public void setSecure(boolean secure) {
        cookie.setSecure(secure);
    }

    /**
     * Checks to see if this {@link GatewayCookie} can only be accessed via HTTP.
     * If this returns true, the {@link GatewayCookie} cannot be accessed through
     * client side script - But only if the browser supports it.
     * For more information, please look <a href="http://www.owasp.org/index.php/HTTPOnly">here</a>
     *
     * @return True if this {@link GatewayCookie} is HTTP-only or false if it isn't
     */
    @Override
    public boolean isHttpOnly() {
        return cookie.isHttpOnly();
    }

    /**
     * Determines if this {@link GatewayCookie} is HTTP only.
     * If set to true, this {@link GatewayCookie} cannot be accessed by a client
     * side script. However, this works only if the browser supports it.
     * For for information, please look
     * <a href="http://www.owasp.org/index.php/HTTPOnly">here</a>.
     *
     * @param httpOnly True if the {@link GatewayCookie} is HTTP only, otherwise false.
     */
    @Override
    public void setHttpOnly(boolean httpOnly) {
        cookie.setHttpOnly(httpOnly);
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     *
     * <p>The implementor must ensure <tt>sgn(x.compareTo(y)) ==
     * -sgn(y.compareTo(x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
     * implies that <tt>x.compareTo(y)</tt> must throw an exception iff
     * <tt>y.compareTo(x)</tt> throws an exception.)
     *
     * <p>The implementor must also ensure that the relation is transitive:
     * <tt>(x.compareTo(y)&gt;0 &amp;&amp; y.compareTo(z)&gt;0)</tt> implies
     * <tt>x.compareTo(z)&gt;0</tt>.
     *
     * <p>Finally, the implementor must ensure that <tt>x.compareTo(y)==0</tt>
     * implies that <tt>sgn(x.compareTo(z)) == sgn(y.compareTo(z))</tt>, for
     * all <tt>z</tt>.
     *
     * <p>It is strongly recommended, but <i>not</i> strictly required that
     * <tt>(x.compareTo(y)==0) == (x.equals(y))</tt>.  Generally speaking, any
     * class that implements the <tt>Comparable</tt> interface and violates
     * this condition should clearly indicate this fact.  The recommended
     * language is "Note: this class has a natural ordering that is
     * inconsistent with equals."
     *
     * <p>In the foregoing description, the notation
     * <tt>sgn(</tt><i>expression</i><tt>)</tt> designates the mathematical
     * <i>signum</i> function, which is defined to return one of <tt>-1</tt>,
     * <tt>0</tt>, or <tt>1</tt> according to whether the value of
     * <i>expression</i> is negative, zero or positive.
     *
     * @param o the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object.
     * @throws NullPointerException if the specified object is null
     * @throws ClassCastException   if the specified object's type prevents it
     *                              from being compared to this object.
     */
    @Override
    public int compareTo(Cookie o) {
        return cookie.compareTo(o);
    }
}
