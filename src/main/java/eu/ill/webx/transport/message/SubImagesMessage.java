package eu.ill.webx.transport.message;


import eu.ill.webx.domain.image.SubImage;

import java.util.ArrayList;
import java.util.List;

public class SubImagesMessage extends Message {

    private long windowId;
    private List<SubImage> subImages = new ArrayList<>();

    public SubImagesMessage() {
    }

    public long getWindowId() {
        return windowId;
    }

    public void setWindowId(long windowId) {
        this.windowId = windowId;
    }

    public List<SubImage> getSubImages() {
        return subImages;
    }

    public void setSubImages(List<SubImage> subImages) {
        this.subImages = subImages;
    }
}
