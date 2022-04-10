package com.estatetrader.entity;

import java.io.Serializable;

/**
 * Created by sunji on 2014/7/24.
 * Add private String service.
 */
public abstract class AbstractReturnCode implements Serializable {

    private       String name;
    private final String desc;
    private final int    code;

    private       String             service;
    private final AbstractReturnCode display;

    /**
     * 初始化一个对外暴露的ReturnCode(用于客户端异常处理)
     */
    public AbstractReturnCode(String desc, int code) {
        this.desc = desc;
        this.code = code;
        this.display = this;
    }

    /**
     * 初始化一个不对外暴露的ReturnCode(仅用于服务端数据分析)
     */
    public AbstractReturnCode(int code, AbstractReturnCode displayAs) {
        this.desc = null;
        this.code = code;
        this.display = displayAs;
    }

    public String getDesc() {
        return desc;
    }
    public int getCode() {
        return code;
    }
    public AbstractReturnCode getDisplay() {
        return display;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getService() {
        return service;
    }
    public void setService(String service) {
        this.service = service;
    }

    /**
     * Returns a string representation of the object. In general, the
     * {@code toString} method returns a string that
     * "textually represents" this object. The result should
     * be a concise but informative representation that is easy for a
     * person to read.
     * It is recommended that all subclasses override this method.
     * <p>
     * The {@code toString} method for class {@code Object}
     * returns a string consisting of the name of the class of which the
     * object is an instance, the at-sign character `{@code @}', and
     * the unsigned hexadecimal representation of the hash code of the
     * object. In other words, this method returns a string equal to the
     * value of:
     * <blockquote>
     * <pre>
     * getClass().getName() + '@' + Integer.toHexString(hashCode())
     * </pre></blockquote>
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return String.format("%d[%s]", code, name);
    }
}
