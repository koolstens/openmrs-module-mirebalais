package org.openmrs.module.mirebalais.printer.impl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAttributeType;
import org.openmrs.contrib.testdata.TestDataManager;
import org.openmrs.contrib.testdata.builder.PatientBuilder;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.module.mirebalais.setup.PrinterSetup;
import org.openmrs.module.pihcore.deploy.bundle.core.LocationAttributeTypeBundle;
import org.openmrs.module.pihcore.deploy.bundle.core.LocationTagBundle;
import org.openmrs.module.pihcore.deploy.bundle.core.PersonAttributeTypeBundle;
import org.openmrs.module.pihcore.deploy.bundle.haiti.HaitiAddressBundle;
import org.openmrs.module.pihcore.deploy.bundle.haiti.HaitiPatientIdentifierTypeBundle;
import org.openmrs.module.pihcore.deploy.bundle.haiti.mirebalais.MirebalaisLocationsBundle;
import org.openmrs.module.pihcore.metadata.core.PersonAttributeTypes;
import org.openmrs.module.pihcore.metadata.haiti.HaitiPatientIdentifierTypes;
import org.openmrs.module.pihcore.metadata.haiti.mirebalais.MirebalaisLocations;
import org.openmrs.module.printer.Printer;
import org.openmrs.module.printer.PrinterModel;
import org.openmrs.module.printer.PrinterModuleActivator;
import org.openmrs.module.printer.PrinterService;
import org.openmrs.module.printer.PrinterType;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test the ZL EMR ID Card Printer functionality
 */
public class ZlEmrIdCardPrinterTest extends BaseModuleContextSensitiveTest {

    @Autowired
    private TestDataManager testDataManager;

    @Autowired
    private PrinterService printerService;

    @Autowired
    LocationTagBundle locationTagBundle;

    @Autowired
    LocationAttributeTypeBundle locationAttributeTypeBundle;

    @Autowired
    MirebalaisLocationsBundle mirebalaisLocationsBundle;

    @Autowired
    HaitiAddressBundle addressBundle;

    @Autowired
    ZlEmrIdCardPrinter zlEmrIdCardPrinter;

    @Autowired
    HaitiPatientIdentifierTypeBundle patientIdentifierTypeBundle;

    @Autowired
    PersonAttributeTypeBundle personAttributeTypeBundle;

    @Before
    public void setup() throws Exception {
        PrinterModuleActivator printerModuleActivator = new PrinterModuleActivator();

        printerModuleActivator.started(); // Create Location Attribute Types Needed
        locationTagBundle.install();
        locationAttributeTypeBundle.install();
        mirebalaisLocationsBundle.install(); // Install Location Metadata for distribution
        patientIdentifierTypeBundle.install(); // Install Patient Identifier Types for distribution
        personAttributeTypeBundle.install(); // Install Person Attribute Types for distribution
        addressBundle.installAddressTemplate(); // Install address template needed for layout on id card
        PrinterSetup.registerPrintHandlers(printerService); // Register print handlers
    }

    @Test
    public void testZlEmrIdCardPrinting() throws Exception {

        // Register printer model
        PrinterModel model = new PrinterModel();
        model.setName("Test Zebra P110i");
        model.setType(PrinterType.ID_CARD);
        model.setPrintHandler("p110iPrintHandler");
        printerService.savePrinterModel(model);

        // Register instance of this printer model
        Printer printer = new Printer();
        printer.setName("Test ZL EMR ID Card Printer");
        printer.setType(PrinterType.ID_CARD);
        printer.setModel(model);
        printer.setPhysicalLocation(null);
        printer.setIpAddress("127.0.0.1");
        printer.setPort("9105");
        printerService.savePrinter(printer);

        // Set location for this printer
        Location location = MetadataUtils.existing(Location.class, MirebalaisLocations.CLINIC_REGISTRATION.uuid());
        printerService.setDefaultPrinter(location, PrinterType.ID_CARD, printer);

        // Create a patient for whom to print an id card
        PatientBuilder pb = testDataManager.patient().birthdate("1948-02-16").gender("M").name("Ringo", "Starr");
        pb.identifier(MetadataUtils.existing(PatientIdentifierType.class, HaitiPatientIdentifierTypes.ZL_EMR_ID.uuid()), "X2ECEX", location);
        pb.personAttribute(MetadataUtils.existing(PersonAttributeType.class, PersonAttributeTypes.TELEPHONE_NUMBER.uuid()), "555-1212");
        pb.address("should be line 2", "should be line 1", "should be line 4", "should be line 5a", "should not exist", "should be line 5b");
        Patient patient = pb.save();

        TestPrinter testPrinter = new TestPrinter("127.0.0.1", 9105, "Windows-1252");
        testPrinter.start();

        zlEmrIdCardPrinter.print(patient, location);

        // Pause up to 30 seconds for printing to happen
        for (int i=0; i<30 && testPrinter.getNumPrintJobs() == 0; i++) {
            Thread.sleep(1000);
        }

        Assert.assertEquals(1, testPrinter.getNumPrintJobs());
        TestPrinter.PrintJob job = testPrinter.getLatestPrintJob();
        Assert.assertTrue(job.containsData("B 75 550 0 0 0 3 100 0 X2ECEX"));

        testPrinter.stop();
    }
}
