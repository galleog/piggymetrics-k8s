package com.github.galleog.piggymetrics.account.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * TODO
 *
 * @author Oleg_Galkin
 */
@Entity
@DiscriminatorValue("1")
public class ConcreteCancelReq extends CancelRequest {
}
