package com.cloud.network.lb;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.cloud.api.command.user.loadbalancer.DeleteSslCertCmd;
import com.cloud.api.command.user.loadbalancer.UploadSslCertCmd;
import com.cloud.context.CallContext;
import com.cloud.dao.EntityManager;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.network.dao.LoadBalancerCertMapDao;
import com.cloud.network.dao.LoadBalancerCertMapVO;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.SslCertDao;
import com.cloud.network.dao.SslCertVO;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.TransactionLegacy;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class CertServiceTest {

    @Before
    public void setUp() {
        final Account account = new AccountVO("testaccount", 1, "networkdomain", (short) 0, UUID.randomUUID().toString());
        final UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
    }

    @After
    public void tearDown() {
        CallContext.unregister();
    }

    @Test
    /**
     * Given a certificate signed by a CA and a valid CA chain, upload should succeed
     */
    public void runUploadSslCertWithCAChain() throws Exception {
        Assume.assumeTrue(isOpenJdk() || isJCEInstalled());

        final TransactionLegacy txn = TransactionLegacy.open("runUploadSslCertWithCAChain");

        final String certFile = URLDecoder.decode(getClass().getResource("/certs/rsa_ca_signed.crt").getFile(), Charset.defaultCharset().name());
        final String keyFile = URLDecoder.decode(getClass().getResource("/certs/rsa_ca_signed.key").getFile(), Charset.defaultCharset().name());
        final String chainFile = URLDecoder.decode(getClass().getResource("/certs/root_chain.crt").getFile(), Charset.defaultCharset().name());

        final String cert = readFileToString(new File(certFile));
        final String key = readFileToString(new File(keyFile));
        final String chain = readFileToString(new File(chainFile));

        final CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(any(SslCertVO.class))).thenReturn(new SslCertVO());

        certService._accountDao = Mockito.mock(AccountDao.class);
        when(certService._accountDao.findByIdIncludingRemoved(anyLong())).thenReturn((AccountVO) account);

        //creating the command
        final UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        final Class<?> _class = uploadCmd.getClass().getSuperclass();

        final Field certField = _class.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        final Field keyField = _class.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        final Field chainField = _class.getDeclaredField("chain");
        chainField.setAccessible(true);
        chainField.set(uploadCmd, chain);

        certService.uploadSslCert(uploadCmd);
    }

    /**
     * JCE is known to be working fine without additional configuration in OpenJDK.
     * This checks if the tests are running in OpenJDK;
     *
     * @return true if openjdk environment
     */
    static boolean isOpenJdk() {
        //TODO: find a better way for OpenJDK detection
        return System.getProperty("java.home").toLowerCase().contains("openjdk");
    }

    /**
     * One can run the tests on Oracle JDK after installing JCE by specifying -Dcloudstack.jce.enabled=true
     *
     * @return true if the jce enable property was set to true
     */
    static boolean isJCEInstalled() {
        return Boolean.getBoolean("cloudstack.jce.enabled");
    }

    @Test
    /**
     * Given a Self-signed Certificate with encrypted key, upload should succeed
     */
    public void runUploadSslCertSelfSignedWithPassword() throws Exception {

        final TransactionLegacy txn = TransactionLegacy.open("runUploadSslCertSelfSignedWithPassword");

        final String certFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed_with_pwd.crt").getFile(), Charset.defaultCharset().name());
        final String keyFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed_with_pwd.key").getFile(), Charset.defaultCharset().name());
        final String password = "test";

        final String cert = readFileToString(new File(certFile));
        final String key = readFileToString(new File(keyFile));

        final CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(any(SslCertVO.class))).thenReturn(new SslCertVO());

        certService._accountDao = Mockito.mock(AccountDao.class);
        when(certService._accountDao.findByIdIncludingRemoved(anyLong())).thenReturn((AccountVO) account);

        //creating the command
        final UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        final Class<?> _class = uploadCmd.getClass().getSuperclass();

        final Field certField = _class.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        final Field keyField = _class.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        final Field passField = _class.getDeclaredField("password");
        passField.setAccessible(true);
        passField.set(uploadCmd, password);

        certService.uploadSslCert(uploadCmd);
    }

    @Test
    /**
     * Given a Self-signed Certificate with non-encrypted key, upload should succeed
     */
    public void runUploadSslCertSelfSignedNoPassword() throws Exception {

        final TransactionLegacy txn = TransactionLegacy.open("runUploadSslCertSelfSignedNoPassword");

        final String certFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed.crt").getFile(), Charset.defaultCharset().name());
        final String keyFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed.key").getFile(), Charset.defaultCharset().name());

        final String cert = readFileToString(new File(certFile));
        final String key = readFileToString(new File(keyFile));

        final CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(any(SslCertVO.class))).thenReturn(new SslCertVO());

        certService._accountDao = Mockito.mock(AccountDao.class);
        when(certService._accountDao.findByIdIncludingRemoved(anyLong())).thenReturn((AccountVO) account);

        //creating the command
        final UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        final Class<?> _class = uploadCmd.getClass().getSuperclass();

        final Field certField = _class.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        final Field keyField = _class.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        certService.uploadSslCert(uploadCmd);
    }

    @Test
    public void runUploadSslCertBadChain() throws IOException, IllegalAccessException, NoSuchFieldException {
        Assume.assumeTrue(isOpenJdk() || isJCEInstalled());

        final String certFile = URLDecoder.decode(getClass().getResource("/certs/rsa_ca_signed.crt").getFile(), Charset.defaultCharset().name());
        final String keyFile = URLDecoder.decode(getClass().getResource("/certs/rsa_ca_signed.key").getFile(), Charset.defaultCharset().name());
        final String chainFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed.crt").getFile(), Charset.defaultCharset().name());

        final String cert = readFileToString(new File(certFile));
        final String key = readFileToString(new File(keyFile));
        final String chain = readFileToString(new File(chainFile));

        final CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(any(SslCertVO.class))).thenReturn(new SslCertVO());

        //creating the command
        final UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        final Class<?> _class = uploadCmd.getClass().getSuperclass();

        final Field certField = _class.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        final Field keyField = _class.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        final Field chainField = _class.getDeclaredField("chain");
        chainField.setAccessible(true);
        chainField.set(uploadCmd, chain);

        try {
            certService.uploadSslCert(uploadCmd);
            fail("The chain given is not the correct chain for the certificate");
        } catch (final Exception e) {
            assertTrue(e.getMessage().contains("Invalid certificate chain"));
        }
    }

    @Test
    public void runUploadSslCertNoRootCert() throws IOException, IllegalAccessException, NoSuchFieldException {

        Assume.assumeTrue(isOpenJdk() || isJCEInstalled());

        final String certFile = URLDecoder.decode(getClass().getResource("/certs/rsa_ca_signed.crt").getFile(), Charset.defaultCharset().name());
        final String keyFile = URLDecoder.decode(getClass().getResource("/certs/rsa_ca_signed.key").getFile(), Charset.defaultCharset().name());
        final String chainFile = URLDecoder.decode(getClass().getResource("/certs/non_root.crt").getFile(), Charset.defaultCharset().name());

        final String cert = readFileToString(new File(certFile));
        final String key = readFileToString(new File(keyFile));
        final String chain = readFileToString(new File(chainFile));

        final CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(any(SslCertVO.class))).thenReturn(new SslCertVO());

        //creating the command
        final UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        final Class<?> _class = uploadCmd.getClass().getSuperclass();

        final Field certField = _class.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        final Field keyField = _class.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        final Field chainField = _class.getDeclaredField("chain");
        chainField.setAccessible(true);
        chainField.set(uploadCmd, chain);

        try {
            certService.uploadSslCert(uploadCmd);
            fail("Chain is given but does not link to the certificate");
        } catch (final Exception e) {
            assertTrue(e.getMessage().contains("Invalid certificate chain"));
        }
    }

    @Test
    public void runUploadSslCertBadPassword() throws IOException, IllegalAccessException, NoSuchFieldException {

        final String certFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed_with_pwd.crt").getFile(), Charset.defaultCharset().name());
        final String keyFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed_with_pwd.key").getFile(), Charset.defaultCharset().name());
        final String password = "bad_password";

        final String cert = readFileToString(new File(certFile));
        final String key = readFileToString(new File(keyFile));

        final CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(any(SslCertVO.class))).thenReturn(new SslCertVO());

        //creating the command
        final UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        final Class<?> _class = uploadCmd.getClass().getSuperclass();

        final Field certField = _class.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        final Field keyField = _class.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        final Field passField = _class.getDeclaredField("password");
        passField.setAccessible(true);
        passField.set(uploadCmd, password);

        try {
            certService.uploadSslCert(uploadCmd);
            fail("Given an encrypted private key with a bad password. Upload should fail.");
        } catch (final Exception e) {
            assertTrue(e.getMessage().contains("please check password and data"));
        }
    }

    @Test
    public void runUploadSslCertBadkeyPair() throws IOException, IllegalAccessException, NoSuchFieldException {
        // Reading appropritate files
        final String certFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed.crt").getFile(), Charset.defaultCharset().name());
        final String keyFile = URLDecoder.decode(getClass().getResource("/certs/non_root.key").getFile(), Charset.defaultCharset().name());

        final String cert = readFileToString(new File(certFile));
        final String key = readFileToString(new File(keyFile));

        final CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(any(SslCertVO.class))).thenReturn(new SslCertVO());

        //creating the command
        final UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        final Class<?> _class = uploadCmd.getClass().getSuperclass();

        final Field certField = _class.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        final Field keyField = _class.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        try {
            certService.uploadSslCert(uploadCmd);
        } catch (final Exception e) {
            assertTrue(e.getMessage().contains("Bad public-private key"));
        }
    }

    @Test
    public void runUploadSslCertBadkeyAlgo() throws IOException, IllegalAccessException, NoSuchFieldException {

        // Reading appropritate files
        final String certFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed.crt").getFile(), Charset.defaultCharset().name());
        final String keyFile = URLDecoder.decode(getClass().getResource("/certs/dsa_self_signed.key").getFile(), Charset.defaultCharset().name());

        final String cert = readFileToString(new File(certFile));
        final String key = readFileToString(new File(keyFile));

        final CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(any(SslCertVO.class))).thenReturn(new SslCertVO());

        //creating the command
        final UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        final Class<?> _class = uploadCmd.getClass().getSuperclass();

        final Field certField = _class.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        final Field keyField = _class.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        try {
            certService.uploadSslCert(uploadCmd);
            fail("Given a private key which has a different algorithm than the certificate, upload should fail");
        } catch (final Exception e) {
            assertTrue(e.getMessage().contains("Public and private key have different algorithms"));
        }
    }

    @Test
    public void runUploadSslCertExpiredCert() throws IOException, IllegalAccessException, NoSuchFieldException {

        // Reading appropritate files
        final String certFile = URLDecoder.decode(getClass().getResource("/certs/expired_cert.crt").getFile(), Charset.defaultCharset().name());
        final String keyFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed.key").getFile(), Charset.defaultCharset().name());

        final String cert = readFileToString(new File(certFile));
        final String key = readFileToString(new File(keyFile));

        final CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(any(SslCertVO.class))).thenReturn(new SslCertVO());

        //creating the command
        final UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        final Class<?> _class = uploadCmd.getClass().getSuperclass();

        final Field certField = _class.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        final Field keyField = _class.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        try {
            certService.uploadSslCert(uploadCmd);
            fail("Given an expired certificate, upload should fail");
        } catch (final Exception e) {
            assertTrue(e.getMessage().contains("Certificate expired"));
        }
    }

    @Test
    public void runUploadSslCertNotX509() throws IOException, IllegalAccessException, NoSuchFieldException {
        // Reading appropritate files
        final String certFile = URLDecoder.decode(getClass().getResource("/certs/non_x509_pem.crt").getFile(), Charset.defaultCharset().name());
        final String keyFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed.key").getFile(), Charset.defaultCharset().name());

        final String cert = readFileToString(new File(certFile));
        final String key = readFileToString(new File(keyFile));

        final CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(any(SslCertVO.class))).thenReturn(new SslCertVO());

        //creating the command
        final UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        final Class<?> _class = uploadCmd.getClass().getSuperclass();

        final Field certField = _class.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        final Field keyField = _class.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        try {
            certService.uploadSslCert(uploadCmd);
            fail("Given a Certificate which is not X509, upload should fail");
        } catch (final Exception e) {
            assertTrue(e.getMessage().contains("Expected X509 certificate"));
        }
    }

    @Test
    public void runUploadSslCertBadFormat() throws IOException, IllegalAccessException, NoSuchFieldException {

        // Reading appropritate files
        final String certFile = URLDecoder.decode(getClass().getResource("/certs/bad_format_cert.crt").getFile(), Charset.defaultCharset().name());
        final String keyFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed.key").getFile(), Charset.defaultCharset().name());

        final String cert = readFileToString(new File(certFile));
        final String key = readFileToString(new File(keyFile));

        final CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(any(SslCertVO.class))).thenReturn(new SslCertVO());

        //creating the command
        final UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        final Class<?> _class = uploadCmd.getClass().getSuperclass();

        final Field certField = _class.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        final Field keyField = _class.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        try {
            certService.uploadSslCert(uploadCmd);
            fail("Given a Certificate in bad format (Not PEM), upload should fail");
        } catch (final Exception e) {
            assertTrue(e.getMessage().contains("Invalid certificate format"));
        }
    }

    @Test
    /**
     * Delete with a valid Id should succeed
     */
    public void runDeleteSslCertValid() throws Exception {

        final TransactionLegacy txn = TransactionLegacy.open("runDeleteSslCertValid");

        final CertServiceImpl certService = new CertServiceImpl();
        final long certId = 1;

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.remove(anyLong())).thenReturn(true);
        when(certService._sslCertDao.findById(anyLong())).thenReturn(new SslCertVO());

        // a rule holding the cert

        certService._lbCertDao = Mockito.mock(LoadBalancerCertMapDao.class);
        when(certService._lbCertDao.listByCertId(anyLong())).thenReturn(null);

        //creating the command
        final DeleteSslCertCmd deleteCmd = new DeleteSslCertCmdExtn();
        final Class<?> _class = deleteCmd.getClass().getSuperclass();

        final Field certField = _class.getDeclaredField("id");
        certField.setAccessible(true);
        certField.set(deleteCmd, certId);

        certService.deleteSslCert(deleteCmd);
    }

    @Test
    public void runDeleteSslCertBoundCert() throws NoSuchFieldException, IllegalAccessException {

        final TransactionLegacy txn = TransactionLegacy.open("runDeleteSslCertBoundCert");

        final CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        final long certId = 1;

        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.remove(anyLong())).thenReturn(true);
        when(certService._sslCertDao.findById(anyLong())).thenReturn(new SslCertVO());

        // rule holding the cert
        certService._lbCertDao = Mockito.mock(LoadBalancerCertMapDao.class);

        final List<LoadBalancerCertMapVO> lbMapList = new ArrayList<>();
        lbMapList.add(new LoadBalancerCertMapVO());

        certService._lbCertDao = Mockito.mock(LoadBalancerCertMapDao.class);
        when(certService._lbCertDao.listByCertId(anyLong())).thenReturn(lbMapList);

        certService._entityMgr = Mockito.mock(EntityManager.class);
        when(certService._entityMgr.findById(eq(LoadBalancerVO.class), anyLong())).thenReturn(new LoadBalancerVO());

        //creating the command
        final DeleteSslCertCmd deleteCmd = new DeleteSslCertCmdExtn();
        final Class<?> _class = deleteCmd.getClass().getSuperclass();

        final Field certField = _class.getDeclaredField("id");
        certField.setAccessible(true);
        certField.set(deleteCmd, certId);

        try {
            certService.deleteSslCert(deleteCmd);
            fail("Delete with a cert id bound to a lb should fail");
        } catch (final Exception e) {
            assertTrue(e.getMessage().contains("Certificate in use by a loadbalancer"));
        }
    }

    @Test
    public void runDeleteSslCertInvalidId() throws NoSuchFieldException, IllegalAccessException {
        final TransactionLegacy txn = TransactionLegacy.open("runDeleteSslCertInvalidId");

        final long certId = 1;
        final CertServiceImpl certService = new CertServiceImpl();

        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.remove(anyLong())).thenReturn(true);
        when(certService._sslCertDao.findById(anyLong())).thenReturn(null);

        // no rule holding the cert
        certService._lbCertDao = Mockito.mock(LoadBalancerCertMapDao.class);
        when(certService._lbCertDao.listByCertId(anyLong())).thenReturn(null);

        //creating the command
        final DeleteSslCertCmd deleteCmd = new DeleteSslCertCmdExtn();
        final Class<?> _class = deleteCmd.getClass().getSuperclass();

        final Field certField = _class.getDeclaredField("id");
        certField.setAccessible(true);
        certField.set(deleteCmd, certId);

        try {
            certService.deleteSslCert(deleteCmd);
            fail("Delete with an invalid ID should fail");
        } catch (final Exception e) {
            assertTrue(e.getMessage().contains("Invalid certificate id"));
        }
    }

    public class UploadSslCertCmdExtn extends UploadSslCertCmd {
        @Override
        public long getEntityOwnerId() {
            return 1;
        }
    }

    public class DeleteSslCertCmdExtn extends DeleteSslCertCmd {
        @Override
        public long getEntityOwnerId() {
            return 1;
        }
    }
}
