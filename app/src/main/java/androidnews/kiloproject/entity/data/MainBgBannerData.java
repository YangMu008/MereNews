package androidnews.kiloproject.entity.data;

public class MainBgBannerData {
    Object path;
    String title;

    public MainBgBannerData(Object path, String title) {
        this.path = path;
        this.title = title;
    }

    public Object getPath() {
        return path;
    }

    public void setPath(Object path) {
        this.path = path;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
