package moe.echo.bramblingnote.note;

import lombok.Getter;
import lombok.Setter;
import moe.echo.bramblingnote.user.UserForReturn;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class NoteForReturn {
    private UUID id;

    private String content;

    private Date date;

    private boolean importance;

    private Date expireAt;

    private UserForReturn user;
}
