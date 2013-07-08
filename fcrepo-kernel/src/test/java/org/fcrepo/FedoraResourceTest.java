/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.utils.FedoraTypesUtils;
import org.fcrepo.utils.JcrPropertyStatementListener;
import org.fcrepo.utils.JcrRdfTools;
import org.fcrepo.utils.NamespaceTools;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.api.JcrConstants;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.util.Symbol;

/**
 * @todo Add Documentation.
 * @author Benjamin Armintor
 * @date May 13, 2013
 */
@RunWith(PowerMockRunner.class)
// PowerMock needs to ignore some packages to prevent class-cast errors
@PowerMockIgnore({"org.slf4j.*", "org.apache.xerces.*", "javax.xml.*",
        "org.xml.sax.*", "javax.management.*"})
@PrepareForTest({NamespaceTools.class, JcrRdfTools.class,
        FedoraTypesUtils.class})
public class FedoraResourceTest {

    FedoraResource testObj;

    Node mockNode;

    Node mockRoot;

    Session mockSession;

    /**
     * @todo Add Documentation.
     */
    @Before
    public void setUp() {
        mockSession = mock(Session.class);
        mockNode = mock(Node.class);
        testObj = new FedoraResource(mockNode);
        assertEquals(mockNode, testObj.getNode());
    }
    
