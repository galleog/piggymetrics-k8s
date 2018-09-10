package com.github.galleog.piggymetrics.account.domain;

import com.github.galleog.piggymetrics.core.domain.AbstractSequencePersistable;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.MappedSuperclass;

/**
 * TODO
 *
 * @author Oleg_Galkin
 */
@Getter
@Setter
@MappedSuperclass
public abstract class TestEntity extends AbstractSequencePersistable<Integer> {
    private Long typeId;
}
