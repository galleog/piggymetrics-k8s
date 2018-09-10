package com.github.galleog.piggymetrics.account.repository;

import static com.ninja_squad.dbsetup.Operations.deleteAllFrom;
import static com.ninja_squad.dbsetup.Operations.sequenceOf;

import com.github.galleog.piggymetrics.account.domain.CancelRequest;
import com.github.galleog.piggymetrics.account.domain.ConcreteCancelReq;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Operation;
import org.assertj.db.api.Assertions;
import org.assertj.db.type.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

/**
 * TODO
 *
 * @author Oleg_Galkin
 */
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@DataJpaTest
class CancelRequestRepositoryTest {
    @Autowired
    private DataSource dataSource;
    @Autowired
    private CancelRequestRepository repository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        Operation operation = sequenceOf(
                deleteAllFrom(
                        "cancel_request"
                )
        );
        DbSetup dbSetup = new DbSetup(DataSourceDestination.with(dataSource), operation);
        dbSetup.launch();
    }

    @Test
    void shouldSave() {
        CancelRequest cancelRequest = new ConcreteCancelReq();
        cancelRequest.setTypeId(1L);

        transactionTemplate.execute(status -> repository.save(cancelRequest));

        Table reqs = new Table(dataSource, "cancel_request");
        Assertions.assertThat(reqs)
                .column("type_id").containsValues(1);
    }
}