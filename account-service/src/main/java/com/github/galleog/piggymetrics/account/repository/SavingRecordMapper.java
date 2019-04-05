package com.github.galleog.piggymetrics.account.repository;

import static com.github.galleog.piggymetrics.account.domain.Tables.SAVINGS;

import com.github.galleog.piggymetrics.account.domain.Saving;
import org.javamoney.moneta.Money;
import org.jooq.Record;
import org.jooq.RecordMapper;

/**
 * {@link RecordMapper} for {@link Saving}.
 */
public class SavingRecordMapper<R extends Record> implements RecordMapper<R, Saving> {
    @Override
    public Saving map(R record) {
        return Saving.builder()
                .moneyAmount(Money.of(
                        record.getValue(SAVINGS.MONEY_AMOUNT),
                        record.getValue(SAVINGS.CURRENCY_CODE)
                )).interest(record.getValue(SAVINGS.INTEREST))
                .deposit(record.getValue(SAVINGS.DEPOSIT))
                .capitalization(record.getValue(SAVINGS.CAPITALIZATION))
                .build();
    }
}
