package io.c4us.masterbackend.domain;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserStructureId implements Serializable {

    private String userId;
    private String structureId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserStructureId constr = (UserStructureId) o;
        return Objects.equals(userId, constr.userId) && Objects.equals(structureId, constr.structureId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, structureId);
    }
}