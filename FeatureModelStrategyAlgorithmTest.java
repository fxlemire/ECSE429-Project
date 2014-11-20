package seg.jUCMNav.tests;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import static java.nio.file.StandardCopyOption.*;
import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fm.FeatureDiagram;
import grl.impl.IntentionalElementRefImpl;
import grl.impl.LinkRefImpl;
import seg.jUCMNav.Messages;
import seg.jUCMNav.editors.UCMNavMultiPageEditor;
import seg.jUCMNav.model.ModelCreationFactory;
import seg.jUCMNav.views.preferences.StrategyEvaluationPreferences;
import ucm.map.ComponentRef;
import ucm.map.Connect;
import ucm.map.EndPoint;
import ucm.map.PathNode;
import ucm.map.PluginBinding;
import ucm.map.RespRef;
import ucm.map.StartPoint;
import ucm.map.Stub;
import ucm.map.UCMmap;
import ucm.map.WaitingPlace;
import urn.URNspec;
import urncore.UCMmodelElement;
import urncore.impl.MetadataImpl;

public class FeatureModelStrategyAlgorithmTest extends TestCase {

	public static void main(String[] args) {
        junit.textui.TestRunner.run(FeatureModelStrategyAlgorithmTest.class);
    }
    public UCMmodelElement componentRefWithLabel;
    public ComponentRef compRef;
    public ComponentRef compRef2;
    public CommandStack cs;
    public UCMNavMultiPageEditor editor;
    public EndPoint end;
    public RespRef resp;

    public Connect connect;
    public FeatureDiagram featureD;
    public UCMmodelElement pathNodeWithLabel;
    public StartPoint start;

    public Stub stub;
    public PluginBinding plugin;
    public PathNode fork;
    public WaitingPlace wait;

    // during teardown, if testBindings==true, call verifyBindings()
    public boolean testBindings;
    public IFile testfile;

    public URNspec urnspec;

    /*
     * @see TestCase#setUp()
     */
    public void setUp() throws Exception {
        super.setUp();
    	
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IProject testproject = workspaceRoot.getProject("jUCMNav-tests"); //$NON-NLS-1$
        if (!testproject.exists())
            testproject.create(null);

        if (!testproject.isOpen())
            testproject.open(null);

        testfile = testproject.getFile("test_cases.jucm"); //$NON-NLS-1$

        // start with clean file
        if (testfile.exists()) {
            testfile.delete(true, false, null);
            System.out.println("File found");
        }

        testfile.create(new ByteArrayInputStream("".getBytes()), false, null); //$NON-NLS-1$
        String workspace = workspaceRoot.getLocation().toString();
        String rootLocation = workspace.substring(0, workspace.length() - 15);
        Files.copy(Paths.get(rootLocation + "test_cases.jucm"), Paths.get(workspace + "/jUCMNav-tests/test_cases.jucm"), REPLACE_EXISTING);
        testfile = testproject.getFile("test_cases.jucm"); //$NON-NLS-1$
        
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(testfile.getName());
        editor = (UCMNavMultiPageEditor) page.openEditor(new FileEditorInput(testfile), desc.getId());

        // generate a top level model element
        urnspec = editor.getModel();
        // urnspec = (URNspec) ModelCreationFactory.getNewURNspec();

        compRef = (ComponentRef) ModelCreationFactory.getNewObject(urnspec, ComponentRef.class);
        start = (StartPoint) ModelCreationFactory.getNewObject(urnspec, StartPoint.class);
        
        // cs = new CommandStack();
        cs = editor.getDelegatingCommandStack();
        
        //Set chosen algorithm
        StrategyEvaluationPreferences.setAlgorithm(Messages.getString("GeneralPreferencePage.GrlStrategiesElementAlgorithm.FeatureModelStrategyAlgorithm"));
        StrategyEvaluationPreferences.setTolerance(0);
        StrategyEvaluationPreferences.setVisualizeAsPositiveRange(true);
        StrategyEvaluationPreferences.setFillElements(true);
	}

	@Test
	public void test1() {
		featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(19);
//		Iterator featureItr = featureD.getNodes().iterator();
//		featureD.getNodes().get(0);
//		while(featureItr.hasNext()) {
//			IntentionalElementRefImpl a = (IntentionalElementRefImpl) featureItr.next();
//			System.out.println("jajajaja");
//		}
		// Get the feature nodes.
		IntentionalElementRefImpl root, pChild1, pChild2, child1, child2;
		Iterator elemItr = featureD.getNodes().iterator();
		root = (IntentionalElementRefImpl) elemItr.next();
		pChild1 = (IntentionalElementRefImpl) elemItr.next();
		pChild2 = (IntentionalElementRefImpl) elemItr.next();
		child1 = (IntentionalElementRefImpl) elemItr.next();
		child2 = (IntentionalElementRefImpl) elemItr.next();
		
		MetadataImpl autoS = (MetadataImpl) pChild1.getDef().getMetadata().get(0);
		assertTrue(autoS.getName().equals("_autoSelected"));
		
		// Get the links
		LinkRefImpl rPC1, rPC2, pCC1, pCC2;
		elemItr = featureD.getConnections().iterator();
		rPC1 = (LinkRefImpl) elemItr.next();
		rPC2 = (LinkRefImpl) elemItr.next();
		pCC1 = (LinkRefImpl) elemItr.next();
		pCC2 = (LinkRefImpl) elemItr.next();
		
		//assertTrue(pCC1.getMetadata())

	}

//	@Test
//	public void testGetEvaluationType() {
//		fail("Not yet implemented");
//	}
//	
//	@Test
//	public void testGetEvaluation() {
//		fail("Not yet implemented");
//	}
//	
//	@Test
//	public void testClearAllAutoSelectedFeatures() {
//		fail("Not yet implemented");
//	}
}
