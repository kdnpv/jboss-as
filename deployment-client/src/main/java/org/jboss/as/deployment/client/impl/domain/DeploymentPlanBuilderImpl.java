/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.deployment.client.impl.domain;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import org.jboss.as.deployment.client.api.DuplicateDeploymentNameException;
import org.jboss.as.deployment.client.api.domain.AddDeploymentPlanBuilder;
import org.jboss.as.deployment.client.api.domain.DeployDeploymentPlanBuilder;
import org.jboss.as.deployment.client.api.domain.DeploymentPlanBuilder;
import org.jboss.as.deployment.client.api.domain.ReplaceDeploymentPlanBuilder;
import org.jboss.as.deployment.client.api.domain.UndeployDeploymentPlanBuilder;
import org.jboss.as.deployment.client.impl.DeploymentActionImpl;
import org.jboss.as.deployment.client.impl.DeploymentContentDistributor;

/**
 * Builder capable of creating a {@link DeploymentPlanImpl}.
 *
 * @author Brian Stansberry
 */
class DeploymentPlanBuilderImpl extends AbstractDeploymentPlanBuilder implements DeploymentPlanBuilder  {

    private final DeploymentContentDistributor deploymentDistributor;

    DeploymentPlanBuilderImpl(DeploymentContentDistributor deploymentDistributor) {
        super();
        if (deploymentDistributor == null)
            throw new IllegalArgumentException("deploymentDistributor is null");
        this.deploymentDistributor = deploymentDistributor;
    }

    DeploymentPlanBuilderImpl(DeploymentPlanBuilderImpl existing) {
        super(existing);
        this.deploymentDistributor = existing.deploymentDistributor;
    }

    DeploymentPlanBuilderImpl(DeploymentPlanBuilderImpl existing, DeploymentActionImpl modification) {
        super(existing, modification);
        this.deploymentDistributor = existing.deploymentDistributor;
    }

    @Override
    public AddDeploymentPlanBuilder add(File file) throws IOException, DuplicateDeploymentNameException {
        String name = file.getName();
        return add(name, name, file.toURI().toURL());
    }

    @Override
    public AddDeploymentPlanBuilder add(URL url) throws IOException, DuplicateDeploymentNameException {
        String name = getName(url);
        return add(name, name, url);
    }

    @Override
    public AddDeploymentPlanBuilder add(String name, File file) throws IOException, DuplicateDeploymentNameException {
        return add(name, file.getName(), file.toURI().toURL());
    }

    @Override
    public AddDeploymentPlanBuilder add(String name, URL url) throws IOException, DuplicateDeploymentNameException {
        String commonName = getName(url);
        return add(name, commonName, url);
    }

    @Override
    public AddDeploymentPlanBuilder add(String name, InputStream stream) throws IOException, DuplicateDeploymentNameException {
        return add(name, name, stream);
    }

    @Override
    public AddDeploymentPlanBuilder add(String name, String commonName,
            InputStream stream) throws IOException, DuplicateDeploymentNameException {
        byte[] hash = deploymentDistributor.distributeDeploymentContent(name, commonName, stream);
        DeploymentActionImpl mod = DeploymentActionImpl.getAddAction(name, commonName, hash);
        return new AddDeploymentPlanBuilderImpl(this, mod);
    }

    @Override
    public DeployDeploymentPlanBuilder deploy(String key) {
        DeploymentActionImpl deployMod = DeploymentActionImpl.getDeployAction(key);
        return new DeployDeploymentPlanBuilderImpl(this, deployMod);
    }

    @Override
    public UndeployDeploymentPlanBuilder undeploy(String key) {
        DeploymentActionImpl mod = DeploymentActionImpl.getUndeployAction(key);
        return new UndeployDeploymentPlanBuilderImpl(this, mod);
    }

    @Override
    public DeploymentPlanBuilder redeploy(String deploymentName) {
        DeploymentActionImpl mod = DeploymentActionImpl.getRedeployAction(deploymentName);
        return new DeploymentPlanBuilderImpl(this, mod);
    }

    @Override
    public ReplaceDeploymentPlanBuilder replace(String replacement, String toReplace) {
        DeploymentActionImpl mod = DeploymentActionImpl.getReplaceAction(replacement, toReplace);
        return new ReplaceDeploymentPlanBuilderImpl(this, mod);
    }

    @Override
    public DeploymentPlanBuilder replace(File file) throws IOException {
        String name = file.getName();
        return replace(name, name, file.toURI().toURL());
    }

    @Override
    public DeploymentPlanBuilder replace(URL url) throws IOException {
        String name = getName(url);
        return replace(name, name, url);
    }

    @Override
    public DeploymentPlanBuilder replace(String name, File file) throws IOException {
        return replace(name, name, file.toURI().toURL());
    }

    @Override
    public DeploymentPlanBuilder replace(String name, URL url) throws IOException {
        return replace(name, name, url);
    }

    @Override
    public DeploymentPlanBuilder replace(String name, InputStream stream) throws IOException {
        return replace(name, name, stream);
    }

    @Override
    public DeploymentPlanBuilder replace(String name, String commonName, InputStream stream) throws IOException {
        byte[] hash = deploymentDistributor.distributeReplacementDeploymentContent(name, commonName, stream);
        DeploymentActionImpl mod = DeploymentActionImpl.getFullReplaceAction(name, commonName, hash);
        return new DeploymentPlanBuilderImpl(this, mod);
    }

    @Override
    public DeploymentPlanBuilder remove(String key) {
        DeploymentActionImpl mod = DeploymentActionImpl.getRemoveAction(key);
        return new DeploymentPlanBuilderImpl(this, mod);
    }

    private AddDeploymentPlanBuilder add(String name, String commonName, URL url) throws IOException, DuplicateDeploymentNameException {
        URLConnection conn = url.openConnection();
        conn.connect();
        InputStream stream = conn.getInputStream();
        try {
            return add(name, commonName, stream);
        }
        finally {
            try { stream.close(); } catch (Exception ignored) {}
        }
    }

    private DeploymentPlanBuilder replace(String name, String commonName, URL url) throws IOException {
        URLConnection conn = url.openConnection();
        conn.connect();
        InputStream stream = conn.getInputStream();
        try {
            return replace(name, commonName, stream);
        }
        finally {
            try { stream.close(); } catch (Exception ignored) {}
        }
    }

    private static String getName(URL url) {
        if ("file".equals(url.getProtocol())) {
            try {
                File f = new File(url.toURI());
                return f.getName();
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(url + " is not a valid URI", e);
            }
        }

        String path = url.getPath();
        int idx = path.lastIndexOf('/');
        while (idx == path.length() - 1) {
            path = path.substring(0, idx);
            idx = path.lastIndexOf('/');
        }
        if (idx == -1) {
            throw new IllegalArgumentException("Cannot derive a deployment name from " +
                    url + " -- use an overloaded method variant that takes a 'name' parameter");
        }

        return path.substring(idx + 1);
    }
}
