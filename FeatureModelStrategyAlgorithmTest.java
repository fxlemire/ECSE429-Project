package seg.jUCMNav.tests;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

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
import grl.EvaluationStrategy;
import grl.impl.IntentionalElementRefImpl;
import grl.impl.LinkRefImpl;
import seg.jUCMNav.editors.UCMNavMultiPageEditor;
import seg.jUCMNav.model.ModelCreationFactory;
import seg.jUCMNav.strategies.EvaluationStrategyManager;
import seg.jUCMNav.views.preferences.StrategyEvaluationPreferences;
import ucm.map.ComponentRef;
import ucm.map.Connect;
import ucm.map.EndPoint;
import ucm.map.PathNode;
import ucm.map.PluginBinding;
import ucm.map.RespRef;
import ucm.map.StartPoint;
import ucm.map.Stub;
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
    
    private final static String ROOT = "Root_";
    private final static String PCHILD1 = "PChild1_";
    private final static String PCHILD2 = "PChild2_";
    private final static String CHILD1 = "Child1_";
    private final static String CHILD2 = "Child2_";

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
        compRef = (ComponentRef) ModelCreationFactory.getNewObject(urnspec, ComponentRef.class);
        start = (StartPoint) ModelCreationFactory.getNewObject(urnspec, StartPoint.class);
        
        // cs = new CommandStack();
        cs = editor.getDelegatingCommandStack();
        
        //Set chosen algorithm
        StrategyEvaluationPreferences.setAlgorithm(String.valueOf(StrategyEvaluationPreferences.FEATURE_MODEL_ALGORITHM));
        StrategyEvaluationPreferences.setTolerance(0);
        StrategyEvaluationPreferences.setVisualizeAsPositiveRange(true);
        StrategyEvaluationPreferences.setFillElements(true);
        StrategyEvaluationPreferences.getPreferenceStore().setValue(StrategyEvaluationPreferences.PREF_AUTOSELECTMANDATORYFEATURES, true);
        
        //Set chosen algorithm
        StrategyEvaluationPreferences.setAlgorithm(StrategyEvaluationPreferences.FEATURE_MODEL_ALGORITHM+ "");//Messages.getString("GeneralPreferencePage.GrlStrategiesElementAlgorithm.FeatureModelStrategyAlgorithm"));
        StrategyEvaluationPreferences.setTolerance(0);
        StrategyEvaluationPreferences.setVisualizeAsPositiveRange(true);
        StrategyEvaluationPreferences.setFillElements(true);
        
        StrategyEvaluationPreferences.getPreferenceStore().setValue(StrategyEvaluationPreferences.PREF_AUTOSELECTMANDATORYFEATURES, true);
        
//        EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(0);
//		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
	}
    
    @Test
    public void test1() {
    	
    }
    
    @Test
    public void test2() {
    	
    }
    
    @Test
    public void test3() {
    	
    }
    
    @Test
    public void test4() {
    	
    }
    
    @Test
    public void test5() {
    	
    }
    
    @Test
    public void test6() {
    	
    }
    
    @Test
    public void test7() {
    	
    }
    
    @Test
    public void test8() {
    	
    }
    
    

	@Test
	public void test20() {
		final int TABNUMBER = 20;
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(0);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {

			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				List metaItr = feature.getDef().getMetadata();
				assertEquals(3, metaItr.size());
				
				metaItr.iterator();
				MetadataImpl numE = (MetadataImpl) feature.getDef().getMetadata().get(0);
				assertTrue(numE.getName().equals("_numEval"));
				assertTrue(numE.getValue().equals("100"));
				
				MetadataImpl qualE = (MetadataImpl) feature.getDef().getMetadata().get(1);
				assertTrue(qualE.getName().equals("_qualEval"));
				assertTrue(qualE.getValue().equals("Satisfied"));
				
				MetadataImpl autoS = (MetadataImpl) feature.getDef().getMetadata().get(2);
				assertTrue(autoS.getName().equals("_autoSelected"));
				
				MetadataImpl warn = (MetadataImpl) feature.getDef().getMetadata().get(3);
				assertTrue(warn == null);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				List metaItr = feature.getDef().getMetadata();
				assertEquals(2, metaItr.size());
				
				metaItr.iterator();
				MetadataImpl numE = (MetadataImpl) feature.getDef().getMetadata().get(0);
				assertTrue(numE.getName().equals("_numEval"));
				assertTrue(numE.getValue().equals("0"));
				
				MetadataImpl qualE = (MetadataImpl) feature.getDef().getMetadata().get(1);
				assertTrue(qualE.getName().equals("_qualEval"));
				assertTrue(qualE.getValue().equals("None"));

			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				
			}
		}

		
		// Get the links
//		LinkRefImpl rPC1, rPC2, pCC1, pCC2;
//		elemItr = featureD.getConnections().iterator();
//		rPC1 = (LinkRefImpl) elemItr.next();
//		rPC2 = (LinkRefImpl) elemItr.next();
//		pCC1 = (LinkRefImpl) elemItr.next();
//		pCC2 = (LinkRefImpl) elemItr.next();
		
		System.out.println("for break point");
	}
	
	private static boolean hasName(IntentionalElementRefImpl feature, String featureName, int diagramTabNumber) {
		return feature.getDef().getName().equals(featureName + diagramTabNumber);
	}
}
