package moe.echo.bramblingnote.note;

import jakarta.servlet.http.HttpSession;
import moe.echo.bramblingnote.user.UserForSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@RestController
public class Controller {
    @Autowired
    private Repository repository;

    private NoteForReturn toNoteForReturn(Note note, UserForSession user) {
        NoteForReturn n = new NoteForReturn();
        n.setId(note.getId());
        n.setContent(note.getContent());
        n.setDate(note.getDate());
        n.setImportance(note.isImportance());
        n.setExpireAt(note.getExpireAt());
        n.setUser(user);
        return n;
    }

    @GetMapping("/health")
    public ResponseEntity<Object> health() {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/")
    public NoteForReturn create(@RequestBody NewNote note, HttpSession session) {
        Object rawUser = session.getAttribute("user");
        if (rawUser instanceof UserForSession user) {
            Note n = new Note();
            n.setContent(note.getContent());
            n.setImportance(note.isImportance());
            n.setDate(new Date());
            n.setExpireAt(null);
            n.setUserId(user.getId());
            return toNoteForReturn(repository.save(n), user);
        }
        throw new ResponseStatusException(HttpStatusCode.valueOf(401), "You are not login yet");
    }

    // TODO: expireAt
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable UUID id, HttpSession session) {
        Object rawUser = session.getAttribute("user");
        if (rawUser instanceof UserForSession user) {
            return repository.findById(id).map(note -> {
                if (user.getId().equals(note.getUserId())) {
                    repository.deleteById(id);
                    return ResponseEntity.ok().build();
                }

                throw new ResponseStatusException(
                        HttpStatusCode.valueOf(401),
                        "You have no permission to access this note"
                );
            }).orElseThrow(() -> new ResponseStatusException(
                    HttpStatusCode.valueOf(404),
                    "Note `" + id + "` not found")
            );
        }
        throw new ResponseStatusException(HttpStatusCode.valueOf(401), "You are not login yet");
    }

    @GetMapping("/")
    public List<NoteForReturn> get(HttpSession session) {
        Object rawUser = session.getAttribute("user");
        if (rawUser instanceof UserForSession user) {
            return repository.findAllByUserId(user.getId()).stream()
                    .map(note -> toNoteForReturn(note, user))
                    .toList();
        }
        throw new ResponseStatusException(HttpStatusCode.valueOf(401), "You are not login yet");
    }

    @GetMapping("/{id}")
    public NoteForReturn getById(@PathVariable UUID id, HttpSession session) {
        Object rawUser = session.getAttribute("user");
        if (rawUser instanceof UserForSession user) {
            return repository.findById(id).map(n -> {
                if (n.getUserId().equals(user.getId())) {
                    return toNoteForReturn(n, user);
                }
                throw new ResponseStatusException(
                        HttpStatusCode.valueOf(401),
                        "You have no permission to note `" + id + "`"
                );
            }).orElseThrow(() -> new ResponseStatusException(
                    HttpStatusCode.valueOf(404),
                    "note `" + id + "` not found"
            ));
        }

        throw new ResponseStatusException(HttpStatusCode.valueOf(401), "You are not login yet");
    }

    @PutMapping("/{id}")
    public NoteForReturn update(@PathVariable UUID id, @RequestBody NewNote note, HttpSession session) {
        Object rawUser = session.getAttribute("user");
        if (rawUser instanceof UserForSession user) {
            return repository.findById(id).map(n -> {
                if (n.getUserId().equals(user.getId())) {
                    String content = note.getContent();
                    if (content != null) {
                        n.setContent(note.getContent());
                    }

                    n.setImportance(note.isImportance());
                    Note newNote = repository.save(n);
                    return toNoteForReturn(newNote, user);
                }
                throw new ResponseStatusException(
                        HttpStatusCode.valueOf(401),
                        "You have no permission to note `" + id + "`"
                );
            }).orElseThrow(() -> new ResponseStatusException(
                    HttpStatusCode.valueOf(404),
                    "note `" + id + "` not found"
            ));
        }

        throw new ResponseStatusException(HttpStatusCode.valueOf(401), "You are not login yet");
    }
}
