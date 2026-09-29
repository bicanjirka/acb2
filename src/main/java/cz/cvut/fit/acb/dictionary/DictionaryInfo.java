package cz.cvut.fit.acb.dictionary;

public class DictionaryInfo {

    private int context;
    private int content;
    private int length;

    public DictionaryInfo(int context, int content, int length) {
        this.context = context;
        this.content = content;
        this.length = length;
    }

    public int getContext() {
        return context;
    }

    public int getContent() {
        return content;
    }

    public int getLength() {
        return length;
    }

    @Override
    public String toString() {
        return "DictionaryInfo{" +
                "ctx=" + context +
                ", cnt=" + content +
                ", len=" + length +
                '}';
    }
}
