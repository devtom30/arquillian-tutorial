package org.arquillian.example;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kimios.kernel.controller.ISecurityController;
import org.kimios.kernel.exception.AccessDeniedException;
import org.kimios.kernel.exception.DataSourceException;
import org.kimios.kernel.security.model.Session;
import org.kimios.kernel.user.model.User;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(Arquillian.class)
public class SimpleBundleTestCaseCopy {

    @ArquillianResource
    BundleContext context;

    private ISecurityController securityController;

    private static String ADMIN_LOGIN = "admin";
    private static String ADMIN_PWD= "kimios";
    private static String ADMIN_USER_SOURCE = "kimios";

    @Deployment
    public static JavaArchive createDeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "test.jar");
        archive.addClasses(
                SimpleBundleTestCaseCopy.class
        );
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addDynamicImportPackages("org.kimios.kernel.*");
                return builder.openStream();
            }
        });

        File exportedFile = new File("exportedFile.jar");
        archive.as(ZipExporter.class).exportTo(exportedFile, true);

        return archive;
    }

    @Before
    public void setUp() {
        // Get the service reference
        ServiceReference<ISecurityController> sref = context.getServiceReference(ISecurityController.class);
        // Get the service
        this.securityController = context.getService(sref);
    }

    @Test
    public void testStartSession() throws Exception {
        try {
            Session sess = this.securityController.startSession(ADMIN_LOGIN, ADMIN_USER_SOURCE, ADMIN_PWD);
            assertNotNull("Session is not null", sess);
            assertTrue("sessionId length > 0", sess.getUid().length() > 0);
        } catch (DataSourceException e) {
            e.printStackTrace();
        } catch (AccessDeniedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetUsers() throws Exception {
        List<User> users = this.securityController.getUsers(ADMIN_USER_SOURCE);
        assertTrue("We have users, at least one, the default user", users.size() > 0);

        // admin is in users list
        boolean adminExists = false;
        for (User user : users) {
            if (user.getUid().equals(ADMIN_LOGIN)) {
                adminExists = true;
                continue;
            }
        }
        assertTrue("admin user exists in data source", adminExists);
    }

    @Test
    public void testBundleContextInjection() throws Exception {
        assertNotNull("BundleContext injected", context);
        System.out.println("BundleContext injected");
        long bundleId = context.getBundle().getBundleId();
        assertEquals("System Bundle ID", 0, bundleId);
        System.out.println("System Bundle ID : " + bundleId);
    }

    @Test
    public void testBundleInjection(@ArquillianResource Bundle bundle) throws Exception {
        // Assert that the bundle is injected
        assertNotNull("Bundle injected", bundle);
        System.out.println("Bundle injected");

        // Assert that the bundle is in state RESOLVED
        // Note when the test bundle contains the test case it
        // must be resolved already when this test method is called
        assertEquals("Bundle RESOLVED", Bundle.RESOLVED, bundle.getState());
        System.out.println(bundle.getState());

        // Start the bundle
        bundle.start();
        assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundle.getState());
        System.out.println( bundle.getState());

        // Assert the bundle context
        BundleContext context = bundle.getBundleContext();
        assertNotNull("BundleContext available", context);
        System.out.println("BundleContext available");

        // Stop the bundle
        bundle.stop();
        assertEquals("Bundle RESOLVED", Bundle.RESOLVED, bundle.getState());
        System.out.println(bundle.getSymbolicName() + " (" + bundle.getBundleId() + ") is in state : " + bundle.getState());

        // Start the bundle
        bundle.start();
        assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundle.getState());
        System.out.println(bundle.getSymbolicName() + " (" + bundle.getBundleId() + ") is in state : " + bundle.getState());
    }
}