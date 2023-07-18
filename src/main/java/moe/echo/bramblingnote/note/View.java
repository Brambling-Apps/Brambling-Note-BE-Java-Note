package moe.echo.bramblingnote.note;

public class View {
    public interface Editable {}
    public interface ViewOnly extends Editable {}
    public interface Internal extends ViewOnly {}
}
