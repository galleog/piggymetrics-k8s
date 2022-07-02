package com.github.galleog.liquibase.tc;

import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.resource.ResourceAccessor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Helper class to define a <a href="https://www.testcontainers.org/">Testcontainers</a> init function.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LiquibaseUpdater {
    private static final String LIQUIBASE_CHANGELOGFILE = "changeLogFile";

    /**
     * Updates the database using the <a href="https://www.liquibase.org/">Liquibase</a> change log defined as a system property.
     *
     * @param connection a database connection
     * @throws LiquibaseException if Liquibase fails
     */
    public static void update(Connection connection) throws LiquibaseException, SQLException {
        Liquibase liquibase = createLiquibase(connection);
        liquibase.update((String) null);

        // Liquibase sets auto commit to false. We need to reset it back because jOOQ requires it
        connection.setAutoCommit(true);
    }

    private static Liquibase createLiquibase(Connection connection) throws LiquibaseException {
        String changeLogFile = System.getProperty(LIQUIBASE_CHANGELOGFILE);
        logger.info("Using changelog file: {}", changeLogFile);
        ResourceAccessor accessor = new FileSystemResourceAccessor(new File("/"));
        return new Liquibase(changeLogFile, accessor, new JdbcConnection(connection));
    }
}
