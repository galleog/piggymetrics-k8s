package com.github.galleog.sk.account.repository;

import com.github.galleog.sk.account.domain.Account;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends CrudRepository<Account, String> {

	Account findByName(String name);

}
