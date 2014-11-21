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
import fm.impl.FeatureImpl;
import grl.ContributionContext;
import grl.ElementLink;
import grl.EvaluationStrategy;
import grl.impl.ContributionImpl;
import grl.impl.DecompositionImpl;
import grl.impl.ElementLinkImpl;
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
import urncore.Metadata;
import urncore.UCMmodelElement;
import urncore.impl.GRLmodelElementImpl;
import urncore.impl.MetadataImpl;

public class FeatureModelStrategyAlgorithmTest {

//	public static void main(String[] args) {
//        junit.textui.TestRunner.run(FeatureModelStrategyAlgorithmTest.class);
//    }
    public UCMmodelElement componentRefWithLabel;
    public static ComponentRef compRef;
    public ComponentRef compRef2;
    public static CommandStack cs;
    public static UCMNavMultiPageEditor editor;
    public EndPoint end;
    public RespRef resp;

    public Connect connect;
    public UCMmodelElement pathNodeWithLabel;
    public static StartPoint start;

    public Stub stub;
    public PluginBinding plugin;
    public PathNode fork;
    public WaitingPlace wait;

    // during teardown, if testBindings==true, call verifyBindings()
    public boolean testBindings;
    public static IFile testfile;

    public static URNspec urnspec;
    
    /** Strategies */
    private final static int NO_SELECTION = 0;
    private final static int USER_SELECTION = 1;
    
    /** Feature Nodes */
    private final static String ROOT = "Root_";
    private final static String PCHILD1 = "PChild1_";
    private final static String PCHILD2 = "PChild2_";
    private final static String PCHILD3 = "PChild3_";
    private final static String PCHILD4 = "PChild4_";
    private final static String PCHILD5 = "PChild5_";
    private final static String PCHILD6 = "PChild6_";
    private final static String CHILD1 = "Child1_";
    private final static String CHILD2 = "Child2_";
    private final static String CHILD3 = "Child3_";
    private final static String GCHILD1 = "GChild1_";
    private final static String GCHILD2 = "GChild2_";
    
    /** Feature Node Metadata */
    private final static String NUMEVAL = "_numEval";
    private final static String QUALEVAL = "_qualEval";
    private final static String QUALEVAL_SATISFIED = "Satisfied";
    private final static String QUALEVAL_NONE = "None";
    private final static String AUTOSELECTED = "_autoSelected";
    private final static String WARNING = "_userSetEvaluationWarning";
    private final static String WARNING_MSG = "100 != 0";
    
    /** Errors */
    private final static String UNKNOWN_NODE = "An unknown node exists on the diagram.";
    private final static String UNKNOWN_LINK = "An unknown link exists on the diagram.";
    private final static String PCHILD2_EXISTS = "This diagram should not have a pChild2 node.";
    private final static String PCHILD2LINK_EXISTS = "This diagram should not have a link to pChild2.";

//    @BeforeClass
//    public void setUpClass() {
//    	
//    }
    
    /*
     * @see TestCase#setUp()
     */
    @BeforeClass
    public static void setUpOnce() throws Exception {
//        super.setUp();
    	
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
	}
    
    @Test
    public void test1() {
		final int TABNUMBER = 1;
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);

