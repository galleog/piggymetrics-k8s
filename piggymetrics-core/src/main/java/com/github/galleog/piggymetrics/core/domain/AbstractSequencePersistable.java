package com.github.galleog.piggymetrics.core.domain;

import lombok.AccessLevel;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.springframework.data.domain.Persistable;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Abstract base class for entities that need to have their own sequence generator for their identifier.
 */
@MappedSuperclass
public abstract class AbstractSequencePersistable<PK extends Serializable> implements Persistable<PK> {
    @Id
    @Nullable
    @Setter(AccessLevel.PROTECTED)
    @Access(AccessType.PROPERTY)
    @GeneratedValue(generator = "SequencePerEntityGenerator")
    @GenericGenerator(
            name = "SequencePerEntityGenerator",
            strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
            parameters = {@Parameter(name = SequenceStyleGenerator.CONFIG_PREFER_SEQUENCE_PER_ENTITY, value = "true")}
    )
    private PK id;

    @Override
    @Nullable
    public PK getId() {
        return this.id;
    }

    @Override
    @Transient
    public boolean isNew() {
        return null == getId();
    }

    @Override
    public String toString() {
        return String.format("Entity of type %s with id: %s", getClass().getName(), getId());
    }

    @Override
    public boolean equals(Object obj) {
        if (null == obj) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!getClass().equals(ClassUtils.getUserClass(obj))) {
            return false;
        }

        AbstractSequencePersistable<?> that = (AbstractSequencePersistable<?>) obj;
        return null != this.getId() && this.getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        int hashCode = 17;
        hashCode += null == getId() ? 0 : getId().hashCode() * 31;
        return hashCode;
    }
}
