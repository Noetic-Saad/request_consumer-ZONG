package com.noeticworld.sgw.requestConsumer.entities;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "event_types", schema = "public", catalog = "sgw")
public class EventTypesEntity {
    private int id;
    private String code;
    private String description;

    @Id
    @Column(name = "id")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Basic
    @Column(name = "code")
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Basic
    @Column(name = "description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventTypesEntity that = (EventTypesEntity) o;
        return id == that.id &&
                Objects.equals(code, that.code) &&
                Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code, description);
    }
}