		assertEquals(0, featureD.getNodes().size());
		assertEquals(0, featureD.getConnections().size());
    }
    
    @Test
    public void test2() {
    	final int TABNUMBER = 2;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);
		
		assertEquals(1, featureD.getNodes().size());
		
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				fail(getFeatureName(PCHILD1, TABNUMBER) + " should not exist");
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				fail(getFeatureName(PCHILD2, TABNUMBER) + " should not exist");
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				fail(getFeatureName(CHILD1, TABNUMBER) + " should not exist");
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				fail(getFeatureName(CHILD2, TABNUMBER) + " should not exist");
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		assertEquals(0, featureD.getConnections().size());
    }
    
    @Test
	public void test2UserSelection() {
		final int TABNUMBER = 2;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(USER_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkUserSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				fail(getFeatureName(PCHILD1, TABNUMBER) + " should not exist");
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				fail("PCHILD2_EXISTS");
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				fail(getFeatureName(CHILD1, TABNUMBER) + " should not exist");
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				fail(getFeatureName(PCHILD2, TABNUMBER) + " should not exist");
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		assertEquals(0, featureD.getConnections().size());
	}
    
    @Test
    public void test3() {
    	final int TABNUMBER = 3;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);
		
		/* FEATURE NODES TEST */
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkContributionLink(link, 50);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkContributionLink(link, 50);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 50);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 50);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
    }
    
    @Test
	public void test3UserSelection() {
		final int TABNUMBER = 3;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(USER_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
				fail("PCHILD2_EXISTS");
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkContributionLink(link, 50);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkContributionLink(link, 50);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 50);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 50);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test4() {
		final int TABNUMBER = 4;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				fail("PCHILD2_EXISTS");
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				fail("PCHILD2LINK_EXISTS");
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test4UserSelection() {
		final int TABNUMBER = 4;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(USER_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				fail("PCHILD2_EXISTS");
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkUserSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				fail("PCHILD2LINK_EXISTS");
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test5() {
		final int TABNUMBER = 5;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				fail("PCHILD2_EXISTS");
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				fail("PCHILD2LINK_EXISTS");
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 0);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test5UserSelection() {
		final int TABNUMBER = 5;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(USER_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				fail("PCHILD2_EXISTS");
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkUserSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				fail("PCHILD2LINK_EXISTS");
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 0);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
 	public void test6() {
 		final int TABNUMBER = 6;
 		
 		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
 		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
 		
 		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);
 
 		// Get the feature nodes.
 		Iterator elemItr = featureD.getNodes().iterator();
 		
 		while (elemItr.hasNext()) {
 			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();
 
 			if (hasName(feature, ROOT, TABNUMBER)) {
 				checkPropagationSelected(feature);
 			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
 				checkAutoSelectedWithoutWarning(feature);
 			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
 				fail("PCHILD2_EXISTS");
 			} else if (hasName(feature, CHILD1, TABNUMBER)) {
 				checkNotSelected(feature);
 			} else if (hasName(feature, CHILD2, TABNUMBER)) {
 				checkAutoSelectedWithoutWarning(feature);
 			} else {
 				fail(UNKNOWN_NODE);
 			}
 		}
 		
 		// Get the links.
 		elemItr = featureD.getConnections().iterator();
 		
 		while (elemItr.hasNext()) {
 			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
 			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();
 
 			FeatureImpl src = (FeatureImpl) link.getSrc();
 			FeatureImpl dest = (FeatureImpl) link.getDest();
 			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
 				checkContributionLink(link, 100);
 			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
 				fail("PCHILD2LINK_EXISTS");
 			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
 				checkContributionLink(link, 0);
 			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
 				checkContributionLink(link, 100);
 			} else {
 				fail(UNKNOWN_LINK);
 			}
 		}
 	}
    
    @Test
 	public void test7() {
 		final int TABNUMBER = 7;
 		
 		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
 		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
 		
 		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);
 
 		// Get the feature nodes.
 		Iterator elemItr = featureD.getNodes().iterator();
 		
 		while (elemItr.hasNext()) {
 			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();
 
 			if (hasName(feature, ROOT, TABNUMBER)) {
 				checkPropagationSelected(feature);
 			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
 				checkAutoSelectedWithoutWarning(feature);
 			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
 				fail("PCHILD2_EXISTS");
 			} else if (hasName(feature, CHILD1, TABNUMBER)) {
 				checkAutoSelectedWithoutWarning(feature);
 			} else if (hasName(feature, CHILD2, TABNUMBER)) {
 				checkAutoSelectedWithoutWarning(feature);
 			} else {
 				fail(UNKNOWN_NODE);
 			}
 		}
 		
 		// Get the links.
 		elemItr = featureD.getConnections().iterator();
 		
 		while (elemItr.hasNext()) {
 			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
 			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();
 
 			FeatureImpl src = (FeatureImpl) link.getSrc();
 			FeatureImpl dest = (FeatureImpl) link.getDest();
 			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
 				checkContributionLink(link, 100);
 			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
 				fail("PCHILD2LINK_EXISTS");
 			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
 				checkDecompositionLink(link);
 			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
 				checkDecompositionLink(link);
 			} else {
 				fail(UNKNOWN_LINK);
 			}
 		}
 	}
    
    @Test
	public void test7UserSelection() {
		final int TABNUMBER = 7;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(USER_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				fail("PCHILD2_EXISTS");
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				fail("PCHILD2LINK_EXISTS");
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
 	public void test8() {
 		final int TABNUMBER = 8;
 		
 		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
 		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
 		
 		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);
 
 		// Get the feature nodes.
 		Iterator elemItr = featureD.getNodes().iterator();
 		
 		while (elemItr.hasNext()) {
 			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();
 
 			if (hasName(feature, ROOT, TABNUMBER)) {
 				checkPropagationSelected(feature);
 			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
 				checkAutoSelectedWithWarning(feature);
 			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
 				fail("PCHILD2_EXISTS");
 			} else if (hasName(feature, CHILD1, TABNUMBER)) {
 				checkNotSelected(feature);
 			} else if (hasName(feature, CHILD2, TABNUMBER)) {
 				checkNotSelected(feature);
 			} else {
 				fail(UNKNOWN_NODE);
 			}
 		}
 		
 		// Get the links.
 		elemItr = featureD.getConnections().iterator();
 		
 		while (elemItr.hasNext()) {
 			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
 			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();
 
 			FeatureImpl src = (FeatureImpl) link.getSrc();
 			FeatureImpl dest = (FeatureImpl) link.getDest();
 			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
 				checkContributionLink(link, 100);
 			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
 				fail("PCHILD2LINK_EXISTS");
 			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
 				checkDecompositionLink(link);
 			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
 				checkDecompositionLink(link);
 			} else {
 				fail(UNKNOWN_LINK);
 			}
 		}
 	}
    
    @Test
	public void test8UserSelection() {
		final int TABNUMBER = 8;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(USER_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				fail("PCHILD2_EXISTS");
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkUserSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				fail("PCHILD2LINK_EXISTS");
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
 	public void test9() {
 		final int TABNUMBER = 9;
 		
 		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
 		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
 		
 		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);
 
 		// Get the feature nodes.
 		Iterator elemItr = featureD.getNodes().iterator();
 		
 		while (elemItr.hasNext()) {
 			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();
 
 			if (hasName(feature, ROOT, TABNUMBER)) {
 				checkPropagationSelected(feature);
 			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
 				checkAutoSelectedWithWarning(feature);
 			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
 				fail("PCHILD2_EXISTS");
 			} else if (hasName(feature, CHILD1, TABNUMBER)) {
 				checkNotSelected(feature);
 			} else if (hasName(feature, CHILD2, TABNUMBER)) {
 				checkNotSelected(feature);
 			} else {
 				fail(UNKNOWN_NODE);
 			}
 		}
 		
 		// Get the links.
 		elemItr = featureD.getConnections().iterator();
 		
 		while (elemItr.hasNext()) {
 			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
 			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();
 
 			FeatureImpl src = (FeatureImpl) link.getSrc();
 			FeatureImpl dest = (FeatureImpl) link.getDest();
 			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
 				checkContributionLink(link, 100);
 			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
 				fail("PCHILD2LINK_EXISTS");
 			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
 				checkDecompositionLink(link);
 			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
 				checkDecompositionLink(link);
 			} else {
 				fail(UNKNOWN_LINK);
 			}
 		}
 	}
    
    @Test
	public void test9UserSelection() {
		final int TABNUMBER = 9;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(USER_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				fail("PCHILD2_EXISTS");
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkUserSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				fail("PCHILD2LINK_EXISTS");
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
 	public void test10() {
 		final int TABNUMBER = 10;
 		
 		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
 		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
 		
 		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);
 
 		// Get the feature nodes.
 		Iterator elemItr = featureD.getNodes().iterator();
 		
 		while (elemItr.hasNext()) {
 			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();
 
 			if (hasName(feature, ROOT, TABNUMBER)) {
 				checkNotSelected(feature);
 			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
 				checkNotSelected(feature);
 			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
 				fail("PCHILD2_EXISTS");
 			} else if (hasName(feature, CHILD1, TABNUMBER)) {
 				checkNotSelected(feature);
 			} else if (hasName(feature, CHILD2, TABNUMBER)) {
 				checkNotSelected(feature);
 			} else {
 				fail(UNKNOWN_NODE);
 			}
 		}
 		
 		// Get the links.
 		elemItr = featureD.getConnections().iterator();
 		
 		while (elemItr.hasNext()) {
 			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
 			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();
 
 			FeatureImpl src = (FeatureImpl) link.getSrc();
 			FeatureImpl dest = (FeatureImpl) link.getDest();
 			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
 				checkContributionLink(link, 100);
 			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
 				fail("PCHILD2LINK_EXISTS");
 			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
 				checkContributionLink(link, 50);
 			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
 				checkContributionLink(link, 50);
 			} else {
 				fail(UNKNOWN_LINK);
 			}
 		}
 	}
    
    @Test
	public void test10UserSelection() {
		final int TABNUMBER = 10;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(USER_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				fail("PCHILD2_EXISTS");
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkUserSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				fail("PCHILD2LINK_EXISTS");
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 50);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 50);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
 	public void test11() {
 		final int TABNUMBER = 11;
 		
 		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
 		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
 		
 		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);
 
 		// Get the feature nodes.
 		Iterator elemItr = featureD.getNodes().iterator();
 		
 		while (elemItr.hasNext()) {
 			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();
 
 			if (hasName(feature, ROOT, TABNUMBER)) {
 				checkNotSelected(feature);
 			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
 				checkNotSelected(feature);
 			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
 				fail("PCHILD2_EXISTS");
 			} else if (hasName(feature, CHILD1, TABNUMBER)) {
 				checkNotSelected(feature);
 			} else if (hasName(feature, CHILD2, TABNUMBER)) {
 				checkNotSelected(feature);
 			} else {
 				fail(UNKNOWN_NODE);
 			}
 		}
 		
 		// Get the links.
 		elemItr = featureD.getConnections().iterator();
 		
 		while (elemItr.hasNext()) {
 			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
 			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();
 
 			FeatureImpl src = (FeatureImpl) link.getSrc();
 			FeatureImpl dest = (FeatureImpl) link.getDest();
 			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
 				checkContributionLink(link, 100);
 			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
 				fail("PCHILD2LINK_EXISTS");
 			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
 				checkContributionLink(link, 100);
 			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
 				checkContributionLink(link, 100);
 			} else {
 				fail(UNKNOWN_LINK);
 			}
 		}
 	}
    
    @Test
	public void test11UserSelection() {
		final int TABNUMBER = 11;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(USER_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				fail("PCHILD2_EXISTS");
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				fail("PCHILD2LINK_EXISTS");
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
 	public void test12() {
 		final int TABNUMBER = 12;
 		
 		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
 		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
 		
 		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);
 
 		// Get the feature nodes.
 		Iterator elemItr = featureD.getNodes().iterator();
 		
 		while (elemItr.hasNext()) {
 			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();
 
 			if (hasName(feature, ROOT, TABNUMBER)) {
 				checkNotSelected(feature);
 			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
 				checkNotSelected(feature);
 			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
 				fail("PCHILD2_EXISTS");
 			} else if (hasName(feature, CHILD1, TABNUMBER)) {
 				checkNotSelected(feature);
 			} else if (hasName(feature, CHILD2, TABNUMBER)) {
 				checkNotSelected(feature);
 			} else {
 				fail(UNKNOWN_NODE);
 			}
 		}
 		
 		// Get the links.
 		elemItr = featureD.getConnections().iterator();
 		
 		while (elemItr.hasNext()) {
 			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
 			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();
 
 			FeatureImpl src = (FeatureImpl) link.getSrc();
 			FeatureImpl dest = (FeatureImpl) link.getDest();
 			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
 				checkContributionLink(link, 100);
 			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
 				fail("PCHILD2LINK_EXISTS");
 			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
 				checkContributionLink(link, 100);
 			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
 				checkContributionLink(link, 0);
 			} else {
 				fail(UNKNOWN_LINK);
 			}
 		}
 	}
    
    @Test
	public void test12UserSelection() {
		final int TABNUMBER = 12;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(USER_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				fail("PCHILD2_EXISTS");
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkUserSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				fail("PCHILD2LINK_EXISTS");
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 0);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test13() {
		final int TABNUMBER = 13;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				fail("PCHILD2_EXISTS");
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				fail("PCHILD2LINK_EXISTS");
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 0);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
  
    @Test
	public void test13UserSelection() {
		final int TABNUMBER = 13;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(USER_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				fail("PCHILD2_EXISTS");
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				fail("PCHILD2LINK_EXISTS");
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 0);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
  
    @Test
	public void test14() {
		final int TABNUMBER = 14;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				fail("PCHILD2_EXISTS");
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				fail("PCHILD2LINK_EXISTS");
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
  
    @Test
	public void test14UserSelection() {
		final int TABNUMBER = 14;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(USER_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				fail("PCHILD2_EXISTS");
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkUserSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				fail("PCHILD2LINK_EXISTS");
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
  
    @Test
	public void test15() {
		final int TABNUMBER = 15;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				fail("PCHILD2_EXISTS");
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				fail("PCHILD2LINK_EXISTS");
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
  
  	@Test
	public void test15UserSelection() {
		final int TABNUMBER = 15;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(USER_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkUserSelectedWithWarning(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				fail("PCHILD2_EXISTS");
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				fail("PCHILD2LINK_EXISTS");
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test16() {
		final int TABNUMBER = 16;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				fail(PCHILD2_EXISTS);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				fail(PCHILD2LINK_EXISTS);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test16UserSelection() {
		final int TABNUMBER = 16;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(USER_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				fail("PCHILD2_EXISTS");
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkUserSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				fail("PCHILD2LINK_EXISTS");
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test17() {
		final int TABNUMBER = 17;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 50);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 50);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test18() {
		final int TABNUMBER = 18;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test19() {
		final int TABNUMBER = 19;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 0);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}

    @Test
	public void test20() {
		final int TABNUMBER = 20;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 0);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test21() {
		final int TABNUMBER = 21;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test22() {
		final int TABNUMBER = 22;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkAutoSelectedWithWarning(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test22UserSelection() {
		final int TABNUMBER = 22;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(USER_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkUserSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test23() {
		final int TABNUMBER = 23;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkAutoSelectedWithWarning(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test24() {
		final int TABNUMBER = 24;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 50);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 50);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test24UserSelection() {
		final int TABNUMBER = 24;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(USER_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkUserSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 50);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 50);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test25() {
		final int TABNUMBER = 25;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test25UserSelection() {
		final int TABNUMBER = 25;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(USER_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkUserSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test26() {
		final int TABNUMBER = 26;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 0);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test26UserSelection() {
		final int TABNUMBER = 26;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(USER_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkUserSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
				fail("PCHILD2LINK_EXISTS");
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 0);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test27() {
		final int TABNUMBER = 27;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 0);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test27UserSelection() {
		final int TABNUMBER = 27;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(USER_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkUserSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 0);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test28() {
		final int TABNUMBER = 28;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test29() {
		final int TABNUMBER = 29;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test30() {
		final int TABNUMBER = 30;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test31() {
		final int TABNUMBER = 31;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 50);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 50);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test32() {
		final int TABNUMBER = 32;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test32UserSelection() {
		final int TABNUMBER = 32;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(USER_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkUserSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
				fail("PCHILD2LINK_EXISTS");
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test33() {
		final int TABNUMBER = 33;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 0);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test34() {
		final int TABNUMBER = 34;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 0);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkContributionLink(link, 100);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test35() {
		final int TABNUMBER = 35;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test36() {
		final int TABNUMBER = 36;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test37() {
		final int TABNUMBER = 37;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
				checkDecompositionLink(link);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
    public void test38() {
		final int TABNUMBER = 38;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, PCHILD3, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, PCHILD4, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, PCHILD5, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, PCHILD6, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkContributionLink(link, 16);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkContributionLink(link, 16);
			} else if (hasName(src, PCHILD3, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkContributionLink(link, 17);
			} else if (hasName(src, PCHILD4, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkContributionLink(link, 17);
			} else if (hasName(src, PCHILD5, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkContributionLink(link, 17);
			} else if (hasName(src, PCHILD6, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkContributionLink(link, 17);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test39() {
		final int TABNUMBER = 39;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD3, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, GCHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, GCHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD2, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD2, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD3, TABNUMBER) && hasName(dest, PCHILD2, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, GCHILD1, TABNUMBER) && hasName(dest, CHILD2, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, GCHILD2, TABNUMBER) && hasName(dest, CHILD2, TABNUMBER)) {
				checkDecompositionLink(link);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
  
    @Test
	public void test39UserSelection() {
		final int TABNUMBER = 39;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(USER_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, CHILD3, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, GCHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, GCHILD2, TABNUMBER)) {
				checkUserSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD2, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD2, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD3, TABNUMBER) && hasName(dest, PCHILD2, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, GCHILD1, TABNUMBER) && hasName(dest, CHILD2, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, GCHILD2, TABNUMBER) && hasName(dest, CHILD2, TABNUMBER)) {
				checkDecompositionLink(link);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    @Test
	public void test40() {
		final int TABNUMBER = 40;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD3, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, GCHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, GCHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD2, TABNUMBER)) {
				checkContributionLink(link, 0);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD2, TABNUMBER)) {
				checkContributionLink(link, 50);
			} else if (hasName(src, CHILD3, TABNUMBER) && hasName(dest, PCHILD2, TABNUMBER)) {
				checkContributionLink(link, 50);
			} else if (hasName(src, GCHILD1, TABNUMBER) && hasName(dest, CHILD2, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, GCHILD2, TABNUMBER) && hasName(dest, CHILD2, TABNUMBER)) {
				checkDecompositionLink(link);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
  
  	@Test
	public void test40UserSelection() {
		final int TABNUMBER = 40;
		
		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(USER_SELECTION);
		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
		
		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);

		// Get the feature nodes.
		Iterator elemItr = featureD.getNodes().iterator();
		
		while (elemItr.hasNext()) {
			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();

			if (hasName(feature, ROOT, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, CHILD1, TABNUMBER)) {
				checkNotSelected(feature);
			} else if (hasName(feature, CHILD2, TABNUMBER)) {
				checkPropagationSelected(feature);
			} else if (hasName(feature, CHILD3, TABNUMBER)) {
				checkAutoSelectedWithoutWarning(feature);
			} else if (hasName(feature, GCHILD1, TABNUMBER)) {
				checkUserSelected(feature);
			} else if (hasName(feature, GCHILD2, TABNUMBER)) {
				checkNotSelected(feature);
			} else {
				fail(UNKNOWN_NODE);
			}
		}
		
		// Get the links.
		elemItr = featureD.getConnections().iterator();
		
		while (elemItr.hasNext()) {
			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();

			FeatureImpl src = (FeatureImpl) link.getSrc();
			FeatureImpl dest = (FeatureImpl) link.getDest();
			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD2, TABNUMBER)) {
				checkContributionLink(link, 0);
			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD2, TABNUMBER)) {
				checkContributionLink(link, 50);
			} else if (hasName(src, CHILD3, TABNUMBER) && hasName(dest, PCHILD2, TABNUMBER)) {
				checkContributionLink(link, 50);
			} else if (hasName(src, GCHILD1, TABNUMBER) && hasName(dest, CHILD2, TABNUMBER)) {
				checkDecompositionLink(link);
			} else if (hasName(src, GCHILD2, TABNUMBER) && hasName(dest, CHILD2, TABNUMBER)) {
				checkDecompositionLink(link);
			} else {
				fail(UNKNOWN_LINK);
			}
		}
	}
    
    private static void checkAutoSelectedWithWarning(IntentionalElementRefImpl feature) {
		checkFeatureMetadata(feature, true, "100", true, QUALEVAL_SATISFIED, true, true, WARNING_MSG);
    }
    
    private static void checkAutoSelectedWithoutWarning(IntentionalElementRefImpl feature) {
		checkFeatureMetadata(feature, true, "100", true, QUALEVAL_SATISFIED, true, false, WARNING_MSG);
    }
    
    private static void checkNotSelected(IntentionalElementRefImpl feature) {
		checkFeatureMetadata(feature, true, "0", true, QUALEVAL_NONE, false, false, WARNING_MSG);
    }
    
    private static void checkPropagationSelected(IntentionalElementRefImpl feature) {
		checkFeatureMetadata(feature, true, "100", true, QUALEVAL_SATISFIED, false, false, WARNING_MSG);
    }
    
    private static void checkUserSelected(IntentionalElementRefImpl feature) {
		checkFeatureMetadata(feature, true, "100", true, QUALEVAL_SATISFIED, false, false, WARNING_MSG);
    }
    
    private static void checkUserSelectedWithWarning(IntentionalElementRefImpl feature) {
		checkFeatureMetadata(feature, true, "100", true, QUALEVAL_SATISFIED, false, true, WARNING_MSG);
    }
    
    private static void checkFeatureMetadata(IntentionalElementRefImpl feature, boolean numEval, String numEvalValue,
    		boolean qualEval, String qualEvalValue, boolean autoSelection, boolean warning, String warningValue) {
    	
    	boolean numExists = false;
		boolean qualExists = false;
		boolean autoExists = false;
		boolean warnExists = false;
		
		Iterator metaItr = feature.getDef().getMetadata().iterator();
		
		while (metaItr.hasNext()) {
			Metadata meta = (Metadata) metaItr.next();
			
			if (isNumEval(meta.getName())) {
				numExists = true;
				assertEquals(numEvalValue, meta.getValue());
			} else if (isQualEval(meta.getName())) {
				qualExists = true;
				assertEquals(qualEvalValue, meta.getValue());
			} else if (isAutoSelected(meta.getName())) {
				autoExists = true;
				assertNotNull(meta.getValue());
			} else if (isWarning(meta.getName())) {
				warnExists = true;
				assertEquals(warningValue, meta.getValue());
			}
		}
		
		assertEquals(numEval, numExists);
		assertEquals(qualEval, qualExists);
		assertEquals(autoSelection, autoExists);
		assertEquals(warning, warnExists);
    }
    
    private static void checkDecompositionLink(ElementLinkImpl pLink) {
		if (pLink instanceof DecompositionImpl) {
			DecompositionImpl decompLink = (DecompositionImpl) pLink;
			assertNotNull(decompLink);
		} else if (pLink instanceof ContributionImpl) {
			fail(pLink.getName() + " should be a Decomposition link but is a Contribution link.");
		} else {
			fail(pLink.getName() + " is neither a Decomposition or Contribution link.");
		}
    }
    
    private static void checkContributionLink(ElementLinkImpl pLink, int pContribValue) {
    	if (pLink instanceof ContributionImpl) {
			ContributionImpl contribLink = (ContributionImpl) pLink;
			assertEquals(pContribValue, contribLink.getQuantitativeContribution());
		} else if (pLink instanceof DecompositionImpl) {
			fail(pLink.getName() + " should be a Contribution link but is a Decomposition link.");
		} else {
			fail(pLink.getName() + " is neither a Decomposition or Contribution link.");
		}
    }
	
	private static String getFeatureName(String featureName, int diagramTabNumber) {
		return featureName + diagramTabNumber;
	}
	
	private static boolean hasName(IntentionalElementRefImpl feature, String featureName, int diagramTabNumber) {
		return feature.getDef().getName().equals(getFeatureName(featureName, diagramTabNumber));
	}
	
	private static boolean hasName(FeatureImpl feature, String featureName, int diagramTabNumber) {
		return feature.getName().equals(getFeatureName(featureName, diagramTabNumber));
	}
	
	private static boolean isNumEval(String name) {
		return name.equals(NUMEVAL);
	}
	
	private static boolean isQualEval(String name) {
		return name.equals(QUALEVAL);
	}
	
	private static boolean isAutoSelected(String name) {
		return name.equals(AUTOSELECTED);
	}
	
	private static boolean isWarning(String name) {
		return name.equals(WARNING);
	}
	
//  //COPY PASTE THIS CASE AND FIX AT THE END
//  @Test
//	public void test19() {
//		final int TABNUMBER = 19;
//		
//		EvaluationStrategy strategy = (EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(NO_SELECTION);
//		EvaluationStrategyManager.getInstance(editor).setStrategy(strategy);
//		
//		FeatureDiagram featureD = (FeatureDiagram) urnspec.getUrndef().getSpecDiagrams().get(TABNUMBER - 1);
//
//		// Get the feature nodes.
//		Iterator elemItr = featureD.getNodes().iterator();
//		
//		while (elemItr.hasNext()) {
//			IntentionalElementRefImpl feature = (IntentionalElementRefImpl) elemItr.next();
//
//			if (hasName(feature, ROOT, TABNUMBER)) {
//				checkPropagationSelected(feature);
//				checkNotSelected(feature);
//			} else if (hasName(feature, PCHILD1, TABNUMBER)) {
//				checkAutoSelectedWithoutWarning(feature);
//				checkAutoSelectedWithWarning(feature);
//				checkNotSelected(feature);
//			} else if (hasName(feature, PCHILD2, TABNUMBER)) {
//				checkAutoSelectedWithoutWarning(feature);
//				checkAutoSelectedWithWarning(feature);
//				checkNotSelected(feature);
//				fail("PCHILD2_EXISTS");
//			} else if (hasName(feature, CHILD1, TABNUMBER)) {
//				checkAutoSelectedWithoutWarning(feature);
//				checkAutoSelectedWithWarning(feature);
//				checkNotSelected(feature);
//			} else if (hasName(feature, CHILD2, TABNUMBER)) {
//				checkAutoSelectedWithoutWarning(feature);
//				checkAutoSelectedWithWarning(feature);
//				checkNotSelected(feature);
//			} else {
//				fail(UNKNOWN_NODE);
//			}
//		}
//		
//		// Get the links.
//		elemItr = featureD.getConnections().iterator();
//		
//		while (elemItr.hasNext()) {
//			LinkRefImpl linkRef  = (LinkRefImpl) elemItr.next();
//			ElementLinkImpl link = (ElementLinkImpl) linkRef.getLink();
//
//			FeatureImpl src = (FeatureImpl) link.getSrc();
//			FeatureImpl dest = (FeatureImpl) link.getDest();
//			if (hasName(src, PCHILD1, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
//				checkDecompositionLink(link);
//				checkContributionLink(link, 0);
//				checkContributionLink(link, 100);
//			} else if (hasName(src, PCHILD2, TABNUMBER) && hasName(dest, ROOT, TABNUMBER)) {
//				checkDecompositionLink(link);
//				checkContributionLink(link, 0);
//				checkContributionLink(link, 100);
//				fail("PCHILD2LINK_EXISTS");
//			} else if (hasName(src, CHILD1, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
//				checkDecompositionLink(link);
//				checkContributionLink(link, 0);
//				checkContributionLink(link, 100);
//			} else if (hasName(src, CHILD2, TABNUMBER) && hasName(dest, PCHILD1, TABNUMBER)) {
//				checkDecompositionLink(link);
//				checkContributionLink(link, 0);
//				checkContributionLink(link, 100);
//			} else {
//				fail(UNKNOWN_LINK);
//			}
//		}
//	}
}