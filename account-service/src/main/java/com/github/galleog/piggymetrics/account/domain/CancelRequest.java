package com.github.galleog.piggymetrics.account.domain;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

/**
 * TODO
 *
 * @author Oleg_Galkin
 */
@Entity
@Table(name = "cancel_request")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type_id", discriminatorType = DiscriminatorType.INTEGER)
@AttributeOverride(name = "typeId", column = @Column(name = "type_id", insertable = false, updatable = false))
public abstract class CancelRequest extends TestEntity {
}
