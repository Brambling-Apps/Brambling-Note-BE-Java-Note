package moe.echo.bramblingnote.note;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface Repository extends JpaRepository<Note, UUID> {
    List<Note> findAllByUserId(UUID userId);
}
