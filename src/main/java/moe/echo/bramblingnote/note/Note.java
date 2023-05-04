package moe.echo.bramblingnote.note;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.Date;
import java.util.UUID;

@Entity
@Getter
@Setter
public class Note {
    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private Date date;

    @Column(nullable = false)
    private boolean importance;

    private Date expireAt;

    @Column(nullable = false)
    private UUID userId;
}
