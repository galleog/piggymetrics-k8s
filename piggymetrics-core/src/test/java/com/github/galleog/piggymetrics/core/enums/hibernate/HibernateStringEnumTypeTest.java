package com.github.galleog.piggymetrics.core.enums.hibernate;

import static com.ninja_squad.dbsetup.Operations.deleteAllFrom;
import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static org.assertj.core.api.Assertions.assertThat;

import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Operation;
import org.assertj.db.type.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

/**
 * Tests for {@link HibernateStringEnumType}.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestPersistenceContext.class)
class HibernateStringEnumTypeTest {
    private static final int ID = 1;

    @PersistenceContext
    private EntityManager em;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        Operation operation = sequenceOf(
                deleteAllFrom("string_enum_entities"),
                insertInto("string_enum_entities")
                        .columns("id", "enum_value")
                        .values(ID, StringNumberEnum.ONE.getKey())
                        .build()
        );

        DbSetup dbSetup = new DbSetup(DataSourceDestination.with(dataSource), operation);
        dbSetup.launch();
    }

    /**
     * Test to read and write enumeration values.
     */
    @Test
    void shouldReadAndWriteCorrectEnumValue() {
        transactionTemplate.execute(status -> {
            StringEnumEntity entity = em.find(StringEnumEntity.class, 1);
            assertThat(entity.getEnumValue()).isSameAs(StringNumberEnum.ONE);

            entity.setEnumValue(StringNumberEnum.TWO);
            em.merge(entity);
            return null;
        });

        Table table = new Table(dataSource, "string_enum_entities");
        org.assertj.db.api.Assertions.assertThat(table).column("enum_value").containsValues(StringNumberEnum.TWO.getKey());
    }
}