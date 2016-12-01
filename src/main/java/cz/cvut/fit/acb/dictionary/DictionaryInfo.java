package cz.cvut.fit.acb.dictionary;

/**
 * @author jiri.bican
 */
public class DictionaryInfo {
	
	private int context;
	private int content;
	private int length;
	private int longestCommonPrefix;
	
	public DictionaryInfo(int context, int content, int length) {
		this(context, content, length, 0);
	}
	
	public DictionaryInfo(int context, int content, int length, int lcp) {
		this.context = context;
		this.content = content;
		this.length = length;
		this.longestCommonPrefix = lcp;
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
	
	public int getLcp() {
		return longestCommonPrefix;
	}
	
	@Override
	public String toString() {
		return "DictionaryInfo{" +
				"ctx=" + context +
				", cnt=" + content +
				", len=" + length +
				", lcp=" + longestCommonPrefix +
				'}';
	}
}
