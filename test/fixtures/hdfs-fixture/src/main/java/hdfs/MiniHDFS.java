package hdfs;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.wait.strategy.Wait;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.Arrays;

/**
 * HDFS test fixture using TestContainers.
 */
public class MiniHDFS {

    private static final String PORT_FILE_NAME = "ports";
    private static final String PID_FILE_NAME = "pid";
    private static final int NAMENODE_RPC_PORT = 8020;
    private static final int DATANODE_DATA_PORT = 9866;
    private static final int FIXED_HOST_PORT = 9999;
    private static final int FIXED_DATANODE_PORT = 9866;

    public static void main(String[] args) throws Exception {
        if (args.length != 1 && args.length != 3) {
            throw new IllegalArgumentException(
                "Expected: MiniHDFS <baseDirectory> [<kerberosPrincipal> <kerberosKeytab>], got: " + Arrays.toString(args)
            );
        }

        Path baseDir = Paths.get(args[0]);
        DockerImageName image = DockerImageName.parse("apache/hadoop:3.4");

        try (Network network = Network.newNetwork();
             GenericContainer<?> namenode = new GenericContainer<>(image)
                 .withNetwork(network)
                 .withNetworkAliases("namenode")
                 .withCommand("hdfs", "namenode")
                 .withExposedPorts(NAMENODE_RPC_PORT)
                 .withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
                     cmd.getHostConfig().withPortBindings(
                         new com.github.dockerjava.api.model.PortBinding(
                             com.github.dockerjava.api.model.Ports.Binding.bindPort(FIXED_HOST_PORT),
                             new com.github.dockerjava.api.model.ExposedPort(NAMENODE_RPC_PORT)
                         )
                     )
                 ))
                 .withEnv("ENSURE_NAMENODE_DIR", "/tmp/hadoop-root/dfs/name")
                 .withEnv("CORE-SITE.XML_fs.defaultFS", "hdfs://namenode:" + NAMENODE_RPC_PORT)
                 .withEnv("HDFS-SITE.XML_dfs.namenode.rpc-address", "0.0.0.0:" + NAMENODE_RPC_PORT)
                 .withEnv("HDFS-SITE.XML_dfs.replication", "1")
                 .withEnv("HDFS-SITE.XML_dfs.permissions.enabled", "false")
                 .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));

             GenericContainer<?> datanode = new GenericContainer<>(image)
                 .withNetwork(network)
                 .withNetworkAliases("datanode")
                 .withCommand("hdfs", "datanode")
                 .withExposedPorts(DATANODE_DATA_PORT)
                 .withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
                     cmd.getHostConfig().withPortBindings(
                         new com.github.dockerjava.api.model.PortBinding(
                             com.github.dockerjava.api.model.Ports.Binding.bindPort(FIXED_DATANODE_PORT),
                             new com.github.dockerjava.api.model.ExposedPort(DATANODE_DATA_PORT)
                         )
                     )
                 ))
                 .withEnv("CORE-SITE.XML_fs.defaultFS", "hdfs://namenode:" + NAMENODE_RPC_PORT)
                 .withEnv("HDFS-SITE.XML_dfs.namenode.rpc-address", "namenode:" + NAMENODE_RPC_PORT)
                 .withEnv("HDFS-SITE.XML_dfs.datanode.address", "0.0.0.0:" + DATANODE_DATA_PORT)
                 .withEnv("HDFS-SITE.XML_dfs.datanode.hostname", "localhost")
                 .withEnv("HDFS-SITE.XML_dfs.replication", "1")
                 .withEnv("HDFS-SITE.XML_dfs.permissions.enabled", "false")
                 .withEnv("HDFS-SITE.XML_dfs.client.use.datanode.hostname", "true")
                 .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))
        ) {
            namenode.start();
            datanode.start();

            // Wait for datanode to register with namenode
            Thread.sleep(5000);

            writeAtomic(baseDir.resolve(PID_FILE_NAME),
                ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
            writeAtomic(baseDir.resolve(PORT_FILE_NAME), Integer.toString(FIXED_HOST_PORT));

            System.out.println("HDFS started. RPC at localhost:" + FIXED_HOST_PORT);
            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("Failed to start HDFS containers: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void writeAtomic(Path path, String content) throws Exception {
        Path tmp = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
        Files.write(tmp, content.getBytes(StandardCharsets.UTF_8));
        Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}
