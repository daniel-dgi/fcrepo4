/**
 * Copyright 2013 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fcrepo.integration.services;

import static org.jgroups.util.Util.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;

import javax.inject.Inject;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.fcrepo.RdfLexicon;
import org.fcrepo.integration.AbstractIT;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.ObjectService;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.update.UpdateAction;

@ContextConfiguration({"/spring-test/repo.xml"})
public class ObjectServiceIT extends AbstractIT {

    @Inject
    private Repository repository;

    @Inject
    ObjectService objectService;

    @Inject
    DatastreamService datastreamService;

    @Test
    public void testGetAllObjectsDatastreamSize() throws Exception {
        Session session = repository.login();

        final double originalSize = objectService.getRepositorySize();

        datastreamService.createDatastreamNode(session,
                "testObjectServiceNode", "application/octet-stream",
                new ByteArrayInputStream("asdf".getBytes()));
        session.save();
        session.logout();

        session = repository.login();

        final double afterSize = objectService.getRepositorySize();

        assertEquals(4.0, afterSize - originalSize);

        session.logout();
    }

    @Test
    public void testGetNamespaceRegistryGraph() throws Exception {
        Session session = repository.login();

        final Dataset registryGraph = objectService.getNamespaceRegistryGraph(session);

        final NamespaceRegistry namespaceRegistry = session.getWorkspace().getNamespaceRegistry();

        logger.info(namespaceRegistry.toString());
        logger.info(registryGraph.toString());
        for (String s : namespaceRegistry.getPrefixes()) {
            if (s.isEmpty() || s.equals("xmlns") || s.equals("jcr")) {
                continue;
            }
            final String uri = namespaceRegistry.getURI(s);
            assertTrue("expected to find JCR namespaces " + s + " in graph", registryGraph.asDatasetGraph().contains(Node.ANY, ResourceFactory.createResource(uri).asNode(), RdfLexicon.HAS_NAMESPACE_PREFIX.asNode(), ResourceFactory.createPlainLiteral(s).asNode()));
        }
        session.logout();
    }

    @Test
    public void testUpdateNamespaceRegistryGraph() throws Exception {
        Session session = repository.login();

        final Dataset registryGraph = objectService.getNamespaceRegistryGraph(session);
        final NamespaceRegistry namespaceRegistry = session.getWorkspace().getNamespaceRegistry();

        UpdateAction.parseExecute("INSERT { <info:abc> <" + RdfLexicon.HAS_NAMESPACE_PREFIX.toString() + "> \"abc\" } WHERE { }", registryGraph);

        assertEquals("abc", namespaceRegistry.getPrefix("info:abc"));
        session.logout();
    }
}