    /**
     * @todo Add Documentation.
     */
    @Test
    public void testPathConstructor() throws RepositoryException {
        final Node mockNode = mock(Node.class);
        final Node mockRoot = mock(Node.class);
        when(mockSession.getRootNode()).thenReturn(mockRoot);
        when(mockRoot.getNode("foo/bar")).thenReturn(mockNode);
        when(mockNode.isNew()).thenReturn(true);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockNode.getMixinNodeTypes()).thenReturn(new NodeType[] { });
        new FedoraResource(mockSession, "/foo/bar", null);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testHasMixin() throws RepositoryException {
        boolean actual;
        final NodeType mockType = mock(NodeType.class);
        final NodeType[] mockTypes = new NodeType[] {mockType};
        when(mockNode.getMixinNodeTypes()).thenReturn(mockTypes);
        actual = FedoraResource.hasMixin(mockNode);
        assertEquals(false, actual);
        when(mockType.getName()).thenReturn(FedoraResource.FEDORA_RESOURCE);
        actual = FedoraResource.hasMixin(mockNode);
        assertEquals(true, actual);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetPath() throws RepositoryException {
        testObj.getPath();
        verify(mockNode).getPath();
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testHasContent() throws RepositoryException {
        testObj.hasContent();
        verify(mockNode).hasNode(JcrConstants.JCR_CONTENT);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetCreatedDate() throws RepositoryException {
        final Property mockProp = mock(Property.class);
        final Calendar someDate = Calendar.getInstance();
        when(mockProp.getDate()).thenReturn(someDate);
        when(mockNode.hasProperty(FedoraResource.JCR_CREATED)).thenReturn(true);
        when(mockNode.getProperty(FedoraResource.JCR_CREATED)).thenReturn(
                mockProp);
        assertEquals(someDate.getTimeInMillis(), testObj.getCreatedDate()
                .getTime());
    }
    
    @Test
    public void testCreatedDateMissing() throws RepositoryException {
    	when(mockNode.hasProperty(FedoraResource.JCR_CREATED)).thenReturn(false);
    	assertNull(testObj.getCreatedDate());
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetLastModifiedDateDefault() throws RepositoryException {
        // test missing JCR_LASTMODIFIED
        final Calendar someDate = Calendar.getInstance();
        someDate.add(Calendar.DATE, -1);
        try {
            when(mockNode.hasProperty(FedoraResource.JCR_LASTMODIFIED))
                    .thenReturn(false);
            final Property mockProp = mock(Property.class);
            when(mockProp.getDate()).thenReturn(someDate);
            when(mockNode.hasProperty(FedoraResource.JCR_CREATED)).thenReturn(
                    true);
            when(mockNode.getProperty(FedoraResource.JCR_CREATED)).thenReturn(
                    mockProp);
            when(mockNode.getSession()).thenReturn(mockSession);
        } catch (final RepositoryException e) {
            e.printStackTrace();
        }
        final Date actual = testObj.getLastModifiedDate();
        assertEquals(someDate.getTimeInMillis(), actual.getTime());
        // this is a read operation, it must not persist the session
        verify(mockSession, never()).save();
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetLastModifiedDate() throws RepositoryException {
        // test existing JCR_LASTMODIFIED
        final Calendar someDate = Calendar.getInstance();
        someDate.add(Calendar.DATE, -1);
        try {
            final Property mockProp = mock(Property.class);
            when(mockProp.getDate()).thenReturn(someDate);
            when(mockNode.hasProperty(FedoraResource.JCR_CREATED)).thenReturn(
                    true);
            when(mockNode.getProperty(FedoraResource.JCR_CREATED)).thenReturn(
                    mockProp);
            when(mockNode.getSession()).thenReturn(mockSession);
        } catch (final RepositoryException e) {
            e.printStackTrace();
        }
        final Property mockMod = mock(Property.class);
        final Calendar modDate = Calendar.getInstance();
        try {
            when(mockNode.hasProperty(FedoraResource.JCR_LASTMODIFIED))
                    .thenReturn(true);
            when(mockNode.getProperty(FedoraResource.JCR_LASTMODIFIED))
                    .thenReturn(mockMod);
            when(mockMod.getDate()).thenReturn(modDate);
        } catch (final RepositoryException e) {
            System.err.println("What are we doing in the second test?");
            e.printStackTrace();
        }
        final Date actual = testObj.getLastModifiedDate();
        assertEquals(modDate.getTimeInMillis(), actual.getTime());
    }

    @Test
    public void testUpdatePropertiesDataset() throws RepositoryException {
    	
         mockStatic(JcrRdfTools.class);
         GraphSubjects mockSubjects = mock(GraphSubjects.class);
         final Resource mockResource = mock(Resource.class);
         when(mockResource.getURI()).thenReturn("info:fedora/xyz");
         when(JcrRdfTools.getGraphSubject(mockSubjects,
         mockNode)).thenReturn(mockResource);
        
         final Model propertiesModel = ModelFactory.createDefaultModel();
         when(JcrRdfTools.getJcrPropertiesModel(mockSubjects,
         mockNode)).thenReturn(propertiesModel);
         final Model treeModel = ModelFactory.createDefaultModel();
         when(JcrRdfTools.getJcrTreeModel(mockSubjects, mockNode, 0, 0))
        	.thenReturn(treeModel);
         
         testObj.updatePropertiesDataset(mockSubjects, "");		 
    }	
	
    @Test
    public void testGetPropertiesDataset() throws RepositoryException {
    
        mockStatic(JcrRdfTools.class);
        GraphSubjects mockSubjects = mock(GraphSubjects.class);
        final Resource mockResource = mock(Resource.class);
        when(mockResource.getURI()).thenReturn("info:fedora/xyz");
        when(JcrRdfTools.getGraphSubject(mockSubjects, mockNode)).thenReturn(mockResource);
        
        final Model propertiesModel = ModelFactory.createDefaultModel();
        when(JcrRdfTools.getJcrPropertiesModel(mockSubjects, mockNode)).thenReturn(propertiesModel);
        final Model treeModel = ModelFactory.createDefaultModel();
        when(JcrRdfTools.getJcrTreeModel(mockSubjects, mockNode, 0, -1)).thenReturn(treeModel);
        final Dataset dataset = testObj.getPropertiesDataset(mockSubjects, 0, -1);
        
        assertTrue(dataset.containsNamedModel("tree"));
        assertEquals(treeModel, dataset.getNamedModel("tree"));
        
        assertEquals(propertiesModel, dataset.getDefaultModel());
        assertEquals("info:fedora/xyz", dataset.getContext().get(Symbol.create("uri")));
    }
    
    @Ignore
    @Test
    public void testGetPropertiesDatasetwithNullContext() throws RepositoryException {
    	
        mockStatic(JcrRdfTools.class);
        mockStatic(DatasetFactory.class);
        
        GraphSubjects mockSubjects = mock(GraphSubjects.class);
        final Resource mockResource = mock(Resource.class);
        when(mockResource.getURI()).thenReturn("info:fedora/xyz");
        when(JcrRdfTools.getGraphSubject(mockSubjects,
        mockNode)).thenReturn(mockResource);
        	
        final Model propertiesModel = ModelFactory.createDefaultModel();
        when(JcrRdfTools.getJcrPropertiesModel(mockSubjects,
        mockNode)).thenReturn(propertiesModel);
        final Model treeModel = ModelFactory.createDefaultModel();	 
        when(JcrRdfTools.getJcrTreeModel(mockSubjects, mockNode, 0,
        -1)).thenReturn(treeModel);
         
        Dataset dataset = mock(Dataset.class);
        when(DatasetFactory.create(propertiesModel)).thenReturn(dataset);
        dataset.addNamedModel("tree", treeModel);
        
        DatasetGraph mockDsGraph = mock(DatasetGraph.class);
        Iterator mockIterator = mock(Iterator.class);
        when(mockDsGraph.listGraphNodes()).thenReturn(mockIterator);
        when(mockIterator.hasNext()).thenReturn(true);
        when(mockIterator.next()).thenReturn(mockNode);
         
        when(dataset.asDatasetGraph()).thenReturn(mockDsGraph);   	
        	
        when(dataset.getContext()).thenReturn(null);
        
        final Dataset testDataset = testObj.getPropertiesDataset(mockSubjects, 
        		0, -1);
        	 
        assertTrue(dataset.containsNamedModel("tree"));
        assertEquals(treeModel, dataset.getNamedModel("tree"));		
        assertEquals(propertiesModel, dataset.getDefaultModel());
        assertEquals("info:fedora/xyz",
        dataset.getContext().get(Symbol.create("uri")));
    }

    @Test
    public void testGetPropertiesDatasetDefaultLimits() throws RepositoryException {

        mockStatic(JcrRdfTools.class);
        GraphSubjects mockSubjects = mock(GraphSubjects.class);
        final Resource mockResource = mock(Resource.class);
        when(mockResource.getURI()).thenReturn("info:fedora/xyz");
        when(JcrRdfTools.getGraphSubject(mockSubjects, mockNode)).thenReturn(mockResource);

        final Model propertiesModel = ModelFactory.createDefaultModel();
        when(JcrRdfTools.getJcrPropertiesModel(mockSubjects, mockNode)).thenReturn(propertiesModel);
        final Model treeModel = ModelFactory.createDefaultModel();
        when(JcrRdfTools.getJcrTreeModel(mockSubjects, mockNode, 0, -1)).thenReturn(treeModel);
        final Dataset dataset = testObj.getPropertiesDataset(mockSubjects);

        assertTrue(dataset.containsNamedModel("tree"));
        assertEquals(treeModel, dataset.getNamedModel("tree"));

        assertEquals(propertiesModel, dataset.getDefaultModel());
        assertEquals("info:fedora/xyz", dataset.getContext().get(Symbol.create("uri")));
    }


    @Test
    public void testGetPropertiesDatasetDefaults() throws RepositoryException {

        mockStatic(JcrRdfTools.class);
        final Resource mockResource = mock(Resource.class);
        when(mockResource.getURI()).thenReturn("info:fedora/xyz");
        when(JcrRdfTools.getGraphSubject(FedoraResource.DEFAULT_SUBJECT_FACTORY, mockNode)).thenReturn(mockResource);

        final Model propertiesModel = ModelFactory.createDefaultModel();
        when(JcrRdfTools.getJcrPropertiesModel(FedoraResource.DEFAULT_SUBJECT_FACTORY, mockNode)).thenReturn(propertiesModel);
        final Model treeModel = ModelFactory.createDefaultModel();
        when(JcrRdfTools.getJcrTreeModel(FedoraResource.DEFAULT_SUBJECT_FACTORY, mockNode, 0, -1)).thenReturn(treeModel);
        final Dataset dataset = testObj.getPropertiesDataset();

        assertTrue(dataset.containsNamedModel("tree"));
        assertEquals(treeModel, dataset.getNamedModel("tree"));

        assertEquals(propertiesModel, dataset.getDefaultModel());
        assertEquals("info:fedora/xyz", dataset.getContext().get(Symbol.create("uri")));
    }

    @Test
    public void testGetVersionDataset() throws RepositoryException {

        mockStatic(JcrRdfTools.class);
        GraphSubjects mockSubjects = mock(GraphSubjects.class);
        final Resource mockResource = mock(Resource.class);
        when(mockResource.getURI()).thenReturn("info:fedora/xyz");
        when(JcrRdfTools.getGraphSubject(mockSubjects, mockNode)).thenReturn(mockResource);

        final Model versionsModel = ModelFactory.createDefaultModel();
        when(JcrRdfTools.getJcrVersionsModel(mockSubjects, mockNode)).thenReturn(versionsModel);
        final Dataset dataset = testObj.getVersionDataset(mockSubjects);

        assertEquals(versionsModel, dataset.getDefaultModel());
        assertEquals("info:fedora/xyz", dataset.getContext().get(Symbol.create("uri")));
    }

    @Test
    public void testGetVersionDatasetDefaultSubject() throws RepositoryException {

        mockStatic(JcrRdfTools.class);
        final Resource mockResource = mock(Resource.class);
        when(mockResource.getURI()).thenReturn("info:fedora/xyz");
        when(JcrRdfTools.getGraphSubject(FedoraResource.DEFAULT_SUBJECT_FACTORY, mockNode)).thenReturn(mockResource);

        final Model versionsModel = ModelFactory.createDefaultModel();
        when(JcrRdfTools.getJcrVersionsModel(FedoraResource.DEFAULT_SUBJECT_FACTORY, mockNode)).thenReturn(versionsModel);
        final Dataset dataset = testObj.getVersionDataset();

        assertEquals(versionsModel, dataset.getDefaultModel());
        assertEquals("info:fedora/xyz", dataset.getContext().get(Symbol.create("uri")));
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetGraphProblems() throws RepositoryException {
        final Problems actual = testObj.getDatasetProblems();
        assertEquals(null, actual);
        final JcrPropertyStatementListener mockListener =
                mock(JcrPropertyStatementListener.class);
        setField("listener", FedoraResource.class, mockListener, testObj);
        testObj.getDatasetProblems();
        verify(mockListener).getProblems();
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testAddVersionLabel() throws RepositoryException {

        mockStatic(FedoraTypesUtils.class);
        final VersionHistory mockVersionHistory = mock(VersionHistory.class);
        final Version mockVersion = mock(Version.class);
        when(mockVersion.getName()).thenReturn("uuid");
        when(FedoraTypesUtils.getBaseVersion(mockNode)).thenReturn(mockVersion);
        when(FedoraTypesUtils.getVersionHistory(mockNode)).thenReturn(
                mockVersionHistory);

        testObj.addVersionLabel("v1.0.0");
        verify(mockVersionHistory).addVersionLabel("uuid", "v1.0.0", true);
    }

    @Test
    public void testIsNew() throws RepositoryException {
        when(mockNode.isNew()).thenReturn(true);
        assertTrue("resource state should be the same as the node state", testObj.isNew());
    }

    @Test
    public void testIsNotNew() throws RepositoryException {
        when(mockNode.isNew()).thenReturn(false);
        assertFalse("resource state should be the same as the node state", testObj.isNew());
    }

    private static void setField(final String name, final Class<?> clazz,
            final Object value, final Object object) {
        try {
            final Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            field.set(object, value);
        } catch (final Throwable t) {
            t.printStackTrace();
        }
    }
}
