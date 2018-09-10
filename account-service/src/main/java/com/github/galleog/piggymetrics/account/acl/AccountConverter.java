package com.github.galleog.piggymetrics.account.acl;

import com.github.galleog.piggymetrics.account.domain.Account;
import com.google.common.base.Converter;
import org.springframework.stereotype.Component;

/**
 * {@link Converter} that converts {@link Account} to {@link AccountDto}.
 */
@Component
public class AccountConverter extends Converter<Account, AccountDto> {
    @Override
    protected AccountDto doForward(Account account) {
        return AccountDto.builder()
                .name(account.getName())
                .incomes(account.getIncomes())
                .expenses(account.getExpenses())
                .saving(account.getSaving())
                .note(account.getNote())
                .lastModifiedDate(account.getLastModifiedDate())
                .build();
    }

    @Override
    protected Account doBackward(AccountDto account) {
        throw new UnsupportedOperationException();
    }
}
