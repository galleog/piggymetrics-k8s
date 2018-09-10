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
 * Entity for tests of {@link HibernateStringEnumType}.
 */
@Entity
@Table(name = "string_enum_entities")
public class StringEnumEntity implements Serializable {
    @Id
    private int id;

    @Getter
    @Setter
    @Column(name = "enum_value")
    @Type(
            type = HibernateStringEnumType.CLASS_NAME,
            parameters = @Parameter(
                    name = AbstractHibernateEnumType.PARAMETER_NAME,
                    value = "com.github.galleog.piggymetrics.core.enums.hibernate.StringNumberEnum"
            )
    )
    private StringNumberEnum enumValue;
}
