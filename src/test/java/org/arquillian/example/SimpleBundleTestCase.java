package org.arquillian.example;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kimios.kernel.controller.ISecurityController;
import org.kimios.kernel.exception.AccessDeniedException;
import org.kimios.kernel.exception.DataSourceException;
import org.kimios.kernel.security.model.Session;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.io.File;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

@RunWith(Arquillian.class)
public class SimpleBundleTestCase {

    @ArquillianResource
    BundleContext context;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "test.jar");
        archive.addClasses(
                SimpleBundleTestCase.class
        );
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
//                builder.addImportPackages(Bundle.class);
//                builder.addImportPackages("org.kimios.kernel.*");
                builder.addDynamicImportPackages("org.kimios.kernel.*");
//                builder.addImportPackages("org.arquillian.example");
                return builder.openStream();
            }
        });

//        File[] files = Maven.configureResolver().useLegacyLocalRepo(true).loadPomFromFile("pom.xml").importRuntimeAndTestDependencies().resolve().withTransitivity().asFile();
//        for (File file : files) {
//            archive.addAsResource(file);
//        }

        File exportedFile = new File("exportedFile.jar");
        archive.as(ZipExporter.class).exportTo(exportedFile, true);

        return archive;
    }

    public Bundle retrieveKimiosKernelBundle() {
        Bundle[] bundles = context.getBundles();
        String pattern = "kimios-kernel";
        Bundle kimiosKernelBundle = null;
        for (Bundle b : bundles) {
            String bundleSymbName = b.getSymbolicName();
            if (bundleSymbName.matches("^.*imios.*$")) {
                System.out.println("bundle " + b.getBundleId() + " - " + bundleSymbName + " in state " + b.getState());

                if (bundleSymbName.matches(pattern)) {
                    kimiosKernelBundle = b;
                    System.out.println("bundle with name matching pattern '" + pattern + "' found.");
                }
            }
        }

        return kimiosKernelBundle;
    }

    @Test
    public void testBundleKimiosKernel() throws Exception {
        // retrieve the bundle
//        Bundle kimiosKernelBundle = this.retrieveKimiosKernelBundle();

        // Get the service reference
//        BundleContext context = kimiosKernelBundle.getBundleContext();
//        BundleContext context = this.context;
        ServiceReference<ISecurityController> sref = context.getServiceReference(ISecurityController.class);
        assertNotNull("ServiceReference not null", sref);

        // Get the service for the reference
        System.out.println("service : " + sref.getClass().getName());
        ISecurityController service = context.getService(sref);
        assertNotNull("Service not null", service);
        System.out.println("Just got the service, now we can test it. Go !");

        try {
            Session sess = service.startSession("admin", "kimios", "kimios");
            assertNotNull("Session is not null", sess);
            assertTrue("sessionId length > 0", sess.getUid().length() > 0);
        } catch (DataSourceException e) {
            e.printStackTrace();
        } catch (AccessDeniedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSecurityController() throws Exception {
//        ServiceReference[] sRefs = this.context.getAllServiceReferences(ISecurityController.class.getName(), null);
        ServiceReference[] sRefs = this.context.getBundle().getBundleContext().getAllServiceReferences(ISecurityController.class.getName(), null);
        System.out.println("All service references");
        System.out.println(sRefs.toString());
    }

    @Test
    public void testOtherBundlesPresence() throws Exception {
        Bundle kimiosKernelBundle = this.retrieveKimiosKernelBundle();
        assertNotNull(kimiosKernelBundle);
        assertEquals(Bundle.ACTIVE, kimiosKernelBundle.getState());
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
        System.out.println(bundle.getState());
        System.out.println(bundle.getSymbolicName());

    }
}