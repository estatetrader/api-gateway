package com.estatetrader.apigw.core.test;

import com.estatetrader.apigw.core.utils.RsaExtensionTokenHelper;
import com.estatetrader.entity.CallerInfo;
import com.estatetrader.entity.ExtensionCallerInfo;
import com.estatetrader.util.AESTokenHelper;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class AESTokenHelperTest {

    private static final AESTokenHelper helper = new AESTokenHelper("6g2yIKq1qsTqWA4+dJWoaKBpCD04HHGll/wrn28QfqI=");

    private static final String TOKEN_H5_10 = "utk_N4TP+Ka8qYbN+/79HSoUX+vSkByoOozHygReuyFwZFPI1bSzxGiBsonusROOcR/vzeGYkgKwE0u+Rb/VdmjdOYPio68pjbEtB5gOdBu4gq42/+vKrGCbQVDe9iWF6rWV";

    private static final String UTK_TOKEN_SUPPLIER_11 = "utk_83z1KaquhvlgizwTqz969Ba3mP+VOL5mjDe5+RedE+Cna1to0kAbIjKZn5Z0Mt/GbxKOd5Pkxsl87Mvcf+kndI5sNsG9p2MriQbLvxjgZf8=";

    private static final String UTK_TOKEN_H5_11 = "utk_x528TnW109NAj52tCZGq/mGCeGeY8mQlt9ayghLlbOuIeeSYptCv+j837+mEDbb0WtcxGVPFCDmSZbYbCfdUVIwWWPPQ9yyAtnF1GqKna4E=";

    @Test
    public void testParseToken() {
        CallerInfo callerInfo = helper.parseToken("utk_Or1a1ZkYoQ1A5rdAdrWhAq/5OO9gHou90J3PTzflMqJm82NN+YqTveq/7CrLF2vLYwNy3rO4Oqfnkgiv2L8A/bZfueeCoueNorNMFQaMjKF7arHnWpdDPE2siDGa0jPkDYm9pco4P4dZhg6G2trwFQ==");
        assertNotNull(callerInfo);
    }

    @Test
    public void testParseToken_h5_10() {
        CallerInfo caller = helper.parseToken(TOKEN_H5_10);

        assertEquals(1, caller.appid);
        assertEquals(9, caller.securityLevel);
        assertEquals(351513, caller.uid);
        assertEquals(2119637902548L, caller.expire);
        assertEquals(-253539349498414L, caller.deviceId);
        assertEquals("13023201392", caller.phoneNumber);

        assertNull(caller.role);
        assertNull(caller.subsystem);
        assertEquals(Long.MIN_VALUE, caller.subSystemMainId);
    }

    @Test
    public void testGenToken_supplier_11() {

        CallerInfo caller = new CallerInfo();
        caller.securityLevel = 72;
        caller.uid = 123;
        caller.key = "abc123!@#".getBytes();
        caller.appid = 13;
        caller.deviceId = 1234567890;
        caller.expire = 123456789123L;

        caller.subsystem = "supplier";
        caller.role = "admin";
        caller.subSystemMainId = 456;



        String utk = helper.generateStringUserToken(caller);
        Assert.assertEquals(caller.role, helper.parseToken(utk).role);

        String dtk = helper.generateStringDeviceToken(caller);
        Assert.assertEquals(caller.deviceId, helper.parseToken(dtk).deviceId);
    }


    @Test
    public void testGenToken_h5_11() {

        CallerInfo caller = new CallerInfo();
        caller.securityLevel = 9;
        caller.uid = 351513;
        caller.key = "abc123!@#".getBytes();
        caller.appid = 1;
        caller.deviceId = -253539349498414L;
        caller.expire = 253539349498414L;
        caller.phoneNumber = "123454756756";

        String utk = helper.generateStringUserToken(caller);
        Assert.assertEquals(caller.uid, helper.parseToken(utk).uid);

        String dtk = helper.generateStringDeviceToken(caller);
        Assert.assertEquals(caller.deviceId, helper.parseToken(dtk).deviceId);
    }

    @Test
    public void testParseToken_supplier_11() {
        CallerInfo caller = helper.parseToken(UTK_TOKEN_SUPPLIER_11);

        assertEquals(13, caller.appid);
        assertEquals(72, caller.securityLevel);
        assertEquals(123, caller.uid);
        assertEquals(1234567890, caller.deviceId);
        assertEquals(123456789123L, caller.expire);
        assertEquals("admin", caller.role);
        assertEquals("supplier", caller.subsystem);
        assertEquals(456, caller.subSystemMainId);

    }

    @Test
    public void testParseToken_h5_11() {
        CallerInfo caller = helper.parseToken(UTK_TOKEN_H5_11);

        assertEquals(1, caller.appid);
        assertEquals(9, caller.securityLevel);
        assertEquals(351513, caller.uid);
        assertTrue(Arrays.equals("abc123!@#".getBytes(), caller.key));
        assertEquals(253539349498414L, caller.expire);
        assertEquals(-253539349498414L, caller.deviceId);
        assertEquals("123454756756", caller.phoneNumber);

        assertNull(caller.role);
        assertNull(caller.subsystem);
        assertEquals(Long.MIN_VALUE, caller.subSystemMainId);
    }

    @Test
    public void parseExtensionToken() {
        String token = "eyJhaWQiOjE1LCJlaWQiOjEwMTk0Nzg4NCwiZXhwaXJlIjoxNTQ4Mjc0MjEzNDYzLCJyb2xlIjoiMCIsInVpZCI6NTk5fQ==.GZaxcqrMfZHlxjySrABkdANQh6eXlQBoOVOPYbjrGLrkYGLWIwZXGZv5BaoheUsUl+F3eWTow8NLZxFlh34+YI9P08NeCN55QZB8uxVOxJ3mdnyI2xtGbBrEhyPaZwH3mlF0zJagmqPf9+LnqXkMiw+ouMGW6kuV4qUG4JO+nJc=";
        ExtensionCallerInfo caller = RsaExtensionTokenHelper.parseToken(token);
        assert  caller != null;
    }
}
