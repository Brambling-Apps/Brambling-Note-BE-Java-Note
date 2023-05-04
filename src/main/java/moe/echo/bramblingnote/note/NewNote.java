package moe.echo.bramblingnote.note;

import lombok.Data;

@Data
public class NewNote {
    private String content;

    private boolean importance;
}
