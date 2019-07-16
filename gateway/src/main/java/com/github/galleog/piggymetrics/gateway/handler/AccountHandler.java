package com.github.galleog.piggymetrics.gateway.handler;

import static com.github.galleog.protobuf.java.type.converter.Converters.bigDecimalConverter;
import static com.github.galleog.protobuf.java.type.converter.Converters.moneyConverter;
import static com.github.galleog.protobuf.java.type.converter.Converters.timestampConverter;

import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto;
import com.github.galleog.piggymetrics.account.grpc.ReactorAccountServiceGrpc;
import com.github.galleog.piggymetrics.gateway.model.account.Account;
import com.github.galleog.piggymetrics.gateway.model.account.Item;
import com.github.galleog.piggymetrics.gateway.model.account.Saving;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Converter;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.stream.Collectors;

/**
 * Routing handler for accounts.
 */
@Component
public class AccountHandler {
    @VisibleForTesting
    static final String ACCOUNT_SERVICE = "account-service";
    @VisibleForTesting
    static final String DEMO_ACCOUNT = "demo";

    private static final Converter<Item, AccountServiceProto.Item> ITEM_CONVERTER = new ItemConverter();
    private static final Converter<Saving, AccountServiceProto.Saving> SAVING_CONVERTER = new SavingConverter();

    @GrpcClient(ACCOUNT_SERVICE)
    private ReactorAccountServiceGrpc.ReactorAccountServiceStub accountServiceStub;

    /**
     * Returns the demo account.
     *
     * @param request the server request
     * @return the found account
     */
    public Mono<ServerResponse> getDemoAccount(ServerRequest request) {
        return getAccountByName(Mono.just(DEMO_ACCOUNT));
    }

    /**
     * Gets the account of the current principal.
     *
     * @param request the server request
     * @return the account of the current principal
     */
    public Mono<ServerResponse> getCurrentAccount(ServerRequest request) {
        return getAccountByName(request.principal().map(Principal::getName));
    }

    /**
     * Updates the account of the current principal.
     *
     * @param request the server request
     * @return the updated account, or {@link HttpStatus#NOT_FOUND} if there is no account for the current principal
     */
    public Mono<ServerResponse> updateCurrentAccount(ServerRequest request) {
        Mono<Account> mono = Mono.zip(request.principal(), request.bodyToMono(Account.class))
                .map(tuple -> toAccountProto(tuple.getT1().getName(), tuple.getT2()))
                .compose(accountServiceStub::updateAccount)
                .map(this::toAccount);
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(mono, Account.class);
    }

    private Mono<ServerResponse> getAccountByName(Mono<String> nameMono) {
        Mono<AccountServiceProto.GetAccountRequest> req = nameMono.map(name ->
                AccountServiceProto.GetAccountRequest.newBuilder()
                        .setName(name)
                        .build()
        );
        Mono<Account> accountMono = req.compose(accountServiceStub::getAccount)
                .map(this::toAccount);
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(accountMono, Account.class);
    }

    private AccountServiceProto.Account toAccountProto(String name, Account account) {
        AccountServiceProto.Account.Builder builder = AccountServiceProto.Account.newBuilder()
                .setName(name)
                .addAllItems(account.getItems().stream()
                        .map(ITEM_CONVERTER::convert)
                        .collect(Collectors.toList()))
                .setSaving(SAVING_CONVERTER.convert(account.getSaving()));
        if (account.getNote() != null) {
            builder.setNote(account.getNote());
        }
        return builder.build();
    }

    private Account toAccount(AccountServiceProto.Account account) {
        Account.AccountBuilder builder = Account.builder()
                .name(account.getName())
                .items(account.getItemsList().stream()
                        .map(item -> ITEM_CONVERTER.reverse().convert(item))
                        .collect(Collectors.toSet()))
                .saving(SAVING_CONVERTER.reverse().convert(account.getSaving()));
        if (account.hasUpdateTime()) {
            builder.updateTime(timestampConverter().reverse().convert(account.getUpdateTime()));
        }
        if (StringUtils.isNotEmpty(account.getNote())) {
            builder.note(account.getNote());
        }
        return builder.build();
    }

    private static final class ItemConverter extends Converter<Item, AccountServiceProto.Item> {
        @Override
        protected AccountServiceProto.Item doForward(Item item) {
            return AccountServiceProto.Item.newBuilder()
                    .setType(item.getType())
                    .setTitle(item.getTitle())
                    .setMoney(moneyConverter().convert(item.getMoneyAmount()))
                    .setPeriod(item.getPeriod())
                    .setIcon(item.getIcon())
                    .build();
        }

        @Override
        protected Item doBackward(AccountServiceProto.Item item) {
            return Item.builder()
                    .type(item.getType())
                    .title(item.getTitle())
                    .moneyAmount(moneyConverter().reverse().convert(item.getMoney()))
                    .period(item.getPeriod())
                    .icon(item.getIcon())
                    .build();
        }
    }

    private static final class SavingConverter extends Converter<Saving, AccountServiceProto.Saving> {
        @Override
        protected AccountServiceProto.Saving doForward(Saving saving) {
            return AccountServiceProto.Saving.newBuilder()
                    .setMoney(moneyConverter().convert(saving.getMoneyAmount()))
                    .setInterest(bigDecimalConverter().convert(saving.getInterest()))
                    .setDeposit(saving.isDeposit())
                    .setCapitalization(saving.isCapitalization())
                    .build();
        }

        @Override
        protected Saving doBackward(AccountServiceProto.Saving saving) {
            return Saving.builder()
                    .moneyAmount(moneyConverter().reverse().convert(saving.getMoney()))
                    .interest(bigDecimalConverter().reverse().convert(saving.getInterest()))
                    .deposit(saving.getDeposit())
                    .capitalization(saving.getCapitalization())
                    .build();
        }
    }
}
