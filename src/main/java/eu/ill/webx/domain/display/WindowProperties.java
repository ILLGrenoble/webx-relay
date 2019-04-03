package eu.ill.webx.domain.display;

import eu.ill.webx.domain.utils.Rectangle;

public class WindowProperties {

    private long id;
    private Rectangle rectangle;
    private int bpp;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Rectangle getRectangle() {
        return rectangle;
    }

    public void setRectangle(Rectangle rectangle) {
        this.rectangle = rectangle;
    }

    public int getBpp() {
        return bpp;
    }

    public void setBpp(int bpp) {
        this.bpp = bpp;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WindowProperties{");
        sb.append("id=").append(id);
        sb.append(", rectangle=").append(rectangle);
        sb.append(", bpp=").append(bpp);
        sb.append('}');
        return sb.toString();
    }
}
