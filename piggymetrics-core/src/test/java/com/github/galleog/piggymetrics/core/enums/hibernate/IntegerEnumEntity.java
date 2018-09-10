package com.github.galleog.piggymetrics.core.enums.hibernate;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * Entity for tests of {@link HibernateIntegerEnumType}.
 */
@Entity
@Table(name = "integer_enum_entities")
public class IntegerEnumEntity implements Serializable {
    @Id
    private int id;

    @Getter
    @Setter
    @Column(name = "enum_value")
    @Type(
            type = HibernateIntegerEnumType.CLASS_NAME,
            parameters = @Parameter(
                    name = AbstractHibernateEnumType.PARAMETER_NAME,
                    value = "com.github.galleog.piggymetrics.core.enums.hibernate.IntegerNumberEnum"
            )
    )
    private IntegerNumberEnum enumValue;
}
