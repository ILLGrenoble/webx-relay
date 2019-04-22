package eu.ill.webx.domain;

public class WindowProperties {

    private long id;
    private int x;
    private int y;
    private int width;
    private int height;
    private int bpp;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
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
        sb.append(", x=").append(x);
        sb.append(", y=").append(y);
        sb.append(", width=").append(width);
        sb.append(", height=").append(height);
        sb.append(", bpp=").append(bpp);
        sb.append('}');
        return sb.toString();
    }
}
