package moe.echo.bramblingnote.note;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface Repository extends CrudRepository<Note, UUID> {
    List<Note> findAllByUserId(UUID userId);
}
