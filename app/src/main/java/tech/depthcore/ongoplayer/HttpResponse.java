package tech.depthcore.ongoplayer;

public class HttpResponse {
    public int code;
    public String body;
    public String contentType;

    public HttpResponse( ) {
        this.code = -1;
        this.body = null;
        this.contentType = null;
    }

    public String toString() {
        return "code=" + code + ", contentType=" + contentType + ", body='" + body + "'";
    }

}
