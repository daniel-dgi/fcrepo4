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
package org.fcrepo.integration.utils;

import com.hp.hpl.jena.rdf.model.Model;
import org.apache.commons.io.IOUtils;
import org.fcrepo.integration.AbstractIT;
import org.fcrepo.rdf.impl.DefaultGraphSubjects;
import org.fcrepo.utils.JcrRdfTools;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertTrue;

@ContextConfiguration({"/spring-test/repo.xml"})
public class JcrRdfToolsIT extends AbstractIT {

    @Inject
    Repository repo;

    final DefaultGraphSubjects defaultGraphSubjects = new DefaultGraphSubjects();

    @Test
    public void shouldMapJcrNodeTypesToRDFS() throws RepositoryException {

        final Session session = repo.login();


        final Model jcrPropertiesModel = JcrRdfTools.withContext(defaultGraphSubjects, session).getJcrPropertiesModel(session.getWorkspace().getNodeTypeManager());

        final ByteArrayOutputStream o = new ByteArrayOutputStream();
        jcrPropertiesModel.write(o, "N3");

        final String s = o.toString();

        assertTrue(s.contains("#fedora:object"));
        logger.info(s);
        session.logout();
    }

}
