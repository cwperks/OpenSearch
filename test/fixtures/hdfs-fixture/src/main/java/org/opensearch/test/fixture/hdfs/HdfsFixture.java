/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.test.fixture.hdfs;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class HdfsFixture extends GenericContainer<HdfsFixture> {
    
    private static final int HDFS_PORT = 9000;
    private static final int WEBHDFS_PORT = 9870;
    
    public HdfsFixture() {
        super(DockerImageName.parse("apache/hadoop:3"));
        withExposedPorts(HDFS_PORT, WEBHDFS_PORT);
        withCommand("hdfs", "namenode", "-format", "-force");
    }
    
    public String getHdfsUrl() {
        return String.format("hdfs://%s:%d", getHost(), getMappedPort(HDFS_PORT));
    }
    
    public String getWebHdfsUrl() {
        return String.format("http://%s:%d", getHost(), getMappedPort(WEBHDFS_PORT));
    }
}