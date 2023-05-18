package moe.echo.bramblingnote.note;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class ServiceImpl {
    private final Repository repository;

    ServiceImpl(Repository repository) {
        this.repository = repository;
    }

    // Do every hour
    @Scheduled(cron = "0 * * * *")
    public void removeExpired() {
        repository.findAll().forEach(note -> {
            Date now = new Date();
            Date expireAt = note.getExpireAt();

            if (expireAt != null && expireAt.before(now)) {
                repository.deleteById(note.getId());
            }
        });
    }

    @CachePut(value = "notes", key = "#note.id")
    public Note save(Note note) {
        return repository.save(note);
    }

    @Cacheable(value = "notes", key = "#id")
    public Note findById(UUID id, UUID userId) {
        return repository.findById(id).map(note -> {
            if (note.getUserId().equals(userId) && note.getExpireAt() == null) {
                return note;
            }

            return null;
        }).orElse(null);
    }

    public List<Note> findAllByUserId(UUID userId) {
        return repository.findAllByUserId(userId)
                .stream().filter(note -> note.getExpireAt() == null)
                .toList();
    }

    @CacheEvict(value = "notes", key = "#id")
    public void undoableDeleteById(UUID id, UUID userId) {
        repository.findById(id).ifPresent(note -> {
            if (note.getUserId().equals(userId) && note.getExpireAt() == null) {
                long now = (new Date()).getTime();
                note.setExpireAt(new Date(now + 1000 * 15));
                repository.save(note);
            }
        });
    }

    public Note undoDeleteById(UUID id, UUID userId) {
        return repository.findById(id).map(note -> {
            Date expireAt = note.getExpireAt();
            if (note.getUserId().equals(userId) && (expireAt == null || expireAt.after(new Date()))) {
                note.setExpireAt(null);
                return repository.save(note);
            }

            return null;
        }).orElse(null);
    }
}
